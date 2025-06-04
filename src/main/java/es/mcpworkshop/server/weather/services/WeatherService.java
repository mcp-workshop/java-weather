package es.mcpworkshop.server.weather.services;

import es.mcpworkshop.server.weather.model.Prediccion;
import reactor.core.publisher.Flux;

public interface WeatherService {

  Flux<Prediccion> getWeatherForecast(String city);
}
