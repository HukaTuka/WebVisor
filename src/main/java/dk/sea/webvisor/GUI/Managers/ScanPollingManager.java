package dk.sea.webvisor.GUI.Managers;

import dk.sea.webvisor.BE.Files;
import dk.sea.webvisor.BLL.ScanningService;

import java.io.IOException;
import java.util.List;
import java.util.function.Consumer;

public class ScanPollingManager {

    private final ScanningService scanningService;
    private Thread thread;
    private volatile boolean running = false;

    public ScanPollingManager(ScanningService scanningService) {
        this.scanningService = scanningService;
    }

    public synchronized void start(Consumer<List<Files>> callback) {
        if (running) {
            return;
        }
        running = true;

        thread = new Thread(() -> {
            while (running) {
                try {
                    List<Files> newPages = scanningService.fetchAndAppendNext();
                    if (!newPages.isEmpty())
                        callback.accept(newPages);

                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    running = false;
                } catch (IOException e) {
                    running = false;
                }
            }
        });

        thread.setDaemon(true);
        thread.start();
    }

    public synchronized void stop() {
        running = false;
        if (thread != null) {
            thread.interrupt();
            thread = null;
        }
    }
}

