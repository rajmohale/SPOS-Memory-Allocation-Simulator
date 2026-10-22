package com.spos.memory.ui;

import com.spos.memory.model.MemoryBlock;
import com.spos.memory.model.AllocationAlgorithm;

import javax.swing.JPanel;
import javax.swing.Scrollable;
import javax.swing.SwingConstants;
import javax.swing.ToolTipManager;
import javax.swing.Timer;
import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.function.Consumer;

final class MemoryLayoutPanel extends JPanel implements Scrollable {
    private static final Color FREE_FILL = new Color(228, 242, 235);
    private static final Color FREE_EDGE = new Color(55, 133, 99);
    private static final Color CANDIDATE_FILL = new Color(255, 238, 176);
    private static final Color CANDIDATE_EDGE = new Color(193, 137, 24);
    private static final Color REJECTED_FILL = new Color(250, 225, 222);
    private static final Color REJECTED_EDGE = new Color(176, 75, 66);
    private static final Color WINNER_FILL = new Color(255, 213, 107);
    private static final Color WINNER_EDGE = new Color(160, 100, 10);
    private static final Color[] PROCESS_FILLS = {
            new Color(218, 233, 247), new Color(248, 231, 213),
            new Color(233, 239, 218), new Color(242, 226, 235)
    };
    private static final Color[] PROCESS_EDGES = {
            new Color(54, 111, 160), new Color(173, 104, 45),
            new Color(104, 127, 61), new Color(145, 88, 119)
    };
    private static final Color INK = new Color(36, 47, 58);

    private List<MemoryBlock> blocks = List.of();
    private List<MemoryBlock> previousBlocks = List.of();
    private int totalMemory = 1;
    private int animationDurationMs = 420;
    private long animationStartedAt;
    private float animationProgress = 1f;
    private final Timer animationTimer;
    private final Timer selectionTimer;
    private final Timer highlightTimer;
    private List<MemoryBlock> scanCandidates = List.of();
    private AllocationAlgorithm selectionAlgorithm;
    private int selectionRequestSize;
    private int scanIndex;
    private int candidateAddress = -1;
    private boolean candidateFits;
    private int highlightedAddress = -1;
    private MemoryBlock selectedBlock;
    private String decisionMessage = "";
    private Consumer<String> decisionStatus;
    private Runnable decisionCompletion;
    private boolean decisionComplete;

    MemoryLayoutPanel() {
        setBackground(Color.WHITE);
        setPreferredSize(new Dimension(520, 480));
        ToolTipManager.sharedInstance().registerComponent(this);
        animationTimer = new Timer(16, event -> advanceAnimation());
        selectionTimer = new Timer(250, event -> advanceSelection());
        highlightTimer = new Timer(1400, event -> {
            highlightedAddress = -1;
            selectedBlock = null;
            repaint();
        });
        highlightTimer.setRepeats(false);
    }

    void animateSelection(List<MemoryBlock> currentBlocks, int requestedSize,
                          AllocationAlgorithm algorithm, int stepDelayMs,
                          Consumer<String> status, Runnable completion) {
        selectionTimer.stop();
        highlightTimer.stop();
        blocks = List.copyOf(currentBlocks);
        scanCandidates = blocks.stream().filter(MemoryBlock::isFree).toList();
        selectionAlgorithm = algorithm;
        selectionRequestSize = requestedSize;
        scanIndex = 0;
        candidateAddress = -1;
        candidateFits = false;
        highlightedAddress = -1;
        selectedBlock = null;
        decisionMessage = "Scanning free blocks for " + requestedSize + " units...";
        decisionStatus = status;
        decisionCompletion = completion;
        decisionComplete = false;
        selectionTimer.setDelay(Math.max(140, stepDelayMs));
        selectionTimer.setInitialDelay(0);
        selectionTimer.start();
        repaint();
    }

    void clearSelectionHighlight() {
        selectionTimer.stop();
        highlightTimer.stop();
        animationTimer.stop();
        previousBlocks = List.of();
        animationProgress = 1f;
        candidateAddress = -1;
        highlightedAddress = -1;
        selectedBlock = null;
        decisionComplete = false;
        decisionCompletion = null;
        decisionStatus = null;
        decisionMessage = "";
        repaint();
    }

    void animateTo(List<MemoryBlock> nextBlocks, int totalMemory, int durationMs) {
        if (blocks.equals(nextBlocks)) {
            return;
        }
        if (selectedBlock != null && nextBlocks.stream().noneMatch(block -> block.isAllocated()
                && block.getStartAddress() == selectedBlock.getStartAddress())) {
            highlightTimer.stop();
            highlightedAddress = -1;
            selectedBlock = null;
        }
        int nextTotalMemory = Math.max(1, totalMemory);
        previousBlocks = this.totalMemory == nextTotalMemory ? blocks : List.of();
        blocks = List.copyOf(nextBlocks);
        this.totalMemory = nextTotalMemory;
        animationDurationMs = Math.max(120, durationMs);
        animationProgress = previousBlocks.isEmpty() ? 1f : 0f;
        animationStartedAt = System.nanoTime();
        setPreferredSize(new Dimension(520, 480));
        revalidate();
        repaint();
        if (animationProgress < 1f) {
            animationTimer.restart();
        }
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        Graphics2D g = (Graphics2D) graphics.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int mapTop = 28;
        int mapHeight = Math.max(1, getHeight() - mapTop - 34);
        int x = 112;
        int width = Math.max(180, getWidth() - x - 24);
        if (animationProgress < 1f) {
            drawBlocks(g, previousBlocks, mapTop, mapHeight, x, width, 1f - animationProgress, false);
            drawBlocks(g, blocks, mapTop, mapHeight, x, width, animationProgress, animationProgress >= 0.82f);
        } else {
            drawBlocks(g, blocks, mapTop, mapHeight, x, width, 1f, true);
        }
        if (!decisionMessage.isBlank()) {
            g.setColor(INK);
            g.setFont(getFont().deriveFont(Font.BOLD, 11f));
            String message = decisionMessage;
            while (message.length() > 3 && g.getFontMetrics().stringWidth(message) > width) {
                message = message.substring(0, message.length() - 4) + "...";
            }
            g.drawString(message, x, 19);
        }
        if (blocks.isEmpty()) {
            g.setColor(INK);
            g.drawString("No memory blocks", x + 12, mapTop + 20);
        }
        g.dispose();
    }

    @Override
    public String getToolTipText(MouseEvent event) {
        int mapTop = 28;
        int mapHeight = Math.max(1, getHeight() - mapTop - 34);
        if (event.getX() < 112 || event.getX() >= getWidth() - 24
            || event.getY() < mapTop || event.getY() >= mapTop + mapHeight) {
            return null;
        }
        int address = (int) ((long) (event.getY() - mapTop) * totalMemory / mapHeight);
        for (MemoryBlock block : blocks) {
            if (address >= block.getStartAddress() && address < block.getEndAddress()) {
                String owner = block.isFree() ? "Free" : "Process " + block.getProcessId();
                return owner + " | " + block.getSize() + " units | addresses "
                        + block.getStartAddress() + " to " + block.getEndAddress();
            }
        }
        return null;
    }

    private void drawBlocks(Graphics2D g, List<MemoryBlock> visibleBlocks, int mapTop, int mapHeight,
                            int x, int width, float opacity, boolean showLabels) {
        Composite previousComposite = g.getComposite();
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, Math.max(0f, Math.min(1f, opacity))));
        int lastAddressLabelY = Integer.MIN_VALUE;
        for (MemoryBlock block : visibleBlocks) {
            int top = mapTop + (int) ((long) mapHeight * block.getStartAddress() / totalMemory);
            int bottom = mapTop + (int) ((long) mapHeight * block.getEndAddress() / totalMemory);
            int height = Math.max(1, bottom - top);
                boolean inspecting = block.getStartAddress() == candidateAddress;
                boolean winner = block.getStartAddress() == highlightedAddress
                    || selectedBlock != null && block.getStartAddress() == selectedBlock.getStartAddress();
                    Color fill = inspecting ? candidateFits ? CANDIDATE_FILL : REJECTED_FILL : winner ? WINNER_FILL
                    : block.isFree() ? FREE_FILL : processFill(block.getProcessId());
                    Color edge = inspecting ? candidateFits ? CANDIDATE_EDGE : REJECTED_EDGE : winner ? WINNER_EDGE
                    : block.isFree() ? FREE_EDGE : processEdge(block.getProcessId());
            g.setColor(fill);
            g.fillRoundRect(x, top, width, height, 7, 7);
            g.setColor(edge);
                g.setStroke(new java.awt.BasicStroke(inspecting || winner ? 2.5f : 1f));
                g.drawRoundRect(x, top, width, height, 7, 7);
            if (showLabels) {
                g.setColor(INK);
                g.setFont(getFont().deriveFont(Font.PLAIN, 11f));
                int addressY = Math.min(getHeight() - 8, top + 12);
                if (addressY - lastAddressLabelY >= 14) {
                    g.drawString(Integer.toString(block.getStartAddress()), 12, addressY);
                    lastAddressLabelY = addressY;
                }
                if (height >= 34) {
                    g.setFont(getFont().deriveFont(Font.BOLD, 12f));
                    String text = block.isFree() ? "FREE  |  " + block.getSize() + " units"
                            : block.getProcessId() + "  |  " + block.getSize() + " units";
                    FontMetrics metrics = g.getFontMetrics();
                    while (text.length() > 3 && metrics.stringWidth(text) > width - 24) {
                        text = text.substring(0, text.length() - 4) + "...";
                    }
                    g.drawString(text, x + 12, top + height / 2 + 4);
                }
            }
        }
        if (showLabels && !visibleBlocks.isEmpty()) {
            MemoryBlock last = visibleBlocks.get(visibleBlocks.size() - 1);
            g.setColor(INK);
            g.setFont(getFont().deriveFont(Font.PLAIN, 11f));
            g.drawString(Integer.toString(last.getEndAddress()), 18, mapTop + mapHeight + 14);
        }
        g.setComposite(previousComposite);
    }

    private void advanceAnimation() {
        long elapsedMs = (System.nanoTime() - animationStartedAt) / 1_000_000;
        animationProgress = Math.min(1f, (float) elapsedMs / animationDurationMs);
        if (animationProgress >= 1f) {
            animationTimer.stop();
            previousBlocks = List.of();
        }
        repaint();
    }

    private void advanceSelection() {
        if (decisionComplete) {
            selectionTimer.stop();
            candidateAddress = -1;
            if (selectedBlock != null) {
                highlightedAddress = selectedBlock.getStartAddress();
                highlightTimer.restart();
            }
            decisionMessage = "";
            repaint();
            Runnable completion = decisionCompletion;
            decisionCompletion = null;
            decisionStatus = null;
            if (completion != null) {
                completion.run();
            }
            return;
        }

        if (scanIndex < scanCandidates.size()) {
            MemoryBlock candidate = scanCandidates.get(scanIndex++);
            candidateAddress = candidate.getStartAddress();
            boolean fits = candidate.getSize() >= selectionRequestSize;
            candidateFits = fits;
            if (!fits) {
                updateDecisionStatus(selectionAlgorithm + " checks address " + candidateAddress + " ("
                        + candidate.getSize() + " units): too small for " + selectionRequestSize + ".");
            } else if (selectionAlgorithm == AllocationAlgorithm.FIRST_FIT) {
                selectedBlock = candidate;
                updateDecisionStatus("First Fit selects the first fitting block at " + candidateAddress
                        + " (" + candidate.getSize() + " units).");
                decisionComplete = true;
            } else {
                boolean improvesSelection = selectedBlock == null
                        || selectionAlgorithm == AllocationAlgorithm.BEST_FIT && candidate.getSize() < selectedBlock.getSize()
                    || selectionAlgorithm == AllocationAlgorithm.WORST_FIT && candidate.getSize() > selectedBlock.getSize();
                if (improvesSelection) {
                    selectedBlock = candidate;
                }
                String choice = improvesSelection ? "new current choice" : "keep address " + selectedBlock.getStartAddress();
                updateDecisionStatus(selectionAlgorithm + " checks address " + candidateAddress + " ("
                    + candidate.getSize() + " units): " + choice + " by block size.");
            }
            repaint();
            return;
        }

        if (selectedBlock == null) {
            updateDecisionStatus("No free block can fit " + selectionRequestSize + " units.");
        } else if (selectionAlgorithm != AllocationAlgorithm.FIRST_FIT) {
            String criterion = selectionAlgorithm == AllocationAlgorithm.BEST_FIT ? "smallest" : "largest";
            updateDecisionStatus(selectionAlgorithm + " selects the " + criterion + " fitting block at "
                    + selectedBlock.getStartAddress() + " (" + selectedBlock.getSize() + " units).");
        }
        candidateAddress = selectedBlock == null ? -1 : selectedBlock.getStartAddress();
        decisionComplete = true;
        repaint();
    }

    private void updateDecisionStatus(String message) {
        decisionMessage = message;
        if (decisionStatus != null) {
            decisionStatus.accept(message);
        }
    }

    private static Color processFill(String processId) {
        return PROCESS_FILLS[Math.floorMod(processId.hashCode(), PROCESS_FILLS.length)];
    }

    private static Color processEdge(String processId) {
        return PROCESS_EDGES[Math.floorMod(processId.hashCode(), PROCESS_EDGES.length)];
    }

    @Override
    public Dimension getPreferredScrollableViewportSize() {
        return new Dimension(520, 480);
    }

    @Override
    public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
        return orientation == SwingConstants.VERTICAL ? 24 : 32;
    }

    @Override
    public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
        return orientation == SwingConstants.VERTICAL ? Math.max(24, visibleRect.height - 24)
                : Math.max(32, visibleRect.width - 32);
    }

    @Override
    public boolean getScrollableTracksViewportWidth() {
        return true;
    }

    @Override
    public boolean getScrollableTracksViewportHeight() {
        return true;
    }
}
