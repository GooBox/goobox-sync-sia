package io.goobox.sync.sia.db;

import io.goobox.sync.common.Utils;
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
import java.util.Optional;

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

    public synchronized static Optional<SyncFile> get(@NotNull final CloudFile file) {
        return get(file.getName());
    }

    public synchronized static Optional<SyncFile> get(@NotNull final Path localPath) {
        return get(Utils.getSyncDir().relativize(localPath).toString());
    }

    public synchronized static Optional<SyncFile> get(@NotNull final String name) {
        final SyncFile res = repo().find(withName(name)).firstOrDefault();
        logger.trace("get({}) = {}", name, res);
        return Optional.ofNullable(res);
    }

    private static String pathToName(final Path localPath) {
        return Utils.getSyncDir().relativize(localPath).toString();
    }

    private synchronized static SyncFile getOrCreate(final CloudFile file) {
        return getOrCreate(file.getName());
    }

    private synchronized static SyncFile getOrCreate(final Path localPath) {
        return getOrCreate(pathToName(localPath));
    }

    private synchronized static SyncFile getOrCreate(String name) {
        return get(name).orElseGet(() -> {
            final SyncFile syncFile = new SyncFile();
            syncFile.setName(name);
            repo().insert(syncFile);
            logger.trace("create({})", name);
            return syncFile;
        });
    }

    public synchronized static void remove(@NotNull final CloudFile file) {
        remove(file.getName());
    }

    public synchronized static void remove(@NotNull final Path localPath) {
        remove(Utils.getSyncDir().relativize(localPath).toString());
    }

    public synchronized static void remove(@NotNull final String name) {
        logger.trace("remove({})", name);
        repo().remove(withName(name));
    }

    public synchronized static long size() {
        return repo().size();
    }

    public synchronized static void setSynced(@NotNull final CloudFile cloudFile, @NotNull final Path localPath) throws IOException {
        logger.trace("setSynced({})", cloudFile);
        SyncFile syncFile = getOrCreate(cloudFile);
        syncFile.setCloudData(cloudFile);
        syncFile.setLocalData(localPath);
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

    /**
     * Marks the given file to be uploaded to the given cloud path.
     *
     * @param localPath to the file to be uploaded.
     * @param cloudPath where the file will be stored.
     * @throws IOException if fail to access the local file.
     */
    public synchronized static void setForUpload(@NotNull final Path localPath, @NotNull final Path cloudPath) throws IOException {

        final String name = pathToName(localPath);
        final Optional<SyncFile> syncFile = get(localPath);
        if (!syncFile.isPresent()) {
            logger.warn("Update state of {} but it doesn't exist in the sync DB", name);
            return;
        }

        final SyncFile file = syncFile.get();
        logger.trace("state({}): {} -> {}", name, file.getState(), SyncState.FOR_UPLOAD);
        file.setLocalData(localPath);
        file.setCloudPath(cloudPath);
        file.setState(SyncState.FOR_UPLOAD);
        repo().update(file);

    }

    private synchronized static void setState(@NotNull final String name, @NotNull final SyncState state) {
        final Optional<SyncFile> syncFile = get(name);
        if (!syncFile.isPresent()) {
            logger.warn("Update state of {} but it doesn't exist in the sync DB", name);
        }
        syncFile.ifPresent(file -> {
            logger.trace("state({}): {} -> {}", name, file.getState(), state);
            file.setState(state);
            repo().update(file);
        });
    }

    private static void setState(@NotNull final Path localPath, @NotNull final SyncState state) {
        setState(pathToName(localPath), state);
    }

    public static void setDownloading(@NotNull final String name) {
        setState(name, SyncState.DOWNLOADING);
    }

    public static void setUploading(@NotNull final Path localPath) {
        setState(localPath, SyncState.UPLOADING);
    }

    public static void setDownloadFailed(@NotNull final String name) {
        setState(name, SyncState.DOWNLOAD_FAILED);
    }

    public static void setUploadFailed(@NotNull final Path localPath) {
        setState(localPath, SyncState.UPLOAD_FAILED);
    }

    public static void setForLocalDelete(@NotNull final Path localPath) {
        setState(localPath, SyncState.FOR_LOCAL_DELETE);
    }

    public static void setForCloudDelete(@NotNull final CloudFile file) {
        setState(file.getName(), SyncState.FOR_CLOUD_DELETE);
    }

    public synchronized static void setConflict(@NotNull final CloudFile cloudFile, @NotNull final Path localPath) throws IOException {
        SyncFile syncFile = getOrCreate(cloudFile);
        syncFile.setCloudData(cloudFile);
        syncFile.setLocalData(localPath);
        syncFile.setState(SyncState.CONFLICT);
        repo().update(syncFile);
    }

    public static void setDeleted(@NotNull final Path localPath) {
        setState(localPath, SyncState.DELETED);
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