package com.hcs.weighbridge.model;

public class CompanyInfo {
    private String companyName;
    private String companyAddress;
    private String contactNumber1;
    private String contactNumber2;

    public CompanyInfo() {
    }

    public CompanyInfo(String companyName, String companyAddress, String contactNumber1, String contactNumber2) {
        this.companyName = companyName;
        this.companyAddress = companyAddress;
        this.contactNumber1 = contactNumber1;
        this.contactNumber2 = contactNumber2;
    }

    public String getCompanyName() {
        return companyName;
    }

    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }

    public String getCompanyAddress() {
        return companyAddress;
    }

    public void setCompanyAddress(String companyAddress) {
        this.companyAddress = companyAddress;
    }

    public String getContactNumber1() {
        return contactNumber1;
    }

    public void setContactNumber1(String contactNumber1) {
        this.contactNumber1 = contactNumber1;
    }

    public String getContactNumber2() {
        return contactNumber2;
    }

    public void setContactNumber2(String contactNumber2) {
        this.contactNumber2 = contactNumber2;
    }
}
