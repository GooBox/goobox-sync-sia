/*
 * Copyright (C) 2017 Junpei Kawamoto
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
import io.methvin.watcher.DirectoryChangeEvent;
import io.methvin.watcher.DirectoryChangeListener;
import io.methvin.watcher.DirectoryWatcher;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import java.io.Closeable;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class FileWatcher implements DirectoryChangeListener, Runnable, Closeable {

    private static final Logger logger = LogManager.getLogger();

    /**
     * Minimum elapsed time this FileWatcher decides file updates end.
     */
    static final long MinElapsedTime = 3000L;

    @NotNull
    private final Path target;
    @NotNull
    private final DirectoryWatcher watcher;

    /**
     * Tracks last modified times of files in progress.
     * The items have to be deleted after they are pushed to sync DB.
     */
    private final Map<Path, Long> trackingFiles = new HashMap<>();

    FileWatcher(@NotNull final Path target, @NotNull final ScheduledExecutorService executor) throws IOException {

        logger.info("Start watching {}", target);
        this.target = target;
        this.watcher = DirectoryWatcher.create(target, this);
        this.watcher.watchAsync(executor);
        executor.scheduleAtFixedRate(this, 0, MinElapsedTime, TimeUnit.MILLISECONDS);

    }

    @Override
    public synchronized void onEvent(final DirectoryChangeEvent event) {
        logger.traceEntry(new ReflectionToStringBuilder(event).toString());
        final long now = System.currentTimeMillis();

        if (event.eventType() == DirectoryChangeEvent.EventType.OVERFLOW) {
            logger.warn("{} is overflowed", event.path());
            return;
        }

        if (event.path().toFile().isDirectory()) {
            logger.trace("{} is a directory and will be ignored", event.path());
            return;
        }
        if (Utils.isExcluded(event.path())) {
            logger.debug("{} is excluded", event.path());
            return;
        }

        final String name = this.getName(event.path());
        switch (event.eventType()) {
            case CREATE:
                logger.trace("{} is created at {}", event.path(), now);
                this.trackingFiles.put(event.path(), now);
                break;

            case MODIFY:
                logger.trace("{} is modified at {}", event.path(), now);
                this.trackingFiles.put(event.path(), now);
                break;

            case DELETE:
                logger.trace("{} is deleted at {}", event.path(), now);
                if (this.trackingFiles.containsKey(event.path())) {
                    this.trackingFiles.remove(event.path());
                }
                DB.get(name).ifPresent(syncFile -> {

                    try {
                        switch (syncFile.getState()) {
                            case SYNCED:
                            case FOR_UPLOAD:
                            case UPLOADING:
                                DB.setDeleted(name);
                                break;

                            case FOR_DOWNLOAD:
                            case DOWNLOADING:
                                break;

                        }
                    } finally {
                        DB.commit();
                    }

                });
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
                try {

                    final boolean shouldBeAdded = DB.get(name).map(syncFile -> {

                        try (final FileInputStream in = new FileInputStream(localPath.toFile())) {
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
                        logger.info("Found modified file {}", name);
                        DB.addNewFile(name, localPath);
                        removePaths.add(localPath);
                    }

                } catch (IOException e) {
                    logger.error("Failed to add a new file {} to the sync DB: {}", name, e.getMessage());
                }

            });
            removePaths.forEach(this.trackingFiles::remove);

        } finally {
            DB.commit();
        }

    }

    @Override
    public void close() throws IOException {
        this.watcher.close();
    }

    private String getName(final Path localPath) {
        return this.target.relativize(localPath).toString();
    }

}
