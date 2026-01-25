package com.hcs.weighbridge.dao;

import com.hcs.weighbridge.model.SerialConfig;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class ConfigDao {

    private final Connection connection;

    public ConfigDao(Connection connection) {
        this.connection = connection;
    }

    public SerialConfig loadSerialConfig() {
        SerialConfig cfg = new SerialConfig();

        try (PreparedStatement ps =
                     connection.prepareStatement(
                             "SELECT config_key, config_value FROM app_config")) {

            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                String k = rs.getString("config_key");
                String v = rs.getString("config_value");

                switch (k) {
                    case "com_port": cfg.setPortName(v); break;
                    case "baud_rate": cfg.setBaudRate(Integer.parseInt(v)); break;
                    case "data_bits": cfg.setDataBits(Integer.parseInt(v)); break;
                    case "stop_bits": cfg.setStopBits(Integer.parseInt(v)); break;
                    case "parity": cfg.setParity(Integer.parseInt(v)); break;
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to load config", e);
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
        try (PreparedStatement ps =
                     connection.prepareStatement(
                             "SELECT config_value FROM app_config WHERE config_key = ?")) {
            ps.setString(1, "ui_scale_factor");
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                return Double.parseDouble(rs.getString("config_value"));
            }
        } catch (Exception e) {
            // If not found or error, return default
            System.err.println("Failed to load UI scale factor: " + e.getMessage());
        }
        return 1.0; // Default scale
    }

    public void saveUiScaleFactor(double scaleFactor) {
        save("ui_scale_factor", String.valueOf(scaleFactor));
    }

    private void save(String key, String value) {
        try (PreparedStatement ps =
                     connection.prepareStatement(
                             "REPLACE INTO app_config (config_key, config_value) VALUES (?, ?)")) {

            ps.setString(1, key);
            ps.setString(2, value);
            ps.executeUpdate();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}