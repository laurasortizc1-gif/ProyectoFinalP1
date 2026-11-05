package edu.co.uniquindio.poo.monedero.viewController;

import edu.co.uniquindio.poo.monedero.app.SceneLoader;
import edu.co.uniquindio.poo.monedero.controller.TransaccionController;
import edu.co.uniquindio.poo.monedero.controller.BancoController;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import edu.co.uniquindio.poo.monedero.model.*;
import edu.co.uniquindio.poo.monedero.exceptions.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;

public class TransaccionViewController {

    @FXML private TextField txtMonto;
    @FXML private ComboBox<String> cbTipoTransaccion;
    @FXML private ComboBox<Cliente> cbDestino;
    @FXML private ComboBox<Monedero> cbMonederoDestino;
    @FXML private CheckBox chkProgramar;
    @FXML private DatePicker dpFecha;
    @FXML private TextField txtHora;
    @FXML private ComboBox<Frecuencia> cbFrecuencia;
    @FXML private Label lblInfo;

    private Monedero monederoActual;
    private Cliente clienteOrigen;

    private final TransaccionController transaccionController =
            BancoController.getInstance().getTransaccionController();

    @FXML
    public void initialize() {
        cbTipoTransaccion.getItems().addAll("Deposito", "Retiro", "Transferencia");
        cbFrecuencia.getItems().addAll(Frecuencia.values());
        cbFrecuencia.setValue(Frecuencia.UNICA);
        txtHora.setText("08:00");

        // Campos de programación solo se habilitan si se marca el check
        dpFecha.disableProperty().bind(chkProgramar.selectedProperty().not());
        txtHora.disableProperty().bind(chkProgramar.selectedProperty().not());
        cbFrecuencia.disableProperty().bind(chkProgramar.selectedProperty().not());

        // Destino solo aplica a transferencias
        cbTipoTransaccion.valueProperty().addListener((obs, viejo, nuevo) -> {
            boolean esTransferencia = "Transferencia".equals(nuevo);
            cbDestino.setDisable(!esTransferencia);
            cbMonederoDestino.setDisable(!esTransferencia);
        });

        // Al elegir cliente destino se cargan sus monederos (excepto el actual)
        cbDestino.valueProperty().addListener((obs, viejo, nuevo) -> {
            cbMonederoDestino.getItems().clear();
            if (nuevo != null) {
                cbMonederoDestino.getItems().setAll(nuevo.getMonederos().stream()
                        .filter(m -> m != monederoActual)
                        .toList());
                if (!cbMonederoDestino.getItems().isEmpty()) {
                    cbMonederoDestino.getSelectionModel().selectFirst();
                }
            }
        });
    }

    public void cargarDatos(Monedero monedero, Cliente origen) {
        this.monederoActual = monedero;
        this.clienteOrigen = origen;

        // Se incluye al propio cliente para mover dinero entre sus monederos
        cbDestino.getItems().setAll(
                BancoController.getInstance().listarClientes()
                        .stream()
                        .filter(c -> c != origen || c.getMonederos().size() > 1)
                        .toList()
        );
        actualizarInfo();
    }

    private void actualizarInfo() {
        lblInfo.setText(String.format("Monedero: %s | Saldo: $%,.2f | Rango: %s (%s)",
                monederoActual.getNombre(), monederoActual.getSaldo(),
                clienteOrigen.getRango().getNombre(), clienteOrigen.getRango().getDescripcionBeneficios()));
    }

    @FXML
    private void ejecutarTransaccion() {
        try {
            if (cbTipoTransaccion.getValue() == null) {
                throw new CampoVacioException("Seleccione un tipo de transacción.");
            }
            if (txtMonto.getText() == null || txtMonto.getText().isBlank()) {
                throw new CampoVacioException("Ingrese un monto.");
            }

            double monto = Double.parseDouble(txtMonto.getText().trim());
            String tipo = cbTipoTransaccion.getValue();
            Cliente destino = cbDestino.getValue();
            Monedero monederoDestino = cbMonederoDestino.getValue();

            if (tipo.equals("Transferencia") && destino == null) {
                throw new CampoVacioException("Seleccione un cliente destino.");
            }
            if (!tipo.equals("Transferencia")) {
                destino = null;
                monederoDestino = null;
            }

            if (chkProgramar.isSelected()) {
                programar(tipo, monto, destino, monederoDestino);
                return;
            }

            Transaccion t = transaccionController.crearTransaccion(
                    tipo, monto, clienteOrigen, destino, monederoActual, monederoDestino
            );
            transaccionController.ejecutarTransaccion(t);

            String mensaje = String.format("Transacción realizada exitosamente%nComisión: $%,.2f%nPuntos ganados: %d",
                    t.getComision(), t.getPuntosOtorgados());
            if (t.isSospechosa()) {
                mensaje += "\n\n⚠ Esta transacción fue marcada como ATÍPICA y se envió una alerta.";
            }
            new Alert(t.isSospechosa() ? Alert.AlertType.WARNING : Alert.AlertType.INFORMATION, mensaje).show();
            txtMonto.clear();
            actualizarInfo();

        } catch (NumberFormatException e) {
            new Alert(Alert.AlertType.ERROR, "El monto debe ser numérico.").show();
        } catch (RuntimeException e) {
            new Alert(Alert.AlertType.ERROR, e.getMessage()).show();
        }
    }

    private void programar(String tipo, double monto, Cliente destino, Monedero monederoDestino) {
        if (dpFecha.getValue() == null) {
            throw new CampoVacioException("Seleccione la fecha de ejecución.");
        }
        LocalTime hora;
        try {
            hora = LocalTime.parse(txtHora.getText().trim());
        } catch (DateTimeParseException e) {
            throw new FechaProgramacionInvalidaException("La hora debe tener formato HH:mm (ej. 08:30)");
        }
        LocalDate fecha = dpFecha.getValue();
        BancoController.getInstance().getProgramador().programar(tipo, monto, clienteOrigen, monederoActual,
                destino, monederoDestino, LocalDateTime.of(fecha, hora), cbFrecuencia.getValue());
        new Alert(Alert.AlertType.INFORMATION, "Transacción programada correctamente. "
                + "Puede verla en la pestaña 'Programadas' del cliente.").show();
    }

    @FXML
    private void volver() {
        MonederoViewController controller =
                (MonederoViewController) SceneLoader.cargarVista(
                        "edu/co/uniquindio/poo/monedero/monedero.fxml"
                );

        controller.cargarMonedero(monederoActual, clienteOrigen);
    }
}
