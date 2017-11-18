package io.goobox.sync.sia.db;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import org.dizitart.no2.Nitrite;
import org.dizitart.no2.objects.ObjectFilter;
import org.dizitart.no2.objects.ObjectRepository;
import org.dizitart.no2.objects.filters.ObjectFilters;

import io.goobox.sync.sia.util.SiaFile;
import io.goobox.sync.storj.Utils;
import io.goobox.sync.storj.db.SyncState;

public class DB {

    private static Nitrite db;

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
        Path dbPath = Utils.getDataDir().resolve("sync.db");
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
    		return contains(	file.getName());
    }

    public synchronized static boolean contains(Path path) {
        return contains(path.toString());
    }

    synchronized static boolean contains(String name) {
        return get(name) != null;
    }

    public synchronized static SyncFile get(SiaFile file) {
        return get(file.getName());
    }

    public synchronized static SyncFile get(Path path) {
        return get(path.toString());
    }

    synchronized static SyncFile get(String name) {
        return repo().find(withName(name)).firstOrDefault();
    }

    private synchronized static SyncFile getOrCreate(SiaFile file) {
        return getOrCreate(file.getName());
    }

    private synchronized static SyncFile getOrCreate(Path path) {
        return getOrCreate(path.toString());
    }

    synchronized static SyncFile getOrCreate(String name) {
        SyncFile syncFile = get(name);
        if (syncFile == null) {
            syncFile = new SyncFile();
            syncFile.setName(name);
            repo().insert(syncFile);
        }
        return syncFile;
    }

    public synchronized static void remove(SiaFile file) {
        remove(file.getName());
    }

    public synchronized static void remove(Path path) {
        remove(path.toString());
    }

    synchronized static void remove(String name) {
        repo().remove(withName(name));
    }

    public synchronized static long size() {
        return repo().size();
    }

    public synchronized static void setSynced(SiaFile siaFile) throws IOException {
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

    public synchronized static void addForUpload(Path path) throws IOException {
        SyncFile syncFile = getOrCreate(path);
        syncFile.setLocalData(path);
        syncFile.setState(SyncState.FOR_UPLOAD);
        repo().update(syncFile);
    }

    public synchronized static void setDownloadFailed(SiaFile file) {
        SyncFile syncFile = get(file);
        syncFile.setState(SyncState.DOWNLOAD_FAILED);
        repo().update(syncFile);
    }

    public synchronized static void setUploadFailed(Path path) {
        SyncFile syncFile = get(path);
        syncFile.setState(SyncState.UPLOAD_FAILED);
        repo().update(syncFile);
    }

    public synchronized static void setForLocalDelete(Path path) {
        SyncFile syncFile = get(path);
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