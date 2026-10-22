package com.spos.memory.ui;

import com.spos.memory.service.AlgorithmComparison;

import javax.swing.JPanel;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.List;

final class ComparisonChartPanel extends JPanel {
    private static final Color INK = new Color(36, 47, 58);
    private static final Color MUTED = new Color(101, 115, 128);
    private static final Color TRACK = new Color(235, 239, 241);
    private static final Color[] COLORS = {
            new Color(54, 133, 98), new Color(54, 111, 160), new Color(190, 119, 54)
    };
    private static final String[] METRICS = {"Success rate", "Utilization", "Fragmentation"};

    private List<AlgorithmComparison.Row> rows = List.of();

    ComparisonChartPanel() {
        setBackground(Color.WHITE);
        setPreferredSize(new java.awt.Dimension(420, 286));
        setToolTipText("");
    }

    void setRows(List<AlgorithmComparison.Row> rows) {
        this.rows = List.copyOf(rows);
        repaint();
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        Graphics2D g = (Graphics2D) graphics.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int labelWidth = 110;
        int right = Math.max(labelWidth + 40, getWidth() - 48);
        int barWidth = right - labelWidth;
        int top = 28;
        int groupHeight = Math.max(76, (getHeight() - top - 12) / Math.max(1, rows.size()));
        g.setFont(getFont().deriveFont(Font.PLAIN, 10f));
        g.setColor(MUTED);
        g.drawString("0%", labelWidth, 16);
        g.drawString("100%", Math.max(labelWidth, right - 27), 16);
        for (int tick = 0; tick <= 4; tick++) {
            int x = labelWidth + barWidth * tick / 4;
            g.setColor(new Color(232, 237, 240));
            g.drawLine(x, top - 8, x, top + groupHeight * rows.size() - 8);
        }
        for (int index = 0; index < rows.size(); index++) {
            AlgorithmComparison.Row row = rows.get(index);
            int y = top + index * groupHeight;
            g.setColor(INK);
            g.setFont(getFont().deriveFont(Font.BOLD, 12f));
            g.drawString(row.algorithm().getDisplayName(), 12, y + 16);
            double successRate = row.successes() + row.failures() == 0 ? 0
                    : row.successes() * 100.0 / (row.successes() + row.failures());
            double utilization = row.statistics().getUtilizationPercent();
            double fragmentation = row.statistics().getExternalFragmentationPercent();
            double[] values = {successRate, utilization, fragmentation};
            for (int metric = 0; metric < values.length; metric++) {
                int barY = y + 23 + metric * 16;
                g.setFont(getFont().deriveFont(Font.PLAIN, 10f));
                g.setColor(MUTED);
                g.drawString(METRICS[metric], 12, barY + 9);
                g.setColor(TRACK);
                g.fillRoundRect(labelWidth, barY, barWidth, 10, 5, 5);
                int fillWidth = (int) Math.round(barWidth * Math.max(0, Math.min(100, values[metric])) / 100.0);
                g.setColor(COLORS[metric]);
                g.fillRoundRect(labelWidth, barY, fillWidth, 10, 5, 5);
                g.setColor(INK);
                g.drawString(String.format("%.1f%%", values[metric]), right + 6, barY + 9);
            }
            if (index < rows.size() - 1) {
                g.setColor(new Color(235, 239, 241));
                g.drawLine(12, y + groupHeight - 4, getWidth() - 12, y + groupHeight - 4);
            }
        }
        if (rows.isEmpty()) {
            g.setColor(MUTED);
            g.setFont(getFont().deriveFont(Font.PLAIN, 12f));
            g.drawString("Run a comparison to see algorithm performance.", 16, 44);
        }
        g.dispose();
    }

    @Override
    public String getToolTipText(java.awt.event.MouseEvent event) {
        if (rows.isEmpty()) {
            return null;
        }
        int top = 28;
        int groupHeight = Math.max(76, (getHeight() - top - 12) / rows.size());
        int rowIndex = (event.getY() - top) / groupHeight;
        if (rowIndex < 0 || rowIndex >= rows.size()) {
            return null;
        }
        AlgorithmComparison.Row row = rows.get(rowIndex);
        return row.algorithm().getDisplayName() + ": " + row.successes() + " allocations succeeded, "
                + row.failures() + " failed; utilization "
                + String.format("%.1f%%", row.statistics().getUtilizationPercent()) + "; external fragmentation "
                + row.statistics().getExternalFragmentation() + " units.";
    }
}
