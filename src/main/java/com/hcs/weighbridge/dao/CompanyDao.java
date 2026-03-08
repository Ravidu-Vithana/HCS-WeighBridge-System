package com.hcs.weighbridge.dao;

import com.hcs.weighbridge.model.CompanyInfo;
import com.hcs.weighbridge.exceptions.AppException;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class CompanyDao {
    private final Connection connection;

    public CompanyDao(Connection connection) {
        this.connection = connection;
    }

    public CompanyInfo getCompanyInfo() {
        CompanyInfo info = new CompanyInfo();
        String sql = "SELECT * FROM company_info LIMIT 1";

        try (PreparedStatement ps = connection.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            if (rs.next()) {
                info.setCompanyName(rs.getString("company_name"));
                info.setCompanyAddress(rs.getString("company_address"));
                info.setContactNumber1(rs.getString("contact_number1"));
                info.setContactNumber2(rs.getString("contact_number2"));
            } else {
                // Return default hardcoded values if no row exists yet to prevent empty fields on UI load
                info.setCompanyName("YOUR COMPANY NAME");
                info.setCompanyAddress("Your Company Address");
                info.setContactNumber1("Contact Number 1");
                info.setContactNumber2("Contact Number 2");
            }
        } catch (SQLException e) {
            throw new AppException("Failed to load company info", e);
        }
        return info;
    }

    public void saveCompanyInfo(CompanyInfo info) {
        String sql = "INSERT INTO company_info (id, company_name, company_address, contact_number1, contact_number2) " +
                     "VALUES (1, ?, ?, ?, ?) " +
                     "ON DUPLICATE KEY UPDATE " +
                     "company_name = VALUES(company_name), " +
                     "company_address = VALUES(company_address), " +
                     "contact_number1 = VALUES(contact_number1), " +
                     "contact_number2 = VALUES(contact_number2)";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, info.getCompanyName());
            ps.setString(2, info.getCompanyAddress());
            ps.setString(3, info.getContactNumber1());
            ps.setString(4, info.getContactNumber2());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new AppException("Failed to save company info", e);
        }
    }
}
