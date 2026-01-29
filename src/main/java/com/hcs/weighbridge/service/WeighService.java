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

    public Record startTransaction(String lorryNo, String customerName,
                                   String productName, String driverName) {

        activeRecord = new Record(lorryNo);
        activeRecord.setCustomerName(customerName);
        activeRecord.setProductName(productName);
        activeRecord.setDriverName(driverName);
        dao.createTransaction(activeRecord);

        return activeRecord;
    }

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

    public Record loadRecord(long id) {
        activeRecord = dao.findById(id);
        return activeRecord;
    }

    public Boolean isPendingRecordAvailable(String lorryNo) { return dao.isPendingRecordAvailable(lorryNo); }

    public Record getActiveRecord() {
        return activeRecord;
    }

    public void clearActiveRecord() {
        activeRecord = null;
    }

    public void clearFullRecord() {
        fullRecord = null;
    }

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

    public boolean isCompleted() {
        return activeRecord != null && activeRecord.getSecondWeight() > 0;
    }

}