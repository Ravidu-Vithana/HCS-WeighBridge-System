package com.hcs.weighbridge.ui;

import com.hcs.weighbridge.MainApp;
import com.hcs.weighbridge.config.DatabaseConfig;
import com.hcs.weighbridge.constants.PrintMode;
import com.hcs.weighbridge.dao.ConfigDao;
import com.hcs.weighbridge.dao.UserDao;
import com.hcs.weighbridge.model.Record;
import com.hcs.weighbridge.model.User;
import com.hcs.weighbridge.serial.WeighReader;
import com.hcs.weighbridge.service.BackupService;
import com.hcs.weighbridge.service.PrintService;
import com.hcs.weighbridge.service.WeighService;
import com.hcs.weighbridge.util.LogUtil;
import com.hcs.weighbridge.util.UiScaler;
import com.hcs.weighbridge.util.UiUtils;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.hcs.weighbridge.util.UiUtils.showToast;

public class MainController {
    private static final Logger logger = LogUtil.getLogger(DatabaseConfig.class);

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
    private Button backupButton;

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
            BackupService backupService,
            User currentUser) {

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

        Platform.runLater(() -> lorryField.requestFocus());
    }

    private void setupKeyboardShortcuts() {
        rootPane.getScene().addEventFilter(KeyEvent.KEY_PRESSED, event -> {
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
        Stage stage = (Stage) rootPane.getScene().getWindow();

        boolean confirmed = UiUtils.showConfirmation(
                stage,
                "Exit Confirmation",
                "Are you sure you want to exit the application?"
        );

        if (confirmed) {
            System.exit(0);
        }
    }

    private void handleLogout() {
        Stage stage = (Stage) rootPane.getScene().getWindow();

        boolean confirmed = UiUtils.showConfirmation(
                stage,
                "Logout Confirmation",
                "Are you sure you want to logout?"
        );

        if (!confirmed) {
            return;
        }

        try {
            if (weighService != null) {
                weighService = null;
            }
            WeighReader reader = MainApp.getWeighReader();
            if (reader != null) {
                reader.stop();
            }

            Stage currentStage = (Stage) rootPane.getScene().getWindow();
            currentStage.close();

            Stage loginStage = new Stage();
            loginStage.initStyle(javafx.stage.StageStyle.UNDECORATED);
            MainApp.getInstance().showLoginView(loginStage);

        } catch (Exception e) {
            System.err.println("Logout failed: " + e.getMessage());
            logger.error("Failed to logout: {}", e.getMessage(), e);
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
            logger.error("Failed to open backup settings: {}", e.getMessage(), e);
            showToast((Stage) rootPane.getScene().getWindow(),
                    rootPane,
                    "Failed to open backup settings: " + e.getMessage(),
                    false);
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
        Task<List<ArrayList<Record>>> task = new Task<List<ArrayList<Record>>>() {
            @Override
            protected List<ArrayList<Record>> call() throws Exception {
                ArrayList<Record> pendingRecords = weighService.getAllPendingRecords();
                ArrayList<Record> completedRecords = weighService.getAllCompletedRecords();
                return Arrays.asList(pendingRecords, completedRecords);
            }
        };

        task.setOnSucceeded(e -> {
            List<ArrayList<Record>> results = task.getValue();
            recentRecords.setAll(results.get(0));
            completeRecords.setAll(results.get(1));
        });

        task.setOnFailed(e -> {
            Throwable ex = task.getException();
            ex.printStackTrace();
            showToast((Stage) rootPane.getScene().getWindow(),
                    rootPane,
                    "Failed to load data: " + ex.getMessage(),
                    false);
        });

        MainApp.getExecutorService().submit(task);
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
            UserDao userDao = new UserDao(DatabaseConfig.getConnection());
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
            logger.error("Failed to open settings: {}", e.getMessage(), e);
            e.printStackTrace();
            showToast((Stage) rootPane.getScene().getWindow(),
                    rootPane,
                    "Failed to open settings: " + e.getMessage(),
                    false);
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
        Platform.runLater(() -> lorryField.requestFocus());
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
            showToast((Stage) rootPane.getScene().getWindow(),
                    rootPane,
                    "Please enter the Lorry Number",
                    false);
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

        Stage stage = (Stage) rootPane.getScene().getWindow();
        boolean confirmed = UiUtils.showConfirmation(
                stage,
                "Save Changes?",
                "Are you sure you want to change these changes?",
                false
        );
        if (!confirmed) {
            return;
        }

        String finalDriver = driver;
        String finalProduct = product;
        String finalCustomer = customer;
        Task<Void> saveTask = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                if (weighService.isPendingRecordAvailable(lorry) && weighService.hasFirstWeight()) {
                    weighService.saveSecondWeight(currentWeight);
                    printSecondTicket();
                    Platform.runLater(() -> {
                        recentRecords.remove(weighService.getActiveRecord());
                        resetRecord();
                        showToast((Stage) rootPane.getScene().getWindow(),
                                rootPane,
                                "Second Weight saved successfully!",
                                true);
                    });
                } else if (weighService.isPendingRecordAvailable(lorry) && !weighService.hasFirstWeight()) {
                    Platform.runLater(() -> {
                        showToast((Stage) rootPane.getScene().getWindow(),
                                rootPane,
                                "Please select the lorry from the table!",
                                false);
                        resetRecord();
                    });
                } else {
                    weighService.startTransaction(lorry, finalCustomer, finalProduct, finalDriver);
                    weighService.saveFirstWeight(currentWeight);
                    printFirstTicket();
                    Platform.runLater(() -> {
                        resetRecord();
                        showToast((Stage) rootPane.getScene().getWindow(),
                                rootPane,
                                "First Weight saved successfully!",
                                true);
                    });
                }
                loadTables();
                return null;
            }
        };

        saveTask.setOnFailed(e -> {
            Throwable ex = saveTask.getException();
            ex.printStackTrace();
            Platform.runLater(() -> showToast((Stage) rootPane.getScene().getWindow(),
                    rootPane,
                    "Save failed: " + ex.getMessage(),
                    false));
        });

        MainApp.getExecutorService().submit(saveTask);
    }

    private void printFirstTicket() {
        Record record = weighService.getActiveRecord();

        if (record == null) {
            showToast((Stage) rootPane.getScene().getWindow(),
                    rootPane,
                    "No active record to print",
                    false);
            return;
        }

        printReceipt(record, PrintMode.FIRST_WEIGHT);
    }

    private void printSecondTicket() {
        Record record = weighService.getFullRecord();

        if (record == null) {
            showToast((Stage) rootPane.getScene().getWindow(),
                    rootPane,
                    "No completed record to print",
                    false);
            return;
        }

        printReceipt(record, PrintMode.SECOND_WEIGHT);
    }

    private void printFullTicket() {
        Record record = weighService.getFullRecord();

        if (record == null) {
            showToast((Stage) rootPane.getScene().getWindow(),
                    rootPane,
                    "No completed record selected",
                    false);
            return;
        }

        printReceipt(record, PrintMode.FULL);
    }

    private void printReceipt(Record record, PrintMode mode) {
        Task<Boolean> printTask = new Task<Boolean>() {
            @Override
            protected Boolean call() throws Exception {
                PrintService printService = new PrintService();
                return printService.printReceiptSilent(record, mode);
            }
        };

        printTask.setOnFailed(e -> {
            Throwable ex = printTask.getException();
            ex.printStackTrace();
            Platform.runLater(() -> showToast((Stage) rootPane.getScene().getWindow(),
                    rootPane,
                    "Print failed: " + ex.getMessage(),
                    false));
        });

        printTask.setOnSucceeded(e -> {
            if (!printTask.getValue()) {
                showToast((Stage) rootPane.getScene().getWindow(),
                        rootPane,
                        "Print failed (see logs)",
                        false);
            }
        });

        MainApp.getExecutorService().submit(printTask);
    }

    public BorderPane getRootPane() {
        return rootPane;
    }
}