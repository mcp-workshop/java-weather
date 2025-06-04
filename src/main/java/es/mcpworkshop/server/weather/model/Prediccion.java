package es.mcpworkshop.server.weather.model;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.List;

public record Prediccion(
    @JsonIgnore Origen origen,
    String elaborado,
    String nombre,
    String provincia,
    PrediccionDia prediccion,
    int id,
    double version) {}

record Origen(
    String productor,
    String web,
    String enlace,
    String language,
    String copyright,
    String notaLegal) {}

record PrediccionDia(List<Dia> dia) {}

record Dia(
    List<Periodo> probPrecipitacion,
    @JsonIgnore List<Periodo> cotaNieveProv,
    @JsonIgnore List<EstadoCielo> estadoCielo,
    @JsonIgnore List<Viento> viento,
    @JsonIgnore List<Periodo> rachaMax,
    Temperatura temperatura,
    SensTermica sensTermica,
    HumedadRelativa humedadRelativa,
    int uvMax,
    String fecha) {}

record Periodo(String value, String periodo) {}

record EstadoCielo(String value, String periodo, String descripcion) {}

record Viento(String direccion, int velocidad, String periodo) {}

record Temperatura(int maxima, int minima, List<Dato> dato) {}

record SensTermica(int maxima, int minima, List<Dato> dato) {}

record HumedadRelativa(int maxima, int minima, List<Dato> dato) {}

record Dato(int value, int hora) {}
