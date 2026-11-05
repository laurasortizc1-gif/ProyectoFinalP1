package edu.co.uniquindio.poo.monedero.model;

public class NotificacionEmail extends Notificacion {
    public NotificacionEmail(String tipo) { super(tipo); }

    @Override public String getCanal() { return "Gmail"; }

    @Override protected String obtenerDestino(Cliente cliente) { return cliente.getEmail(); }
}
