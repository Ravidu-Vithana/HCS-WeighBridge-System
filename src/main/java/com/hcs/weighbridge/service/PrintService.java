package com.hcs.weighbridge.service;

import com.hcs.weighbridge.model.Record;
import com.hcs.weighbridge.ui.ReceiptController;
import com.hcs.weighbridge.util.LogUtil;
import com.sun.javafx.print.PrintHelper;
import com.sun.javafx.print.Units;
import javafx.fxml.FXMLLoader;
import javafx.print.*;
import javafx.scene.Node;
import javafx.scene.control.Separator;
import javafx.scene.layout.*;
import javafx.scene.transform.Scale;
import org.apache.logging.log4j.Logger;

import java.io.IOException;

/**
 * Service class for printing weighbridge receipts on A5 landscape paper.
 * Supports three print modes: FIRST_WEIGHT, SECOND_WEIGHT, and FULL.
 * Handles two-pass printing where the same paper is inserted twice.
 */
public class PrintService {

    private static final Logger logger = LogUtil.getLogger(PrintService.class);

    // A5 dimensions in millimeters
    private static final double A5_WIDTH_MM = 210.0;   // A5 long edge
    private static final double A5_HEIGHT_MM = 148.0;  // A5 short edge

    private static final double MM_TO_POINTS = 2.83465; // Conversion factor

    // A5 dimensions in points (PostScript points: 1/72 inch)
    private static final double A5_WIDTH_POINTS = A5_WIDTH_MM * MM_TO_POINTS;   // ~595 points
    private static final double A5_HEIGHT_POINTS = A5_HEIGHT_MM * MM_TO_POINTS; // ~420 points

    // FXML resource path
    private static final String RECEIPT_FXML_PATH = "/main/receipt.fxml";

    public PrintService() {
        logger.info("PrintService initialized");
        logger.debug("A5 dimensions - Width: {}mm ({}pt), Height: {}mm ({}pt)",
                A5_WIDTH_MM, A5_WIDTH_POINTS, A5_HEIGHT_MM, A5_HEIGHT_POINTS);
    }

    /**
     * Prints a receipt for the given record in the specified mode.
     * Shows print dialog for user to confirm settings.
     *
     * @param record The record to print
     * @param mode The print mode (FIRST_WEIGHT, SECOND_WEIGHT, or FULL)
     * @return true if printing was successful, false otherwise
     */
    public boolean printReceipt(Record record, ReceiptController.PrintMode mode) {
        logger.info("=== Starting print job for receipt #{} in mode: {} ===", record.getId(), mode);

        try {
            // Load FXML and controller
            logger.debug("Loading FXML from: {}", RECEIPT_FXML_PATH);
            FXMLLoader loader = new FXMLLoader(getClass().getResource(RECEIPT_FXML_PATH));
            VBox receiptNode = loader.load();
            ReceiptController controller = loader.getController();
            logger.debug("FXML loaded successfully, controller obtained");

            // Set data and mode
            logger.debug("Setting record data - ID: {}, Lorry: {}, Customer: {}",
                    record.getId(), record.getLorryNumber(), record.getCustomerName());
            controller.setRecord(record);

            logger.debug("Setting print mode: {}", mode);
            controller.setMode(mode);

            // Get printer
            Printer printer = Printer.getDefaultPrinter();
            if (printer == null) {
                logger.error("No default printer found!");
                System.err.println("ERROR: No printer found!");
                return false;
            }
            logger.info("Using printer: {}", printer.getName());

            // Create print job
            PrinterJob job = PrinterJob.createPrinterJob(printer);
            if (job == null) {
                logger.error("Could not create printer job for printer: {}", printer.getName());
                System.err.println("ERROR: Could not create printer job!");
                return false;
            }
            logger.debug("Printer job created successfully");

            // Configure page layout
            PageLayout pageLayout = configurePageLayout(printer);
            if (pageLayout == null) {
                logger.error("Could not configure page layout!");
                System.err.println("ERROR: Could not configure page layout!");
                job.endJob();
                return false;
            }
            logPageLayout(pageLayout);

            // Show print dialog
            logger.debug("Showing print dialog...");
            boolean proceed = job.showPrintDialog(null);
            if (!proceed) {
                logger.info("User cancelled print dialog");
                job.endJob();
                return false;
            }
            logger.debug("User confirmed print dialog");

            // Scale content to fit page
            scaleNodeToFitPage(receiptNode, pageLayout);

            // Print the page
            logger.info("Sending page to printer...");
            boolean success = job.printPage(pageLayout, receiptNode);

            if (success) {
                logger.info("Page sent to printer successfully");
                job.endJob();
                logger.info("=== Print job completed successfully ===");
                System.out.println("Receipt printed successfully!");
                return true;
            } else {
                logger.error("Failed to print page!");
                System.err.println("ERROR: Failed to print page!");
                job.endJob();
                return false;
            }

        } catch (IOException e) {
            logger.error("Error loading receipt FXML: {}", e.getMessage(), e);
            System.err.println("ERROR: Could not load receipt template: " + e.getMessage());
            return false;
        } catch (Exception e) {
            logger.error("Unexpected error printing receipt: {}", e.getMessage(), e);
            System.err.println("ERROR: Unexpected error during printing: " + e.getMessage());
            return false;
        }
    }

    /**
     * Prints a receipt silently without showing the print dialog.
     * Uses default printer and settings.
     *
     * @param record The record to print
     * @param mode The print mode
     * @return true if successful
     */
    public boolean printReceiptSilent(Record record, ReceiptController.PrintMode mode) {
        logger.info("=== Starting SILENT print job for receipt #{} in mode: {} ===", record.getId(), mode);

        try {
            logger.debug("Loading FXML from: {}", RECEIPT_FXML_PATH);
            FXMLLoader loader = new FXMLLoader(getClass().getResource(RECEIPT_FXML_PATH));
            VBox receiptNode = loader.load();
            ReceiptController controller = loader.getController();
            logger.debug("FXML loaded successfully");

            logger.debug("Setting record data and mode");
            controller.setRecord(record);
            controller.setMode(mode);

            Printer printer = Printer.getDefaultPrinter();
            if (printer == null) {
                logger.error("No default printer found!");
                System.err.println("ERROR: No printer found!");
                return false;
            }
            logger.info("Using printer: {}", printer.getName());

            PrinterJob job = PrinterJob.createPrinterJob(printer);
            if (job == null) {
                logger.error("Could not create printer job");
                System.err.println("ERROR: Could not create printer job!");
                return false;
            }
            logger.debug("Printer job created");

            PageLayout pageLayout = configurePageLayout(printer);
            if (pageLayout == null) {
                logger.error("Could not configure page layout!");
                System.err.println("ERROR: Could not configure page layout!");
                job.endJob();
                return false;
            }
            logPageLayout(pageLayout);

            maximizeContentWidth(receiptNode, pageLayout);
            scaleNodeToFitPage(receiptNode, pageLayout);

            logger.info("Sending page to printer (silent mode)...");
            boolean success = job.printPage(pageLayout, receiptNode);

            if (success) {
                logger.info("Page sent to printer successfully");
                job.endJob();
                logger.info("=== Silent print job completed successfully ===");
                System.out.println("Receipt printed successfully!");
                return true;
            } else {
                logger.error("Failed to print page!");
                System.err.println("ERROR: Failed to print page!");
                job.endJob();
                return false;
            }

        } catch (Exception e) {
            logger.error("Error printing receipt: {}", e.getMessage(), e);
            System.err.println("ERROR: " + e.getMessage());
            return false;
        }
    }

    /**
     * Prints a receipt to a specific printer by name.
     *
     * @param record The record to print
     * @param mode The print mode
     * @param printerName Name of the printer to use
     * @return true if successful
     */
    public boolean printReceiptWithPrinter(Record record,
                                           ReceiptController.PrintMode mode,
                                           String printerName) {
        logger.info("=== Starting print job for receipt #{} in mode: {} to printer: {} ===",
                record.getId(), mode, printerName);

        try {
            // Find the specified printer
            logger.debug("Searching for printer: {}", printerName);
            Printer selectedPrinter = null;
            for (Printer printer : Printer.getAllPrinters()) {
                if (printer.getName().equalsIgnoreCase(printerName)) {
                    selectedPrinter = printer;
                    logger.info("Found printer: {}", printer.getName());
                    break;
                }
            }

            if (selectedPrinter == null) {
                logger.error("Printer not found: {}", printerName);
                System.err.println("ERROR: Printer not found: " + printerName);
                listAvailablePrinters();
                return false;
            }

            logger.debug("Loading FXML from: {}", RECEIPT_FXML_PATH);
            FXMLLoader loader = new FXMLLoader(getClass().getResource(RECEIPT_FXML_PATH));
            VBox receiptNode = loader.load();
            ReceiptController controller = loader.getController();
            logger.debug("FXML loaded successfully");

            controller.setRecord(record);
            controller.setMode(mode);

            PrinterJob job = PrinterJob.createPrinterJob(selectedPrinter);
            if (job == null) {
                logger.error("Could not create printer job for: {}", printerName);
                System.err.println("ERROR: Could not create printer job!");
                return false;
            }
            logger.debug("Printer job created for: {}", printerName);

            PageLayout pageLayout = configurePageLayout(selectedPrinter);
            if (pageLayout == null) {
                logger.error("Could not configure page layout!");
                System.err.println("ERROR: Could not configure page layout!");
                job.endJob();
                return false;
            }
            logPageLayout(pageLayout);

            scaleNodeToFitPage(receiptNode, pageLayout);

            logger.info("Sending page to printer: {}", printerName);
            boolean success = job.printPage(pageLayout, receiptNode);

            if (success) {
                logger.info("Page sent to printer successfully");
                job.endJob();
                logger.info("=== Print job completed successfully ===");
                System.out.println("Receipt printed successfully to: " + printerName);
                return true;
            } else {
                logger.error("Failed to print page!");
                System.err.println("ERROR: Failed to print page!");
                job.endJob();
                return false;
            }

        } catch (Exception e) {
            logger.error("Error printing receipt: {}", e.getMessage(), e);
            System.err.println("ERROR: " + e.getMessage());
            return false;
        }
    }

    /**
     * Configures the page layout for A5 landscape printing.
     * CRITICAL: Paper is inserted with LONG EDGE FIRST (210mm edge).
     * This means physical orientation is landscape, but we configure as PORTRAIT
     * in the page layout because the printer sees it as portrait when inserted.
     */
    /**
     * Configures the page layout for A5 landscape printing.
     * Paper is inserted with LONG EDGE FIRST (210mm edge), so we use LANDSCAPE orientation.
     */
    /**
     * Configures the page layout for A5 printing when inserted long-edge-first.
     * The printer sees this as PORTRAIT, so we need to create paper accordingly.
     */
    private PageLayout configurePageLayout(Printer printer) {
        logger.debug("=== Configuring page layout ===");

        try {
            PrinterAttributes attr = printer.getPrinterAttributes();
            logger.debug("Printer attributes obtained for: {}", printer.getName());

            // Find or create A5 paper - BUT with swapped dimensions
            Paper a5Paper = findA5Paper(attr);
            if (a5Paper == null) {
                logger.warn("A5 paper not found in printer's supported sizes");
                logger.info("Creating custom A5 paper size for long-edge-first insertion");
                a5Paper = createCustomA5PaperForLongEdgeFirst();
            } else {
                logger.info("Found A5 paper in printer's supported sizes: {}", a5Paper.getName());
            }

            logPaperDetails(a5Paper);

            // CRITICAL: Use PORTRAIT orientation because printer sees it that way
            PageOrientation orientation = PageOrientation.PORTRAIT;
            logger.info("Using orientation: PORTRAIT (paper inserted long-edge-first, printer sees it as portrait)");

            // Try to get maximum printable area
            PageLayout pageLayout = null;

            // Try different margin types
            try {
                pageLayout = printer.createPageLayout(
                        a5Paper,
                        orientation,
                        Printer.MarginType.HARDWARE_MINIMUM
                );
                logger.info("Using HARDWARE_MINIMUM margins");
            } catch (Exception e) {
                logger.warn("HARDWARE_MINIMUM not supported, trying DEFAULT");
                try {
                    pageLayout = printer.createPageLayout(
                            a5Paper,
                            orientation,
                            Printer.MarginType.DEFAULT
                    );
                } catch (Exception e2) {
                    logger.warn("DEFAULT not supported, trying EQUAL");
                    pageLayout = printer.createPageLayout(
                            a5Paper,
                            orientation,
                            Printer.MarginType.EQUAL
                    );
                }
            }

            logger.debug("Page layout created successfully");
            return pageLayout;

        } catch (Exception e) {
            logger.error("Error configuring page layout: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * Creates a custom A5 paper size for long-edge-first insertion.
     * Since printer sees it as PORTRAIT, we need to swap dimensions.
     * Physical: 210mm x 148mm (landscape)
     * Printer sees: 148mm x 210mm (portrait)
     */
    private Paper createCustomA5PaperForLongEdgeFirst() {
        // When paper is inserted long-edge-first, printer sees:
        // Width = physical height = 148mm
        // Height = physical width = 210mm

        double printerWidthPoints = A5_HEIGHT_MM * MM_TO_POINTS;   // 148mm -> ~420 points
        double printerHeightPoints = A5_WIDTH_MM * MM_TO_POINTS;   // 210mm -> ~595 points

        logger.debug("Creating paper for long-edge-first insertion:");
        logger.debug("Physical paper: {}x{}mm (landscape)", A5_WIDTH_MM, A5_HEIGHT_MM);
        logger.debug("Printer sees: {}x{}mm (portrait)", A5_HEIGHT_MM, A5_WIDTH_MM);
        logger.debug("Creating paper: {}x{} points", printerWidthPoints, printerHeightPoints);

        Paper customPaper = PrintHelper.createPaper(
                "Custom A5 Long-Edge-First",
                printerWidthPoints,   // width: 148mm in points (~420)
                printerHeightPoints,  // height: 210mm in points (~595)
                Units.POINT
        );

        logger.info("Custom A5 paper created for long-edge-first insertion");
        return customPaper;
    }

    /**
     * Updates the isA5Size method to handle swapped dimensions
     */
    private boolean isA5Size(double width, double height) {
        double tolerance = 10.0; // Points tolerance

        // When paper is inserted long-edge-first, printer sees swapped dimensions:
        // Expected: 148x210mm (420x595 points) as PORTRAIT

        boolean isCorrectSize = Math.abs(width - (A5_HEIGHT_MM * MM_TO_POINTS)) < tolerance &&
                Math.abs(height - (A5_WIDTH_MM * MM_TO_POINTS)) < tolerance;

        if (isCorrectSize) {
            logger.trace("Detected A5 size for long-edge-first: {}x{} points", width, height);
        }

        return isCorrectSize;
    }

    /**
     * Finds A5 paper in the printer's supported paper sizes.
     */
    private Paper findA5Paper(PrinterAttributes attr) {
        logger.debug("Searching for A5 paper in supported papers...");

        int paperCount = 0;
        for (Paper paper : attr.getSupportedPapers()) {
            paperCount++;
            double width = paper.getWidth();
            double height = paper.getHeight();

            logger.trace("Checking paper #{}: {} ({}x{} points)",
                    paperCount, paper.getName(), width, height);

            if (isA5Size(width, height)) {
                logger.info("Found A5 paper: {} ({}x{} points)", paper.getName(), width, height);
                return paper;
            }
        }

        logger.debug("Checked {} paper sizes, A5 not found", paperCount);
        return null;
    }

    /**
     * Checks if the given dimensions match A5 size (within tolerance).
     * A5 can be in portrait (148x210mm) or landscape (210x148mm) orientation.
     */
//    private boolean isA5Size(double width, double height) {
//        double tolerance = 10.0; // Points tolerance (larger to account for variations)
//
//        // A5 portrait: 148mm x 210mm (420 x 595 points)
//        boolean portrait = Math.abs(width - A5_HEIGHT_POINTS) < tolerance &&
//                Math.abs(height - A5_WIDTH_POINTS) < tolerance;
//
//        // A5 landscape: 210mm x 148mm (595 x 420 points)
//        boolean landscape = Math.abs(width - A5_WIDTH_POINTS) < tolerance &&
//                Math.abs(height - A5_HEIGHT_POINTS) < tolerance;
//
//        if (portrait) {
//            logger.trace("Detected A5 portrait: {}x{} points", width, height);
//        } else if (landscape) {
//            logger.trace("Detected A5 landscape: {}x{} points", width, height);
//        }
//
//        return portrait || landscape;
//    }

    /**
     * Creates a custom A5 paper size in LANDSCAPE orientation.
     */
    private Paper createCustomA5Paper() {
        logger.debug("Creating custom A5 paper - {}mm x {}mm (landscape)", A5_WIDTH_MM, A5_HEIGHT_MM);

        // Create A5 landscape: width=210mm, height=148mm
        Paper customPaper = PrintHelper.createPaper(
                "Custom A5 Landscape",
                A5_WIDTH_POINTS,   // width: 210mm in points (~595)
                A5_HEIGHT_POINTS,  // height: 148mm in points (~420)
                Units.POINT
        );

        logger.info("Custom A5 paper created: {}x{} points ({}x{}mm)",
                A5_WIDTH_POINTS, A5_HEIGHT_POINTS,
                A5_WIDTH_MM, A5_HEIGHT_MM);
        return customPaper;
    }

    /**
     * Scales the node to fit within the printable area of the page.
     */
    private void scaleNodeToFitPage(Node node, PageLayout pageLayout) {
        logger.debug("=== Scaling content to fit page ===");

        double printableWidth = pageLayout.getPrintableWidth();
        double printableHeight = pageLayout.getPrintableHeight();
        logger.debug("Printable area: {}x{} points", printableWidth, printableHeight);

        // Force layout calculation
        node.applyCss();

        double nodeWidth = node.getBoundsInLocal().getWidth();
        double nodeHeight = node.getBoundsInLocal().getHeight();
        logger.debug("Content size after layout: {}x{} points", nodeWidth, nodeHeight);

        // Calculate scale factors
        double scaleX = printableWidth / nodeWidth;
        double scaleY = printableHeight / nodeHeight;
        logger.trace("Scale factors - X: {}, Y: {}", scaleX, scaleY);

        // Use the smaller scale to ensure everything fits
        double scale = Math.min(scaleX, scaleY);
        logger.debug("Initial scale factor: {}", scale);

        // If content is smaller than printable area, don't scale up
        if (scale > 1.0) {
            logger.debug("Content is smaller than printable area, keeping at 100%");
            scale = 1.0;
        }

        // Set minimum scale if needed
        if (scale < 0.95) {
            logger.warn("Scaling down to {}% to fit page", (int)(scale * 100));
        }

        if (scale != 1.0) {
            Scale scaleTransform = new Scale(scale, scale);
            node.getTransforms().add(scaleTransform);
            logger.debug("Scale transform applied: {}", scale);
        } else {
            logger.debug("No scaling needed");
        }
    }

    /**
     * Adjusts the receipt content to maximize width usage
     */
    private void maximizeContentWidth(VBox receiptNode, PageLayout pageLayout) {
        double printableWidth = pageLayout.getPrintableWidth();

        // Get all HBox containers and adjust their spacing/spread
        for (Node node : receiptNode.getChildren()) {
            if (node instanceof HBox) {
                HBox hbox = (HBox) node;

                // Make HBox fill width
                hbox.setMaxWidth(Double.MAX_VALUE);
                hbox.setPrefWidth(printableWidth);

                // Adjust spacing based on available width
                double currentSpacing = hbox.getSpacing();
                double availableWidth = printableWidth - hbox.getPadding().getLeft() - hbox.getPadding().getRight();

                // For the main row with two grids, distribute space evenly
                if ("rowMain".equals(hbox.getId())) {
                    // Increase spacing to push content to edges
                    double optimalSpacing = availableWidth * 0.2; // 20% of width as spacing
                    hbox.setSpacing(optimalSpacing);

                    // Adjust child GridPane widths
                    for (Node child : hbox.getChildren()) {
                        if (child instanceof GridPane) {
                            GridPane grid = (GridPane) child;
                            grid.setPrefWidth(availableWidth * 0.35); // Each grid takes 35% of width
                            grid.setMaxWidth(Double.MAX_VALUE);
                        }
                    }
                }
                // For other HBox containers
                else if ("rowOut".equals(hbox.getId())) {
                    hbox.setSpacing(availableWidth * 0.3); // More spacing for two items
                }
            }
            // Adjust separators to full width
            else if (node instanceof Separator) {
                Separator sep = (Separator) node;
                sep.setPrefWidth(printableWidth);
            }
            // Adjust GridPane for signatures
            else if (node instanceof GridPane && "rowSign".equals(((GridPane) node).getId())) {
                GridPane grid = (GridPane) node;
                grid.setPrefWidth(printableWidth);

                // Distribute columns evenly
                for (ColumnConstraints cc : grid.getColumnConstraints()) {
                    cc.setHgrow(Priority.ALWAYS);
                    cc.setFillWidth(true);
                }
            }
        }

        logger.info("Content width maximized to use {} points", printableWidth);
    }

    /**
     * Logs detailed information about the page layout.
     */
    private void logPageLayout(PageLayout layout) {
        logger.debug("=== Page Layout Details ===");
        logger.debug("Orientation: {}", layout.getPageOrientation());
        logger.debug("Paper: {}", layout.getPaper().getName());
        logger.debug("Paper size: {}x{} points", layout.getPaper().getWidth(), layout.getPaper().getHeight());
        logger.debug("Printable area: {}x{} points", layout.getPrintableWidth(), layout.getPrintableHeight());
        logger.debug("Margins - Left: {}, Right: {}, Top: {}, Bottom: {}",
                layout.getLeftMargin(), layout.getRightMargin(),
                layout.getTopMargin(), layout.getBottomMargin());
        logger.debug("=========================");
    }

    /**
     * Logs detailed information about paper.
     */
    private void logPaperDetails(Paper paper) {
        logger.debug("=== Paper Details ===");
        logger.debug("Name: {}", paper.getName());
        logger.debug("Width: {} points ({} mm)", paper.getWidth(), paper.getWidth() / MM_TO_POINTS);
        logger.debug("Height: {} points ({} mm)", paper.getHeight(), paper.getHeight() / MM_TO_POINTS);
        logger.debug("====================");
    }

    /**
     * Lists all available printers and their supported paper sizes.
     */
    public static void listAvailablePrinters() {
        Logger logger = LogUtil.getLogger(PrintService.class);
        logger.info("=== Listing Available Printers ===");
        System.out.println("\n=== Available Printers ===");

        int printerCount = 0;
        for (Printer printer : Printer.getAllPrinters()) {
            printerCount++;
            boolean isDefault = printer == Printer.getDefaultPrinter();

            String printerInfo = String.format("Printer #%d: %s%s",
                    printerCount, printer.getName(), isDefault ? " (DEFAULT)" : "");
            logger.info(printerInfo);
            System.out.println(printerInfo);

            logger.debug("Listing supported papers for: {}", printer.getName());
            System.out.println("  Supported papers:");

            int paperCount = 0;
            for (Paper paper : printer.getPrinterAttributes().getSupportedPapers()) {
                paperCount++;
                String paperInfo = String.format("    %d. %s (%.0fx%.0f points = %.1fx%.1fmm)",
                        paperCount,
                        paper.getName(),
                        paper.getWidth(),
                        paper.getHeight(),
                        paper.getWidth() / MM_TO_POINTS,
                        paper.getHeight() / MM_TO_POINTS);
                logger.debug(paperInfo);
                System.out.println(paperInfo);
            }
        }

        if (printerCount == 0) {
            logger.warn("No printers found on system!");
            System.out.println("No printers found!");
        } else {
            logger.info("Total printers found: {}", printerCount);
            System.out.println("\nTotal printers: " + printerCount);
        }
        logger.info("=================================");
    }

    /**
     * Tests printer configuration and logs detailed information.
     */
    public void testPrinterConfiguration() {
        logger.info("=== Running Printer Configuration Test ===");
        System.out.println("\n=== Printer Configuration Test ===");

        Printer printer = Printer.getDefaultPrinter();
        if (printer == null) {
            logger.error("No default printer found!");
            System.err.println("ERROR: No default printer!");
            return;
        }

        logger.info("Testing with printer: {}", printer.getName());
        System.out.println("Printer: " + printer.getName());

        PageLayout layout = configurePageLayout(printer);
        if (layout != null) {
            logPageLayout(layout);
            System.out.println("\nConfiguration successful!");
            System.out.println("Orientation: " + layout.getPageOrientation());
            System.out.println("Paper: " + layout.getPaper().getName());
            System.out.println(String.format("Printable area: %.0fx%.0f points",
                    layout.getPrintableWidth(), layout.getPrintableHeight()));
        } else {
            logger.error("Failed to configure page layout!");
            System.err.println("ERROR: Configuration failed!");
        }

        logger.info("=== Test Complete ===");
    }
}