package es.mcpworkshop.server.weather.ai;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.logaritex.mcp.annotation.McpResource;
import io.modelcontextprotocol.spec.McpSchema;
import jakarta.annotation.PostConstruct;
import java.io.BufferedReader;
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
      uri = "file://municipios",
      name = "All the locations in AEMET",
      description = "Provides all the AEMET codes and locations in Spain")
  public McpSchema.ReadResourceResult getAemetLocations(McpSchema.ReadResourceRequest request) {
    log.info("Retrieving Aemet locations in Spain");

    try {
      String theFile = mapper.writeValueAsString(this.aemetCodes);
      log.info("Converted Aemet locations in Spain to JSON");
      return new McpSchema.ReadResourceResult(
          List.of(
              new McpSchema.TextResourceContents(
                  request.uri(), MediaType.APPLICATION_JSON_VALUE, theFile)));
    } catch (JsonProcessingException e) {
      throw new RuntimeException(e);
    }
  }

  record AemetCityCode(String city, String code) {}
}
