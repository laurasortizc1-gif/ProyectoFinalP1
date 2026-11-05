package edu.co.uniquindio.poo.monedero.model;

/**
 * Rango del cliente según sus puntos acumulados. Cada rango ofrece:
 *  - multiplicador de puntos (bono de puntos adicionales)
 *  - descuento sobre la comisión de transferencias (tarifa reducida)
 */
public class Rango {
    private String nombre;
    private int puntosMin;
    private int puntosMax;
    private double multiplicadorPuntos;
    private double descuentoComision;

    public Rango(String nombre, int puntosMin, int puntosMax, double multiplicadorPuntos) {
        this(nombre, puntosMin, puntosMax, multiplicadorPuntos, 0);
    }

    public Rango(String nombre, int puntosMin, int puntosMax, double multiplicadorPuntos, double descuentoComision) {
        this.nombre = nombre;
        this.puntosMin = puntosMin;
        this.puntosMax = puntosMax;
        this.multiplicadorPuntos = multiplicadorPuntos;
        this.descuentoComision = descuentoComision;
    }

    /**
     * Función que calcula el rango con base en los puntos acumulados.
     * Bronce: 0–500, Plata: 501–1000, Oro: 1001–5000, Platino: más de 5000.
     */
    public static Rango calcularRango(int puntos) {
        if (puntos <= 500) return new Rango("Bronce", 0, 500, 1.0, 0.0);
        if (puntos <= 1000) return new Rango("Plata", 501, 1000, 1.1, 0.10);
        if (puntos <= 5000) return new Rango("Oro", 1001, 5000, 1.3, 0.25);
        return new Rango("Platino", 5001, Integer.MAX_VALUE, 1.5, 0.50);
    }

    public String getDescripcionBeneficios() {
        return String.format("Puntos x%.1f | %d%% menos comisión en transferencias",
                multiplicadorPuntos, Math.round(descuentoComision * 100));
    }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public int getPuntosMin() { return puntosMin; }
    public void setPuntosMin(int puntosMin) { this.puntosMin = puntosMin; }

    public int getPuntosMax() { return puntosMax; }
    public void setPuntosMax(int puntosMax) { this.puntosMax = puntosMax; }

    public double getMultiplicadorPuntos() { return multiplicadorPuntos; }
    public void setMultiplicadorPuntos(double multiplicadorPuntos) { this.multiplicadorPuntos = multiplicadorPuntos; }

    public double getDescuentoComision() { return descuentoComision; }
    public void setDescuentoComision(double descuentoComision) { this.descuentoComision = descuentoComision; }

    @Override
    public String toString() { return nombre; }
}
