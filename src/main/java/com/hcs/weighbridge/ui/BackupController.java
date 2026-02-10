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

        closeButton.setOnAction(e -> ((Stage) closeButton.getScene().getWindow()).close());
    }

    private void updateLatestBackupLabel() {
        String lastDate = configDao.getLastBackupDate();
        latestBackupLabel.setText(lastDate != null ? lastDate : "Never");
    }

    private void handleBackupNow() {
        try {
            backupService.performBackup();
            showToast((Stage) mainControllerRootPane.getScene().getWindow(),
                    mainControllerRootPane,
                    "Backup completed successfully!",
                    true);
            updateLatestBackupLabel();
        } catch (Exception e) {
            e.printStackTrace();
            showToast((Stage) mainControllerRootPane.getScene().getWindow(),
                    mainControllerRootPane,
                    "Backup failed: " + e.getMessage(),
                    false);
        }
    }

    private void handleRestoreFromFile() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Backup SQL File");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("SQL Files", "*.sql"));
        fileChooser.setInitialDirectory(new File("backups")); // Default to backup dir if exists

        File selectedFile = fileChooser.showOpenDialog(restoreFileButton.getScene().getWindow());
        if (selectedFile != null) {
            try {
                backupService.restoreFromBackup(selectedFile);
                showToast((Stage) mainControllerRootPane.getScene().getWindow(),
                        mainControllerRootPane,
                        "Restore completed! Some data might have been skipped if tables were not empty.",
                        true);
            } catch (Exception e) {
                e.printStackTrace();
                showToast((Stage) mainControllerRootPane.getScene().getWindow(),
                    mainControllerRootPane,
                        "Restore failed: " + e.getMessage(),
                        false);
            }
        }
    }

}
