package edu.co.uniquindio.poo.monedero.model;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import edu.co.uniquindio.poo.monedero.controller.TransaccionController;
import edu.co.uniquindio.poo.monedero.exceptions.*;
import edu.co.uniquindio.poo.monedero.service.*;
import edu.co.uniquindio.poo.monedero.util.Ordenamiento;

/** Pruebas de las funcionalidades avanzadas del enunciado. */
public class FuncionalidadesAvanzadasTest {

    private TransaccionController tc;
    private ServicioNotificaciones notificaciones;
    private Cliente ana;
    private Cliente beto;
    private Monedero mAna;
    private Monedero mBeto;

    @BeforeEach
    public void setUp() {
        notificaciones = new ServicioNotificaciones();
        tc = new TransaccionController(notificaciones);
        ana = new Cliente(1, "Ana", "ana@test.com", "3001234567");
        beto = new Cliente(2, "Beto", "beto@test.com", "3007654321");
        mAna = new Monedero(1, "Ana-Diario", ana);
        mBeto = new Monedero(2, "Beto-Diario", beto);
        ana.agregarMonedero(mAna);
        beto.agregarMonedero(mBeto);
        mAna.setSaldo(100000);
    }

    //  Puntos y rangos

    @Test
    public void puntosSegunTipoDeTransaccion() {
        tc.ejecutarTransaccion(tc.crearTransaccion("Deposito", 1000, ana, null, mAna));      // 10
        tc.ejecutarTransaccion(tc.crearTransaccion("Retiro", 1000, ana, null, mAna));        // 20
        tc.ejecutarTransaccion(tc.crearTransaccion("Transferencia", 1000, ana, beto, mAna)); // 30
        assertEquals(60, ana.getPuntosAcumulados());
        assertEquals(3, ana.getHistorialPuntos().size());
    }

    @Test
    public void rangosSegunPuntos() {
        assertEquals("Bronce", Rango.calcularRango(500).getNombre());
        assertEquals("Plata", Rango.calcularRango(501).getNombre());
        assertEquals("Plata", Rango.calcularRango(1000).getNombre());
        assertEquals("Oro", Rango.calcularRango(1001).getNombre());
        assertEquals("Oro", Rango.calcularRango(5000).getNombre());
        assertEquals("Platino", Rango.calcularRango(5001).getNombre());
    }

    @Test
    public void revertirTransaccionRecalculaPuntosYSaldo() {
        Transaccion t = tc.ejecutarTransaccion(tc.crearTransaccion("Deposito", 60000, ana, null, mAna));
        assertEquals("Plata", ana.getRango().getNombre()); // 600 pts
        tc.revertirTransaccion(t);
        assertEquals(0, ana.getPuntosAcumulados());
        assertEquals("Bronce", ana.getRango().getNombre());
        assertEquals(100000, mAna.getSaldo());
        assertTrue(t.isRevertida());
        assertThrows(TransaccionNoReversibleException.class, () -> tc.revertirTransaccion(t));
    }

    //  Canje de beneficios

    @Test
    public void canjeSinPuntosLanzaExcepcion() {
        assertThrows(PuntosInsuficientesException.class, () -> ana.canjearPuntos(new BeneficioBonoSaldo()));
    }

    @Test
    public void canjeBonoSaldoDescuentaPuntosYSumaSaldo() {
        mAna.setSaldo(0);
        tc.ejecutarTransaccion(tc.crearTransaccion("Deposito", 100000, ana, null, mAna)); // 1000 pts
        ana.canjearPuntos(new BeneficioBonoSaldo());
        assertEquals(0, ana.getPuntosDisponibles());
        assertEquals(1000, ana.getPuntosAcumulados()); // el rango no baja por canjear
        assertEquals(100050, mAna.getSaldo());
    }

    @Test
    public void retirosSinCargoEliminaComision() {
        Retiro r1 = (Retiro) tc.ejecutarTransaccion(tc.crearTransaccion("Retiro", 10000, ana, null, mAna));
        assertEquals(50, r1.getComision()); // 0,5 %
        mAna.setSaldo(100000);
        tc.ejecutarTransaccion(tc.crearTransaccion("Deposito", 50000, ana, null, mAna)); // +500 pts
        ana.canjearPuntos(new BeneficioRetirosSinCargo());
        Transaccion r2 = tc.ejecutarTransaccion(tc.crearTransaccion("Retiro", 10000, ana, null, mAna));
        assertEquals(0, r2.getComision());
    }

    @Test
    public void comisionTransferenciaConDescuentoDeRangoYBeneficio() {
        Transaccion t1 = tc.ejecutarTransaccion(tc.crearTransaccion("Transferencia", 10000, ana, beto, mAna));
        assertEquals(100, t1.getComision()); // 1 % en Bronce
        ana.canjearPuntos(new BeneficioDescuentoTransferencia()); // tiene 300 pts
        Transaccion t2 = tc.ejecutarTransaccion(tc.crearTransaccion("Transferencia", 10000, ana, beto, mAna));
        assertEquals(90, t2.getComision()); // 10 % menos
    }

    //  Multimonedero

    @Test
    public void transferenciaEntreMonederosPropiosSinComision() {
        Monedero ahorros = new Monedero(3, "Ana-Ahorros", ana);
        ana.agregarMonedero(ahorros);
        Transaccion t = tc.ejecutarTransaccion(
                tc.crearTransaccion("Transferencia", 30000, ana, ana, mAna, ahorros));
        assertEquals(0, t.getComision());
        assertEquals(70000, mAna.getSaldo());
        assertEquals(30000, ahorros.getSaldo());
        assertEquals(100000, ana.getSaldoTotal());
    }

    // Atípicas

    @Test
    public void detectaTransaccionAtipica() {
        for (double m : new double[]{1000, 1200, 900, 1100, 1000}) {
            tc.ejecutarTransaccion(tc.crearTransaccion("Retiro", m, ana, null, mAna));
        }
        Transaccion grande = tc.ejecutarTransaccion(tc.crearTransaccion("Retiro", 20000, ana, null, mAna));
        assertTrue(grande.isSospechosa());
        assertTrue(ana.getNotificaciones().stream()
                .anyMatch(n -> n.getTipo().equals(ServicioNotificaciones.SOSPECHOSA)));
    }

    @Test
    public void estadisticasDelDetector() {
        DetectorTransaccionesAtipicas d = new DetectorTransaccionesAtipicas(notificaciones);
        List<Transaccion> lista = List.of(new Deposito(mAna, ana, 2), new Deposito(mAna, ana, 4),
                new Deposito(mAna, ana, 4), new Deposito(mAna, ana, 4), new Deposito(mAna, ana, 5),
                new Deposito(mAna, ana, 5), new Deposito(mAna, ana, 7), new Deposito(mAna, ana, 9));
        DetectorTransaccionesAtipicas.Estadisticas e = d.calcularEstadisticas(lista);
        assertEquals(5.0, e.promedio(), 1e-9);
        assertEquals(2.0, e.desviacion(), 1e-9);
        assertEquals(9.0, e.umbral(), 1e-9);
    }

    //  Programadas y ordenamiento

    @Test
    public void mergeSortOrdenaCorrectamente() {
        List<Integer> r = Ordenamiento.mergeSort(List.of(5, 3, 9, 1, 7, 2), Comparator.naturalOrder());
        assertEquals(List.of(1, 2, 3, 5, 7, 9), r);
    }

    @Test
    public void programadasSeProcesanEnOrdenDeFecha() {
        ProgramadorTransacciones p = new ProgramadorTransacciones(tc, notificaciones);
        LocalDateTime base = LocalDateTime.now().plusMinutes(5);
        p.programar("Deposito", 300, ana, mAna, null, null, base.plusDays(3), Frecuencia.UNICA);
        p.programar("Deposito", 100, ana, mAna, null, null, base.plusDays(1), Frecuencia.UNICA);
        p.programar("Deposito", 200, ana, mAna, null, null, base.plusDays(2), Frecuencia.UNICA);

        List<TransaccionProgramada> orden = p.obtenerOrdenadas();
        assertEquals(100.0, orden.get(0).getMonto());
        assertEquals(300.0, orden.get(2).getMonto());

        p.procesarPendientes(base.plusDays(4));
        List<Double> montos = mAna.getHistorial().stream().map(Transaccion::getMonto).toList();
        assertEquals(List.of(100.0, 200.0, 300.0), montos);
        assertFalse(orden.get(0).isActiva());
    }

    @Test
    public void programadaRecurrenteSeReprograma() {
        ProgramadorTransacciones p = new ProgramadorTransacciones(tc, notificaciones);
        LocalDateTime inicio = LocalDateTime.now().plusMinutes(5);
        TransaccionProgramada tp = p.programar("Transferencia", 1000, ana, mAna, beto, null, inicio, Frecuencia.SEMANAL);
        p.procesarPendientes(inicio.plusDays(15)); // vencida 3 veces: día 0, 7 y 14
        assertEquals(3, tp.getEjecuciones());
        assertEquals(3000, mBeto.getSaldo());
        assertTrue(tp.isActiva());
    }

    @Test
    public void programarEnElPasadoLanzaExcepcion() {
        ProgramadorTransacciones p = new ProgramadorTransacciones(tc, notificaciones);
        assertThrows(FechaProgramacionInvalidaException.class, () -> p.programar("Deposito", 100, ana, mAna,
                null, null, LocalDateTime.now().minusDays(1), Frecuencia.UNICA));
    }

    //  Reportes y alertas

    @Test
    public void reporteAgrupaGastosPorTipo() {
        tc.ejecutarTransaccion(tc.crearTransaccion("Retiro", 1000, ana, null, mAna));
        tc.ejecutarTransaccion(tc.crearTransaccion("Retiro", 2000, ana, null, mAna));
        tc.ejecutarTransaccion(tc.crearTransaccion("Transferencia", 5000, ana, beto, mAna));
        AnalizadorGastos a = new AnalizadorGastos();
        assertEquals(3000.0, a.gastoPorTipo(ana).get("Retiro"), 0.001);
        assertEquals(5000.0, a.gastoPorTipo(ana).get("Transferencia"), 0.001);
        assertTrue(a.generarReporte(ana).contains("REPORTE"));
        assertEquals(5000.0, a.totalIngresos(beto));
    }

    @Test
    public void alertaSaldoBajoPorWhatsApp() {
        ana.setCanalPreferido(CanalNotificacion.WHATSAPP);
        tc.ejecutarTransaccion(tc.crearTransaccion("Retiro", 90000, ana, null, mAna));
        Notificacion n = ana.getNotificaciones().get(ana.getNotificaciones().size() - 1);
        assertEquals("WhatsApp", n.getCanal());
        assertEquals(ServicioNotificaciones.SALDO_BAJO, n.getTipo());
    }
}
