package ru.mirea.appliancerepair.fx;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.stage.Stage;

import java.io.IOException;

public class ApplianceRepairApplication extends Application {
    @Override
    public void start(Stage stage) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("main-view.fxml"));
            Scene scene = new Scene(root);
            stage.setTitle("Сервис ремонта бытовой техники");
            stage.setScene(scene);
            stage.setMinWidth(1000);
            stage.setMinHeight(650);
            stage.show();
        } catch (IOException e) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Ошибка запуска");
            alert.setHeaderText("Не удалось открыть главное окно");
            alert.setContentText(e.getMessage());
            alert.showAndWait();
        }
    }

    public static void main(String[] args) { launch(args); }
}
