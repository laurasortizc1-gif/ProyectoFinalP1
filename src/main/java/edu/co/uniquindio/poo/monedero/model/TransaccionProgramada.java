package edu.co.uniquindio.poo.monedero.model;

import java.time.LocalDateTime;

/**
 * Plantilla de una transacción recurrente. En cada ejecución se crea una
 * transacción nueva (Depósito/Retiro/Transferencia) a partir de estos datos.
 */
public class TransaccionProgramada implements Comparable<TransaccionProgramada> {
    private static int contadorIds = 0;

    private final int id;
    private final String tipo;
    private final double monto;
    private final Cliente clienteOrigen;
    private final Monedero monederoOrigen;
    private final Cliente clienteDestino;
    private final Monedero monederoDestino;
    private final Frecuencia frecuencia;
    private LocalDateTime proximaEjecucion;
    private boolean activa = true;
    private boolean recordatorioEnviado = false;
    private int ejecuciones = 0;
    private String ultimoResultado = "Pendiente";

    public TransaccionProgramada(String tipo, double monto, Cliente clienteOrigen, Monedero monederoOrigen,
                                 Cliente clienteDestino, Monedero monederoDestino,
                                 LocalDateTime primeraEjecucion, Frecuencia frecuencia) {
        this.id = ++contadorIds;
        this.tipo = tipo;
        this.monto = monto;
        this.clienteOrigen = clienteOrigen;
        this.monederoOrigen = monederoOrigen;
        this.clienteDestino = clienteDestino;
        this.monederoDestino = monederoDestino;
        this.proximaEjecucion = primeraEjecucion;
        this.frecuencia = frecuencia;
    }

    /** Se llama después de ejecutar: calcula la siguiente fecha o la desactiva. */
    public void avanzar(String resultado) {
        ejecuciones++;
        ultimoResultado = resultado;
        recordatorioEnviado = false;
        LocalDateTime siguiente = frecuencia.siguiente(proximaEjecucion);
        if (siguiente == null) {
            activa = false;
        } else {
            proximaEjecucion = siguiente;
        }
    }

    public boolean estaPendiente(LocalDateTime ahora) {
        return activa && !proximaEjecucion.isAfter(ahora);
    }

    public void cancelar() { activa = false; ultimoResultado = "Cancelada"; }

    @Override
    public int compareTo(TransaccionProgramada otra) {
        return this.proximaEjecucion.compareTo(otra.proximaEjecucion);
    }

    public int getId() { return id; }
    public String getTipo() { return tipo; }
    public double getMonto() { return monto; }
    public Cliente getClienteOrigen() { return clienteOrigen; }
    public Monedero getMonederoOrigen() { return monederoOrigen; }
    public Cliente getClienteDestino() { return clienteDestino; }
    public Monedero getMonederoDestino() { return monederoDestino; }
    public Frecuencia getFrecuencia() { return frecuencia; }
    public LocalDateTime getProximaEjecucion() { return proximaEjecucion; }
    public boolean isActiva() { return activa; }
    public boolean isRecordatorioEnviado() { return recordatorioEnviado; }
    public void marcarRecordatorioEnviado() { this.recordatorioEnviado = true; }
    public int getEjecuciones() { return ejecuciones; }
    public String getUltimoResultado() { return ultimoResultado; }

    public String getEstado() {
        return activa ? "Activa" : "Finalizada";
    }
}
