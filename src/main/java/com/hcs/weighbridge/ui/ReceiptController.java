package com.hcs.weighbridge.ui;

import com.hcs.weighbridge.model.Record;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

public class ReceiptController {

    // Row containers
    @FXML private VBox rowHeader;
    @FXML private HBox rowMain;
    @FXML private HBox rowOut;
    @FXML private HBox rowNet;
    @FXML private GridPane rowSign;

    // Separators
    @FXML private Separator sepAfterMain;
    @FXML private Separator sepAfterOut;

    // Labels
    @FXML private Label receiptLbl, dateInLbl, timeInLbl, lorryLbl,
            firstWeightLbl, customerLbl, productLbl, driverLbl,
            dateOutLbl, timeOutLbl, secondWeightLbl, netWeightLbl;

    public enum PrintMode {
        FIRST_WEIGHT,
        SECOND_WEIGHT,
        FULL
    }

    public void setRecord(Record r) {
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

    public void setMode(PrintMode mode) {

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
                show(rowHeader, rowMain, rowOut, rowNet, rowSign);
                break;
        }
    }

    private void hide(javafx.scene.Node... nodes) {
        for (javafx.scene.Node n : nodes) {
            if (n != null) {
                n.setVisible(false);
                n.setManaged(false);
            }
        }
    }

    private void show(javafx.scene.Node... nodes) {
        for (javafx.scene.Node n : nodes) {
            if (n != null) {
                n.setVisible(true);
                n.setManaged(true);
            }
        }
    }
}
