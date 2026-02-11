package com.hcs.weighbridge;

import com.hcs.weighbridge.config.DatabaseConfig;
import com.hcs.weighbridge.dao.ConfigDao;
import com.hcs.weighbridge.dao.WeighDataDao;
import com.hcs.weighbridge.model.SerialConfig;
import com.hcs.weighbridge.serial.WeighReader;
import com.hcs.weighbridge.service.WeighService;
import com.hcs.weighbridge.ui.MainController;
import com.hcs.weighbridge.ui.UiModel;
import com.hcs.weighbridge.util.LogUtil;
import com.hcs.weighbridge.util.SecurityUtil;
import com.hcs.weighbridge.util.UiUtils;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.apache.logging.log4j.Logger;

import java.sql.Connection;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainApp extends Application {

    private static final Logger logger = LogUtil.getLogger(DatabaseConfig.class);
    private static WeighReader weighReader;
    private static MainApp instance;
    private static final ExecutorService executorService = Executors.newFixedThreadPool(4);

    public MainApp() {
        instance = this;
    }

    public static MainApp getInstance() {
        return instance;
    }

    public static WeighReader getWeighReader() {
        return weighReader;
    }

    public static ExecutorService getExecutorService() {
        return executorService;
    }
    
    private void initializeApplication() {
        try {
            logger.info("Initializing WeighBridge Application...");
            
            // Initialize security configuration
            SecurityUtil.initialize();
            logger.info("Security configuration initialized");
            
            // Initialize database connection
            Connection connection = DatabaseConfig.getConnection();
            logger.info("Database connection established");
            
            // Initialize configuration DAO
            ConfigDao configDao = new ConfigDao(connection);
            
            // Initialize backup service
            com.hcs.weighbridge.service.BackupService backupService = 
                new com.hcs.weighbridge.service.BackupService(connection, configDao);
            
            // Perform auto-restore if enabled
            backupService.autoRestoreIfEnabled();
            logger.info("Auto-restore check completed");
            
            // Check for scheduled backups
            backupService.checkAndRunScheduledBackup();
            logger.info("Scheduled backup check completed");
            
            logger.info("Application initialization completed successfully");
            
        } catch (Exception e) {
            logger.error("FATAL: Failed to initialize application: {}", e.getMessage(), e);
            UiUtils.showAlert("Fatal Error", "Application initialization failed: " + e.getMessage());
            Platform.exit();
            System.exit(1);
        }
    }
    
    @Override
    public void start(Stage stage) throws Exception {
        stage.initStyle(StageStyle.UNDECORATED);
        try {
            initializeApplication();
            showLoginView(stage);
        } catch (Exception e) {
            logger.fatal("Fatal startup error", e);
            UiUtils.showAlert("Fatal Error", "Application failed to start.");
            Platform.exit();
            System.exit(1);
        }
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
            logger.error("Failed to initialize login screen: {}", e.getMessage(), e);
        }
    }

    public void showMainView(com.hcs.weighbridge.model.User currentUser) {
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
            com.hcs.weighbridge.service.BackupService backupService = new com.hcs.weighbridge.service.BackupService(
                    connection, configDao);

            controller.init(uiModel, weighService, configDao, backupService, currentUser);
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

        } catch (Exception e) {
            logger.error("Failed to initialize main screen: {}", e.getMessage(), e);
        }
    }

    @Override
    public void stop() {
        if (weighReader != null) {
            weighReader.stop();
        }
        executorService.shutdown();
        DatabaseConfig.closeConnection();
        Platform.exit();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
