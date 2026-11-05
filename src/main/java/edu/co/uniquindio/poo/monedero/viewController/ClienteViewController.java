package edu.co.uniquindio.poo.monedero.viewController;

import edu.co.uniquindio.poo.monedero.app.SceneLoader;
import edu.co.uniquindio.poo.monedero.controller.BancoController;
import edu.co.uniquindio.poo.monedero.model.*;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class ClienteViewController {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    // ----- Resumen -----
    @FXML private Label lblNombre;
    @FXML private Label lblEmail;
    @FXML private Label lblTelefono;
    @FXML private Label lblPuntos;
    @FXML private Label lblPuntosDisponibles;
    @FXML private Label lblRango;
    @FXML private Label lblBeneficiosRango;
    @FXML private Label lblSaldoTotal;
    @FXML private ComboBox<CanalNotificacion> cbCanal;

    @FXML private TableView<Monedero> tablaMonederos;
    @FXML private TableColumn<Monedero, String> colNombreMonedero;
    @FXML private TableColumn<Monedero, Number> colSaldoMonedero;

    // ----- Puntos y beneficios -----
    @FXML private ComboBox<Beneficio> cbBeneficios;
    @FXML private Label lblBeneficiosActivos;
    @FXML private TableView<RegistroPuntos> tablaPuntos;
    @FXML private TableColumn<RegistroPuntos, String> colFechaPuntos;
    @FXML private TableColumn<RegistroPuntos, String> colTipoPuntos;
    @FXML private TableColumn<RegistroPuntos, String> colDescPuntos;
    @FXML private TableColumn<RegistroPuntos, Number> colCantPuntos;

    // ----- Programadas -----
    @FXML private TableView<TransaccionProgramada> tablaProgramadas;
    @FXML private TableColumn<TransaccionProgramada, String> colProgFecha;
    @FXML private TableColumn<TransaccionProgramada, String> colProgTipo;
    @FXML private TableColumn<TransaccionProgramada, Number> colProgMonto;
    @FXML private TableColumn<TransaccionProgramada, String> colProgFrecuencia;
    @FXML private TableColumn<TransaccionProgramada, String> colProgDestino;
    @FXML private TableColumn<TransaccionProgramada, String> colProgEstado;
    @FXML private TableColumn<TransaccionProgramada, String> colProgResultado;

    // ----- Reportes -----
    @FXML private TextArea txtReporte;
    @FXML private BarChart<String, Number> graficoGastos;

    // ----- Notificaciones -----
    @FXML private ListView<String> listaNotificaciones;

    private Cliente cliente;
    private final BancoController banco = BancoController.getInstance();

    @FXML
    public void initialize() {
        colNombreMonedero.setCellValueFactory(m -> new SimpleStringProperty(m.getValue().getNombre()));
        colSaldoMonedero.setCellValueFactory(m -> new SimpleDoubleProperty(m.getValue().getSaldo()));

        colFechaPuntos.setCellValueFactory(r -> new SimpleStringProperty(r.getValue().getFecha().format(FMT)));
        colTipoPuntos.setCellValueFactory(r -> new SimpleStringProperty(r.getValue().getTipo().name()));
        colDescPuntos.setCellValueFactory(r -> new SimpleStringProperty(r.getValue().getDescripcion()));
        colCantPuntos.setCellValueFactory(r -> new SimpleIntegerProperty(r.getValue().getPuntos()));

        colProgFecha.setCellValueFactory(p -> new SimpleStringProperty(p.getValue().getProximaEjecucion().format(FMT)));
        colProgTipo.setCellValueFactory(p -> new SimpleStringProperty(p.getValue().getTipo()));
        colProgMonto.setCellValueFactory(p -> new SimpleDoubleProperty(p.getValue().getMonto()));
        colProgFrecuencia.setCellValueFactory(p -> new SimpleStringProperty(p.getValue().getFrecuencia().toString()));
        colProgDestino.setCellValueFactory(p -> new SimpleStringProperty(
                p.getValue().getClienteDestino() != null ? p.getValue().getClienteDestino().getNombre() : "-"));
        colProgEstado.setCellValueFactory(p -> new SimpleStringProperty(p.getValue().getEstado()));
        colProgResultado.setCellValueFactory(p -> new SimpleStringProperty(p.getValue().getUltimoResultado()));

        cbBeneficios.getItems().setAll(Beneficio.catalogo());
        cbCanal.getItems().setAll(CanalNotificacion.values());
        cbCanal.valueProperty().addListener((obs, viejo, nuevo) -> {
            if (cliente != null && nuevo != null) cliente.setCanalPreferido(nuevo);
        });
    }

    public void cargarCliente(Cliente cliente) {
        this.cliente = cliente;
        cbCanal.setValue(cliente.getCanalPreferido());
        refrescar();
        generarReporte();
    }

    private void refrescar() {
        lblNombre.setText(cliente.getNombre());
        lblEmail.setText(cliente.getEmail());
        lblTelefono.setText(cliente.getTelefono());
        lblPuntos.setText(String.valueOf(cliente.getPuntosAcumulados()));
        lblPuntosDisponibles.setText(String.valueOf(cliente.getPuntosDisponibles()));
        lblRango.setText(cliente.getRango().getNombre());
        lblBeneficiosRango.setText(cliente.getRango().getDescripcionBeneficios());
        lblSaldoTotal.setText(String.format("$%,.2f", cliente.getSaldoTotal()));
        lblBeneficiosActivos.setText("Beneficios activos: " + cliente.getBeneficiosActivos());

        tablaMonederos.getItems().setAll(cliente.getMonederos());
        tablaPuntos.getItems().setAll(cliente.getHistorialPuntos());
        tablaProgramadas.getItems().setAll(banco.getProgramador().obtenerDeCliente(cliente));

        // Notificaciones de la más reciente a la más antigua
        List<Notificacion> notis = cliente.getNotificaciones();
        listaNotificaciones.getItems().setAll(
                notis.reversed().stream().map(Notificacion::toString).toList());
    }

    // ---------------- Puntos ----------------

    @FXML
    private void canjearBeneficio() {
        try {
            banco.canjearBeneficio(cliente, cbBeneficios.getValue());
            mostrar(Alert.AlertType.INFORMATION, "Beneficio canjeado: " + cbBeneficios.getValue().getDescripcion());
            refrescar();
        } catch (RuntimeException e) {
            mostrar(Alert.AlertType.ERROR, e.getMessage());
        }
    }

    // ---------------- Programadas ----------------

    @FXML
    private void cancelarProgramada() {
        try {
            banco.getProgramador().cancelar(tablaProgramadas.getSelectionModel().getSelectedItem());
            refrescar();
        } catch (RuntimeException e) {
            mostrar(Alert.AlertType.ERROR, e.getMessage());
        }
    }

    @FXML
    private void procesarProgramadas() {
        List<String> resultados = banco.getProgramador().procesarPendientes(LocalDateTime.now());
        mostrar(Alert.AlertType.INFORMATION, resultados.isEmpty()
                ? "No hay transacciones programadas pendientes por ejecutar."
                : String.join("\n", resultados));
        refrescar();
    }

    // ---------------- Reportes ----------------

    @FXML
    private void generarReporte() {
        txtReporte.setText(banco.getAnalizador().generarReporte(cliente));

        XYChart.Series<String, Number> serie = new XYChart.Series<>();
        banco.getAnalizador().gastoPorMes(cliente)
                .forEach((mes, total) -> serie.getData().add(new XYChart.Data<>(mes.toString(), total)));
        graficoGastos.getData().clear();
        graficoGastos.getData().add(serie);
    }

    @FXML
    private void analizarAtipicas() {
        List<Transaccion> atipicas = banco.getDetector().analizarHistorial(cliente);
        StringBuilder sb = new StringBuilder();
        var stats = banco.getDetector().calcularEstadisticas(
                cliente.obtenerTransaccionesRealizadas().stream().filter(t -> !t.isRevertida()).toList());
        sb.append(String.format("ANÁLISIS DE TRANSACCIONES ATÍPICAS%n"));
        sb.append(String.format("Transacciones analizadas: %d%n", stats.cantidad()));
        sb.append(String.format("Promedio: $%,.2f%nDesviación estándar: $%,.2f%n", stats.promedio(), stats.desviacion()));
        sb.append(String.format("Umbral (promedio + 2σ): $%,.2f%n%n", stats.umbral()));
        if (stats.cantidad() < 5) {
            sb.append("Se necesitan al menos 5 transacciones para un análisis confiable.");
        } else if (atipicas.isEmpty()) {
            sb.append("No se encontraron transacciones atípicas.");
        } else {
            sb.append("Transacciones marcadas como sospechosas:\n");
            atipicas.forEach(t -> sb.append("  ⚠ ").append(t).append(" — ")
                    .append(t.getFechaCreacion().format(FMT)).append("\n"));
        }
        txtReporte.setText(sb.toString());
        refrescar();
    }

    // ---------------- Navegación ----------------

    @FXML
    private void abrirMonedero() {
        Monedero monedero = tablaMonederos.getSelectionModel().getSelectedItem();

        if (monedero == null) {
            new Alert(Alert.AlertType.WARNING, "Seleccione un monedero").show();
            return;
        }

        MonederoViewController controller = (MonederoViewController) SceneLoader.cargarVista(
                "edu/co/uniquindio/poo/monedero/monedero.fxml"
        );

        controller.cargarMonedero(monedero, cliente);
    }

    @FXML
    private void volver() {
        SceneLoader.cargarVista("edu/co/uniquindio/poo/monedero/banco.fxml");
    }

    private void mostrar(Alert.AlertType tipo, String texto) {
        Alert a = new Alert(tipo, texto);
        a.setHeaderText(null);
        a.show();
    }
}
