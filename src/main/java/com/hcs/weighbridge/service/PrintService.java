package com.hcs.weighbridge.service;

import com.hcs.weighbridge.constants.PrintMode;
import com.hcs.weighbridge.model.Record;
import com.hcs.weighbridge.util.AppException;
import com.hcs.weighbridge.util.LogUtil;
import net.sf.jasperreports.engine.*;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;
import net.sf.jasperreports.engine.export.JRPrintServiceExporter;
import net.sf.jasperreports.export.SimpleExporterInput;
import net.sf.jasperreports.export.SimplePrintServiceExporterConfiguration;
import org.apache.logging.log4j.Logger;

import javax.print.PrintServiceLookup;
import javax.print.attribute.HashPrintRequestAttributeSet;
import javax.print.attribute.PrintRequestAttributeSet;
import javax.print.attribute.standard.MediaSizeName;
import javax.print.attribute.standard.OrientationRequested;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Service class for printing weighbridge receipts using JasperReports.
 * Supports three print modes: FIRST_WEIGHT, SECOND_WEIGHT, and FULL.
 * Handles two-pass printing where the same paper is inserted twice.
 */
public class PrintService {

    private static final Logger logger = LogUtil.getLogger(PrintService.class);
    private static final String RECEIPT_JRXML_PATH = "/reports/receipt.jrxml";

    public PrintService() {
        logger.info("PrintService initialized (JasperReports backend)");
    }

    /**
     * Prints a receipt for the given record in the specified mode.
     * Shows print dialog for user to confirm settings.
     *
     * @param record The record to print
     * @param mode   The print mode (FIRST_WEIGHT, SECOND_WEIGHT, or FULL)
     */
    public void printReceipt(Record record, PrintMode mode) {
        printReceiptInternal(record, mode, null, true);
    }

    /**
     * Prints a receipt silently without showing the print dialog.
     * Uses default printer and settings.
     *
     * @param record The record to print
     * @param mode   The print mode
     */
    public void printReceiptSilent(Record record, PrintMode mode) {
        printReceiptInternal(record, mode, null, false);
    }

    /**
     * Prints a receipt to a specific printer by name.
     *
     * @param record      The record to print
     * @param mode        The print mode
     * @param printerName Name of the printer to use
     */
    public void printReceiptWithPrinter(Record record, PrintMode mode, String printerName) {
        javax.print.PrintService selectedPrinter = findPrinter(printerName);
        if (selectedPrinter == null) {
            throw new AppException("Printer not found: " + printerName);
        }
        printReceiptInternal(record, mode, selectedPrinter, false);
    }

    private void printReceiptInternal(Record record, PrintMode mode,
            javax.print.PrintService specificPrinter, boolean showDialog) {
        logger.info("=== Starting JasperReports print job ===");
        logger.info("Record ID: {}, Mode: {}", record.getId(), mode);

        try {
            // 1. Compile Report
            InputStream reportStream = getClass().getResourceAsStream(RECEIPT_JRXML_PATH);
            if (reportStream == null) {
                throw new AppException("Could not find report template: " + RECEIPT_JRXML_PATH);
            }
            JasperReport jasperReport = JasperCompileManager.compileReport(reportStream);

            // 2. Prepare Parameters
            Map<String, Object> parameters = new HashMap<>();
            parameters.put("PrintMode", mode.name());
            parameters.put("CompanyName", "HORAWADUNNA COPRA STORES WEIGHBRIDGE");
            parameters.put("CompanyAddress", "283/1, Bammanna Road, Kudalupoththa, Narangoda.");
            parameters.put("CompanyContactNumber1", "0776136447");
            parameters.put("CompanyContactNumber2", "0372246292");
            parameters.put("Technology", "Technology Provided by HCS Solutions +94 76 3738202");

            // 3. Prepare Data Source
            JRBeanCollectionDataSource dataSource = new JRBeanCollectionDataSource(Collections.singletonList(record));

            // 4. Fill Report
            JasperPrint jasperPrint = JasperFillManager.fillReport(jasperReport, parameters, dataSource);

            if (showDialog) {
                if (!JasperPrintManager.printReport(jasperPrint, true)) {
                    throw new AppException("Printing cancelled or failed by user");
                }
            } else {
                JRPrintServiceExporter exporter = new JRPrintServiceExporter();
                exporter.setExporterInput(new SimpleExporterInput(jasperPrint));

                SimplePrintServiceExporterConfiguration configuration = new SimplePrintServiceExporterConfiguration();

                PrintRequestAttributeSet printRequestAttributeSet = new HashPrintRequestAttributeSet();
                printRequestAttributeSet.add(MediaSizeName.ISO_A4);
                printRequestAttributeSet.add(OrientationRequested.PORTRAIT);

                configuration.setPrintRequestAttributeSet(printRequestAttributeSet);

                if (specificPrinter != null) {
                    configuration.setPrintService(specificPrinter);
                } else {
                    javax.print.PrintService defaultPrinter = PrintServiceLookup.lookupDefaultPrintService();
                    if (defaultPrinter != null) {
                        configuration.setPrintService(defaultPrinter);
                    } else {
                        logger.warn(
                                "No default printer found, letting JasperReports handle it (might fail or show dialog)");
                    }
                }

                configuration.setDisplayPageDialog(false);
                configuration.setDisplayPrintDialog(false);

                exporter.setConfiguration(configuration);
                exporter.exportReport();

                logger.info("Print job sent successfully");
            }

        } catch (JRException e) {
            throw new AppException("JasperReports error: " + e.getMessage(), e);
        } catch (AppException e) {
            throw e;
        } catch (Exception e) {
            throw new AppException("Unexpected printing error: " + e.getMessage(), e);
        }
    }

    private javax.print.PrintService findPrinter(String printerName) {
        javax.print.PrintService[] services = PrintServiceLookup.lookupPrintServices(null, null);
        for (javax.print.PrintService service : services) {
            if (service.getName().equalsIgnoreCase(printerName)) {
                return service;
            }
        }
        return null;
    }

    /**
     * Legacy method for listing printers (optional, kept for compatibility if
     * needed)
     */
    public static void listAvailablePrinters() {
        javax.print.PrintService[] services = PrintServiceLookup.lookupPrintServices(null, null);
        for (javax.print.PrintService service : services) {
            System.out.println("Printer: " + service.getName());
        }
    }
}
