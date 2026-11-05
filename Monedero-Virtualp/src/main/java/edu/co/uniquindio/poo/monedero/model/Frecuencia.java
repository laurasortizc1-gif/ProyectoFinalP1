package edu.co.uniquindio.poo.monedero.model;

import java.time.LocalDateTime;

/**
 * Frecuencia de una transacción programada. Cada constante sabe calcular
 * su siguiente fecha de ejecución (polimorfismo a nivel de enum).
 */
public enum Frecuencia {
    UNICA("Una sola vez") {
        @Override public LocalDateTime siguiente(LocalDateTime f) { return null; }
    },
    DIARIA("Diaria") {
        @Override public LocalDateTime siguiente(LocalDateTime f) { return f.plusDays(1); }
    },
    SEMANAL("Semanal") {
        @Override public LocalDateTime siguiente(LocalDateTime f) { return f.plusWeeks(1); }
    },
    MENSUAL("Mensual") {
        @Override public LocalDateTime siguiente(LocalDateTime f) { return f.plusMonths(1); }
    };

    private final String nombre;

    Frecuencia(String nombre) { this.nombre = nombre; }

    /** @return la próxima fecha de ejecución o null si no se repite. */
    public abstract LocalDateTime siguiente(LocalDateTime fechaActual);

    @Override
    public String toString() { return nombre; }
}
