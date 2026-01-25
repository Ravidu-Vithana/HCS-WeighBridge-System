package com.hcs.weighbridge.model;

import javafx.beans.property.*;

public class Record {
    private long id;
    private final StringProperty lorryNumber = new SimpleStringProperty();
    private final StringProperty customerName = new SimpleStringProperty();
    private final StringProperty productName = new SimpleStringProperty();
    private final StringProperty driverName = new SimpleStringProperty();
    private final StringProperty dateIn = new SimpleStringProperty();
    private final StringProperty dateOut = new SimpleStringProperty();
    private final StringProperty timeIn = new SimpleStringProperty();
    private final StringProperty timeOut = new SimpleStringProperty();
    private final IntegerProperty firstWeight = new SimpleIntegerProperty();
    private final IntegerProperty secondWeight = new SimpleIntegerProperty();
    private final IntegerProperty netWeight = new SimpleIntegerProperty();

    // Getters and setters

    public Record(String lorryNumber) {
        this.lorryNumber.set(lorryNumber);
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getLorryNumber() { return lorryNumber.get(); }
    public void setLorryNumber(String lorryNumber) { this.lorryNumber.set(lorryNumber); }

    public int getFirstWeight() { return firstWeight.get(); }
    public void setFirstWeight(int weight) { this.firstWeight.set(weight); }

    public String getDateIn() { return dateIn.get(); }
    public void setDateIn(String dateIn) { this.dateIn.set(dateIn); }

    public String getDateOut() { return dateOut.get(); }
    public void setDateOut(String dateOut) { this.dateOut.set(dateOut); }

    public String getTimeIn() { return timeIn.get(); }
    public void setTimeIn(String timeIn) { this.timeIn.set(timeIn); }

    public String getTimeOut() { return timeOut.get(); }
    public void setTimeOut(String timeOut) { this.timeOut.set(timeOut); }

    public int getSecondWeight() { return secondWeight.get(); }
    public void setSecondWeight(int weight) { this.secondWeight.set(weight); }

    public int getNetWeight() { return netWeight.get(); }
    public void setNetWeight(int weight) { this.netWeight.set(weight); }

    public String getCustomerName() { return customerName.get(); }
    public void setCustomerName(String name) { this.customerName.set(name); }

    public String getProductName() { return productName.get(); }
    public void setProductName(String name) { this.productName.set(name); }

    public String getDriverName() { return driverName.get(); }
    public void setDriverName(String name) { this.driverName.set(name); }
}