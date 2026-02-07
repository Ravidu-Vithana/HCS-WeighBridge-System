package com.hcs.weighbridge.ui;

import com.hcs.weighbridge.model.Record;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

public class ReceiptController {

    @FXML
    private VBox root;

    // ================= ROW CONTAINERS =================
    @FXML private VBox rowHeader;
    @FXML private HBox rowMain;
    @FXML private HBox rowOut;
    @FXML private HBox rowNet;
    @FXML private GridPane rowSign;

    // ================= SEPARATORS =================
    @FXML private Separator sepAfterMain;
    @FXML private Separator sepAfterOut;

    // ================= LABELS =================
    @FXML private Label receiptLbl;
    @FXML private Label dateInLbl;
    @FXML private Label timeInLbl;
    @FXML private Label lorryLbl;
    @FXML private Label firstWeightLbl;

    @FXML private Label customerLbl;
    @FXML private Label productLbl;
    @FXML private Label driverLbl;

    @FXML private Label dateOutLbl;
    @FXML private Label timeOutLbl;
    @FXML private Label secondWeightLbl;

    @FXML private Label netWeightLbl;

    // ================= PRINT MODES =================
    public enum PrintMode {
        FIRST_WEIGHT,
        SECOND_WEIGHT,
        FULL
    }

    @FXML
    private void initialize() {
        // A5 portrait dimensions in points: 420 x 595 (printer sees it as portrait)
        root.setPrefWidth(420);   // 148mm in points
        root.setPrefHeight(595);  // 210mm in points

        // Make root fill its parent
        root.setMaxWidth(Double.MAX_VALUE);
        root.setMaxHeight(Double.MAX_VALUE);

        // Apply CSS to ensure proper layout calculation
        Platform.runLater(() -> {
            root.applyCss();
            root.layout();
        });
    }

    // ================= DATA BINDING =================
    public void setRecord(Record r) {

        // NOTE: ensure ID is generated before printing if this matters
        receiptLbl.setText(String.valueOf(r.getId()));

        dateInLbl.setText(r.getDateIn());
        timeInLbl.setText(r.getTimeIn());
        lorryLbl.setText(r.getLorryNumber());
        firstWeightLbl.setText(r.getFirstWeight() + " kg");

        customerLbl.setText(r.getCustomerName());
        productLbl.setText(r.getProductName());
        driverLbl.setText(r.getDriverName());

        dateOutLbl.setText(r.getDateOut());
        timeOutLbl.setText(r.getTimeOut());
        secondWeightLbl.setText(r.getSecondWeight() + " kg");

        netWeightLbl.setText(r.getNetWeight() + " kg");
    }

    // ================= MODE HANDLING =================
    public void setMode(PrintMode mode) {

        // Reset everything first (VERY IMPORTANT)
        show(rowHeader, rowMain, sepAfterMain, rowOut, sepAfterOut, rowNet, rowSign);

        switch (mode) {

            case FIRST_WEIGHT:
                show(rowHeader, rowMain, rowSign);
                hide(rowOut, rowNet, sepAfterOut);
                break;

            case SECOND_WEIGHT:
                hide(rowHeader, rowMain, rowSign, sepAfterMain);
                show(rowOut, rowNet);
                break;

            case FULL:
                // Everything already visible
                break;
        }
    }

    // ================= VISIBILITY HELPERS =================
    private void hide(Node... nodes) {
        for (Node n : nodes) {
            if (n != null) {
                n.setVisible(false);
                n.setManaged(true);
            }
        }
    }

    private void show(Node... nodes) {
        for (Node n : nodes) {
            if (n != null) {
                n.setVisible(true);
                n.setManaged(true);
            }
        }
    }
}
