package com.hcs.weighbridge.serial;

import com.fazecast.jSerialComm.SerialPort;
import com.hcs.weighbridge.model.SerialConfig;
import com.hcs.weighbridge.util.LogUtil;
import org.apache.logging.log4j.Logger;

import java.io.InputStream;
import java.util.function.BiConsumer;

public class WeighReader {

    private static final Logger logger = LogUtil.getLogger(WeighReader.class);
    private final SerialPort port;
    private volatile boolean running = true;
    private final BiConsumer<Integer, Character> listener;

    private static final int DECIMAL_PLACES = 0;

    public WeighReader(SerialConfig cfg, BiConsumer<Integer, Character> listener) {
        logger.debug("Creating WeighReader instance with config: {} and listener: {}",
                cfg, listener != null ? "provided" : "null");
        this.listener = listener;

        port = SerialPort.getCommPort(cfg.getPortName());
        logger.info("Retrieved SerialPort for: {}", cfg.getPortName());

        port.setComPortParameters(
                cfg.getBaudRate(),
                cfg.getDataBits(),
                cfg.getStopBits() == 1
                        ? SerialPort.ONE_STOP_BIT
                        : SerialPort.TWO_STOP_BITS,
                cfg.getParity()
        );

        logger.debug("Serial port parameters set - Baud: {}, Data Bits: {}, Stop Bits: {}, Parity: {}",
                cfg.getBaudRate(), cfg.getDataBits(),
                cfg.getStopBits() == 1 ? "ONE" : "TWO",
                cfg.getParity());

        port.setComPortTimeouts(
                SerialPort.TIMEOUT_READ_BLOCKING,
                0,
                0
        );
        logger.trace("Serial port timeouts set to TIMEOUT_READ_BLOCKING");
    }

    public void start() {
        logger.info("Starting WeighReader thread");
        int connectionAttempts = 0;

        while (running) {
            try {
                if (!port.isOpen()) {
                    connectionAttempts++;
                    logger.debug("Attempting to open serial port (attempt #{})", connectionAttempts);

                    if (!port.openPort()) {
                        logger.warn("Failed to open serial port on attempt #{}", connectionAttempts);
                        Thread.sleep(2000);
                        continue;
                    }

                    logger.info("Serial port opened successfully on attempt #{}", connectionAttempts);
                    connectionAttempts = 0; // Reset counter on success
                }

                readLoop();

            } catch (InterruptedException e) {
                logger.warn("WeighReader thread interrupted: {}", e.getMessage());
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                logger.error("Unexpected error in WeighReader main loop: {}", e.getMessage(), e);
                System.err.println("Serial error: " + e.getMessage());

                if (port.isOpen()) {
                    logger.debug("Closing serial port due to error");
                    port.closePort();
                }

                try {
                    logger.debug("Waiting 2 seconds before retry after error");
                    Thread.sleep(2000);
                } catch (InterruptedException ie) {
                    logger.warn("Interrupted while waiting to retry: {}", ie.getMessage());
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }

        if (!running) {
            logger.info("WeighReader stopped normally");
        } else {
            logger.info("WeighReader exiting main loop");
        }
    }

    private void readLoop() throws Exception {
        logger.debug("Entering readLoop()");
        InputStream in = port.getInputStream();
        StringBuilder buffer = new StringBuilder();
        int bytesRead = 0;
        int framesProcessed = 0;

        while (running && port.isOpen()) {
            int b = in.read();
            if (b < 0) {
                logger.trace("read() returned -1 (end of stream)");
                continue;
            }

            bytesRead++;
            char c = (char) b;

            if (c == '\r') {
                framesProcessed++;
                String frameContent = buffer.toString();
                logger.trace("Received carriage return, frame buffer: '{}'", frameContent);

                if (!frameContent.trim().isEmpty()) {
                    handleFrame(frameContent);
                } else {
                    logger.trace("Empty frame, ignoring");
                }

                buffer.setLength(0);

                // Periodic logging
                if (framesProcessed % 100 == 0) {
                    logger.debug("Processed {} frames, {} total bytes read",
                            framesProcessed, bytesRead);
                }
            }
            else if (c == '\n') {
                logger.trace("Ignoring newline character");
            }
            else {
                // Log occasional characters for debugging
                if (logger.isTraceEnabled() && bytesRead % 50 == 0) {
                    logger.trace("Read character #{}: '{}' (0x{})",
                            bytesRead, c, Integer.toHexString(b));
                }
                buffer.append(c);
            }
        }

        logger.debug("Exiting readLoop() - Processed {} frames, {} total bytes",
                framesProcessed, bytesRead);
    }

    private void handleFrame(String rawFrame) {
        logger.trace("Processing raw frame: '{}'", rawFrame);

        // Remove control characters
        String frame = rawFrame.replaceAll("[\\x00-\\x1F]", "").trim();

        if (frame.isEmpty()) {
            logger.trace("Frame is empty after trimming control characters");
            return;
        }

        logger.debug("Frame after cleanup: '{}' (length: {})", frame, frame.length());

        if (!frame.matches("[A-Z][+-][0-9.]+")) {
            logger.warn("Invalid frame format: '{}'. Expected pattern: [A-Z][+-][0-9.]+", frame);
            return;
        }

        char status = frame.charAt(0);
        char polarity = frame.charAt(1);
        String valuePart = frame.substring(2);

        logger.trace("Parsed frame - Status: '{}', Polarity: '{}', Value: '{}'",
                status, polarity, valuePart);

        try {
            double weightKg;

            if (valuePart.contains(".")) {
                weightKg = Double.parseDouble(valuePart);
                logger.trace("Parsed decimal value: {}", weightKg);
            } else {
                int raw = Integer.parseInt(valuePart);
                weightKg = raw / Math.pow(10, DECIMAL_PLACES);
                logger.trace("Parsed integer value: {}, converted to: {} ({} decimal places)",
                        raw, weightKg, DECIMAL_PLACES);
            }

            if (polarity == '-') {
                logger.trace("Applying negative polarity to weight");
                weightKg = -weightKg;
            }

            int roundedKg = (int) Math.round(weightKg / 5.0) * 5;
            logger.debug("Raw weight: {} kg, Rounded to nearest 5kg: {} kg", weightKg, roundedKg);

            // Log every weight reading at debug level
            logger.debug("Weight: {} kg | Status: {} | Polarity: {}",
                    roundedKg, status, polarity);

            // Console output preserved
            System.out.println("Weight: " + roundedKg + " kg | Status: " + status);

            if (status == 'P' || status == 'T') {
                logger.info("Stable weight detected: {} kg. Notifying listener.", roundedKg);
                if (listener != null) {
                    listener.accept(roundedKg, status);
                    logger.trace("Listener notified with weight: {} kg, status: '{}'", roundedKg, status);
                } else {
                    logger.warn("No listener registered to receive weight data");
                }
            } else {
                logger.debug("Non-stable status '{}', not notifying listener", status);
            }

        } catch (NumberFormatException e) {
            logger.error("Failed to parse weight value '{}' from frame: '{}'", valuePart, frame, e);
            System.err.println("Invalid frame: " + frame);
        } catch (Exception e) {
            logger.error("Unexpected error processing frame: '{}'", frame, e);
            System.err.println("Invalid frame: " + frame);
        }
    }

    public void stop() {
        logger.info("Stopping WeighReader...");
        running = false;

        if (port.isOpen()) {
            logger.debug("Closing serial port");
            port.closePort();
            logger.info("Serial port closed");
        } else {
            logger.debug("Serial port was already closed");
        }

        logger.info("WeighReader stopped successfully");
    }

    // Additional helper methods for logging

    public boolean isRunning() {
        logger.trace("isRunning() called, returning: {}", running);
        return running;
    }

    public boolean isPortOpen() {
        boolean open = port != null && port.isOpen();
        logger.trace("isPortOpen() called, returning: {}", open);
        return open;
    }

    public String getPortName() {
        String name = port != null ? port.getSystemPortName() : "null";
        logger.trace("getPortName() called, returning: {}", name);
        return name;
    }
}