/*
 * Copyright (C) 2017-2018 Junpei Kawamoto
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package io.goobox.sync.sia;

import io.goobox.sync.common.Utils;
import io.goobox.sync.sia.db.DB;
import io.goobox.sync.sia.db.SyncFile;
import io.methvin.watcher.DirectoryChangeEvent;
import io.methvin.watcher.DirectoryChangeListener;
import io.methvin.watcher.DirectoryWatcher;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class FileWatcher implements DirectoryChangeListener, Runnable, Closeable {

    private static final Logger logger = LoggerFactory.getLogger(FileWatcher.class);

    /**
     * Minimum elapsed time this FileWatcher decides file updates end.
     */
    static final long MinElapsedTime = 3000L;

    @NotNull
    private final Path syncDir;
    @NotNull
    private final DirectoryWatcher watcher;

    /**
     * Tracks last modified times of files in progress.
     * The items have to be deleted after they are pushed to sync DB.
     */
    private final Map<Path, Long> trackingFiles = new HashMap<>();

    FileWatcher(@NotNull final Path syncDir, @NotNull final ScheduledExecutorService executor) throws IOException {

        logger.info("Start watching {}", syncDir);
        this.syncDir = syncDir;
        this.watcher = DirectoryWatcher.builder().path(syncDir).listener(this).build();
        this.watcher.watchAsync(executor);
        executor.scheduleAtFixedRate(this, 0, MinElapsedTime, TimeUnit.MILLISECONDS);

    }

    @Override
    public synchronized void onEvent(final DirectoryChangeEvent event) {
        logger.trace(new ReflectionToStringBuilder(event).toString());

        if (event.eventType() == DirectoryChangeEvent.EventType.OVERFLOW) {
            logger.warn("{} is overflowed", event.path());
            return;
        }

        if (Utils.isExcluded(event.path())) {
            logger.debug("{} is excluded", event.path());
            return;
        }

        switch (event.eventType()) {
            case CREATE:
                try {
                    Files.walk(event.path())
                            .filter(path -> !Files.isDirectory(path))
                            .forEach(this::onCreate);
                } catch (IOException | UncheckedIOException e) {
                    logger.error("Failed walking the file tree: {}", e.getMessage());
                }
                break;

            case MODIFY:
                if (!Files.isDirectory(event.path())) {
                    this.onModify(event.path());
                }
                break;

            case DELETE:
                if (this.trackingFiles.containsKey(event.path())) {
                    this.trackingFiles.remove(event.path());
                }
                DB.getFiles()
                        .map(SyncFile::getLocalPath)
                        .filter(Optional::isPresent)
                        .map(Optional::get)
                        .filter(localPath -> localPath.startsWith(event.path()))
                        .forEach(this::onDelete);
                break;
        }

    }

    @Override
    public synchronized void run() {

        final long now = System.currentTimeMillis();
        try {

            final List<Path> removePaths = new ArrayList<>();
            this.trackingFiles.forEach((localPath, lastModifiedTime) -> {

                if (now - lastModifiedTime < FileWatcher.MinElapsedTime) {
                    logger.trace("{} in modified {} msec before and skipped", localPath, now - lastModifiedTime);
                    return;
                }

                final String name = getName(localPath);
                final boolean shouldBeAdded = DB.get(name).map(syncFile -> {

                    try (final InputStream in = Files.newInputStream(localPath)) {
                        final String digest = DigestUtils.sha512Hex(in);
                        if (syncFile.getLocalDigest().map(digest::equals).orElse(false)) {
                            logger.trace("File {} is modified but the contents are not changed", name);
                            removePaths.add(localPath);
                            return false;
                        }
                    } catch (final IOException e) {
                        logger.error("Failed to compute digest of {}: {}", name, e.getMessage());
                    }
                    return true;

                }).orElse(true);

                if (shouldBeAdded) {
                    try {
                        logger.info("Found modified file {}", name);
                        DB.addNewFile(name, localPath);
                        App.getInstance().ifPresent(app -> app.refreshOverlayIcon(localPath));
                        removePaths.add(localPath);
                    } catch (IOException e) {
                        logger.error("Failed to add a new file {} to the sync DB: {}", name, e.getMessage());
                    }
                }

            });
            removePaths.forEach(this.trackingFiles::remove);

        } finally {
            DB.commit();
        }

    }

    @Override
    public void close() {
        try {
            logger.info("Closing the file watcher");
            this.watcher.close();
        } catch (final IOException e) {
            logger.error("Failed to stop file watching service: {}", e.getMessage());
        }
    }

    @NotNull
    private String getName(@NotNull final Path localPath) {
        return this.syncDir.relativize(localPath).toString();
    }

    private void onCreate(@NotNull final Path localPath) {
        final long now = System.currentTimeMillis();
        logger.debug("{} is created at {}", localPath, now);
        this.trackingFiles.put(localPath, now);
    }

    private void onModify(@NotNull final Path localPath) {
        final long now = System.currentTimeMillis();
        logger.debug("{} is modified at {}", localPath, now);
        this.trackingFiles.put(localPath, now);
    }

    private void onDelete(@NotNull final Path localPath) {
        logger.info("{} is deleted", localPath);
        if (this.trackingFiles.containsKey(localPath)) {
            this.trackingFiles.remove(localPath);
        }
        final String name = this.getName(localPath);
        DB.get(name).ifPresent(syncFile -> {
            DB.setDeleted(name);
            DB.commit();
        });
    }

}
