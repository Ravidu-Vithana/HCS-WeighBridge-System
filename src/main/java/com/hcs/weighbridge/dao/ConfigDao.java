package com.hcs.weighbridge.dao;

import com.hcs.weighbridge.model.SerialConfig;
import com.hcs.weighbridge.util.AppException;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class ConfigDao {

    private final Connection connection;

    public ConfigDao(Connection connection) {
        this.connection = connection;
    }

    public SerialConfig loadSerialConfig() {
        SerialConfig cfg = new SerialConfig();

        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT config_key, config_value FROM app_config")) {

            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                String k = rs.getString("config_key");
                String v = rs.getString("config_value");

                switch (k) {
                    case "com_port":
                        cfg.setPortName(v);
                        break;
                    case "baud_rate":
                        cfg.setBaudRate(Integer.parseInt(v));
                        break;
                    case "data_bits":
                        cfg.setDataBits(Integer.parseInt(v));
                        break;
                    case "stop_bits":
                        cfg.setStopBits(Integer.parseInt(v));
                        break;
                    case "parity":
                        cfg.setParity(Integer.parseInt(v));
                        break;
                }
            }
        } catch (Exception e) {
            throw new AppException("Failed to load serial configuration", e);
        }

        return cfg;
    }

    public void saveSerialConfig(SerialConfig cfg) {
        save("com_port", cfg.getPortName());
        save("baud_rate", String.valueOf(cfg.getBaudRate()));
        save("data_bits", String.valueOf(cfg.getDataBits()));
        save("stop_bits", String.valueOf(cfg.getStopBits()));
        save("parity", String.valueOf(cfg.getParity()));
    }

    public double getUiScaleFactor() {
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT config_value FROM app_config WHERE config_key = ?")) {
            ps.setString(1, "ui_scale_factor");
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                return Double.parseDouble(rs.getString("config_value"));
            }
        } catch (SQLException e) {
            throw new AppException("Failed to load UI scale factor", e);
        } catch (Exception e) {
            throw new AppException("Unexpected error loading UI scale factor", e);
        }
        return 1.0; // Default scale
    }

    public void saveUiScaleFactor(double scaleFactor) {
        save("ui_scale_factor", String.valueOf(scaleFactor));
    }

    public boolean isAutoRestoreEnabled() {
        return Boolean.parseBoolean(getValue("auto_restore_enabled", "true"));
    }

    public void setAutoRestoreEnabled(boolean enabled) {
        save("auto_restore_enabled", String.valueOf(enabled));
    }

    public String getBackupFrequency() {
        return getValue("backup_frequency", "Weekly");
    }

    public void setBackupFrequency(String frequency) {
        save("backup_frequency", frequency);
    }

    public String getLastBackupDate() {
        return getValue("last_backup_date", null);
    }

    public void setLastBackupDate(String date) {
        save("last_backup_date", date);
    }

    private String getValue(String key, String defaultValue) {
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT config_value FROM app_config WHERE config_key = ?")) {
            ps.setString(1, key);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                return rs.getString("config_value");
            }
        } catch (SQLException e) {
            throw new AppException("Failed to load config for key: " + key, e);
        }
        return defaultValue;
    }

    private void save(String key, String value) {
        try (PreparedStatement ps = connection.prepareStatement(
                "REPLACE INTO app_config (config_key, config_value) VALUES (?, ?)")) {

            ps.setString(1, key);
            ps.setString(2, value);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new AppException("Failed to save config key: " + key, e);
        }
    }
}