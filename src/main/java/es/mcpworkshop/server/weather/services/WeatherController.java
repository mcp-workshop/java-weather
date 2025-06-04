package es.mcpworkshop.server.weather.services;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/weather")
public class WeatherController {

  WeatherService weatherService;

  public WeatherController(WeatherService weatherService) {
    this.weatherService = weatherService;
  }

  @GetMapping("/{cityCode}")
  public Object getWeather(@PathVariable String cityCode) {
    return weatherService.getWeatherForecast(cityCode);
  }
}
