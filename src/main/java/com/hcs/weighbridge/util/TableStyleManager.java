package com.hcs.weighbridge.util;

import javafx.scene.control.TableColumn;
import java.util.HashMap;
import java.util.Map;

public class TableStyleManager {
    private static final Map<String, String> columnStyles = new HashMap<>();

    public static void saveColumnStyle(TableColumn<?, ?> column) {
        if (column.getText() != null) {
            columnStyles.put(column.getText(), column.getStyle());
        }
    }

    public static void restoreColumnStyle(TableColumn<?, ?> column) {
        if (column.getText() != null && columnStyles.containsKey(column.getText())) {
            column.setStyle(columnStyles.get(column.getText()));
        }
    }

    public static void clear() {
        columnStyles.clear();
    }
}