package es.mcpworkshop.server.weather.services;

import es.mcpworkshop.server.weather.model.Prediccion;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/weather")
public class WeatherController {

  WeatherService weatherService;

  public WeatherController(WeatherService weatherService) {
    this.weatherService = weatherService;
  }

  @GetMapping("/{aemetLocationCode}")
  public Flux<Prediccion> getWeather(@PathVariable String aemetLocationCode) {
    return Flux.fromIterable(weatherService.getWeatherForecast(aemetLocationCode));
  }
}
