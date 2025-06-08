package es.mcpworkshop.server.weather.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.logaritex.mcp.annotation.McpResource;
import io.modelcontextprotocol.spec.McpSchema;
import jakarta.annotation.PostConstruct;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;

@Service
public class AemetLocationProvider {

  Map<String, String> aemetCodes;
  ObjectMapper mapper;

  private static final Logger log = LoggerFactory.getLogger(AemetLocationProvider.class);

  public AemetLocationProvider(ObjectMapper mapper) {
    this.mapper = mapper;
  }

  @PostConstruct
  public void init() {
    try (BufferedReader reader =
        Files.newBufferedReader(Paths.get("src/main/resources/municipios.csv"))) {
      this.aemetCodes =
          reader
              .lines()
              .map(
                  line -> {
                    String[] values = line.split(";");
                    return new AemetCityCode(StringUtils.stripAccents(values[0]), values[1]);
                  })
              .collect(
                  Collectors.toUnmodifiableMap(
                      AemetCityCode::city,
                      AemetCityCode::code,
                      (existing, replacement) -> replacement));
      log.info("Leidos {} municipios", this.aemetCodes.size());
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @McpResource(
      uri = "aemet-codes://{location}",
      name = "Aemet Location codes",
      description = "Provides the AEMET code for a given location")
  public McpSchema.ReadResourceResult getAemetCode(
      McpSchema.ReadResourceRequest request, String location) {
    String strippedValue = StringUtils.stripAccents(location);
    log.info("Retrieving Aemet code for location {}", strippedValue);
    String code = this.aemetCodes.getOrDefault(strippedValue, "28079");
    log.info("Found Aemet code {} for location {}", code, strippedValue);
    return new McpSchema.ReadResourceResult(
        List.of(new McpSchema.TextResourceContents(request.uri(), "text/plain", code)));
  }

  @McpResource(
      uri = "file://predictions/{city}",
      name = "stored predictions for a city",
      description = "Provides all the weather predictions for a locations in Spain")
  public McpSchema.ReadResourceResult getAemetPredictions(
      McpSchema.ReadResourceRequest request, String city) {
    log.info("Retrieving Aemet locations in Spain");

    try {
      File file = new File("predictions/" + city + ".json");
      var lines = String.join("", Files.readAllLines(file.toPath()));
      log.info("Converted Aemet locations in Spain to JSON");
      return new McpSchema.ReadResourceResult(
          List.of(
              new McpSchema.TextResourceContents(
                  request.uri(), MediaType.TEXT_PLAIN_VALUE, lines)));
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  record AemetCityCode(String city, String code) {}
}
