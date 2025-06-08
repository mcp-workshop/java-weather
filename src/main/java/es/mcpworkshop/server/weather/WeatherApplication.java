package es.mcpworkshop.server.weather;

import com.logaritex.mcp.spring.SpringAiMcpAnnotationProvider;
import es.mcpworkshop.server.weather.ai.AemetLocationProvider;
import es.mcpworkshop.server.weather.services.WeatherService;
import io.modelcontextprotocol.server.McpServerFeatures;
import java.util.List;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class WeatherApplication {

  public static void main(String[] args) {
    SpringApplication.run(WeatherApplication.class, args);
  }

  @Bean
  MethodToolCallbackProvider methodToolCallbackProvider(WeatherService weatherService) {
    return MethodToolCallbackProvider.builder().toolObjects(weatherService).build();
  }

  @Bean
  List<McpServerFeatures.SyncResourceSpecification> resourceSpecs(
      AemetLocationProvider aemetLocationProvider) {
    return SpringAiMcpAnnotationProvider.createSyncResourceSpecifications(
        List.of(aemetLocationProvider));
  }
}
