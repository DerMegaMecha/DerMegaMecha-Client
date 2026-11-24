package com.dermegamecha.launcher.core;

import java.util.function.Consumer;

public interface DownloadListener {
    void update(DownloadProgress progress);
    default void failed(Throwable error) {}
}
