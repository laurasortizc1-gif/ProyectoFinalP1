package edu.co.uniquindio.poo.monedero.model;

import java.time.LocalDateTime;

import edu.co.uniquindio.poo.monedero.exceptions.EntidadNoEncontradaException;
import edu.co.uniquindio.poo.monedero.exceptions.MontoNegativoException;
import edu.co.uniquindio.poo.monedero.exceptions.TransaccionNoReversibleException;
import edu.co.uniquindio.poo.monedero.model.interfaces.ITransaccion;

/**
 * Clase abstracta base de todas las transacciones (Abstracción + Herencia).
 * Las subclases sobrescriben ejecutar(), revertir(), calcularPuntos() y la
 * lógica de comisiones (Polimorfismo).
 */
public abstract class Transaccion implements ITransaccion {
    private static int contadorIds = 0;

    protected int idTransaccion;
    protected double monto;
    protected String tipo;
    protected LocalDateTime fechaCreacion;
    protected boolean programada;
    protected LocalDateTime fechaEjecucion;
    protected Frecuencia frecuencia;
    protected Cliente clienteOrigen;
    protected Cliente clienteDestino;
    protected Monedero monedero;
    protected Monedero monederoDestino;

    // Nuevos atributos
    protected double comision;
    protected int puntosOtorgados;
    protected boolean sospechosa;
    protected boolean revertida;

    public Transaccion() {
        this.idTransaccion = ++contadorIds;
        this.fechaCreacion = LocalDateTime.now();
    }

    public Transaccion(Monedero monederoOrigen, Cliente origen, Cliente destino, double monto) {
        this();
        this.monedero = monederoOrigen;
        this.clienteOrigen = origen;
        this.clienteDestino = destino;
        this.monto = monto;
    }

    // ---------------- Métodos polimórficos ----------------

    /** Deshace el efecto de la transacción sobre los saldos. */
    public abstract void revertir();

    /** Comisión base antes de beneficios. Por defecto no hay comisión. */
    public double calcularComisionBase() {
        return 0;
    }

    /** Aplica descuentos del cliente (rango/beneficios) a la comisión base. */
    public double aplicarBeneficios(Cliente cliente, double comisionBase) {
        return comisionBase;
    }

    /** Indica si la transacción representa una salida de dinero (gasto). */
    public boolean esGasto() {
        return false;
    }

    // ---------------- Validaciones comunes ----------------

    protected void validarMonto() {
        if (monto <= 0) {
            throw new MontoNegativoException("El monto debe ser mayor a 0");
        }
    }

    protected void validarMonedero() {
        if (monedero == null) {
            throw new EntidadNoEncontradaException("Debe especificar el monedero de la transacción");
        }
    }

    protected void validarNoRevertida() {
        if (revertida) {
            throw new TransaccionNoReversibleException("La transacción #" + idTransaccion + " ya fue revertida");
        }
    }

    public void programarTransaccion(LocalDateTime fecha, Frecuencia frecuencia) {
        this.programada = true;
        this.fechaEjecucion = fecha;
        this.frecuencia = frecuencia;
    }

    public String getEstado() {
        if (revertida) return "Revertida";
        if (sospechosa) return "⚠ Sospechosa";
        return "OK";
    }

    // Getters / Setters

    public int getIdTransaccion() { return idTransaccion; }
    public void setIdTransaccion(int idTransaccion) { this.idTransaccion = idTransaccion; }

    public double getMonto() { return monto; }
    public void setMonto(double monto) { this.monto = monto; }

    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }

    public LocalDateTime getFechaCreacion() { return fechaCreacion; }
    public void setFechaCreacion(LocalDateTime fechaCreacion) { this.fechaCreacion = fechaCreacion; }

    public boolean isProgramada() { return programada; }
    public void setProgramada(boolean programada) { this.programada = programada; }

    public LocalDateTime getFechaEjecucion() { return fechaEjecucion; }
    public void setFechaEjecucion(LocalDateTime fechaEjecucion) { this.fechaEjecucion = fechaEjecucion; }

    public Frecuencia getFrecuencia() { return frecuencia; }
    public void setFrecuencia(Frecuencia frecuencia) { this.frecuencia = frecuencia; }

    public Cliente getClienteOrigen() { return clienteOrigen; }
    public void setClienteOrigen(Cliente clienteOrigen) { this.clienteOrigen = clienteOrigen; }

    public Cliente getClienteDestino() { return clienteDestino; }
    public void setClienteDestino(Cliente clienteDestino) { this.clienteDestino = clienteDestino; }

    public Monedero getMonedero() { return monedero; }
    public void setMonedero(Monedero monedero) { this.monedero = monedero; }

    public Monedero getMonederoDestino() { return monederoDestino; }
    public void setMonederoDestino(Monedero monederoDestino) { this.monederoDestino = monederoDestino; }

    public double getComision() { return comision; }
    public void setComision(double comision) {
        if (comision < 0) throw new IllegalArgumentException("La comisión no puede ser negativa");
        this.comision = comision;
    }

    public int getPuntosOtorgados() { return puntosOtorgados; }
    void setPuntosOtorgados(int puntosOtorgados) { this.puntosOtorgados = puntosOtorgados; }

    public boolean isSospechosa() { return sospechosa; }
    public void marcarSospechosa() { this.sospechosa = true; }

    public boolean isRevertida() { return revertida; }

    @Override
    public String toString() {
        return "#" + idTransaccion + " " + tipo + " $" + String.format("%,.2f", monto);
    }
}
