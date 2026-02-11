package com.hcs.weighbridge.dao;

import com.hcs.weighbridge.constants.RecordStatus;
import com.hcs.weighbridge.model.Record;
import com.hcs.weighbridge.util.AppException;
import com.hcs.weighbridge.util.SecurityUtil;
import java.sql.*;
import java.util.ArrayList;

public class WeighDataDao {

    private final Connection connection;

    public WeighDataDao(Connection connection) {
        this.connection = connection;
    }

    public void createTransaction(Record record) {
        String sql = "INSERT INTO weigh_data (lorry_no, customer_name, product_name, driver_name) VALUES (?, ?, ?, ?)";

        try (PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, SecurityUtil.encrypt(record.getLorryNumber()));
            ps.setString(2, SecurityUtil.encrypt(record.getCustomerName()));
            ps.setString(3, SecurityUtil.encrypt(record.getProductName()));
            ps.setString(4, SecurityUtil.encrypt(record.getDriverName()));
            ps.executeUpdate();

            ResultSet rs = ps.getGeneratedKeys();
            if (rs.next()) {
                long id = rs.getLong(1);
                record.setId(id);
            }

        } catch (SQLException e) {
            throw new AppException("Failed to create transaction", e);
        } catch (Exception e) {
            throw new AppException("Critical error creating transaction", e);
        }
    }

    public void saveFirstWeight(long recordId, int weight, String date, String time) {
        String sql = "UPDATE weigh_data SET first_weight=?, date_in=?, time_in=?, status=? WHERE id=?";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, weight);
            ps.setString(2, date);
            ps.setString(3, time);
            ps.setString(4, RecordStatus.PENDING.toString());
            ps.setLong(5, recordId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new AppException("Failed to save first weight", e);
        }
    }

    public void saveSecondWeightAndComplete(long recordId, int secondWeight, String dateOut, String timeOut) {
        String sql = "UPDATE weigh_data " +
                "SET second_weight=?, " +
                "date_out=?, " +
                "time_out=?, " +
                "net_weight=ABS(second_weight - first_weight), " +
                "status=? " +
                "WHERE id=? AND first_weight IS NOT NULL";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, secondWeight);
            ps.setString(2, dateOut);
            ps.setString(3, timeOut);
            ps.setString(4, RecordStatus.COMPLETED.toString());
            ps.setLong(5, recordId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new AppException("Failed to complete transaction", e);
        }
    }

    public ArrayList<Record> getAllRecordsFromStatus(RecordStatus status) {
        String sql = "SELECT * FROM weigh_data WHERE status = ? ORDER BY id DESC";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, status.toString());

            ArrayList<Record> records = new ArrayList<>();

            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                records.add(getRecordFromResultSet(rs));
            }
            return records;

        } catch (SQLException e) {
            throw new AppException("Failed to retrieve " + status + " records", e);
        } catch (Exception e) {
            throw new AppException("Unexpected error retrieving records", e);
        }
    }

    public Record findById(long id) {
        String sql = "SELECT * FROM weigh_data WHERE id = ?";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, id);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                return getRecordFromResultSet(rs);
            }

            return null;

        } catch (SQLException e) {
            throw new AppException("Failed to load record by ID: " + id, e);
        } catch (Exception e) {
            throw new AppException("Unexpected error loading record", e);
        }
    }

    public Boolean isPendingRecordAvailable(String lorryNumber) {
        String sql = "SELECT * FROM weigh_data WHERE lorry_no = ? AND status=?";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, SecurityUtil.encrypt(lorryNumber));
            ps.setString(2, RecordStatus.PENDING.toString());
            ResultSet rs = ps.executeQuery();

            return rs.next();

        } catch (SQLException e) {
            throw new AppException("Failed to check pending record for lorry: " + lorryNumber, e);
        } catch (Exception e) {
            throw new AppException("Unexpected error checking pending record", e);
        }
    }

    private Record getRecordFromResultSet(ResultSet rs) throws Exception {
        Record record = new Record(SecurityUtil.decrypt(rs.getString("lorry_no")));
        record.setId(rs.getLong("id"));
        record.setDateIn(rs.getString("date_in"));
        record.setDateOut(rs.getString("date_out"));
        record.setTimeIn(rs.getString("time_in"));
        record.setTimeOut(rs.getString("time_out"));
        record.setFirstWeight(rs.getInt("first_weight"));
        record.setSecondWeight(rs.getInt("second_weight"));
        record.setNetWeight(rs.getInt("net_weight"));
        record.setCustomerName(SecurityUtil.decrypt(rs.getString("customer_name")));
        record.setProductName(SecurityUtil.decrypt(rs.getString("product_name")));
        record.setDriverName(SecurityUtil.decrypt(rs.getString("driver_name")));
        return record;
    }

}
