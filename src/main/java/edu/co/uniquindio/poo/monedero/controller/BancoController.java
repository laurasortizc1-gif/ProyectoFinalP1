package edu.co.uniquindio.poo.monedero.controller;

import edu.co.uniquindio.poo.monedero.exceptions.CampoVacioException;
import edu.co.uniquindio.poo.monedero.exceptions.ClienteYaExisteException;
import edu.co.uniquindio.poo.monedero.exceptions.MonederoYaExisteException;
import edu.co.uniquindio.poo.monedero.exceptions.NombreDuplicadoException;
import edu.co.uniquindio.poo.monedero.model.*;
import edu.co.uniquindio.poo.monedero.service.AnalizadorGastos;
import edu.co.uniquindio.poo.monedero.service.DetectorTransaccionesAtipicas;
import edu.co.uniquindio.poo.monedero.service.ProgramadorTransacciones;
import edu.co.uniquindio.poo.monedero.service.ServicioNotificaciones;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

public class BancoController {

    private static final BancoController instance = new BancoController();

    private final Banco banco;
    private final ServicioNotificaciones notificaciones;
    private final TransaccionController transaccionController;
    private final ProgramadorTransacciones programador;
    private final AnalizadorGastos analizador;

    private BancoController() {
        this.banco = new Banco("Banco Virtual");
        this.notificaciones = new ServicioNotificaciones();
        this.transaccionController = new TransaccionController(notificaciones);
        this.programador = new ProgramadorTransacciones(transaccionController, notificaciones);
        this.analizador = new AnalizadorGastos();
        cargarDatosIniciales();  // ← Datos quemados aquí
    }

    public static BancoController getInstance() {
        return instance;
    }

    private void cargarDatosIniciales() {

        Cliente c1 = new Cliente(1, "Alice", "alice@mail.com", "1111111");
        Cliente c2 = new Cliente(2, "Bob", "bob@mail.com", "2222222");
        Cliente c3 = new Cliente(3, "Charlie", "charlie@mail.com", "3333333");

        banco.registrarCliente(c1);
        banco.registrarCliente(c2);
        banco.registrarCliente(c3);

        Monedero m1 = new Monedero(1, "Monedero-Alice", c1);
        Monedero m2 = new Monedero(2, "Monedero-Bob", c2);
        Monedero m3 = new Monedero(3, "Monedero-Charlie", c3);

        c1.agregarMonedero(m1);
        c2.agregarMonedero(m2);
        c3.agregarMonedero(m3);

        banco.registrarMonedero(m1);
        banco.registrarMonedero(m2);
        banco.registrarMonedero(m3);

        m1.setSaldo(150000);
        m2.setSaldo(87000);
        m3.setSaldo(50000);

        // Multimonedero: Alice tiene además un monedero de ahorros
        Monedero ahorrosAlice = new Monedero(4, "Ahorros-Alice", c1);
        c1.agregarMonedero(ahorrosAlice);
        banco.registrarMonedero(ahorrosAlice);

        // Historial de ejemplo para reportes y detección de atípicas
        LocalDateTime hoy = LocalDateTime.now();
        sembrar(new Deposito(m1, c1, 20000), hoy.minusDays(60));
        sembrar(new Retiro(m1, c1, 15000), hoy.minusDays(52));
        sembrar(new Deposito(m1, c1, 18000), hoy.minusDays(45));
        sembrar(new Retiro(m1, c1, 12000), hoy.minusDays(33));
        sembrar(crearTransferencia(c1, m1, c2, 10000), hoy.minusDays(20));
        sembrar(new Retiro(m1, c1, 16000), hoy.minusDays(10));
        sembrar(new Retiro(m1, c1, 95000), hoy.minusDays(2)); // ← atípica

        // Transacción programada de ejemplo: Bob transfiere a Charlie cada semana
        programador.programar("Transferencia", 5000, c2, m2, c3, m3, hoy.plusDays(1), Frecuencia.SEMANAL);
    }

    private Transferencia crearTransferencia(Cliente origen, Monedero m, Cliente destino, double monto) {
        Transferencia t = new Transferencia();
        t.setClienteOrigen(origen);
        t.setClienteDestino(destino);
        t.setMonedero(m);
        t.setMonto(monto);
        return t;
    }

    private void sembrar(Transaccion t, LocalDateTime fecha) {
        transaccionController.ejecutarTransaccion(t);
        t.setFechaCreacion(fecha);
    }

    // ------------------- Puntos / reportes / programadas -------------------

    public void canjearBeneficio(Cliente cliente, Beneficio beneficio) {
        cliente.canjearPuntos(beneficio);
        notificaciones.notificar(cliente, ServicioNotificaciones.BENEFICIO,
                "Canjeaste: " + beneficio.getDescripcion() + ". Puntos disponibles: " + cliente.getPuntosDisponibles());
    }

    /** Clientes ordenados de mayor a menor puntaje (Stream API). */
    public List<Cliente> rankingClientes() {
        return banco.getListaClientes().stream()
                .sorted(Comparator.comparingInt(Cliente::getPuntosAcumulados).reversed()
                        .thenComparing(Cliente::getNombre))
                .toList();
    }

    public TransaccionController getTransaccionController() { return transaccionController; }
    public ProgramadorTransacciones getProgramador() { return programador; }
    public AnalizadorGastos getAnalizador() { return analizador; }
    public DetectorTransaccionesAtipicas getDetector() { return transaccionController.getDetector(); }
    public ServicioNotificaciones getNotificaciones() { return notificaciones; }

    public String registrarCliente(Cliente cliente) {
        try {
            if (banco.buscarCliente(cliente.getEmail()) != null) {
                throw new ClienteYaExisteException("Ya existe un cliente registrado con este correo");
            }
            boolean nombreRepetido = banco.getListaClientes().stream()
                    .anyMatch(c -> c.getNombre().equalsIgnoreCase(cliente.getNombre()));
            if (nombreRepetido) {
                throw new NombreDuplicadoException("Ya existe un cliente con el nombre: " + cliente.getNombre());
            }
            banco.getListaClientes().add(cliente);
            return "Cliente registrado exitosamente";
        } catch (RuntimeException ex) {
            return "Error: " + ex.getMessage();
        }
    }

    public Cliente buscarCliente(String email) {
        return banco.buscarCliente(email);
    }

    public String actualizarCliente(String email, String nuevoNombre, String nuevoTelefono) {
        if (nuevoNombre == null || nuevoNombre.isBlank())
            throw new CampoVacioException("El nombre no puede estar vacío");
        if (nuevoTelefono == null || nuevoTelefono.isBlank())
            throw new CampoVacioException("El teléfono no puede estar vacío");
        Cliente actual = banco.buscarCliente(email);
        if (actual != null && !actual.getNombre().equalsIgnoreCase(nuevoNombre)) {
            for (Cliente c : banco.getListaClientes()) {
                if (c.getNombre().equalsIgnoreCase(nuevoNombre)) {
                    throw new NombreDuplicadoException("Ya existe un cliente con el nombre: " + nuevoNombre);
                }
            }
        }
        return banco.actualizarCliente(email, nuevoNombre, nuevoTelefono);
    }


    public String actualizarMonedero(String nombreAnterior, String nuevoNombre) {
        if (nuevoNombre == null || nuevoNombre.isBlank())
            throw new CampoVacioException("El nuevo nombre no puede estar vacío");
        Monedero existente = banco.buscarMonedero(nuevoNombre);
        if (existente != null && !nombreAnterior.equalsIgnoreCase(nuevoNombre)) {
            throw new MonederoYaExisteException("Ya existe un monedero con el nombre: " + nuevoNombre);
        }
        return banco.actualizarMonedero(nombreAnterior, nuevoNombre);
    }



    public String eliminarCliente(String email) {
        return banco.eliminarCliente(email);
    }

    public List<Cliente> listarClientes() {
        return banco.getListaClientes();
    }

    public String registrarMonederoParaCliente(Monedero monedero, Cliente cliente) {
        if (banco.buscarMonedero(monedero.getNombre()) != null) {
            throw new MonederoYaExisteException("Ya existe un monedero con el nombre: " + monedero.getNombre());
        }
        for (Monedero m : cliente.getMonederos()) {
            if (m.getNombre().equalsIgnoreCase(monedero.getNombre())) {
                throw new NombreDuplicadoException("Este cliente ya tiene un monedero llamado: " + monedero.getNombre());
            }
        }
        cliente.agregarMonedero(monedero);
        return banco.registrarMonedero(monedero);
    }

    public String eliminarMonedero(String nombre) {
        Monedero m = banco.buscarMonedero(nombre);
        if (m != null && m.getDueño() != null) {
            m.getDueño().getMonederos().remove(m);
        }
        return banco.eliminarMonedero(nombre);
    }

    public List<Monedero> listarMonederos() {
        return banco.getListaMonederos();
    }

    public String getNombreBanco() {
        return banco.getNombre();
    }

    public Banco getBanco() {
        return banco;
    }
}
