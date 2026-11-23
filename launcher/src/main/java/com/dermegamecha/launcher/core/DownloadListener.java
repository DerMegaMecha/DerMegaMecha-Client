package com.dermegamecha.launcher.core;

import java.util.function.Consumer;

public record DownloadProgress(String phase, String file, long downloaded, long total,
                               int completed, int fileCount, double bytesPerSecond) {
    public double fraction() { return total <= 0 ? 0 : Math.min(1.0, (double) downloaded / total); }
}

public interface DownloadListener {
    void update(DownloadProgress progress);
    default void failed(Throwable error) {}
}
