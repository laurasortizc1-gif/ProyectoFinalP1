package edu.co.uniquindio.poo.monedero.model;

import edu.co.uniquindio.poo.monedero.exceptions.CampoVacioException;
import edu.co.uniquindio.poo.monedero.exceptions.EmailInvalidoException;
import edu.co.uniquindio.poo.monedero.exceptions.EntidadNoEncontradaException;
import edu.co.uniquindio.poo.monedero.exceptions.PuntosInsuficientesException;
import edu.co.uniquindio.poo.monedero.exceptions.TelefonoInvalidoException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Cliente {
    private int idCliente;
    private String nombre;
    private String email;
    private String telefono;
    private List<Monedero> monederos;
    private Banco banco;

    // ----- Sistema de puntos -----
    /** Puntos ganados (netos de reversiones). Determinan el rango. */
    private int puntosAcumulados;
    /** Puntos que se pueden canjear (acumulados - canjeados). */
    private int puntosDisponibles;
    private Rango rango;
    private final List<RegistroPuntos> historialPuntos;
    private List<Beneficio> beneficiosCanjeados;

    // ----- Beneficios activos -----
    private LocalDateTime descuentoTransferenciaHasta;
    private LocalDateTime retirosSinCargoHasta;

    // ----- Notificaciones -----
    private CanalNotificacion canalPreferido = CanalNotificacion.EMAIL;
    private final List<Notificacion> notificaciones;

    public Cliente(int id, String nombre, String email, String telefono) {
        validarNombre(nombre);
        validarEmail(email);
        validarTelefono(telefono);

        this.idCliente = id;
        this.nombre = nombre;
        this.email = email;
        this.telefono = telefono;

        this.monederos = new ArrayList<>();
        this.beneficiosCanjeados = new ArrayList<>();
        this.historialPuntos = new ArrayList<>();
        this.notificaciones = new ArrayList<>();
        this.puntosAcumulados = 0;
        this.puntosDisponibles = 0;
        this.rango = Rango.calcularRango(0);
    }

    // =====================================================================
    // Validaciones
    // =====================================================================

    private void validarNombre(String nombre) {
        if (nombre == null || nombre.isBlank()) {
            throw new CampoVacioException("El nombre no puede estar vacío");
        }
    }

    private void validarEmail(String email) {
        if (email == null || email.isBlank()) {
            throw new CampoVacioException("El email no puede estar vacío");
        }
        if (!email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")) {
            throw new EmailInvalidoException("El formato del email es inválido");
        }
    }

    private void validarTelefono(String telefono) {
        if (telefono == null || telefono.isBlank()) {
            throw new CampoVacioException("El teléfono no puede estar vacío");
        }
        if (!telefono.matches("\\d{7,12}")) {
            throw new TelefonoInvalidoException("El teléfono debe ser numérico y de 7 a 12 dígitos");
        }
    }

    // =====================================================================
    // Transacciones
    // =====================================================================

    public void realizarTransaccion(Transaccion t) {
        t.ejecutar();
        actualizarPuntos(t);
    }

    /** Calcula la comisión final de una transacción según rango y beneficios. */
    public double calcularComision(Transaccion t) {
        double base = t.calcularComisionBase();
        return Math.round(t.aplicarBeneficios(this, base) * 100.0) / 100.0;
    }

    /** Suma los puntos de una transacción aplicando el multiplicador del rango. */
    public void actualizarPuntos(Transaccion t) {
        int puntosBase = t.calcularPuntos();
        int puntosFinal = (int) Math.round(puntosBase * rango.getMultiplicadorPuntos());
        t.setPuntosOtorgados(puntosFinal);
        if (puntosFinal > 0) {
            historialPuntos.add(new RegistroPuntos(RegistroPuntos.TipoMovimiento.GANADOS, puntosFinal,
                    t.getTipo() + " #" + t.getIdTransaccion()));
        }
        recalcularPuntos();
    }

    /**
     * Revierte la transacción y descuenta los puntos que generó.
     * Los puntos y el rango se recalculan dinámicamente desde el historial.
     */
    public void revertirTransaccion(Transaccion t) {
        t.revertir();
        if (t.getPuntosOtorgados() > 0) {
            historialPuntos.add(new RegistroPuntos(RegistroPuntos.TipoMovimiento.REVERTIDOS,
                    -t.getPuntosOtorgados(), "Reversión de " + t.getTipo() + " #" + t.getIdTransaccion()));
        }
        recalcularPuntos();
    }

    /** Recalcula puntos acumulados, disponibles y rango a partir del historial (Stream API). */
    public void recalcularPuntos() {
        puntosAcumulados = historialPuntos.stream()
                .filter(r -> r.getTipo() != RegistroPuntos.TipoMovimiento.CANJEADOS)
                .mapToInt(RegistroPuntos::getPuntos)
                .sum();
        puntosDisponibles = historialPuntos.stream()
                .mapToInt(RegistroPuntos::getPuntos)
                .sum();
        rango = Rango.calcularRango(Math.max(0, puntosAcumulados));
    }

    public void canjearPuntos(Beneficio b) {
        if (b == null) {
            throw new CampoVacioException("Seleccione un beneficio");
        }
        if (puntosDisponibles < b.getCostoPuntos()) {
            throw new PuntosInsuficientesException("Puntos insuficientes: tiene " + puntosDisponibles
                    + " y el beneficio cuesta " + b.getCostoPuntos());
        }
        b.aplicar(this); // si falla, no se descuentan puntos
        historialPuntos.add(new RegistroPuntos(RegistroPuntos.TipoMovimiento.CANJEADOS,
                -b.getCostoPuntos(), "Canje: " + b.getDescripcion()));
        beneficiosCanjeados.add(b);
        recalcularPuntos();
    }

    // ----- Beneficios activos -----

    void activarDescuentoTransferencia(int dias) {
        LocalDateTime base = tieneDescuentoTransferencia() ? descuentoTransferenciaHasta : LocalDateTime.now();
        descuentoTransferenciaHasta = base.plusDays(dias);
    }

    void activarRetirosSinCargo(int meses) {
        LocalDateTime base = tieneRetirosSinCargo() ? retirosSinCargoHasta : LocalDateTime.now();
        retirosSinCargoHasta = base.plusMonths(meses);
    }

    public boolean tieneDescuentoTransferencia() {
        return descuentoTransferenciaHasta != null && LocalDateTime.now().isBefore(descuentoTransferenciaHasta);
    }

    public boolean tieneRetirosSinCargo() {
        return retirosSinCargoHasta != null && LocalDateTime.now().isBefore(retirosSinCargoHasta);
    }

    public String getBeneficiosActivos() {
        List<String> activos = new ArrayList<>();
        if (tieneDescuentoTransferencia()) activos.add("10% menos comisión transferencias");
        if (tieneRetirosSinCargo()) activos.add("Retiros sin cargo");
        return activos.isEmpty() ? "Ninguno" : String.join(", ", activos);
    }

    // =====================================================================
    // Consultas
    // =====================================================================

    public double consultarSaldo(Monedero monedero) {
        if (!monederos.contains(monedero)) {
            throw new EntidadNoEncontradaException("El monedero no pertenece al cliente");
        }
        return monedero.getSaldo();
    }

    public double getSaldoTotal() {
        return monederos.stream().mapToDouble(Monedero::getSaldo).sum();
    }

    public List<Transaccion> consultarHistorial(Monedero monedero) {
        if (!monederos.contains(monedero)) {
            throw new EntidadNoEncontradaException("El monedero no pertenece al cliente");
        }
        return Collections.unmodifiableList(monedero.getHistorial());
    }

    /** Transacciones iniciadas por el cliente en cualquiera de sus monederos. */
    public List<Transaccion> obtenerTransaccionesRealizadas() {
        return monederos.stream()
                .flatMap(m -> m.getHistorial().stream())
                .filter(t -> t.getMonedero() != null && t.getMonedero().getDueño() == this)
                .distinct()
                .toList();
    }

    /** Transferencias recibidas desde otros clientes. */
    public List<Transaccion> obtenerTransferenciasRecibidas() {
        return monederos.stream()
                .flatMap(m -> m.getHistorial().stream())
                .filter(t -> t instanceof Transferencia && monederos.contains(t.getMonederoDestino())
                        && t.getMonedero().getDueño() != this)
                .distinct()
                .toList();
    }

    public void agregarMonedero(Monedero monedero) {
        monederos.add(monedero);
    }

    // ----- Notificaciones -----

    void recibirNotificacion(Notificacion n) {
        notificaciones.add(n);
    }

    public List<Notificacion> getNotificaciones() {
        return Collections.unmodifiableList(notificaciones);
    }

    // =====================================================================
    // Getters / Setters
    // =====================================================================

    public int getIdCliente() { return idCliente; }
    public void setIdCliente(int idCliente) { this.idCliente = idCliente; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { validarNombre(nombre); this.nombre = nombre; }

    public String getEmail() { return email; }
    public void setEmail(String email) { validarEmail(email); this.email = email; }

    public String getTelefono() { return telefono; }
    public void setTelefono(String telefono) { validarTelefono(telefono); this.telefono = telefono; }

    public List<Monedero> getMonederos() { return monederos; }
    public void setMonederos(List<Monedero> monederos) { this.monederos = monederos; }

    public int getPuntosAcumulados() { return puntosAcumulados; }
    public int getPuntosDisponibles() { return puntosDisponibles; }
    public Rango getRango() { return rango; }

    public List<RegistroPuntos> getHistorialPuntos() { return Collections.unmodifiableList(historialPuntos); }

    public List<Beneficio> getBeneficiosCanjeados() { return beneficiosCanjeados; }
    public void setBeneficiosCanjeados(List<Beneficio> beneficiosCanjeados) { this.beneficiosCanjeados = beneficiosCanjeados; }

    public CanalNotificacion getCanalPreferido() { return canalPreferido; }
    public void setCanalPreferido(CanalNotificacion canal) {
        if (canal == null) throw new CampoVacioException("Seleccione un canal de notificación");
        this.canalPreferido = canal;
    }

    public Banco getBanco() { return banco; }
    public void setBanco(Banco banco) { this.banco = banco; }

    @Override
    public String toString() {
        return nombre;
    }
}
