package edu.co.uniquindio.poo.monedero.service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import edu.co.uniquindio.poo.monedero.controller.TransaccionController;
import edu.co.uniquindio.poo.monedero.exceptions.CampoVacioException;
import edu.co.uniquindio.poo.monedero.exceptions.EntidadNoEncontradaException;
import edu.co.uniquindio.poo.monedero.exceptions.FechaProgramacionInvalidaException;
import edu.co.uniquindio.poo.monedero.exceptions.MontoNegativoException;
import edu.co.uniquindio.poo.monedero.model.Cliente;
import edu.co.uniquindio.poo.monedero.model.Frecuencia;
import edu.co.uniquindio.poo.monedero.model.Monedero;
import edu.co.uniquindio.poo.monedero.model.Tarifas;
import edu.co.uniquindio.poo.monedero.model.Transaccion;
import edu.co.uniquindio.poo.monedero.model.TransaccionProgramada;
import edu.co.uniquindio.poo.monedero.util.Ordenamiento;

/**
 * Gestiona las transacciones programadas/recurrentes. Antes de procesarlas
 * las ordena por fecha de ejecución con Merge Sort, de modo que siempre se
 * ejecuta primero la más antigua.
 */
public class ProgramadorTransacciones {
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final List<TransaccionProgramada> programadas = new ArrayList<>();
    private final TransaccionController transaccionController;
    private final ServicioNotificaciones notificaciones;

    public ProgramadorTransacciones(TransaccionController transaccionController,
                                    ServicioNotificaciones notificaciones) {
        this.transaccionController = transaccionController;
        this.notificaciones = notificaciones;
    }

    public TransaccionProgramada programar(String tipo, double monto, Cliente origen, Monedero monederoOrigen,
                                           Cliente destino, Monedero monederoDestino,
                                           LocalDateTime fecha, Frecuencia frecuencia) {
        if (tipo == null || tipo.isBlank()) throw new CampoVacioException("Seleccione el tipo de transacción");
        if (monto <= 0) throw new MontoNegativoException("El monto debe ser mayor a 0");
        if (origen == null || monederoOrigen == null) throw new EntidadNoEncontradaException("Falta el monedero de origen");
        if (frecuencia == null) throw new CampoVacioException("Seleccione la frecuencia");
        if (fecha == null || fecha.isBefore(LocalDateTime.now().minusMinutes(1))) {
            throw new FechaProgramacionInvalidaException("La fecha de ejecución debe ser futura");
        }
        if (tipo.toLowerCase().startsWith("transfer") && destino == null) {
            throw new CampoVacioException("Seleccione un cliente destino");
        }
        TransaccionProgramada tp = new TransaccionProgramada(tipo, monto, origen, monederoOrigen,
                destino, monederoDestino, fecha, frecuencia);
        programadas.add(tp);
        notificaciones.notificar(origen, ServicioNotificaciones.PROGRAMADA, String.format(
                "%s de $%,.2f programada para %s (%s)", tipo, monto, fecha.format(FMT), frecuencia));
        return tp;
    }

    /** Lista de programadas ordenada por fecha de ejecución (Merge Sort). */
    public List<TransaccionProgramada> obtenerOrdenadas() {
        return Ordenamiento.mergeSort(programadas,
                Comparator.comparing(TransaccionProgramada::getProximaEjecucion));
    }

    public List<TransaccionProgramada> obtenerDeCliente(Cliente c) {
        return obtenerOrdenadas().stream()
                .filter(tp -> tp.getClienteOrigen() == c)
                .toList();
    }

    /**
     * Ejecuta, en orden de fecha, todas las programadas vencidas. Una recurrente
     * atrasada varias veces se ejecuta una vez por cada periodo vencido.
     * @return mensajes con el resultado de cada ejecución.
     */
    public List<String> procesarPendientes(LocalDateTime ahora) {
        List<String> resultados = new ArrayList<>();
        List<TransaccionProgramada> pendientes;
        // Se repite porque al avanzar una recurrente puede seguir vencida
        while (!(pendientes = obtenerOrdenadas().stream()
                .filter(tp -> tp.estaPendiente(ahora))
                .toList()).isEmpty()) {
            for (TransaccionProgramada tp : pendientes) {
                resultados.add(ejecutar(tp));
            }
        }
        enviarRecordatorios(ahora);
        return resultados;
    }

    private String ejecutar(TransaccionProgramada tp) {
        String resultado;
        try {
            Transaccion t = transaccionController.crearTransaccion(tp.getTipo(), tp.getMonto(),
                    tp.getClienteOrigen(), tp.getClienteDestino(), tp.getMonederoOrigen(), tp.getMonederoDestino());
            t.programarTransaccion(tp.getProximaEjecucion(), tp.getFrecuencia());
            transaccionController.ejecutarTransaccion(t);
            resultado = "Ejecutada: " + t;
        } catch (RuntimeException e) {
            resultado = "Fallida: " + e.getMessage();
            notificaciones.notificar(tp.getClienteOrigen(), ServicioNotificaciones.PROGRAMADA,
                    "No se pudo ejecutar la transacción programada #" + tp.getId() + ": " + e.getMessage());
        }
        tp.avanzar(resultado);
        return "Programada #" + tp.getId() + " → " + resultado;
    }

    /** Recordatorio para las que se ejecutan dentro de las próximas 24 h. */
    public void enviarRecordatorios(LocalDateTime ahora) {
        programadas.stream()
                .filter(TransaccionProgramada::isActiva)
                .filter(tp -> !tp.isRecordatorioEnviado())
                .filter(tp -> Duration.between(ahora, tp.getProximaEjecucion()).toHours() < Tarifas.HORAS_RECORDATORIO)
                .forEach(tp -> {
                    tp.marcarRecordatorioEnviado();
                    notificaciones.notificar(tp.getClienteOrigen(), ServicioNotificaciones.RECORDATORIO,
                            String.format("Recuerda: %s de $%,.2f se ejecutará el %s",
                                    tp.getTipo(), tp.getMonto(), tp.getProximaEjecucion().format(FMT)));
                });
    }

    public void cancelar(TransaccionProgramada tp) {
        if (tp == null) throw new CampoVacioException("Seleccione una transacción programada");
        tp.cancelar();
    }
}
