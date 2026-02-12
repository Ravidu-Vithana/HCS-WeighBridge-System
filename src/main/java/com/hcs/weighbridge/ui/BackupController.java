package com.hcs.weighbridge.ui;

import com.hcs.weighbridge.dao.ConfigDao;
import com.hcs.weighbridge.service.BackupService;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.BorderPane;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.application.Platform;
import javafx.concurrent.Task;
import com.hcs.weighbridge.MainApp;

import java.io.File;

import static com.hcs.weighbridge.util.UiUtils.showToast;

public class BackupController {

    @FXML
    private ComboBox<String> frequencyCombo;
    @FXML
    private Button backupNowButton;
    @FXML
    private ToggleButton autoRestoreToggle;
    @FXML
    private Button restoreFileButton;
    @FXML
    private Label latestBackupLabel;
    @FXML
    private Button testConnectionButton;
    @FXML
    private Button closeButton;

    private ConfigDao configDao;
    private BackupService backupService;
    private MainController mainController;
    private BorderPane mainControllerRootPane;

    public void init(ConfigDao configDao, BackupService backupService, MainController mainController) {
        this.configDao = configDao;
        this.backupService = backupService;
        this.mainController = mainController;
        this.mainControllerRootPane = mainController.getRootPane();

        setupUi();
    }

    private void setupUi() {
        // Frequency
        frequencyCombo.getItems().addAll("Daily", "Weekly", "Monthly");
        frequencyCombo.setValue(configDao.getBackupFrequency());
        frequencyCombo.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                configDao.setBackupFrequency(newVal);
            }
        });

        // Backup Now
        backupNowButton.setOnAction(e -> handleBackupNow());

        // Auto Restore
        boolean autoRestore = configDao.isAutoRestoreEnabled();
        autoRestoreToggle.setSelected(autoRestore);
        autoRestoreToggle.setText(autoRestore ? "Enabled" : "Disabled");

        restoreFileButton.visibleProperty().bind(autoRestoreToggle.selectedProperty().not());

        autoRestoreToggle.selectedProperty().addListener((obs, oldVal, newVal) -> {
            configDao.setAutoRestoreEnabled(newVal);
            autoRestoreToggle.setText(newVal ? "Enabled" : "Disabled");
        });

        // Restore File
        restoreFileButton.setOnAction(e -> handleRestoreFromFile());

        // Latest Backup
        updateLatestBackupLabel();

        // Test Connection
        testConnectionButton.setOnAction(e -> handleTestConnection());

        closeButton.setOnAction(e -> ((Stage) closeButton.getScene().getWindow()).close());
    }

    private void updateLatestBackupLabel() {
        String lastDate = configDao.getLastBackupDate();
        latestBackupLabel.setText(lastDate != null ? lastDate : "Never");
    }

    private void handleBackupNow() {
        Task<Void> backupTask = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                backupService.performBackup();
                return null;
            }
        };

        backupTask.setOnSucceeded(e -> {
            showToast((Stage) frequencyCombo.getScene().getWindow(),
                    frequencyCombo,
                    "Backup completed successfully!",
                    true);
            updateLatestBackupLabel();
        });

        backupTask.setOnFailed(e -> {
            Throwable ex = backupTask.getException();
            ex.printStackTrace();
            showToast((Stage) frequencyCombo.getScene().getWindow(),
                    frequencyCombo,
                    "Backup failed: " + ex.getMessage(),
                    false);
        });

        MainApp.getExecutorService().submit(backupTask);
    }

    private void handleRestoreFromFile() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Backup SQL File");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("SQL Files", "*.sql"));
        fileChooser.setInitialDirectory(new File("backups")); // Default to backup dir if exists

        File selectedFile = fileChooser.showOpenDialog(restoreFileButton.getScene().getWindow());
        if (selectedFile != null) {
            Task<Void> restoreTask = new Task<Void>() {
                @Override
                protected Void call() throws Exception {
                    backupService.restoreFromBackup(selectedFile);
                    return null;
                }
            };

            restoreTask.setOnSucceeded(e -> {
                showToast((Stage) frequencyCombo.getScene().getWindow(),
                        frequencyCombo,
                        "Restore completed! Some data might have been skipped if tables were not empty.",
                        true);
            });

            restoreTask.setOnFailed(e -> {
                Throwable ex = restoreTask.getException();
                ex.printStackTrace();
                showToast((Stage) frequencyCombo.getScene().getWindow(),
                        frequencyCombo,
                        "Restore failed: " + ex.getMessage(),
                        false);
            });

            MainApp.getExecutorService().submit(restoreTask);
        }
    }

    private void handleTestConnection() {
        testConnectionButton.setDisable(true);

        Task<String> testTask = new Task<String>() {
            @Override
            protected String call() throws Exception {
                try (java.sql.Connection conn = com.hcs.weighbridge.config.DatabaseConfig.getConnection();
                        java.sql.Statement stmt = conn.createStatement()) {
                    stmt.executeQuery("SELECT 1");
                    return "Connection Successful";
                }
            }
        };

        testTask.setOnSucceeded(e -> {
            testConnectionButton.setDisable(false);
            showToast((Stage) testConnectionButton.getScene().getWindow(),
                    testConnectionButton,
                    testTask.getValue(),
                    true);
        });

        testTask.setOnFailed(e -> {
            testConnectionButton.setDisable(false);
            Throwable ex = testTask.getException();
            String errorMessage = ex.getMessage();
            if (ex.getCause() != null) {
                errorMessage = ex.getCause().getMessage();
            }

            showToast((Stage) testConnectionButton.getScene().getWindow(),
                    testConnectionButton,
                    "Connection Failed: " + errorMessage,
                    false);
        });

        MainApp.getExecutorService().submit(testTask);
    }

}
