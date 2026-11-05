package edu.co.uniquindio.poo.monedero.viewController;

import edu.co.uniquindio.poo.monedero.app.SceneLoader;
import edu.co.uniquindio.poo.monedero.controller.BancoController;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import edu.co.uniquindio.poo.monedero.model.Cliente;
import edu.co.uniquindio.poo.monedero.model.Monedero;
import edu.co.uniquindio.poo.monedero.model.Transaccion;

import java.time.format.DateTimeFormatter;

public class MonederoViewController {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @FXML private Label lblNombreMonedero;
    @FXML private Label lblSaldo;

    @FXML private TableView<Transaccion> tablaHistorial;
    @FXML private TableColumn<Transaccion, Number> colIdTrans;
    @FXML private TableColumn<Transaccion, String> colTipoTrans;
    @FXML private TableColumn<Transaccion, Number> colMontoTrans;
    @FXML private TableColumn<Transaccion, Number> colComisionTrans;
    @FXML private TableColumn<Transaccion, Number> colPuntosTrans;
    @FXML private TableColumn<Transaccion, String> colFechaTrans;
    @FXML private TableColumn<Transaccion, String> colEstadoTrans;

    private Monedero monedero;
    private Cliente clienteDueño;

    @FXML
    public void initialize() {
        colIdTrans.setCellValueFactory(t -> new SimpleIntegerProperty(t.getValue().getIdTransaccion()));
        colTipoTrans.setCellValueFactory(t -> new SimpleStringProperty(describirTipo(t.getValue())));
        colMontoTrans.setCellValueFactory(t -> new SimpleDoubleProperty(t.getValue().getMonto()));
        colComisionTrans.setCellValueFactory(t -> new SimpleDoubleProperty(t.getValue().getComision()));
        colPuntosTrans.setCellValueFactory(t -> new SimpleIntegerProperty(t.getValue().getPuntosOtorgados()));
        colFechaTrans.setCellValueFactory(
                t -> new SimpleStringProperty(t.getValue().getFechaCreacion().format(FMT)));
        colEstadoTrans.setCellValueFactory(t -> new SimpleStringProperty(t.getValue().getEstado()));

        // Resalta las transacciones sospechosas y revertidas
        tablaHistorial.setRowFactory(tv -> new TableRow<>() {
            @Override
            protected void updateItem(Transaccion t, boolean empty) {
                super.updateItem(t, empty);
                getStyleClass().removeAll("fila-sospechosa", "fila-revertida");
                if (!empty && t != null) {
                    if (t.isRevertida()) getStyleClass().add("fila-revertida");
                    else if (t.isSospechosa()) getStyleClass().add("fila-sospechosa");
                }
            }
        });
    }

    /** Una transferencia que llega a este monedero se muestra como "recibida". */
    private String describirTipo(Transaccion t) {
        if (t.getMonedero() != monedero) {
            return "Transferencia recibida";
        }
        if (t.getMonederoDestino() != null && t.getMonederoDestino().getDueño() == clienteDueño) {
            return "Transferencia a " + t.getMonederoDestino().getNombre();
        }
        return t.getTipo();
    }

    public void cargarMonedero(Monedero m, Cliente dueño) {
        this.monedero = m;
        this.clienteDueño = dueño;
        refrescar();
    }

    private void refrescar() {
        lblNombreMonedero.setText(monedero.getNombre());
        lblSaldo.setText(String.format("$%,.2f%s", monedero.getSaldo(),
                monedero.tieneSaldoBajo() ? "  ⚠ saldo bajo" : ""));
        tablaHistorial.getItems().setAll(monedero.getHistorial());
        tablaHistorial.refresh();
    }

    @FXML
    private void revertirTransaccion() {
        Transaccion seleccionada = tablaHistorial.getSelectionModel().getSelectedItem();
        if (seleccionada == null) {
            new Alert(Alert.AlertType.WARNING, "Seleccione una transacción").show();
            return;
        }
        if (seleccionada.getMonedero() != monedero) {
            new Alert(Alert.AlertType.WARNING,
                    "Solo el monedero que originó la transferencia puede revertirla").show();
            return;
        }
        try {
            BancoController.getInstance().getTransaccionController().revertirTransaccion(seleccionada);
            new Alert(Alert.AlertType.INFORMATION, "Transacción revertida. Puntos descontados: "
                    + seleccionada.getPuntosOtorgados()).show();
            refrescar();
        } catch (RuntimeException e) {
            new Alert(Alert.AlertType.ERROR, e.getMessage()).show();
        }
    }

    @FXML
    private void abrirTransaccion() {
        TransaccionViewController controller =
                (TransaccionViewController) SceneLoader.cargarVista(
                        "edu/co/uniquindio/poo/monedero/transaccion.fxml"
                );

        controller.cargarDatos(monedero, clienteDueño);
    }

    @FXML
    private void volver() {
        ClienteViewController controller =
                (ClienteViewController) SceneLoader.cargarVista(
                        "edu/co/uniquindio/poo/monedero/cliente.fxml"
                );

        controller.cargarCliente(clienteDueño);
    }
}
