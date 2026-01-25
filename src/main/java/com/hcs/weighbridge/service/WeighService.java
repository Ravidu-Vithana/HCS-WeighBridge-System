package com.hcs.weighbridge.service;

import com.hcs.weighbridge.dao.WeighDataDao;
import com.hcs.weighbridge.model.Record;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class WeighService {

    private final WeighDataDao dao;
    private Record activeRecord;
    private Record fullRecord;
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");

    public WeighService(WeighDataDao dao) {
        this.dao = dao;
    }

    /**
     * Starts a new weigh transaction and saves initial record to database
     */
    public Record startTransaction(String lorryNo, String customerName,
                                   String productName, String driverName) {

        activeRecord = new Record(lorryNo);
        activeRecord.setCustomerName(customerName);
        activeRecord.setProductName(productName);
        activeRecord.setDriverName(driverName);
        dao.createTransaction(activeRecord);

        return activeRecord;
    }

    /**
     * Saves the first weight for the active transaction
     */
    public void saveFirstWeight(int weight) {
        if (activeRecord == null) {
            throw new IllegalStateException("No active transaction");
        }

        LocalDateTime now = LocalDateTime.now();
        String date = now.format(dateFormatter);
        String time = now.format(timeFormatter);

        activeRecord.setFirstWeight(weight);
        activeRecord.setDateIn(date);
        activeRecord.setTimeIn(time);

        dao.saveFirstWeight(activeRecord.getId(), weight, date, time);
    }

    /**
     * Saves the second weight and completes the transaction
     */
    public Record saveSecondWeight(int weight) {
        if (activeRecord == null) {
            throw new IllegalStateException("No active transaction");
        }

        LocalDateTime now = LocalDateTime.now();
        String dateOut = now.format(dateFormatter);
        String timeOut = now.format(timeFormatter);

        int netWeight = Math.abs(weight - activeRecord.getFirstWeight());

        activeRecord.setSecondWeight(weight);
        activeRecord.setDateOut(dateOut);
        activeRecord.setTimeOut(timeOut);
        activeRecord.setNetWeight(netWeight);

        dao.saveSecondWeightAndComplete(activeRecord.getId(), weight, dateOut, timeOut);

        Record completedRecord = activeRecord;
        fullRecord = activeRecord;
        activeRecord = null;

        return completedRecord;
    }

    /**
     * Loads a record by ID for editing or completion
     */
    public Record loadRecord(long id) {
        activeRecord = dao.findById(id);
        return activeRecord;
    }

    public Boolean isPendingRecordAvailble(String lorryNo) {
        return dao.isPendingRecordAvailable(lorryNo);
    }

    /**
     * Gets the current active record
     */
    public Record getActiveRecord() {
        return activeRecord;
    }

    /**
     * Clears the active record without completing it
     */
    public void clearActiveRecord() {
        activeRecord = null;
    }

    public void clearFullRecord() {
        fullRecord = null;
    }

    /**
     * Checks if active transaction has first weight
     */
    public boolean hasFirstWeight() {
        return activeRecord != null && activeRecord.getFirstWeight() > 0;
    }

    public void setFirstWeightRecord(Record record) {
        activeRecord = record;
    }

    public Record getFullRecord() {
        return fullRecord;
    }

    public void setFullRecord(Record record) {
        fullRecord = record;
    }

    /**
     * Checks if active transaction is completed
     */
    public boolean isCompleted() {
        return activeRecord != null && activeRecord.getSecondWeight() > 0;
    }
}