package com.hcs.weighbridge.ui;

import com.hcs.weighbridge.MainApp;
import com.hcs.weighbridge.dao.ConfigDao;
import com.hcs.weighbridge.model.Record;
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
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;

public class RecordsController {
    private static final Logger logger = LogUtil.getLogger(RecordsController.class);

    @FXML
    private BorderPane rootPane;
    @FXML
    private Button backButton;
    @FXML
    private TableView<Record> completeRecordsTable;
    @FXML
    private Button prevButton;
    @FXML
    private Button nextButton;
    @FXML
    private Label pageLabel;

    private WeighService weighService;
    private ConfigDao configDao;
    private UiScaler uiScaler;

    private final ObservableList<Record> completeRecords = FXCollections.observableArrayList();

    private int currentPage = 1;
    private int rowsPerPage = 20;
    private int totalRecords = 0;
    private int totalPages = 1;

    public void init(WeighService weighService, ConfigDao configDao) {
        this.weighService = weighService;
        this.configDao = configDao;

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
                totalRecords = weighService.getCompletedRecordsCount();
                totalPages = (int) Math.ceil((double) totalRecords / rowsPerPage);
                if (totalPages == 0) totalPages = 1;

                if (currentPage > totalPages) {
                    currentPage = totalPages;
                }

                int offset = (currentPage - 1) * rowsPerPage;
                ArrayList<Record> records = weighService.getCompletedRecordsWithPagination(offset, rowsPerPage);

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
