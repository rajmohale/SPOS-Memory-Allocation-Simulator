package com.spos.memory;

import com.spos.memory.ui.SimulatorFrame;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;

public final class Main {
    private Main() {
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) {
                // Keep Swing's default look and feel when the platform theme is unavailable.
            }
            new SimulatorFrame().setVisible(true);
        });
    }
}
