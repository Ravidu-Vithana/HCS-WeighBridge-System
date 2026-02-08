package com.hcs.weighbridge;

import com.hcs.weighbridge.config.DatabaseConfig;
import com.hcs.weighbridge.dao.ConfigDao;
import com.hcs.weighbridge.dao.WeighDataDao;
import com.hcs.weighbridge.model.SerialConfig;
import com.hcs.weighbridge.serial.WeighReader;
import com.hcs.weighbridge.service.WeighService;
import com.hcs.weighbridge.ui.MainController;
import com.hcs.weighbridge.ui.UiModel;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.sql.Connection;

public class MainApp extends Application {

    private static WeighReader weighReader; // Static to be accessible if needed, or better managed via instance
    private static MainApp instance;

    public MainApp() {
        instance = this;
    }

    public static MainApp getInstance() {
        return instance;
    }

    @Override
    public void start(Stage stage) throws Exception {
        stage.initStyle(StageStyle.UNDECORATED);
        showLoginView(stage);
    }

    public void showLoginView(Stage stage) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/auth/login.fxml"));
            Scene scene = new Scene(loader.load());
            stage.setTitle("WeighBridge System - Login");
            stage.setScene(scene);
            stage.setMaximized(true);
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void showMainView() {
        try {
            Stage stage = new Stage();
            stage.initStyle(StageStyle.UNDECORATED);
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/main/main.fxml"));
            Scene scene = new Scene(loader.load());
            MainController controller = loader.getController();

            UiModel uiModel = new UiModel();
            Connection connection = DatabaseConfig.getConnection();
            WeighDataDao weighDataDao = new WeighDataDao(connection);
            WeighService weighService = new WeighService(weighDataDao);
            ConfigDao configDao = new ConfigDao(connection);

            controller.init(uiModel, weighService, configDao);
            SerialConfig serialConfig = configDao.loadSerialConfig();

            if (weighReader != null) {
                weighReader.stop();
            }

            weighReader = new WeighReader(
                    serialConfig,
                    (weight, status) -> Platform.runLater(() -> controller.updateLiveWeight(weight, status)));

            Thread serialThread = new Thread(
                    weighReader::start,
                    "WeighReader-Thread");
            serialThread.setDaemon(true);
            serialThread.start();

            stage.setTitle("WeighBridge System");
            stage.setScene(scene);
            stage.setMaximized(true);
            stage.show();

            // Close the login stage if it's open (implied by context of calling from
            // LoginController which has a stage)
            // Ideally passing the stage to showMainView would be better to reuse it, but
            // creating new one is also fine.
            // Let's modify to reuse stage if possible or close previous.
            // For now, let's keep it simple: Create new Main Stage. Login Stage will be
            // closed by LoginController.

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void stop() {
        if (weighReader != null) {
            weighReader.stop();
        }
        DatabaseConfig.closeConnection();
        Platform.exit();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
