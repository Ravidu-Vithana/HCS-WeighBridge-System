package com.hcs.weighbridge.service;

import com.hcs.weighbridge.model.Record;
import com.hcs.weighbridge.ui.ReceiptController;
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
     * @return true if printing was successful, false otherwise
     */
    public boolean printReceipt(Record record, ReceiptController.PrintMode mode) {
        return printReceiptInternal(record, mode, null, true);
    }

    /**
     * Prints a receipt silently without showing the print dialog.
     * Uses default printer and settings.
     *
     * @param record The record to print
     * @param mode   The print mode
     * @return true if successful
     */
    public boolean printReceiptSilent(Record record, ReceiptController.PrintMode mode) {
        return printReceiptInternal(record, mode, null, false);
    }

    /**
     * Prints a receipt to a specific printer by name.
     *
     * @param record      The record to print
     * @param mode        The print mode
     * @param printerName Name of the printer to use
     * @return true if successful
     */
    public boolean printReceiptWithPrinter(Record record, ReceiptController.PrintMode mode, String printerName) {
        javax.print.PrintService selectedPrinter = findPrinter(printerName);
        if (selectedPrinter == null) {
            logger.error("Printer not found: {}", printerName);
            return false;
        }
        return printReceiptInternal(record, mode, selectedPrinter, false);
    }

    private boolean printReceiptInternal(Record record, ReceiptController.PrintMode mode,
                                         javax.print.PrintService specificPrinter, boolean showDialog) {
        logger.info("=== Starting JasperReports print job ===");
        logger.info("Record ID: {}, Mode: {}", record.getId(), mode);

        try {
            // 1. Compile Report
            InputStream reportStream = getClass().getResourceAsStream(RECEIPT_JRXML_PATH);
            if (reportStream == null) {
                logger.error("Could not find report template: {}", RECEIPT_JRXML_PATH);
                return false;
            }
            JasperReport jasperReport = JasperCompileManager.compileReport(reportStream);

            // 2. Prepare Parameters
            Map<String, Object> parameters = new HashMap<>();
            parameters.put("PrintMode", mode.name());
            // Add any other scalar parameters if needed

            // 3. Prepare Data Source
            JRBeanCollectionDataSource dataSource = new JRBeanCollectionDataSource(Collections.singletonList(record));

            // 4. Fill Report
            JasperPrint jasperPrint = JasperFillManager.fillReport(jasperReport, parameters, dataSource);

            // 5. Print
            if (showDialog) {
                // Use JasperPrintManager to show standard dialog
                return JasperPrintManager.printReport(jasperPrint, true);
            } else {
                // Silent printing
                JRPrintServiceExporter exporter = new JRPrintServiceExporter();
                exporter.setExporterInput(new SimpleExporterInput(jasperPrint));

                SimplePrintServiceExporterConfiguration configuration = new SimplePrintServiceExporterConfiguration();

                // Configure Print Request Attributes for A4 Portrait (Hack for A5 paper treated
                // as A4)
                PrintRequestAttributeSet printRequestAttributeSet = new HashPrintRequestAttributeSet();
                printRequestAttributeSet.add(MediaSizeName.ISO_A4);
                printRequestAttributeSet.add(OrientationRequested.PORTRAIT);
                // We don't need to restrict printable area if the report design handles it.
                // The report is designed to use only the top ~148mm (A5 height).

                configuration.setPrintRequestAttributeSet(printRequestAttributeSet);

                if (specificPrinter != null) {
                    configuration.setPrintService(specificPrinter);
                } else {
                    // Use default printer if 'specificPrinter' is null
                    // However, JRPrintServiceExporter defaults to looking up default if not set?
                    // Actually, if we don't set PrintService, it might fail or look for default.
                    // Ideally we find the default printer explicitly if none provided.
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
                return true;
            }

        } catch (JRException e) {
            logger.error("JasperReports error: {}", e.getMessage(), e);
            return false;
        } catch (Exception e) {
            logger.error("Unexpected error: {}", e.getMessage(), e);
            return false;
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
