package io.goobox.sync.sia.db;

import io.goobox.sync.sia.model.SiaFile;
import io.goobox.sync.storj.Utils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dizitart.no2.Nitrite;
import org.dizitart.no2.objects.ObjectFilter;
import org.dizitart.no2.objects.ObjectRepository;
import org.dizitart.no2.objects.filters.ObjectFilters;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

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
        return ObjectFilters.eq("name", name);
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

    public synchronized static boolean contains(SiaFile file) {
        return contains(file.getName());
    }

    public synchronized static boolean contains(Path localPath) {
        return contains(Utils.getSyncDir().relativize(localPath).toString());
    }

    private synchronized static boolean contains(String name) {
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

    private synchronized static SyncFile get(String name) {
        final SyncFile res = repo().find(withName(name)).firstOrDefault();
        logger.trace("get({}) = {}", name, res);
        return res;
    }

    private synchronized static SyncFile getOrCreate(SiaFile file) {
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

    public synchronized static void remove(SiaFile file) {
        remove(file.getName());
    }

    public synchronized static void remove(Path localPath) {
        remove(Utils.getSyncDir().relativize(localPath).toString());
    }

    private synchronized static void remove(String name) {
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

    public synchronized static void addForDownload(SiaFile file) {
        SyncFile syncFile = getOrCreate(file);
        syncFile.setCloudData(file);
        syncFile.setState(SyncState.FOR_DOWNLOAD);
        repo().update(syncFile);
    }

    public synchronized static void setDownloading(SiaFile file) {
        final SyncFile syncFile = get(file);
        syncFile.setState(SyncState.DOWNLOADING);
        repo().update(syncFile);
    }

    public synchronized static void addForUpload(Path localPath) throws IOException {
        SyncFile syncFile = getOrCreate(localPath);
        syncFile.setLocalData(localPath);
        syncFile.setState(SyncState.FOR_UPLOAD);
        repo().update(syncFile);
    }

    public synchronized static void setUploading(Path localPath) {
        final SyncFile syncFile = get(localPath);
        syncFile.setState(SyncState.UPLOADING);
        repo().update(syncFile);
    }

    public synchronized static void setDownloadFailed(SiaFile file) {
        SyncFile syncFile = get(file);
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

    public static void setConflict(SiaFile siaFile) throws IOException {
        SyncFile syncFile = getOrCreate(siaFile);
        syncFile.setCloudData(siaFile);
        syncFile.setLocalData(siaFile.getLocalPath());
        syncFile.setState(SyncState.CONFLICT);
        repo().update(syncFile);
    }

    public static void main(String[] args) {
        List<SyncFile> files = repo().find().toList();
        for (SyncFile file : files) {
            System.out.println(file);
        }
    }

}