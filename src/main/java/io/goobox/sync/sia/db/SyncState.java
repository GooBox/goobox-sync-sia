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

package io.goobox.sync.sia.db;

public enum SyncState {

    SYNCED,
    /**
     * Set by CheckStateTask to mark this file will be downloaded by DownloadRemoteFileTask.
     */
    FOR_DOWNLOAD,
    /**
     * Set by DownloadRemoteFileTask to mark this file is now being downloaded.
     */
    DOWNLOADING,
    /**
     * Set by CheckStateTask to mark this file will be uploaded by UploadLocalFileTask.
     */
    FOR_UPLOAD,
    /**
     * Set by UploadLocalFileTask to mark this file is now being uploaded.
     */
    UPLOADING,
    FOR_LOCAL_DELETE,
    FOR_CLOUD_DELETE,
    DOWNLOAD_FAILED,
    UPLOAD_FAILED,
    CONFLICT,
    /**
     * Set by FileWatcher to mark this file is a found new file or modified.
     */
    MODIFIED,
    /**
     * Set by FileWatcher to mark this file was deleted.
     */
    DELETED;

    public boolean isSynced() {
        return this == SYNCED;
    }

    public boolean isSynchronizing() {
        return this == FOR_DOWNLOAD
                || this == DOWNLOADING
                || this == FOR_UPLOAD
                || this == UPLOADING
                || this == FOR_LOCAL_DELETE
                || this == FOR_CLOUD_DELETE;
    }

    public boolean isFailed() {
        return this == DOWNLOAD_FAILED
                || this == UPLOAD_FAILED;
    }

    public boolean isConflict() {
        return this == CONFLICT;
    }

}
