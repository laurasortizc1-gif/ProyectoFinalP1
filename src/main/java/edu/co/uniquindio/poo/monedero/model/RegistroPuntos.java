package edu.co.uniquindio.poo.monedero.model;

import java.time.LocalDateTime;

/** Movimiento en el historial de puntos del cliente. */
public class RegistroPuntos {

    public enum TipoMovimiento { GANADOS, REVERTIDOS, CANJEADOS }

    private final LocalDateTime fecha;
    private final TipoMovimiento tipo;
    private final int puntos; // positivo = suma, negativo = resta
    private final String descripcion;

    public RegistroPuntos(TipoMovimiento tipo, int puntos, String descripcion) {
        this.fecha = LocalDateTime.now();
        this.tipo = tipo;
        this.puntos = puntos;
        this.descripcion = descripcion;
    }

    public LocalDateTime getFecha() { return fecha; }
    public TipoMovimiento getTipo() { return tipo; }
    public int getPuntos() { return puntos; }
    public String getDescripcion() { return descripcion; }
}
