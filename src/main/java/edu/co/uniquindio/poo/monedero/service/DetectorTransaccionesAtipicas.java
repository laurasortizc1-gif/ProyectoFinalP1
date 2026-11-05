package edu.co.uniquindio.poo.monedero.service;

import java.util.List;

import edu.co.uniquindio.poo.monedero.model.Cliente;
import edu.co.uniquindio.poo.monedero.model.Transaccion;

/**
 * Detección de transacciones atípicas.
 *
 * Una transacción es atípica si: monto > promedio + 2 × desviación estándar,
 * calculados sobre el historial de transacciones del cliente (no revertidas).
 *
 * Complejidad: O(n), siendo n el número de transacciones del cliente:
 *   - 1 recorrido para el promedio (Stream average)
 *   - 1 recorrido para la varianza
 *   - 1 recorrido para filtrar las que superan el umbral
 *   O(3n) = O(n). Espacio O(n) por la lista de transacciones.
 */
public class DetectorTransaccionesAtipicas {

    /** Mínimo de transacciones para que la estadística sea representativa. */
    public static final int MIN_HISTORIAL = 5;

    private final ServicioNotificaciones notificaciones;

    public record Estadisticas(int cantidad, double promedio, double desviacion, double umbral) {}

    public DetectorTransaccionesAtipicas(ServicioNotificaciones notificaciones) {
        this.notificaciones = notificaciones;
    }

    /** Promedio, desviación estándar poblacional y umbral (Stream API). */
    public Estadisticas calcularEstadisticas(List<Transaccion> transacciones) {
        double promedio = transacciones.stream()
                .mapToDouble(Transaccion::getMonto)
                .average()
                .orElse(0);
        double varianza = transacciones.stream()
                .mapToDouble(t -> Math.pow(t.getMonto() - promedio, 2))
                .average()
                .orElse(0);
        double desviacion = Math.sqrt(varianza);
        return new Estadisticas(transacciones.size(), promedio, desviacion, promedio + 2 * desviacion);
    }

    private List<Transaccion> historialValido(Cliente cliente) {
        return cliente.obtenerTransaccionesRealizadas().stream()
                .filter(t -> !t.isRevertida())
                .toList();
    }

    /**
     * Analiza TODO el historial del cliente, marca las atípicas y alerta
     * por las que no estaban marcadas antes.
     */
    public List<Transaccion> analizarHistorial(Cliente cliente) {
        List<Transaccion> historial = historialValido(cliente);
        if (historial.size() < MIN_HISTORIAL) {
            return List.of();
        }
        Estadisticas e = calcularEstadisticas(historial);
        List<Transaccion> atipicas = historial.stream()
                .filter(t -> t.getMonto() > e.umbral())
                .toList();
        atipicas.stream()
                .filter(t -> !t.isSospechosa())
                .forEach(t -> marcarYAlertar(cliente, t, e));
        return atipicas;
    }

    /**
     * Evalúa una transacción recién hecha contra el comportamiento previo del
     * cliente (historial sin incluirla). Se llama automáticamente en cada operación.
     */
    public boolean evaluarTransaccion(Cliente cliente, Transaccion nueva) {
        List<Transaccion> previas = historialValido(cliente).stream()
                .filter(t -> t != nueva)
                .toList();
        if (previas.size() < MIN_HISTORIAL) {
            return false;
        }
        Estadisticas e = calcularEstadisticas(previas);
        if (nueva.getMonto() > e.umbral()) {
            marcarYAlertar(cliente, nueva, e);
            return true;
        }
        return false;
    }

    private void marcarYAlertar(Cliente cliente, Transaccion t, Estadisticas e) {
        t.marcarSospechosa();
        notificaciones.notificar(cliente, ServicioNotificaciones.SOSPECHOSA, String.format(
                "%s por $%,.2f supera tu comportamiento habitual (promedio $%,.2f, umbral $%,.2f). "
                        + "Si no la reconoces, contáctanos.",
                t.getTipo(), t.getMonto(), e.promedio(), e.umbral()));
    }
}
