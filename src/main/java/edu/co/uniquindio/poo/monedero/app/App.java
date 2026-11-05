package edu.co.uniquindio.poo.monedero.app;

import edu.co.uniquindio.poo.monedero.controller.BancoController;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.scene.control.Alert;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.time.LocalDateTime;
import java.util.List;

public class App extends Application {

    /** Revisa cada 30 s si hay transacciones programadas vencidas. */
    private Timeline programadorAutomatico;

    @Override
    public void start(Stage stage) {
        stage.setTitle("Banco Virtual");
        stage.setWidth(1000);
        stage.setHeight(680);
        stage.setMinWidth(900);
        stage.setMinHeight(620);

        SceneLoader.setMainStage(stage);

        Object controller = SceneLoader.cargarVista("edu/co/uniquindio/poo/monedero/banco.fxml");

        if (controller == null) {
            System.err.println("No se pudo cargar la vista principal (banco.fxml); revisar rutas y module-info.java");
            new Alert(Alert.AlertType.ERROR,
                    "Error al cargar la interfaz principal; revisar la consola para más detalles")
                    .showAndWait();
            stage.close();
            return;
        }

        if (!stage.isShowing()) {
            stage.show();
        }
        stage.centerOnScreen();

        iniciarProgramadorAutomatico();

        stage.setOnCloseRequest(ev -> {
            if (programadorAutomatico != null) programadorAutomatico.stop();
            System.out.println("cerrando app");
        });
    }

    private void iniciarProgramadorAutomatico() {
        programadorAutomatico = new Timeline(new KeyFrame(Duration.seconds(30), e -> {
            List<String> resultados = BancoController.getInstance().getProgramador()
                    .procesarPendientes(LocalDateTime.now());
            resultados.forEach(System.out::println);
        }));
        programadorAutomatico.setCycleCount(Animation.INDEFINITE);
        programadorAutomatico.play();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
