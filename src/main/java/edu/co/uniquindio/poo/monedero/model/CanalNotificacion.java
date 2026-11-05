package edu.co.uniquindio.poo.monedero.model;

/** Canales disponibles para enviar notificaciones al cliente. */
public enum CanalNotificacion {
    EMAIL("Gmail") {
        @Override public Notificacion crear(String tipo) { return new NotificacionEmail(tipo); }
    },
    SMS("SMS") {
        @Override public Notificacion crear(String tipo) { return new NotificacionSMS(tipo); }
    },
    WHATSAPP("WhatsApp") {
        @Override public Notificacion crear(String tipo) { return new NotificacionWhatsApp(tipo); }
    };

    private final String nombre;

    CanalNotificacion(String nombre) { this.nombre = nombre; }

    /** Fábrica: crea la notificación concreta del canal. */
    public abstract Notificacion crear(String tipo);

    @Override
    public String toString() { return nombre; }
}
