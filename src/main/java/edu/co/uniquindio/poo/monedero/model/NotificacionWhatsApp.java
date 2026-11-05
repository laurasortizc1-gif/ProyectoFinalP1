package edu.co.uniquindio.poo.monedero.model;

public class NotificacionWhatsApp extends Notificacion {
    public NotificacionWhatsApp(String tipo) { super(tipo); }

    @Override public String getCanal() { return "WhatsApp"; }

    @Override protected String obtenerDestino(Cliente cliente) { return "+57 " + cliente.getTelefono(); }
}
