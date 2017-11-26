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

package io.goobox.sync.sia.db;

public enum SyncState {

    SYNCED,
    FOR_DOWNLOAD,
    DOWNLOADING,
    FOR_UPLOAD,
    UPLOADING,
    FOR_LOCAL_DELETE,
    FOR_CLOUD_DELETE,
    DOWNLOAD_FAILED,
    UPLOAD_FAILED,
    CONFLICT;

    public boolean isSynced() {
        return this == SYNCED;
    }

    public boolean isPending() {
        return this == FOR_DOWNLOAD
                || this == FOR_UPLOAD
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
