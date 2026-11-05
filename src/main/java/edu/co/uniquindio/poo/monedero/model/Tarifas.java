package edu.co.uniquindio.poo.monedero.model;

/** Constantes de negocio del monedero (comisiones y umbrales). */
public final class Tarifas {
    /** 1 % del monto por transferencia. */
    public static final double COMISION_TRANSFERENCIA = 0.01;
    /** 0,5 % del monto por retiro. */
    public static final double COMISION_RETIRO = 0.005;
    /** Saldo por debajo del cual se envía alerta de saldo bajo. */
    public static final double SALDO_MINIMO_ALERTA = 20000;
    /** Horas de anticipación para recordar una transacción programada. */
    public static final int HORAS_RECORDATORIO = 24;

    private Tarifas() {}
}
