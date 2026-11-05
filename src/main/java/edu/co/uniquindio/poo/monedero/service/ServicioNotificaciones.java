package edu.co.uniquindio.poo.monedero.service;

import edu.co.uniquindio.poo.monedero.model.Cliente;
import edu.co.uniquindio.poo.monedero.model.Monedero;
import edu.co.uniquindio.poo.monedero.model.Notificacion;
import edu.co.uniquindio.poo.monedero.model.Tarifas;

/** Envía alertas y notificaciones por el canal preferido del cliente. */
public class ServicioNotificaciones {

    public static final String SALDO_BAJO = "Alerta de saldo bajo";
    public static final String SOSPECHOSA = "Transacción sospechosa";
    public static final String RECORDATORIO = "Recordatorio";
    public static final String PROGRAMADA = "Transacción programada";
    public static final String MOVIMIENTO = "Movimiento";
    public static final String BENEFICIO = "Beneficio";

    public void notificar(Cliente cliente, String tipo, String mensaje) {
        if (cliente == null) return;
        Notificacion n = cliente.getCanalPreferido().crear(tipo); // polimorfismo por canal
        n.enviar(cliente, mensaje);
    }

    /** Envía alerta si el saldo del monedero quedó por debajo del mínimo. */
    public void verificarSaldoBajo(Monedero monedero) {
        if (monedero != null && monedero.tieneSaldoBajo()) {
            notificar(monedero.getDueño(), SALDO_BAJO, String.format(
                    "Tu monedero '%s' tiene saldo bajo: $%,.2f (mínimo recomendado $%,.0f)",
                    monedero.getNombre(), monedero.getSaldo(), Tarifas.SALDO_MINIMO_ALERTA));
        }
    }
}
