package es.mcpworkshop.server.weather.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import es.mcpworkshop.server.weather.model.Prediccion;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
  public Flux<Prediccion> getWeatherForecast(String city) {
    return callAemetProxy(city)
        .flatMapMany(this::callRealMethod)
        .doOnError(error -> log.error("Error en el proceso: ", error));
  }

  private Mono<AemetResponse> callAemetProxy(String city) {
    return webClient
        .get()
        .uri(
            "/api/prediccion/especifica/municipio/diaria/{municipio_aemet_code}?api_key={api_key}",
            city,
            apiKey)
        .retrieve()
        .onStatus(
            HttpStatusCode::isError,
            clientResponse -> Mono.error(new RuntimeException("Error en primera llamada")))
        .bodyToMono(AemetResponse.class);
  }

  private Flux<Prediccion> callRealMethod(AemetResponse aemetResponse) {
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
                return objectMapper.readValue(
                    json,
                    objectMapper
                        .getTypeFactory()
                        .constructCollectionType(List.class, Prediccion.class));
              } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
              }
            });
  }

  record AemetResponse(String descripcion, Integer estado, String datos, String metadatos) {}
}
