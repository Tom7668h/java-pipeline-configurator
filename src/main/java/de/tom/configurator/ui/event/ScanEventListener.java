package de.tom.configurator.ui.event;

import de.tom.configurator.model.ScanEvent;

/**
 * Listener interface for real-time scan event dispatching to the UI.
 */
@FunctionalInterface
public interface ScanEventListener {
    void onScanProcessed(ScanEvent event);
}
