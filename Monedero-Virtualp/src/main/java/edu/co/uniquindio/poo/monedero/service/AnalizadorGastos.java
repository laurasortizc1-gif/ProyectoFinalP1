package edu.co.uniquindio.poo.monedero.service;

import java.time.DayOfWeek;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.Comparator;
import java.util.DoubleSummaryStatistics;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

import edu.co.uniquindio.poo.monedero.model.Cliente;
import edu.co.uniquindio.poo.monedero.model.Transaccion;

/** Reportes de patrones de gasto construidos con Stream API. */
public class AnalizadorGastos {

    private List<Transaccion> gastos(Cliente c) {
        return c.obtenerTransaccionesRealizadas().stream()
                .filter(Transaccion::esGasto)
                .filter(t -> !t.isRevertida())
                .toList();
    }

    /** Gasto total agrupado por tipo (Retiro / Transferencia). */
    public Map<String, Double> gastoPorTipo(Cliente c) {
        return gastos(c).stream()
                .collect(Collectors.groupingBy(Transaccion::getTipo, TreeMap::new,
                        Collectors.summingDouble(Transaccion::getMonto)));
    }

    /** Gasto total por mes, ordenado cronológicamente. */
    public Map<YearMonth, Double> gastoPorMes(Cliente c) {
        return gastos(c).stream()
                .collect(Collectors.groupingBy(t -> YearMonth.from(t.getFechaCreacion()), TreeMap::new,
                        Collectors.summingDouble(Transaccion::getMonto)));
    }

    /** Gasto total por monedero. */
    public Map<String, Double> gastoPorMonedero(Cliente c) {
        return gastos(c).stream()
                .collect(Collectors.groupingBy(t -> t.getMonedero().getNombre(), TreeMap::new,
                        Collectors.summingDouble(Transaccion::getMonto)));
    }

    public DoubleSummaryStatistics estadisticasGasto(Cliente c) {
        return gastos(c).stream().mapToDouble(Transaccion::getMonto).summaryStatistics();
    }

    public double totalComisiones(Cliente c) {
        return c.obtenerTransaccionesRealizadas().stream()
                .filter(t -> !t.isRevertida())
                .mapToDouble(Transaccion::getComision)
                .sum();
    }

    public double totalIngresos(Cliente c) {
        double depositos = c.obtenerTransaccionesRealizadas().stream()
                .filter(t -> !t.esGasto() && !t.isRevertida())
                .mapToDouble(Transaccion::getMonto)
                .sum();
        double recibidas = c.obtenerTransferenciasRecibidas().stream()
                .filter(t -> !t.isRevertida())
                .mapToDouble(Transaccion::getMonto)
                .sum();
        return depositos + recibidas;
    }

    public List<Transaccion> mayoresGastos(Cliente c, int n) {
        return gastos(c).stream()
                .sorted(Comparator.comparingDouble(Transaccion::getMonto).reversed())
                .limit(n)
                .toList();
    }

    /** Día de la semana en el que el cliente más gasta. */
    public String diaDeMayorGasto(Cliente c) {
        return gastos(c).stream()
                .collect(Collectors.groupingBy(t -> t.getFechaCreacion().getDayOfWeek(),
                        Collectors.summingDouble(Transaccion::getMonto)))
                .entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .map(d -> d.getDisplayName(TextStyle.FULL, Locale.of("es", "CO")))
                .orElse("Sin datos");
    }

    public String generarReporte(Cliente c) {
        DoubleSummaryStatistics st = estadisticasGasto(c);
        double ingresos = totalIngresos(c);
        StringBuilder sb = new StringBuilder();
        sb.append("REPORTE DE PATRONES DE GASTO — ").append(c.getNombre()).append("\n");
        sb.append("Rango: ").append(c.getRango().getNombre())
          .append(" | Puntos acumulados: ").append(c.getPuntosAcumulados()).append("\n\n");

        sb.append(String.format("Ingresos totales:      $%,.2f%n", ingresos));
        sb.append(String.format("Gastos totales:        $%,.2f (%d operaciones)%n", st.getSum(), st.getCount()));
        sb.append(String.format("Comisiones pagadas:    $%,.2f%n", totalComisiones(c)));
        if (st.getCount() > 0) {
            sb.append(String.format("Gasto promedio:        $%,.2f%n", st.getAverage()));
            sb.append(String.format("Gasto máximo:          $%,.2f%n", st.getMax()));
            if (ingresos > 0) {
                sb.append(String.format("Gasto / ingresos:      %.1f %%%n", st.getSum() * 100 / ingresos));
            }
            sb.append("Día de mayor gasto:    ").append(diaDeMayorGasto(c)).append("\n");
        }

        sb.append("\nGasto por tipo:\n");
        gastoPorTipo(c).forEach((k, v) -> sb.append(String.format("  • %-15s $%,.2f%n", k, v)));
        sb.append("\nGasto por monedero:\n");
        gastoPorMonedero(c).forEach((k, v) -> sb.append(String.format("  • %-15s $%,.2f%n", k, v)));
        sb.append("\nGasto por mes:\n");
        gastoPorMes(c).forEach((k, v) -> sb.append(String.format("  • %-15s $%,.2f%n", k, v)));
        sb.append("\nTop 3 gastos:\n");
        mayoresGastos(c, 3).forEach(t -> sb.append("  • ").append(t).append("\n"));
        return sb.toString();
    }
}
