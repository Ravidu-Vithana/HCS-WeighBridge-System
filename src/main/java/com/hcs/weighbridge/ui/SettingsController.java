package com.hcs.weighbridge.ui;

import com.fazecast.jSerialComm.SerialPort;
import com.hcs.weighbridge.dao.ConfigDao;
import com.hcs.weighbridge.model.SerialConfig;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.stage.Stage;
import javafx.util.StringConverter;

import java.net.URL;
import java.util.ResourceBundle;

public class SettingsController implements Initializable {

    @FXML private ComboBox<String> portCombo;
    @FXML private ComboBox<Integer> baudCombo;
    @FXML private ComboBox<Integer> dataBitsCombo;
    @FXML private ComboBox<Integer> stopBitsCombo;
    @FXML private ComboBox<String> parityCombo;

    @FXML private Slider scaleSlider;
    @FXML private Label scaleLabel;

    private ConfigDao configDao;
    private MainController mainController;

    // Use this method to set dependencies
    public void setDependencies(ConfigDao dao, MainController mainController) {
        this.configDao = dao;
        this.mainController = mainController;
        loadData();
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Initialize UI components
        try{
            setupComboBoxes();
            setupScaleSlider();
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    private void setupComboBoxes() {
        for (SerialPort p : SerialPort.getCommPorts()) {
            portCombo.getItems().add(p.getSystemPortName());
        }
        baudCombo.getItems().addAll(1200, 2400, 4800, 9600);
        dataBitsCombo.getItems().addAll(7, 8);
        stopBitsCombo.getItems().addAll(1, 2);
        parityCombo.getItems().addAll("NONE", "EVEN", "ODD");
    }

    private void setupScaleSlider() {
        scaleSlider.setMin(0.5);
        scaleSlider.setMax(2.0);
        scaleSlider.setMajorTickUnit(0.5);
        scaleSlider.setMinorTickCount(4);
        scaleSlider.setShowTickLabels(true);
        scaleSlider.setShowTickMarks(true);
        scaleSlider.setSnapToTicks(true);

        scaleSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            scaleLabel.setText(String.format("%.1fx", newVal.doubleValue()));
        });
    }

    private void loadData() {
        if (configDao == null) return;

        try {
            double currentScale = configDao.getUiScaleFactor();
            scaleSlider.setValue(currentScale);
            scaleLabel.setText(String.format("%.1fx", currentScale));

            SerialConfig cfg = configDao.loadSerialConfig();

            if (cfg.getPortName() != null && !cfg.getPortName().isEmpty()) {
                portCombo.setValue(cfg.getPortName());
            }

            if (cfg.getBaudRate() > 0) {
                baudCombo.setValue(cfg.getBaudRate());
            }

            if (cfg.getDataBits() > 0) {
                dataBitsCombo.setValue(cfg.getDataBits());
            }

            if (cfg.getStopBits() > 0) {
                stopBitsCombo.setValue(cfg.getStopBits());
            }

            String parityValue = cfg.getParity() == SerialPort.EVEN_PARITY ? "EVEN" :
                    cfg.getParity() == SerialPort.ODD_PARITY ? "ODD" : "NONE";
            parityCombo.setValue(parityValue);

        } catch (Exception e) {
            System.err.println("Failed to load settings: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void save() {
        if (configDao == null) {
            showAlert("Configuration DAO not initialized!");
            return;
        }

        try {
            SerialConfig cfg = new SerialConfig();
            cfg.setPortName(portCombo.getValue());

            if (baudCombo.getValue() != null) {
                cfg.setBaudRate(baudCombo.getValue());
            }

            if (dataBitsCombo.getValue() != null) {
                cfg.setDataBits(dataBitsCombo.getValue());
            }

            if (stopBitsCombo.getValue() != null) {
                cfg.setStopBits(stopBitsCombo.getValue());
            }

            String parity = parityCombo.getValue();
            if ("EVEN".equals(parity)) {
                cfg.setParity(SerialPort.EVEN_PARITY);
            } else if ("ODD".equals(parity)) {
                cfg.setParity(SerialPort.ODD_PARITY);
            } else {
                cfg.setParity(SerialPort.NO_PARITY);
            }

            configDao.saveSerialConfig(cfg);

            double scaleFactor = scaleSlider.getValue();
            configDao.saveUiScaleFactor(scaleFactor);

            if (mainController != null) {
                mainController.reloadWithScale(scaleFactor);
                showAlert("Settings saved successfully!\nUI scale has been applied.");
            } else {
                showAlert("Settings saved successfully!\nPlease restart the application for UI scale to take full effect.");
            }

            close();

        } catch (Exception e) {
            System.err.println("Failed to save settings: " + e.getMessage());
            e.printStackTrace();
            showAlert("Failed to save settings: " + e.getMessage());
        }
    }

    @FXML
    private void close() {
        Stage stage = (Stage) portCombo.getScene().getWindow();
        stage.close();
    }

    private void showAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Settings");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}