package es.mcpworkshop.server.weather.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import es.mcpworkshop.server.weather.model.Prediccion;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
public class AemetWeatherService implements WeatherService {

  @Value("${opendata.api-key}")
  String apiKey;

  WebClient webClient;
  ObjectMapper objectMapper;

  private static final Logger log = LoggerFactory.getLogger(AemetWeatherService.class);

  public AemetWeatherService(WebClient.Builder webClientBuilder, ObjectMapper objectMapper) {
    this.webClient = webClientBuilder.baseUrl("https://opendata.aemet.es/opendata").build();
    this.objectMapper = objectMapper;
  }

  @Override
  @Tool(
      description =
          """
  Retrieves the weather forecast for a location's AEMET code. Please get the AEMET code for a location,
  by checking the  aemet-codes://{location} resource.
  """)
  public List<Prediccion> getWeatherForecast(
      @ToolParam(description = "The location AEMET code") String aemetLocationCode) {
    return callAemetProxy(aemetLocationCode)
        .flatMapMany(this::callRealMethod)
        .doOnError(error -> log.error("Error en el proceso: ", error))
        .log()
        .collectList()
        .block();
  }

  private Mono<AemetResponse> callAemetProxy(String aemetLocationCode) {
    log.info("Calling Aemet proxy for {}", aemetLocationCode);
    return webClient
        .get()
        .uri(
            "/api/prediccion/especifica/municipio/diaria/{municipio_aemet_code}?api_key={api_key}",
            aemetLocationCode,
            apiKey)
        .retrieve()
        .onStatus(
            HttpStatusCode::isError,
            clientResponse -> {
              log.error("Error en el proceso");
              return Mono.error(new RuntimeException("Error en primera llamada"));
            })
        .bodyToMono(AemetResponse.class);
  }

  private Flux<Prediccion> callRealMethod(AemetResponse aemetResponse) {
    log.info("Respuesta de AEMET: {}", aemetResponse);
    if (aemetResponse.estado != HttpStatus.OK.value()) {
      return Flux.error(new IllegalArgumentException("URL no válida"));
    }
    return webClient
        .get()
        .uri(aemetResponse.datos())
        .retrieve()
        .bodyToMono(String.class)
        .flatMapIterable(
            json -> {
              try {
                List<Prediccion> prediccion =
                    objectMapper.readValue(
                        json,
                        objectMapper
                            .getTypeFactory()
                            .constructCollectionType(List.class, Prediccion.class));
                log.info("Predicciónes obtenidas: {}", prediccion.size());
                savePrediccion(prediccion.get(0).nombre(), json);
                return prediccion;
              } catch (JsonProcessingException e) {
                log.error("Error al procesar la respuesta JSON: ", e);
                throw new RuntimeException(e);
              }
            });
  }

  private void savePrediccion(String city, String json) {
    try {
      Files.writeString(Paths.get("predictions/", city, ".json"), json, StandardCharsets.UTF_8);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  record AemetResponse(String descripcion, Integer estado, String datos, String metadatos) {}
}
