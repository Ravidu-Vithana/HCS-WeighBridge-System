package com.hcs.weighbridge.ui;

import com.hcs.weighbridge.constants.PrintMode;
import com.hcs.weighbridge.dao.ConfigDao;
import com.hcs.weighbridge.model.Record;
import com.hcs.weighbridge.service.PrintService;
import com.hcs.weighbridge.service.WeighService;
import com.hcs.weighbridge.util.UiScaler;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.layout.BorderPane;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.util.ArrayList;

public class MainController {

    // ---------- FXML Components ----------
    @FXML
    private BorderPane rootPane;
    @FXML
    private Label liveWeightLabel;
    @FXML
    private Label statusLabel;

    @FXML
    private TextField lorryField;
    @FXML
    private TextField customerField;
    @FXML
    private TextField productField;
    @FXML
    private TextField driverField;

    @FXML
    private TableView<Record> recentRecordsTable;
    @FXML
    private TableView<Record> completeRecordsTable;

    @FXML
    private Button newButton;
    @FXML
    private Button saveButton;
    @FXML
    private Button printFirstButton;
    @FXML
    private Button printFullButton;
    @FXML
    private Button settingsButton;
    @FXML
    private Button exitButton;
    @FXML
    private Button logoutButton;
    @FXML
    private Button backupButton; // Can be null if not yet in FXML, but we will add it

    private UiModel model;
    private WeighService weighService;
    private ConfigDao configDao;
    private com.hcs.weighbridge.service.BackupService backupService;
    private com.hcs.weighbridge.model.User currentUser;
    private UiScaler uiScaler;

    private final ObservableList<Record> recentRecords = FXCollections.observableArrayList();
    private final ObservableList<Record> completeRecords = FXCollections.observableArrayList();

    private enum Phase {
        IDLE,
        FIRST_WEIGHT_DONE,
        COMPLETED
    }

    public void init(UiModel model,
            WeighService weighService,
            ConfigDao configDao,
            com.hcs.weighbridge.service.BackupService backupService,
            com.hcs.weighbridge.model.User currentUser) {

        this.model = model;
        this.weighService = weighService;
        this.configDao = configDao;
        this.backupService = backupService;
        this.currentUser = currentUser;

        double scaleFactor = configDao.getUiScaleFactor();
        this.uiScaler = new UiScaler(scaleFactor);

        setupTables();
        loadTables();
        bindUi();
        javafx.application.Platform.runLater(() -> {
            applyScaling();
            if (rootPane != null && rootPane.getScene() != null) {
                rootPane.getScene().getRoot().applyCss();
                setupKeyboardShortcuts();
            }
        });

        // initializeSampleData();

        settingsButton.setOnAction(e -> openSettings());
        if (backupButton != null) {
            boolean isAdmin = currentUser != null && currentUser.getRole() == com.hcs.weighbridge.model.Role.ADMIN;
            backupButton.setVisible(isAdmin);
            backupButton.setManaged(isAdmin);
            backupButton.setOnAction(e -> openBackupSettings());
        }

        newButton.setOnAction(e -> resetRecord());
        saveButton.setOnAction(e -> saveRecord());
        printFirstButton.setOnAction(e -> printFirstTicket());
        printFullButton.setOnAction(e -> printFullTicket());
        logoutButton.setOnAction(e -> handleLogout());
        exitButton.setOnAction(e -> handleExit());
    }

    private void setupKeyboardShortcuts() {
        rootPane.getScene().setOnKeyPressed(event -> {
            switch (event.getCode()) {
                case F9:
                    resetRecord();
                    event.consume();
                    break;
                case F12:
                    saveRecord();
                    event.consume();
                    break;
                case F10:
                    printFirstTicket();
                    event.consume();
                    break;
                case F11:
                    printFullTicket();
                    event.consume();
                    break;
            }
        });
    }

    private void handleExit() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Exit Confirmation");
        alert.setHeaderText(null);
        alert.setContentText("Are you sure you want to exit the application?");

        if (alert.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            System.exit(0);
        }
    }

    private void handleLogout() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Logout Confirmation");
        alert.setHeaderText(null);
        alert.setContentText("Are you sure you want to logout?");

        if (alert.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) {
            return;
        }

        try {
            // Stop components
            if (weighService != null) {
                // weighService.stop(); // If needed
            }
            com.hcs.weighbridge.serial.WeighReader reader = com.hcs.weighbridge.MainApp.getWeighReader();
            if (reader != null) {
                reader.stop();
            }

            // Close current window
            Stage currentStage = (Stage) rootPane.getScene().getWindow();
            currentStage.close();

            // Open Login Window
            Stage loginStage = new Stage();
            loginStage.initStyle(javafx.stage.StageStyle.UNDECORATED);
            com.hcs.weighbridge.MainApp.getInstance().showLoginView(loginStage);

        } catch (Exception e) {
            System.err.println("Logout failed: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void openBackupSettings() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/main/backup_view.fxml"));
            Parent root = loader.load();

            BackupController controller = loader.getController();
            controller.init(configDao, backupService, this);

            Scene scene = new Scene(root);
            Stage stage = new Stage();
            stage.setTitle("Backup & Restore Settings");
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setScene(scene);
            stage.setResizable(false);
            stage.showAndWait();

        } catch (Exception e) {
            System.err.println("Failed to open backup settings: " + e.getMessage());
            e.printStackTrace();
            showAlert("Failed to open backup settings: " + e.getMessage());
        }
    }

    private void setupTables() {
        recentRecordsTable.setItems(recentRecords);

        TableColumn<Record, String> dateCol = (TableColumn<Record, String>) recentRecordsTable.getColumns().get(0);
        TableColumn<Record, String> timeCol = (TableColumn<Record, String>) recentRecordsTable.getColumns().get(1);
        TableColumn<Record, String> receiptCol = (TableColumn<Record, String>) recentRecordsTable.getColumns().get(2);
        TableColumn<Record, String> lorryCol = (TableColumn<Record, String>) recentRecordsTable.getColumns().get(3);
        TableColumn<Record, String> weightCol = (TableColumn<Record, String>) recentRecordsTable.getColumns().get(4);

        dateCol.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getDateIn()));
        timeCol.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getTimeIn()));
        receiptCol
                .setCellValueFactory(cellData -> new SimpleStringProperty(String.valueOf(cellData.getValue().getId())));
        lorryCol.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getLorryNumber()));
        weightCol.setCellValueFactory(
                cellData -> new SimpleStringProperty(String.valueOf(cellData.getValue().getFirstWeight())));

        completeRecordsTable.setItems(completeRecords);

        TableColumn<Record, String> dateInCol = (TableColumn<Record, String>) completeRecordsTable.getColumns().get(0);
        TableColumn<Record, String> dateOutCol = (TableColumn<Record, String>) completeRecordsTable.getColumns().get(1);
        TableColumn<Record, String> timeInCol = (TableColumn<Record, String>) completeRecordsTable.getColumns().get(2);
        TableColumn<Record, String> timeOutCol = (TableColumn<Record, String>) completeRecordsTable.getColumns().get(3);
        TableColumn<Record, String> receiptNoCol = (TableColumn<Record, String>) completeRecordsTable.getColumns()
                .get(4);
        TableColumn<Record, String> lorryNoCol = (TableColumn<Record, String>) completeRecordsTable.getColumns().get(5);
        TableColumn<Record, String> firstWtCol = (TableColumn<Record, String>) completeRecordsTable.getColumns().get(6);
        TableColumn<Record, String> secondWtCol = (TableColumn<Record, String>) completeRecordsTable.getColumns()
                .get(7);
        TableColumn<Record, String> netWtCol = (TableColumn<Record, String>) completeRecordsTable.getColumns().get(8);
        TableColumn<Record, String> customerCol = (TableColumn<Record, String>) completeRecordsTable.getColumns()
                .get(9);
        TableColumn<Record, String> productCol = (TableColumn<Record, String>) completeRecordsTable.getColumns()
                .get(10);
        TableColumn<Record, String> driverCol = (TableColumn<Record, String>) completeRecordsTable.getColumns().get(11);

        dateInCol.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getDateIn()));
        dateOutCol.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getDateOut()));
        timeInCol.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getTimeIn()));
        timeOutCol.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getTimeOut()));
        receiptNoCol
                .setCellValueFactory(cellData -> new SimpleStringProperty(String.valueOf(cellData.getValue().getId())));
        lorryNoCol.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getLorryNumber()));
        firstWtCol.setCellValueFactory(
                cellData -> new SimpleStringProperty(String.valueOf(cellData.getValue().getFirstWeight())));
        secondWtCol.setCellValueFactory(
                cellData -> new SimpleStringProperty(String.valueOf(cellData.getValue().getSecondWeight())));
        netWtCol.setCellValueFactory(
                cellData -> new SimpleStringProperty(String.valueOf(cellData.getValue().getNetWeight())));
        customerCol.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getCustomerName()));
        productCol.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getProductName()));
        driverCol.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getDriverName()));

        recentRecordsTable.getSelectionModel().selectedItemProperty().addListener(
                (obs, oldSelection, newSelection) -> {
                    if (newSelection != null) {
                        handleRecentWeighingClick(newSelection);
                    }
                });

        completeRecordsTable.getSelectionModel().selectedItemProperty().addListener(
                (obs, oldSelection, newSelection) -> {
                    if (newSelection != null) {
                        handleCompleteRecordClick(newSelection);
                    }
                });
    }

    private void loadTables() {
        ArrayList<Record> pendingRecords = weighService.getAllPendingRecords();
        ArrayList<Record> completedRecords = weighService.getAllCompletedRecords();
        recentRecords.clear();
        completeRecords.clear();
        recentRecords.addAll(pendingRecords);
        completeRecords.addAll(completedRecords);
    }

    private void handleRecentWeighingClick(Record newSelection) {
        completeRecordsTable.getSelectionModel().clearSelection();
        loadRecordToFields(newSelection);
        weighService.setFirstWeightRecord(newSelection);
    }

    private void handleCompleteRecordClick(Record newSelection) {
        recentRecordsTable.getSelectionModel().clearSelection();
        weighService.setFullRecord(newSelection);
        clearAllFields();
    }

    private void initializeSampleData() {
        for (int i = 1; i <= 8; i++) {
            Record record = new Record(new String[] { "ABC-1234", "XYZ-5678", "LMN-9012", "PQR-3456",
                    "STU-7890", "VWX-2345", "YZA-6789", "BCD-0123" }[i - 1]);
            record.setId(i);
            record.setDateIn("2026-01-25");
            record.setTimeIn(String.format("%02d:%02d", 8 + i, (i * 15) % 60));
            record.setLorryNumber(new String[] { "ABC-1234", "XYZ-5678", "LMN-9012", "PQR-3456",
                    "STU-7890", "VWX-2345", "YZA-6789", "BCD-0123" }[i - 1]);
            record.setFirstWeight(15000 - (i * 300));
            recentRecords.add(record);
        }

        Record record1 = new Record("DEF-9012");
        record1.setId(1);
        record1.setDateIn("2026-01-24");
        record1.setDateOut("2026-01-24");
        record1.setTimeIn("10:30");
        record1.setTimeOut("14:45");
        record1.setLorryNumber("DEF-9012");
        record1.setFirstWeight(18000);
        record1.setSecondWeight(5000);
        record1.setNetWeight(13000);
        record1.setCustomerName("ABC Traders");
        record1.setProductName("Cement");
        record1.setDriverName("John Silva");
        completeRecords.add(record1);

        Record record2 = new Record("GHI-3456");
        record2.setId(2);
        record2.setDateIn("2026-01-24");
        record2.setDateOut("2026-01-24");
        record2.setTimeIn("11:00");
        record2.setTimeOut("15:20");
        record2.setLorryNumber("GHI-3456");
        record2.setFirstWeight(16500);
        record2.setSecondWeight(4500);
        record2.setNetWeight(12000);
        record2.setCustomerName("XYZ Industries");
        record2.setProductName("Sand");
        record2.setDriverName("Kamal Perera");
        completeRecords.add(record2);
    }

    private void bindUi() {
        liveWeightLabel.textProperty().bind(
                model.liveWeightProperty().asString("%d"));
        statusLabel.textProperty().bind(model.statusProperty());
    }

    private void openSettings() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/main/settings.fxml"));
            Parent settingsRoot = loader.load();

            SettingsController controller = loader.getController();
            com.hcs.weighbridge.dao.UserDao userDao = new com.hcs.weighbridge.dao.UserDao(
                    com.hcs.weighbridge.config.DatabaseConfig.getConnection());
            controller.setDependencies(configDao, userDao, this, currentUser);

            Scene scene = new Scene(settingsRoot);

            Stage stage = new Stage();
            stage.setTitle("Settings");
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setScene(scene);
            stage.setResizable(false);

            stage.showAndWait();

        } catch (Exception e) {
            System.err.println("Failed to open settings: " + e.getMessage());
            e.printStackTrace();
            showAlert("Failed to open settings: " + e.getMessage());
        }
    }

    private void applyScaling() {
        if (rootPane != null && uiScaler != null) {
            uiScaler.applyScaling(rootPane);
        }
    }

    public void reloadWithScale(double newScaleFactor) {
        configDao.saveUiScaleFactor(newScaleFactor);
        uiScaler.setScaleFactor(newScaleFactor);
        applyScaling();

        if (rootPane.getScene() != null) {
            rootPane.getScene().getRoot().applyCss();
        }
    }

    public void updateLiveWeight(int weight, char statusChar) {
        boolean stable = (statusChar == 'P' || statusChar == 'T');
        model.updateLive(weight, stable ? "STABLE" : "UNSTABLE");

        if (stable) {
            if (weighService.isCompleted()) {
                Record completed = weighService.getActiveRecord();
                completeRecords.add(completed);
                recentRecords.remove(completed);
                weighService.clearActiveRecord();
            }
        }
    }

    private void resetRecord() {
        weighService.clearActiveRecord();
        weighService.clearFullRecord();
        recentRecordsTable.getSelectionModel().clearSelection();
        completeRecordsTable.getSelectionModel().clearSelection();
        clearAllFields();
    }

    private void clearAllFields() {
        lorryField.clear();
        lorryField.setEditable(true);
        customerField.clear();
        productField.clear();
        driverField.clear();
    }

    private void loadRecordToFields(Record record) {
        if (record != null) {
            lorryField.setText(record.getLorryNumber());
            lorryField.setEditable(false);
            customerField.setText(record.getCustomerName());
            productField.setText(record.getProductName());
            driverField.setText(record.getDriverName());
        }
    }

    private void saveRecord() {
        String lorry = lorryField.getText().trim();
        String customer = customerField.getText().trim();
        String product = productField.getText().trim();
        String driver = driverField.getText().trim();

        if (lorry.isEmpty()) {
            showAlert("Please enter the Lorry Number");
            return;
        }
        if (product.isEmpty()) {
            product = "-";
        }
        if (driver.isEmpty()) {
            driver = "-";
        }
        if (customer.isEmpty()) {
            customer = "-";
        }
        int currentWeight = model.liveWeightProperty().get();

        if (weighService.isPendingRecordAvailable(lorry) && weighService.hasFirstWeight()) {
            weighService.saveSecondWeight(currentWeight);
            printSecondTicket();
            recentRecords.remove(weighService.getActiveRecord());
            resetRecord();
            showAlert("Second Weight saved successfully!");
        } else if (weighService.isPendingRecordAvailable(lorry) && !weighService.hasFirstWeight()) {
            showAlert("Please select the lorry from the table!");
            resetRecord();
        } else {
            weighService.startTransaction(lorry, customer, product, driver);
            weighService.saveFirstWeight(currentWeight);
            printFirstTicket();
            resetRecord();
            showAlert("First Weight saved successfully!");
        }
        loadTables();

    }

    private void printFirstTicket() {
        Record record = weighService.getActiveRecord();

        if (record == null) {
            showAlert("No active record to print");
            return;
        }

        printReceipt(record, PrintMode.FIRST_WEIGHT);
    }

    private void printSecondTicket() {
        Record record = weighService.getFullRecord();

        if (record == null) {
            showAlert("No completed record to print");
            return;
        }

        printReceipt(record, PrintMode.SECOND_WEIGHT);
    }

    private void printFullTicket() {
        Record record = weighService.getFullRecord();

        if (record == null) {
            showAlert("No completed record selected");
            return;
        }

        printReceipt(record, PrintMode.FULL);
    }

    private void printReceipt(Record record, PrintMode mode) {

        try {
            PrintService printService = new PrintService();
            printService.printReceiptSilent(record, mode);

        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Print failed: " + e.getMessage());
        }
    }

    private void showAlert(String msg) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }
}