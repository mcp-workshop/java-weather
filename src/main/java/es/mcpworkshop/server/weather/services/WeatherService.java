package es.mcpworkshop.server.weather.services;

import es.mcpworkshop.server.weather.model.Prediccion;
import java.util.List;

public interface WeatherService {

  List<Prediccion> getWeatherForecast(String city);
}
