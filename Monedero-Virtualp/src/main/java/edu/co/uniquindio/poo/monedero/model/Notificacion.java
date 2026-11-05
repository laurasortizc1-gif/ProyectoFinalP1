package edu.co.uniquindio.poo.monedero.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import edu.co.uniquindio.poo.monedero.model.interfaces.INotificable;

/**
 * Notificación abstracta. Cada canal (Gmail, SMS, WhatsApp) define su destino
 * y su forma de entrega. El envío real se simula (no hay credenciales de APIs
 * externas): se registra en la bandeja del cliente y en consola.
 */
public abstract class Notificacion implements INotificable {
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private String tipo;
    private String mensaje;
    private String destino;
    private LocalDateTime fecha;

    protected Notificacion(String tipo) {
        this.tipo = tipo;
    }

    /** Nombre del canal de envío. */
    public abstract String getCanal();

    /** Dirección/teléfono al que se envía según el canal. */
    protected abstract String obtenerDestino(Cliente cliente);

    @Override
    public void enviar(Cliente cliente, String mensaje) {
        this.mensaje = mensaje;
        this.destino = obtenerDestino(cliente);
        this.fecha = LocalDateTime.now();
        cliente.recibirNotificacion(this);
        System.out.println(this);
    }

    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }

    public String getMensaje() { return mensaje; }
    public void setMensaje(String mensaje) { this.mensaje = mensaje; }

    public String getDestino() { return destino; }
    public LocalDateTime getFecha() { return fecha; }

    @Override
    public String toString() {
        return "[" + (fecha != null ? fecha.format(FMT) : "--") + "] " + getCanal()
                + " → " + destino + " | " + tipo + ": " + mensaje;
    }
}
