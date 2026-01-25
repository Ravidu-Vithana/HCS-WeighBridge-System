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

import java.sql.Connection;

public class MainApp extends Application {

    private WeighReader weighReader;

    @Override
    public void start(Stage stage) throws Exception {

        FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/main/main.fxml")
        );

        Scene scene = new Scene(loader.load());
        MainController controller = loader.getController();

        UiModel uiModel = new UiModel();

        Connection connection = DatabaseConfig.getConnection();
        WeighDataDao weighDataDao = new WeighDataDao(connection);

        WeighService weighService = new WeighService(weighDataDao);

        ConfigDao configDao = new ConfigDao(connection);
        controller.init(uiModel, weighService, configDao);
        SerialConfig serialConfig = configDao.loadSerialConfig();

        weighReader = new WeighReader(
                serialConfig,
                (weight, status) -> Platform.runLater(() ->
                        controller.updateLiveWeight(weight, status)
                )
        );

        Thread serialThread = new Thread(
                weighReader::start,
                "WeighReader-Thread"
        );
        serialThread.setDaemon(true);
        serialThread.start();

        stage.setTitle("WeighBridge System");
        stage.setScene(scene);
        stage.setMaximized(true);
        stage.show();
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
