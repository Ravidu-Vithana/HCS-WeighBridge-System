package com.hcs.weighbridge.ui;

import javafx.beans.property.*;

public class UiModel {

    private final IntegerProperty liveWeight = new SimpleIntegerProperty(0);
    private final StringProperty status = new SimpleStringProperty("—");

    private final StringProperty receiptNo = new SimpleStringProperty("—");
    private final StringProperty lorryNo = new SimpleStringProperty("");

    private final DoubleProperty firstWeight = new SimpleDoubleProperty();
    private final DoubleProperty secondWeight = new SimpleDoubleProperty();
    private final DoubleProperty netWeight = new SimpleDoubleProperty();

    public IntegerProperty liveWeightProperty() {
        return liveWeight;
    }

    public StringProperty statusProperty() {
        return status;
    }

    public StringProperty receiptNoProperty() {
        return receiptNo;
    }

    public StringProperty lorryNoProperty() {
        return lorryNo;
    }

    public DoubleProperty firstWeightProperty() {
        return firstWeight;
    }

    public DoubleProperty secondWeightProperty() {
        return secondWeight;
    }

    public DoubleProperty netWeightProperty() {
        return netWeight;
    }

    public void updateLive(int weight, String statusText) {
        this.liveWeight.set(weight);
        this.status.set(statusText);
    }

    public void setReceipt(String receipt) {
        this.receiptNo.set(receipt);
    }

    public void setLorryNo(String lorry) {
        this.lorryNo.set(lorry);
    }

    public void setFirstWeight(double w) {
        this.firstWeight.set(w);
    }

    public void setSecondWeight(double w) {
        this.secondWeight.set(w);
        this.netWeight.set(Math.abs(w - firstWeight.get()));
    }

    public void reset() {
        liveWeight.set(0);
        status.set("—");
        receiptNo.set("—");
        lorryNo.set("");
        firstWeight.set(0.0);
        secondWeight.set(0.0);
        netWeight.set(0.0);
    }
}
