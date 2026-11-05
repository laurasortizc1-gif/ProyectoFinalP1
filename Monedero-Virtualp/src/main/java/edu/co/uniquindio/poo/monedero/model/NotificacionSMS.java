package edu.co.uniquindio.poo.monedero.model;

public class NotificacionSMS extends Notificacion {
    public NotificacionSMS(String tipo) { super(tipo); }

    @Override public String getCanal() { return "SMS"; }

    @Override protected String obtenerDestino(Cliente cliente) { return cliente.getTelefono(); }
}
