package io.goobox.sync.sia.db;

import io.goobox.sync.sia.model.SiaFile;
import io.goobox.sync.storj.Utils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dizitart.no2.Nitrite;
import org.dizitart.no2.objects.Cursor;
import org.dizitart.no2.objects.ObjectFilter;
import org.dizitart.no2.objects.ObjectRepository;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.dizitart.no2.objects.filters.ObjectFilters.eq;

public class DB {

    private static Nitrite db;
    private static final Logger logger = LogManager.getLogger();
    public static final String DatabaseFileName = "sync.sia.db";

    private static Nitrite db() {
        if (db == null || db.isClosed()) {
            db = open();
        }
        return db;
    }

    private static ObjectRepository<SyncFile> repo() {
        return db().getRepository(SyncFile.class);
    }

    private static ObjectFilter withName(String name) {
        return eq("name", name);
    }

    private static Nitrite open() {
        Path dbPath = Utils.getDataDir().resolve(DatabaseFileName);
        return Nitrite.builder()
                .compressed()
                .filePath(dbPath.toFile())
                .openOrCreate();
    }

    public synchronized static void close() {
        db().close();
    }

    public synchronized static void commit() {
        db().commit();
    }

    public synchronized static boolean contains(CloudFile file) {
        return contains(file.getName());
    }

    public synchronized static boolean contains(Path localPath) {
        return contains(Utils.getSyncDir().relativize(localPath).toString());
    }

    public synchronized static boolean contains(String name) {
        boolean res = (get(name) != null);
        logger.trace("contains({}) = {}", name, res);
        return res;
    }

    public synchronized static SyncFile get(SiaFile file) {
        return get(file.getName());
    }

    public synchronized static SyncFile get(Path localPath) {
        return get(Utils.getSyncDir().relativize(localPath).toString());
    }

    public synchronized static SyncFile get(String name) {
        final SyncFile res = repo().find(withName(name)).firstOrDefault();
        logger.trace("get({}) = {}", name, res);
        return res;
    }

    private synchronized static SyncFile getOrCreate(CloudFile file) {
        return getOrCreate(file.getName());
    }

    private synchronized static SyncFile getOrCreate(Path localPath) {
        return getOrCreate(Utils.getSyncDir().relativize(localPath).toString());
    }

    private synchronized static SyncFile getOrCreate(String name) {
        SyncFile syncFile = get(name);
        if (syncFile == null) {
            syncFile = new SyncFile();
            syncFile.setName(name);
            repo().insert(syncFile);
            logger.trace("create({})", name);
        }
        return syncFile;
    }

    public synchronized static void remove(CloudFile file) {
        remove(file.getName());
    }

    public synchronized static void remove(Path localPath) {
        remove(Utils.getSyncDir().relativize(localPath).toString());
    }

    public synchronized static void remove(String name) {
        logger.trace("remove({})", name);
        repo().remove(withName(name));
    }

    public synchronized static long size() {
        return repo().size();
    }

    public synchronized static void setSynced(SiaFile siaFile) throws IOException {
        logger.trace("setSynced({})", siaFile);
        SyncFile syncFile = getOrCreate(siaFile);
        syncFile.setCloudData(siaFile);
        syncFile.setLocalData(siaFile.getLocalPath());
        syncFile.setState(SyncState.SYNCED);
        repo().update(syncFile);
    }

    /**
     * Add a new file to this database; the status of the file is MODIFIED.
     *
     * @param localPath of the new file.
     * @throws IOException if fail to access the file.
     */
    public synchronized static void addNewFile(final Path localPath) throws IOException {
        final SyncFile syncFile = getOrCreate(localPath);
        syncFile.setLocalData(localPath);
        syncFile.setState(SyncState.MODIFIED);
        repo().update(syncFile);
    }

    /**
     * Adds a new cloud file to this database and marks it will be downloaded.
     *
     * @param file      representing a cloud file.
     * @param localPath where the file to be downloaded to.
     */
    public synchronized static void addForDownload(@NotNull final CloudFile file, @NotNull final Path localPath) throws IOException {
        SyncFile syncFile = getOrCreate(file);
        syncFile.setCloudData(file);
        syncFile.setLocalPath(localPath);
        syncFile.setTemporaryPath(Files.createTempFile(null, null));
        syncFile.setState(SyncState.FOR_DOWNLOAD);
        repo().update(syncFile);
    }

    /**
     * Updates information of the given file and marks it as MODIFIED.
     *
     * @param localPath to the file.
     * @throws IOException if fail to access the file.
     */
    public synchronized static void setModified(final Path localPath) throws IOException {
        DB.addNewFile(localPath);
    }

    public synchronized static void setDownloading(@NotNull final String name) {
        final SyncFile syncFile = get(name);
        syncFile.setState(SyncState.DOWNLOADING);
        repo().update(syncFile);
    }

    /**
     * Marks the given file to be uploaded to the given cloud path.
     *
     * @param localPath to the file to be uploaded.
     * @param cloudPath where the file will be stored.
     * @throws IOException if fail to access the local file.
     */
    public synchronized static void setForUpload(@NotNull final Path localPath, @NotNull final Path cloudPath) throws IOException {
        SyncFile syncFile = get(localPath);
        syncFile.setLocalData(localPath);
        syncFile.setCloudPath(cloudPath);
        syncFile.setState(SyncState.FOR_UPLOAD);
        repo().update(syncFile);
    }

    public synchronized static void setUploading(Path localPath) {
        final SyncFile syncFile = get(localPath);
        syncFile.setState(SyncState.UPLOADING);
        repo().update(syncFile);
    }

    public synchronized static void setDownloadFailed(@NotNull final String name) {
        SyncFile syncFile = get(name);
        syncFile.setState(SyncState.DOWNLOAD_FAILED);
        repo().update(syncFile);
    }

    public synchronized static void setUploadFailed(Path localPath) {
        SyncFile syncFile = get(localPath);
        syncFile.setState(SyncState.UPLOAD_FAILED);
        repo().update(syncFile);
    }

    public synchronized static void setForLocalDelete(Path localPath) {
        SyncFile syncFile = get(localPath);
        syncFile.setState(SyncState.FOR_LOCAL_DELETE);
        repo().update(syncFile);
    }

    public synchronized static void setForCloudDelete(SiaFile file) {
        SyncFile syncFile = get(file);
        syncFile.setState(SyncState.FOR_CLOUD_DELETE);
        repo().update(syncFile);
    }

    public synchronized static void setConflict(SiaFile siaFile) throws IOException {
        SyncFile syncFile = getOrCreate(siaFile);
        syncFile.setCloudData(siaFile);
        syncFile.setLocalData(siaFile.getLocalPath());
        syncFile.setState(SyncState.CONFLICT);
        repo().update(syncFile);
    }

    public synchronized static void setDeleted(final Path localPath) {
        final SyncFile syncFile = get(localPath);
        syncFile.setState(SyncState.DELETED);
        repo().update(syncFile);
    }

    public synchronized static Cursor<SyncFile> getModifiedFiles() {
        return getFiles(SyncState.MODIFIED);
    }

    public synchronized static Cursor<SyncFile> getDeletedFiles() {
        return getFiles(SyncState.DELETED);
    }

    public synchronized static Cursor<SyncFile> getSyncedFiles() {
        return getFiles(SyncState.SYNCED);
    }

    public synchronized static Cursor<SyncFile> getFiles(final SyncState state) {
        return repo().find(eq("state", state));
    }

    public static void main(String[] args) {
        List<SyncFile> files = repo().find().toList();
        for (SyncFile file : files) {
            System.out.println(file);
        }
    }

}