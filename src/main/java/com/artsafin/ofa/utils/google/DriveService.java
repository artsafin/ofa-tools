package com.artsafin.ofa.utils.google;

import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.joining;

public class DriveService {
    public static class FindException extends Exception {
        public FindException(String message) {
            super(message);
        }

        public FindException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    public class Folder {
        private final String id;
        private final String name;

        public Folder(String id) {
            this.id = id;
            this.name = null;
        }

        public Folder(String id, String name) {
            this.id = id;
            this.name = name;
        }

        public String getId() {
            return id;
        }

        public Optional<String> getName() {
            return Optional.ofNullable(name);
        }

        public String getUrl() {
            return "https://drive.google.com/drive/u/0/folders/" + id;
        }

        public File moveFileHere(String movedFileId) throws IOException {
            Drive service = getOrCreateService();

            File file = service.files().get(movedFileId)
                    .setFields("parents")
                    .execute();

            String prevParents = String.join(",", file.getParents());

            return service.files().update(movedFileId, null)
                    .setAddParents(id)
                    .setRemoveParents(prevParents)
                    .setFields("id, parents")
                    .execute();
        }

        public List<File> deleteIfExists(String title) throws IOException {
            Drive service = getOrCreateService();

            List<File> fileList = service.files().list()
                    .setSpaces("drive")
                    .setQ(String.format("(trashed = false) and (name = '%s') and (mimeType = 'application/vnd.google-apps.spreadsheet') and ('%s' in parents)", title, id))
                    .setFields("files(id, name)")
                    .execute()
                    .getFiles();

            for (File file : fileList) {
                service.files()
                        .update(file.getId(), new File().setTrashed(true).setParents(Collections.emptyList()))
                        .execute();
            }

            return fileList;
        }
    }

    private final GoogleServiceFactory factory;
    private Drive service;

    public DriveService(GoogleServiceFactory factory) {
        this.factory = factory;
    }

    private Drive getOrCreateService() throws IOException {
        if (service == null) {
            service = factory.createDriveService();
        }

        return service;
    }

    public Folder findOneFolder(String parentFolder, String folderName) throws FindException, IOException {
        Drive service = getOrCreateService();

        FileList response = service.files().list()
                .setSpaces("drive")
                .setQ(String.format("(name = '%s') and (mimeType = 'application/vnd.google-apps.folder') and ('%s' in parents)", folderName, parentFolder))
                .setFields("files(id, name)")
                .execute();

        List<File> fileList = response.getFiles();

        if (fileList.size() == 0) {
            throw new FindException("Couldn't find destination folder ID: " + folderName);
        }

        if (fileList.size() > 1) {
            throw new FindException(String.format("Too many folders match the folder name %s: %s", folderName, fileList.stream().map(File::getId).collect(joining())));
        }

        File file = fileList.get(0);

        return new Folder(file.getId(), file.getName());
    }

    public void downloadAsPdf(String fileId, String fileName) throws IOException {
        Drive service = getOrCreateService();

        FileOutputStream fos = new FileOutputStream(fileName);

        service.files().export(fileId, "application/pdf").executeMediaAndDownloadTo(fos);

        fos.close();
    }
}
