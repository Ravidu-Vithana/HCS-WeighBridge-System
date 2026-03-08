package com.hcs.weighbridge.ui;

import javafx.fxml.FXML;
import javafx.scene.control.DatePicker;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class FiltersController {

    @FXML
    private TextField lorryField;

    @FXML
    private TextField ticketField;

    @FXML
    private DatePicker fromDatePicker;

    @FXML
    private DatePicker toDatePicker;

    private RecordsController parentController;
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public void init(RecordsController parentController, String currentLorry, String currentTicket, String currentFromDate, String currentToDate) {
        this.parentController = parentController;

        if (currentLorry != null) lorryField.setText(currentLorry);
        if (currentTicket != null) ticketField.setText(currentTicket);
        
        if (currentFromDate != null && !currentFromDate.trim().isEmpty()) {
            fromDatePicker.setValue(LocalDate.parse(currentFromDate, dateFormatter));
        }
        if (currentToDate != null && !currentToDate.trim().isEmpty()) {
            toDatePicker.setValue(LocalDate.parse(currentToDate, dateFormatter));
        }
        fromDatePicker.setEditable(false);
        toDatePicker.setEditable(false);
    }

    @FXML
    private void clearAllFilters() {
        lorryField.clear();
        ticketField.clear();
        fromDatePicker.setValue(null);
        toDatePicker.setValue(null);
    }

    @FXML
    private void clearDates() {
        fromDatePicker.setValue(null);
        toDatePicker.setValue(null);
    }

    @FXML
    private void applyFilters() {
        String lorryNo = lorryField.getText();
        String ticketNo = ticketField.getText();
        String fromDate = fromDatePicker.getValue() != null ? fromDatePicker.getValue().format(dateFormatter) : null;
        String toDate = toDatePicker.getValue() != null ? toDatePicker.getValue().format(dateFormatter) : null;

        parentController.applyFilters(lorryNo, ticketNo, fromDate, toDate);
        close();
    }

    @FXML
    private void close() {
        Stage stage = (Stage) lorryField.getScene().getWindow();
        stage.close();
    }
}
