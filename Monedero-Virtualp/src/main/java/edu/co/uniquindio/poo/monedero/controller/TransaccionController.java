package edu.co.uniquindio.poo.monedero.controller;

import edu.co.uniquindio.poo.monedero.exceptions.CampoVacioException;
import edu.co.uniquindio.poo.monedero.exceptions.EntidadNoEncontradaException;
import edu.co.uniquindio.poo.monedero.model.*;
import edu.co.uniquindio.poo.monedero.service.DetectorTransaccionesAtipicas;
import edu.co.uniquindio.poo.monedero.service.ServicioNotificaciones;

import java.time.LocalDateTime;

/**
 * Orquesta el ciclo completo de una transacción:
 * validar → calcular comisión → ejecutar → puntos → detección atípica → alertas.
 */
public class TransaccionController {

    private final ServicioNotificaciones notificaciones;
    private final DetectorTransaccionesAtipicas detector;

    public TransaccionController() {
        this(new ServicioNotificaciones());
    }

    public TransaccionController(ServicioNotificaciones notificaciones) {
        this.notificaciones = notificaciones;
        this.detector = new DetectorTransaccionesAtipicas(notificaciones);
    }

    public Transaccion crearTransaccion(String tipo, double monto, Cliente origen, Cliente destino, Monedero monedero) {
        return crearTransaccion(tipo, monto, origen, destino, monedero, null);
    }

    public Transaccion crearTransaccion(String tipo, double monto, Cliente origen, Cliente destino,
                                        Monedero monedero, Monedero monederoDestino) {
        if (tipo == null || tipo.isBlank()) {
            throw new CampoVacioException("Seleccione un tipo de transacción");
        }
        if (monedero == null) {
            throw new EntidadNoEncontradaException("Debe seleccionar un monedero");
        }
        if (origen != null && !origen.getMonederos().contains(monedero)) {
            throw new EntidadNoEncontradaException("El monedero no pertenece al cliente de origen");
        }

        Transaccion transaccion = switch (tipo.toLowerCase()) {
            case "deposito", "depósito" -> new Deposito();
            case "retiro" -> new Retiro();
            case "transferencia" -> {
                if (destino == null) {
                    throw new CampoVacioException("Debe seleccionar un cliente destino");
                }
                yield new Transferencia();
            }
            default -> throw new IllegalArgumentException("Tipo de transacción no válido: " + tipo);
        };

        transaccion.setMonto(monto);
        transaccion.setClienteOrigen(origen != null ? origen : monedero.getDueño());
        transaccion.setClienteDestino(destino);
        transaccion.setMonedero(monedero);
        transaccion.setMonederoDestino(monederoDestino);
        return transaccion;
    }

    public void programarTransaccion(Transaccion transaccion, LocalDateTime fecha, Frecuencia frecuencia) {
        transaccion.programarTransaccion(fecha, frecuencia);
    }

    /**
     * Ejecuta la transacción. Las excepciones de negocio se propagan para que
     * la interfaz las muestre (antes se "tragaban" y se mostraba éxito).
     */
    public Transaccion ejecutarTransaccion(Transaccion transaccion) {
        Cliente cliente = transaccion.getClienteOrigen() != null
                ? transaccion.getClienteOrigen()
                : transaccion.getMonedero().getDueño();

        transaccion.setComision(cliente.calcularComision(transaccion));
        transaccion.ejecutar();
        cliente.actualizarPuntos(transaccion);

        detector.evaluarTransaccion(cliente, transaccion);
        notificaciones.verificarSaldoBajo(transaccion.getMonedero());

        if (transaccion instanceof Transferencia && transaccion.getClienteDestino() != cliente) {
            notificaciones.notificar(transaccion.getClienteDestino(), ServicioNotificaciones.MOVIMIENTO,
                    String.format("Recibiste $%,.2f de %s en '%s'", transaccion.getMonto(),
                            cliente.getNombre(), transaccion.getMonederoDestino().getNombre()));
        }
        return transaccion;
    }

    /** Revierte una transacción y recalcula puntos/rango del cliente. */
    public void revertirTransaccion(Transaccion transaccion) {
        if (transaccion == null) {
            throw new CampoVacioException("Seleccione una transacción");
        }
        Cliente cliente = transaccion.getMonedero().getDueño();
        cliente.revertirTransaccion(transaccion);
        notificaciones.notificar(cliente, ServicioNotificaciones.MOVIMIENTO,
                "Se revirtió " + transaccion + ". Puntos descontados: " + transaccion.getPuntosOtorgados());
    }

    public int calcularPuntos(Transaccion transaccion) {
        return transaccion.calcularPuntos();
    }

    public DetectorTransaccionesAtipicas getDetector() {
        return detector;
    }
}
