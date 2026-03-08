package com.hcs.weighbridge.ui;

import com.hcs.weighbridge.MainApp;
import com.hcs.weighbridge.config.DatabaseConfig;
import com.hcs.weighbridge.dao.ConfigDao;
import com.hcs.weighbridge.dao.CompanyDao;
import com.hcs.weighbridge.constants.PrintMode;
import com.hcs.weighbridge.model.Record;
import com.hcs.weighbridge.model.CompanyInfo;
import com.hcs.weighbridge.service.PrintService;
import com.hcs.weighbridge.service.WeighService;
import com.hcs.weighbridge.util.LogUtil;
import com.hcs.weighbridge.util.UiScaler;
import com.hcs.weighbridge.util.UiUtils;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Modality;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;

public class RecordsController {
    private static final Logger logger = LogUtil.getLogger(RecordsController.class);

    @FXML
    private BorderPane rootPane;
    @FXML
    private Button backButton;
    @FXML
    private Button setFiltersButton;
    @FXML
    private Button printFullButton;
    @FXML
    private TableView<Record> completeRecordsTable;
    @FXML
    private Button prevButton;
    @FXML
    private Button nextButton;
    @FXML
    private Label pageLabel;

    private WeighService weighService;
    private UiScaler uiScaler;

    private final ObservableList<Record> completeRecords = FXCollections.observableArrayList();

    private int currentPage = 1;
    private final int rowsPerPage = 20;
    private int totalRecords = 0;
    private int totalPages = 1;

    private String currentLorryFilter = null;
    private String currentTicketFilter = null;
    private String currentFromDate = null;
    private String currentToDate = null;

    public void init(WeighService weighService, ConfigDao configDao) {
        this.weighService = weighService;

        double scaleFactor = configDao.getUiScaleFactor();
        this.uiScaler = new UiScaler(scaleFactor);

        setupTable();
        bindEvents();

        Platform.runLater(() -> {
            applyScaling();
            if (rootPane != null && rootPane.getScene() != null) {
                rootPane.getScene().getRoot().applyCss();
            }
            loadData();
        });
    }

    private void bindEvents() {
        backButton.setOnAction(e -> handleBack());
        setFiltersButton.setOnAction(e -> openFiltersModal());
        printFullButton.setOnAction(e -> printFullTicket());
        prevButton.setOnAction(e -> {
            if (currentPage > 1) {
                currentPage--;
                loadData();
            }
        });
        nextButton.setOnAction(e -> {
            if (currentPage < totalPages) {
                currentPage++;
                loadData();
            }
        });
    }

    private void applyScaling() {
        if (rootPane != null && uiScaler != null) {
            uiScaler.applyScaling(rootPane);
        }
    }

    private void handleBack() {
        Stage stage = (Stage) rootPane.getScene().getWindow();
        stage.close();
    }

    private void openFiltersModal() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/main/filters.fxml"));
            Parent root = loader.load();

            FiltersController controller = loader.getController();
            controller.init(this, currentLorryFilter, currentTicketFilter, currentFromDate, currentToDate);

            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.initOwner(rootPane.getScene().getWindow());
            stage.setTitle("Set Filters");

            Scene scene = new Scene(root);
            stage.setScene(scene);

            if (uiScaler != null) {
                uiScaler.applyScaling(root);
            }

            stage.showAndWait();
        } catch (Exception e) {
            logger.error("Failed to open filters modal", e);
            UiUtils.showToast((Stage) rootPane.getScene().getWindow(),
                    rootPane,
                    "Failed to open filters: " + e.getMessage(),
                    false);
        }
    }

    public void applyFilters(String lorryNo, String ticketNo, String fromDate, String toDate) {
        this.currentLorryFilter = lorryNo;
        this.currentTicketFilter = ticketNo;
        this.currentFromDate = fromDate;
        this.currentToDate = toDate;
        this.currentPage = 1;
        loadData();
    }

    private void printFullTicket() {
        Record selectedRecord = completeRecordsTable.getSelectionModel().getSelectedItem();

        if (selectedRecord == null) {
            UiUtils.showToast((Stage) rootPane.getScene().getWindow(),
                    rootPane,
                    "No completed record selected",
                    false);
            return;
        }

        UiUtils.showToast((Stage) rootPane.getScene().getWindow(),
                rootPane,
                "Please wait...",
                true);

        printReceipt(selectedRecord, PrintMode.FULL);
    }

    private void printReceipt(Record record, PrintMode mode) {
        Task<Void> printTask = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                PrintService printService = new PrintService();
                CompanyDao companyDao = new CompanyDao(DatabaseConfig.getConnection());
                CompanyInfo companyInfo = companyDao.getCompanyInfo();
                printService.printReceiptSilent(record, mode, companyInfo);
                return null;
            }
        };

        printTask.setOnFailed(e -> {
            Throwable ex = printTask.getException();
            logger.error("Print task failed", ex);
            Platform.runLater(() -> UiUtils.showToast((Stage) rootPane.getScene().getWindow(),
                    rootPane,
                    "Print failed: " + ex.getMessage(),
                    false));
        });

        printTask.setOnSucceeded(e -> {
            UiUtils.showToast((Stage) rootPane.getScene().getWindow(),
                    rootPane,
                    "Print job sent successfully",
                    true);
        });

        MainApp.getExecutorService().submit(printTask);
    }

    private void setupTable() {
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

        setupTableColumnResize();
    }

    private void setupTableColumnResize() {
        double[] completePrefWidths = {80, 80, 70, 70, 90, 100, 90, 90, 90, 120, 120, 120};
        double completeTotalMin = 0;
        for (double w : completePrefWidths) completeTotalMin += w;
        final double completeMin = completeTotalMin;

        completeRecordsTable.widthProperty().addListener((obs, oldVal, newVal) -> {
            double tableWidth = newVal.doubleValue();
            if (tableWidth <= 0) return;
            if (tableWidth >= completeMin) {
                completeRecordsTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
            } else {
                completeRecordsTable.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);
                for (int i = 0; i < completeRecordsTable.getColumns().size(); i++) {
                    completeRecordsTable.getColumns().get(i).setPrefWidth(completePrefWidths[i]);
                }
            }
        });
    }

    private void loadData() {
        Task<Void> task = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                totalRecords = weighService.getFilteredCompletedRecordsCount(currentLorryFilter, currentTicketFilter, currentFromDate, currentToDate);
                totalPages = (int) Math.ceil((double) totalRecords / rowsPerPage);
                if (totalPages == 0) totalPages = 1;

                if (currentPage > totalPages) {
                    currentPage = totalPages;
                }

                int offset = (currentPage - 1) * rowsPerPage;
                ArrayList<Record> records = weighService.getFilteredCompletedRecords(currentLorryFilter, currentTicketFilter, currentFromDate, currentToDate, offset, rowsPerPage);

                Platform.runLater(() -> {
                    completeRecords.setAll(records);
                    updatePaginationControls();
                });

                return null;
            }
        };

        task.setOnFailed(e -> {
            Throwable ex = task.getException();
            logger.error("Failed to load paginated records: {}", ex.getMessage(), ex);
            UiUtils.showToast((Stage) rootPane.getScene().getWindow(),
                    rootPane,
                    "Failed to load data: " + ex.getMessage(),
                    false);
        });

        MainApp.getExecutorService().submit(task);
    }

    private void updatePaginationControls() {
        pageLabel.setText(String.format("Page %d of %d", currentPage, totalPages));
        prevButton.setDisable(currentPage <= 1);
        nextButton.setDisable(currentPage >= totalPages);
    }
}
