package com.spos.memory.ui;

import com.spos.memory.model.AllocationAlgorithm;
import com.spos.memory.model.AllocationResult;
import com.spos.memory.model.HistoryEntry;
import com.spos.memory.model.MemoryStatistics;
import com.spos.memory.service.AllocationRequest;
import com.spos.memory.service.AlgorithmComparison;
import com.spos.memory.service.CsvReportExporter;
import com.spos.memory.service.MemoryManager;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JSlider;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.Timer;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public final class SimulatorFrame extends JFrame {
    private static final Color PAGE = new Color(244, 247, 249);
    private static final Color INK = new Color(32, 44, 55);
    private static final Color MUTED = new Color(101, 115, 128);
    private static final Color ACCENT = new Color(35, 107, 137);
    private static final Color PRIMARY_BUTTON = new Color(205, 231, 239);

    private final JTextField memorySizeField = new JTextField("1024", 7);
    private final JTextField processIdField = new JTextField(9);
    private final JTextField processSizeField = new JTextField(7);
    private final JTextField deallocateIdField = new JTextField(9);
    private final JComboBox<AllocationAlgorithm> algorithmBox = new JComboBox<>(AllocationAlgorithm.values());
    private final JSlider speedSlider = new JSlider(1, 10, 6);
    private final JLabel statusLabel = new JLabel("Ready");
    private final JLabel[] statisticValues = new JLabel[6];
    private final DefaultTableModel historyModel = model("Time", "Operation", "Result", "Details", "Allocated", "Free");
    private final DefaultTableModel comparisonModel = model("Algorithm", "Success", "Failures", "Utilization", "Largest free", "Fragmentation");
    private final DefaultTableModel queueModel = model("Process", "Size");
    private final JTable historyTable = new JTable(historyModel);
    private final JTable comparisonTable = new JTable(comparisonModel);
    private final JTable queueTable = new JTable(queueModel);
    private final MemoryLayoutPanel memoryLayoutPanel = new MemoryLayoutPanel();
    private final ComparisonChartPanel comparisonChartPanel = new ComparisonChartPanel();
    private final List<AllocationRequest> pendingRequests = new ArrayList<>();
    private final Timer playbackTimer = new Timer(700, event -> runNextRequest());

    private MemoryManager manager = new MemoryManager(1024, AllocationAlgorithm.FIRST_FIT);
    private boolean selectionInProgress;

    public SimulatorFrame() {
        super("Dynamic Memory Simulator");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(960, 650));
        setSize(1360, 900);
        setLocationRelativeTo(null);
        playbackTimer.setInitialDelay(0);
        getContentPane().setBackground(PAGE);
        setLayout(new BorderLayout(0, 0));
        add(buildHeader(), BorderLayout.NORTH);
        add(buildWorkspace(), BorderLayout.CENTER);
        add(buildStatusBar(), BorderLayout.SOUTH);
        algorithmBox.addActionListener(event -> {
            if (selectionInProgress) {
                algorithmBox.setSelectedItem(manager.getAlgorithm());
                setStatus("Wait for the current allocation animation to finish before changing strategy.");
                return;
            }
            manager.setAlgorithm((AllocationAlgorithm) algorithmBox.getSelectedItem());
            setStatus("Allocation strategy: " + manager.getAlgorithm());
        });
        refreshView();
    }

    private JPanel buildHeader() {
        JPanel header = new JPanel(new BorderLayout(18, 8));
        header.setBackground(Color.WHITE);
        header.setBorder(BorderFactory.createEmptyBorder(16, 22, 14, 22));
        JPanel titleBlock = new JPanel(new GridLayout(2, 1, 0, 3));
        titleBlock.setOpaque(false);
        JLabel title = new JLabel("MEMORY ALLOCATION LAB");
        title.setFont(new Font("Segoe UI", Font.BOLD, 20));
        title.setForeground(INK);
        JLabel subtitle = new JLabel("Dynamic partitioning  /  fragmentation analysis");
        subtitle.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        subtitle.setForeground(MUTED);
        titleBlock.add(title);
        titleBlock.add(subtitle);
        JPanel controls = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 4));
        controls.setOpaque(false);
        controls.add(label("Strategy"));
        algorithmBox.setPreferredSize(new Dimension(135, 30));
        controls.add(algorithmBox);
        controls.add(label("Total units"));
        styleField(memorySizeField);
        controls.add(memorySizeField);
        JButton resetButton = button("Reset simulation", true);
        resetButton.addActionListener(event -> resetSimulation());
        controls.add(resetButton);
        header.add(titleBlock, BorderLayout.WEST);
        header.add(controls, BorderLayout.EAST);
        return header;
    }

    private JSplitPane buildWorkspace() {
        JPanel left = new JPanel(new BorderLayout(0, 12));
        left.setOpaque(false);
        left.setBorder(BorderFactory.createEmptyBorder(16, 20, 12, 10));
        left.add(buildStatistics(), BorderLayout.NORTH);
        left.add(buildMemoryArea(), BorderLayout.CENTER);
        left.add(buildActions(), BorderLayout.SOUTH);

        JTabbedPane tabs = new JTabbedPane();
        tabs.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        tabs.addTab("History", buildHistoryPanel());
        tabs.addTab("Compare", buildComparisonPanel());
        tabs.addTab("Step queue", buildQueuePanel());
        tabs.setPreferredSize(new Dimension(380, 500));
        JPanel right = new JPanel(new BorderLayout());
        right.setOpaque(false);
        right.setBorder(BorderFactory.createEmptyBorder(16, 10, 12, 20));
        right.add(tabs, BorderLayout.CENTER);

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, left, right);
        split.setResizeWeight(0.62);
        split.setBorder(null);
        split.setDividerSize(7);
        split.setBackground(PAGE);
        split.setContinuousLayout(true);
        split.setOneTouchExpandable(true);
        return split;
    }

    private JPanel buildStatistics() {
        JPanel panel = new JPanel(new GridLayout(1, 6, 6, 0));
        panel.setOpaque(false);
        String[] names = {"TOTAL", "ALLOCATED", "FREE", "UTILIZATION", "LARGEST FREE", "FRAGMENTATION"};
        Color[] accents = {new Color(54, 111, 160), new Color(54, 133, 98),
            new Color(190, 119, 54), new Color(55, 123, 135),
            new Color(104, 127, 61), new Color(145, 88, 119)};
        for (int index = 0; index < names.length; index++) {
            JPanel metric = new JPanel(new GridLayout(2, 1, 0, 3));
            metric.setBackground(Color.WHITE);
            metric.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(2, 0, 0, 0, accents[index]),
                BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(new Color(224, 230, 234)),
                    BorderFactory.createEmptyBorder(5, 6, 5, 6))));
            JLabel name = new JLabel(names[index]);
            name.setFont(new Font("Segoe UI", Font.BOLD, 8));
            name.setForeground(MUTED);
            statisticValues[index] = new JLabel("0");
            statisticValues[index].setFont(new Font("Segoe UI", Font.BOLD, 13));
            statisticValues[index].setForeground(accents[index]);
            metric.add(name);
            metric.add(statisticValues[index]);
            panel.add(metric);
        }
        return panel;
    }

    private JPanel buildMemoryArea() {
        JPanel panel = titledPanel("MEMORY MAP");
        panel.setLayout(new BorderLayout(0, 8));
        JLabel key = new JLabel("  ■ Allocated     ■ Free memory");
        key.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        key.setForeground(MUTED);
        JScrollPane scroll = new JScrollPane(memoryLayoutPanel);
        scroll.setBorder(BorderFactory.createLineBorder(new Color(230, 235, 238)));
        panel.add(key, BorderLayout.NORTH);
        panel.add(scroll, BorderLayout.CENTER);
        return panel;
    }

    private JPanel buildActions() {
        JPanel outer = new JPanel(new GridLayout(2, 1, 0, 6));
        outer.setOpaque(false);
        JPanel allocation = titledPanel("ALLOCATE PROCESS");
        allocation.setLayout(new FlowLayout(FlowLayout.LEFT, 8, 8));
        styleField(processIdField);
        styleField(processSizeField);
        JButton allocateButton = button("Allocate", true);
        JButton queueButton = button("Queue", false);
        allocation.add(label("ID"));
        allocation.add(processIdField);
        allocation.add(label("Size"));
        allocation.add(processSizeField);
        allocation.add(allocateButton);
        allocation.add(queueButton);
        allocateButton.addActionListener(event -> allocateDirectly());
        queueButton.addActionListener(event -> enqueueRequest());

        JPanel release = titledPanel("DEALLOCATE / EXECUTE");
        release.setLayout(new FlowLayout(FlowLayout.LEFT, 8, 8));
        styleField(deallocateIdField);
        JButton deallocateButton = button("Deallocate", false);
        JButton stepButton = button("Next Step", true);
        JButton runButton = button("Run queue", false);
        release.add(label("Process ID"));
        release.add(deallocateIdField);
        release.add(deallocateButton);
        release.add(stepButton);
        release.add(runButton);
        deallocateButton.addActionListener(event -> deallocateProcess());
        stepButton.addActionListener(event -> runNextRequest());
        runButton.addActionListener(event -> runAllRequests());
        outer.add(allocation);
        outer.add(release);
        return outer;
    }

    private JPanel buildHistoryPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 10));
        panel.setBackground(PAGE);
        panel.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        configureTable(historyTable);
        historyTable.getColumnModel().getColumn(3).setPreferredWidth(300);
        JButton exportButton = button("Export CSV", true);
        exportButton.addActionListener(event -> exportReport());
        panel.add(new JScrollPane(historyTable), BorderLayout.CENTER);
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        footer.setOpaque(false);
        footer.add(exportButton);
        panel.add(footer, BorderLayout.SOUTH);
        return panel;
    }

    private JPanel buildComparisonPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 10));
        panel.setBackground(PAGE);
        panel.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        configureTable(comparisonTable);
        JButton compareButton = button("Compare all algorithms", true);
        compareButton.addActionListener(event -> compareAlgorithms());
        JLabel note = new JLabel("Runs queued requests from the current memory map for each strategy.");
        note.setForeground(MUTED);
        note.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        panel.add(note, BorderLayout.NORTH);
        JPanel chart = titledPanel("PERFORMANCE COMPARISON");
        chart.setLayout(new BorderLayout());
        chart.add(new JScrollPane(comparisonChartPanel), BorderLayout.CENTER);
        JSplitPane comparisonSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
            new JScrollPane(comparisonTable), chart);
        comparisonSplit.setResizeWeight(0.35);
        comparisonSplit.setDividerSize(6);
        comparisonSplit.setBorder(null);
        panel.add(comparisonSplit, BorderLayout.CENTER);
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        footer.setOpaque(false);
        footer.add(compareButton);
        panel.add(footer, BorderLayout.SOUTH);
        return panel;
    }

    private JPanel buildQueuePanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 10));
        panel.setBackground(PAGE);
        panel.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        configureTable(queueTable);
        JButton removeButton = button("Remove selected", false);
        removeButton.addActionListener(event -> removeSelectedRequest());
        JPanel queueHeading = new JPanel(new BorderLayout(8, 0));
        queueHeading.setOpaque(false);
        queueHeading.add(new JLabel("Pending requests"), BorderLayout.CENTER);
        queueHeading.add(removeButton, BorderLayout.EAST);
        panel.add(queueHeading, BorderLayout.NORTH);
        panel.add(new JScrollPane(queueTable), BorderLayout.CENTER);
        JButton playButton = button("Play", true);
        JButton pauseButton = button("Pause", false);
        JButton nextButton = button("Next Step", false);
        JButton resetButton = button("Reset", false);
        playButton.addActionListener(event -> playQueue());
        pauseButton.addActionListener(event -> pauseQueue());
        nextButton.addActionListener(event -> runNextRequest());
        resetButton.addActionListener(event -> resetSimulation());
        JPanel transport = new JPanel(new GridLayout(1, 4, 6, 0));
        transport.setOpaque(false);
        transport.add(playButton);
        transport.add(pauseButton);
        transport.add(nextButton);
        transport.add(resetButton);
        speedSlider.setOpaque(false);
        speedSlider.setMajorTickSpacing(3);
        speedSlider.setPaintTicks(true);
        speedSlider.setToolTipText("Playback and memory animation speed");
        speedSlider.setPreferredSize(new Dimension(150, 30));
        speedSlider.addChangeListener(event -> updatePlaybackSpeed());
        JPanel speedRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 7, 0));
        speedRow.setOpaque(false);
        speedRow.add(label("Speed"));
        speedRow.add(speedSlider);
        JPanel footer = new JPanel(new GridLayout(2, 1, 0, 5));
        footer.setOpaque(false);
        footer.add(transport);
        footer.add(speedRow);
        panel.add(footer, BorderLayout.SOUTH);
        return panel;
    }

    private JPanel buildStatusBar() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(new Color(235, 240, 243));
        panel.setBorder(BorderFactory.createEmptyBorder(7, 20, 7, 20));
        statusLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        statusLabel.setForeground(MUTED);
        panel.add(statusLabel, BorderLayout.WEST);
        JLabel addressHint = new JLabel("Addresses are zero-based; block end addresses are exclusive.");
        addressHint.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        addressHint.setForeground(MUTED);
        panel.add(addressHint, BorderLayout.EAST);
        return panel;
    }

    private void allocateDirectly() {
        AllocationRequest request = readRequestFields();
        if (request == null) {
            return;
        }
        processIdField.setText("");
        processSizeField.setText("");
        beginAnimatedAllocation(request, false);
    }

    private void enqueueRequest() {
        AllocationRequest request = readRequestFields();
        if (request == null) {
            return;
        }
        pendingRequests.add(request);
        queueModel.addRow(new Object[]{request.processId(), request.size()});
        processIdField.setText("");
        processSizeField.setText("");
        setStatus("Queued " + request.processId() + " for step-by-step execution.");
    }

    private AllocationRequest readRequestFields() {
        String processId = processIdField.getText().trim();
        if (processId.isEmpty()) {
            showValidation("Enter a process ID.");
            processIdField.requestFocusInWindow();
            return null;
        }
        try {
            return new AllocationRequest(processId, Integer.parseInt(processSizeField.getText().trim()));
        } catch (NumberFormatException exception) {
            showValidation("Process size must be a whole number greater than zero.");
            processSizeField.requestFocusInWindow();
            return null;
        } catch (IllegalArgumentException exception) {
            showValidation(exception.getMessage());
            processSizeField.requestFocusInWindow();
            return null;
        }
    }

    private void deallocateProcess() {
        if (selectionInProgress) {
            setStatus("Wait for the current allocation animation to finish first.");
            return;
        }
        String processId = deallocateIdField.getText().trim();
        if (processId.isEmpty()) {
            showValidation("Enter the ID of an allocated process.");
            deallocateIdField.requestFocusInWindow();
            return;
        }
        boolean success = manager.deallocate(processId);
        deallocateIdField.setText("");
        setStatus(success ? "Deallocated " + processId + "." : "No allocated process found with ID " + processId + ".");
        refreshView();
    }

    private void runNextRequest() {
        if (selectionInProgress) {
            return;
        }
        if (pendingRequests.isEmpty()) {
            playbackTimer.stop();
            setStatus("The step queue is empty.");
            return;
        }
        beginAnimatedAllocation(pendingRequests.get(0), true);
    }

    private void runAllRequests() {
        if (pendingRequests.isEmpty()) {
            setStatus("The step queue is empty.");
            return;
        }
        playQueue();
    }

    private void removeSelectedRequest() {
        if (selectionInProgress) {
            setStatus("Wait for the current allocation animation before editing the queue.");
            return;
        }
        int selectedRow = queueTable.getSelectedRow();
        if (selectedRow < 0) {
            setStatus("Select a queued request to remove.");
            return;
        }
        pendingRequests.remove(selectedRow);
        queueModel.removeRow(selectedRow);
        setStatus("Removed the selected request from the queue.");
    }

    private void compareAlgorithms() {
        AlgorithmComparison comparison = AlgorithmComparison.run(manager, new ArrayList<>(pendingRequests));
        comparisonModel.setRowCount(0);
        comparisonChartPanel.setRows(comparison.getRows());
        for (AlgorithmComparison.Row row : comparison.getRows()) {
            MemoryStatistics statistics = row.statistics();
            comparisonModel.addRow(new Object[]{row.algorithm(), row.successes(), row.failures(),
                    percent(statistics.getUtilizationPercent()), statistics.getLargestFreeBlock(),
                    statistics.getExternalFragmentation() + " (" + percent(statistics.getExternalFragmentationPercent()) + ")"});
        }
        setStatus("Compared First Fit, Best Fit, and Worst Fit on the same map and request sequence.");
        refreshView();
    }

    private void resetSimulation() {
        playbackTimer.stop();
        memoryLayoutPanel.clearSelectionHighlight();
        selectionInProgress = false;
        int totalMemory;
        try {
            totalMemory = Integer.parseInt(memorySizeField.getText().trim());
            if (totalMemory <= 0) {
                throw new NumberFormatException();
            }
        } catch (NumberFormatException exception) {
            showValidation("Total memory must be a whole number greater than zero.");
            memorySizeField.requestFocusInWindow();
            return;
        }
        AllocationAlgorithm algorithm = (AllocationAlgorithm) algorithmBox.getSelectedItem();
        manager = new MemoryManager(totalMemory, algorithm);
        manager.recordStep("Reset simulation and initialized " + totalMemory + " units.", true);
        pendingRequests.clear();
        queueModel.setRowCount(0);
        comparisonModel.setRowCount(0);
        comparisonChartPanel.setRows(List.of());
        setStatus("Simulation reset to " + totalMemory + " free units.");
        refreshView();
    }

    private void exportReport() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Export simulation report");
        chooser.setSelectedFile(new java.io.File("memory-simulation-report.csv"));
        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        try {
            CsvReportExporter.export(chooser.getSelectedFile().toPath(), manager);
            setStatus("CSV report exported to " + chooser.getSelectedFile().getName() + ".");
        } catch (IOException exception) {
            JOptionPane.showMessageDialog(this, "Could not write the report: " + exception.getMessage(),
                    "Export failed", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void refreshView() {
        MemoryStatistics statistics = manager.getStatistics();
        statisticValues[0].setText(statistics.getTotalMemory() + " units");
        statisticValues[1].setText(statistics.getAllocatedMemory() + " units");
        statisticValues[2].setText(statistics.getFreeMemory() + " units");
        statisticValues[3].setText(percent(statistics.getUtilizationPercent()));
        statisticValues[4].setText(statistics.getLargestFreeBlock() + " units");
        statisticValues[5].setText(statistics.getExternalFragmentation() + " units");
        memoryLayoutPanel.animateTo(manager.getBlocks(), manager.getTotalMemory(), animationDuration());
        historyModel.setRowCount(0);
        for (HistoryEntry entry : manager.getHistory()) {
            MemoryStatistics snapshot = entry.getStatistics();
            historyModel.addRow(new Object[]{entry.getFormattedTime(), entry.getOperation(),
                    entry.isSuccess() ? "Success" : "Failed", entry.getDetails(),
                    snapshot.getAllocatedMemory(), snapshot.getFreeMemory()});
        }
        if (historyTable.getRowCount() > 0) {
            historyTable.scrollRectToVisible(historyTable.getCellRect(historyTable.getRowCount() - 1, 0, true));
        }
    }

    private void showValidation(String message) {
        JOptionPane.showMessageDialog(this, message, "Check input", JOptionPane.WARNING_MESSAGE);
        setStatus(message);
    }

    private void setStatus(String message) {
        statusLabel.setText(message);
    }

    private void playQueue() {
        if (selectionInProgress) {
            setStatus("The current allocation animation is still running.");
            return;
        }
        if (pendingRequests.isEmpty()) {
            setStatus("Queue allocation requests before starting playback.");
            return;
        }
        updatePlaybackSpeed();
        playbackTimer.start();
        setStatus("Playing queued allocation requests.");
    }

    private void pauseQueue() {
        playbackTimer.stop();
        setStatus("Step execution paused.");
    }

    private void updatePlaybackSpeed() {
        int speed = speedSlider.getValue();
        playbackTimer.setDelay(animationDuration() + 180);
        playbackTimer.setInitialDelay(0);
    }

    private int animationDuration() {
        return 760 - speedSlider.getValue() * 55;
    }

    private void beginAnimatedAllocation(AllocationRequest request, boolean queuedStep) {
        if (selectionInProgress) {
            return;
        }
        selectionInProgress = true;
        memoryLayoutPanel.animateSelection(manager.getBlocks(), request.size(), manager.getAlgorithm(),
                Math.max(150, animationDuration() / 2), this::setStatus, () -> {
                    AllocationResult result = manager.allocate(request.processId(), request.size());
                    if (queuedStep) {
                        manager.recordStep("Step executed for " + request.processId() + ": "
                                + (result.isSuccess() ? "success" : "failure") + ".", result.isSuccess());
                        pendingRequests.remove(0);
                        queueModel.removeRow(0);
                    }
                    selectionInProgress = false;
                    setStatus(result.getMessage());
                    refreshView();
                });
    }

    private static String percent(double value) {
        return String.format("%.1f%%", value);
    }

    private static JLabel label(String text) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        label.setForeground(MUTED);
        return label;
    }

    private static JPanel titledPanel(String title) {
        JPanel panel = new JPanel();
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder(BorderFactory.createLineBorder(new Color(224, 230, 234)), title),
                BorderFactory.createEmptyBorder(7, 8, 7, 8)));
        return panel;
    }

    private static JButton button(String text, boolean primary) {
        JButton button = new JButton(text);
        Color baseColor = primary ? PRIMARY_BUTTON : new Color(239, 244, 246);
        button.setFont(new Font("Segoe UI", Font.BOLD, 11));
        button.setFocusPainted(false);
        button.setPreferredSize(new Dimension(Math.max(88, text.length() * 8 + 24), 31));
        button.setBackground(baseColor);
        button.setForeground(Color.BLACK);
        button.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        button.setOpaque(true);
        button.setContentAreaFilled(true);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent event) {
                if (button.isEnabled()) {
                    button.setBackground(baseColor.brighter());
                }
            }

            @Override
            public void mouseExited(MouseEvent event) {
                button.setBackground(baseColor);
            }
        });
        return button;
    }

    private static void styleField(JTextField field) {
        field.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        field.setPreferredSize(new Dimension(field.getPreferredSize().width, 30));
        field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(207, 216, 222)),
                BorderFactory.createEmptyBorder(3, 7, 3, 7)));
    }

    private static void configureTable(JTable table) {
        table.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        table.setRowHeight(27);
        table.setFillsViewportHeight(true);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setGridColor(new Color(232, 237, 240));
        table.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 10));
        table.getTableHeader().setBackground(new Color(240, 244, 246));
        table.getTableHeader().setForeground(INK);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);
    }

    private static DefaultTableModel model(String... columns) {
        return new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
    }
}
