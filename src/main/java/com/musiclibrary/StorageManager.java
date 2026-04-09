package com.musiclibrary;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.storage.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;

public class StorageManager {
    private Storage storage;
    private String bucketName;

    public StorageManager(String bucketName) throws IOException {
        this.bucketName = bucketName;
        GoogleCredentials credentials = GoogleCredentials.getApplicationDefault()
            .createScoped(Collections.singletonList("https://www.googleapis.com/auth/cloud-platform"));

        this.storage = StorageOptions.newBuilder()
            .setCredentials(credentials)
            .build()
            .getService();

        System.out.println("✅ Connected to Google Cloud Storage");
    }

    public String uploadSong(String localFilePath, String destinationFileName) throws IOException {
        BlobId blobId = BlobId.of(bucketName, destinationFileName);
        BlobInfo blobInfo = BlobInfo.newBuilder(blobId)
            .setContentType("audio/mpeg")
            .build();
        storage.createFrom(blobInfo, Paths.get(localFilePath));
        String gcsUrl = "gs://" + bucketName + "/" + destinationFileName;
        System.out.println("✅ Uploaded: " + gcsUrl);
        return gcsUrl;
    }

    public void downloadSong(String gcsFileName, String localDestination) {
        Blob blob = storage.get(BlobId.of(bucketName, gcsFileName));
        if (blob == null) {
            System.out.println("❌ File not found in GCS: " + gcsFileName);
            return;
        }
        blob.downloadTo(Paths.get(localDestination));
        System.out.println("✅ Downloaded to: " + localDestination);
    }

    /**
     * Downloads a song from GCS to a temp file and returns the local path.
     * The gcsUrl can be in format: gs://bucket/filename or just filename
     */
    public String downloadToTemp(String gcsUrl) throws IOException {
        String fileName = gcsUrl;
        if (gcsUrl.startsWith("gs://")) {
            fileName = gcsUrl.substring(gcsUrl.lastIndexOf("/") + 1);
        }
        Path tempFile = Files.createTempFile("music_", "_" + fileName);
        tempFile.toFile().deleteOnExit();
        System.out.println("⬇️  Downloading \"" + fileName + "\" from cloud...");
        Blob blob = storage.get(BlobId.of(bucketName, fileName));
        if (blob == null) {
            throw new IOException("File not found in GCS: " + fileName);
        }
        blob.downloadTo(tempFile);
        System.out.println("✅ Ready to play!");
        return tempFile.toString();
    }

    public void listSongs() {
        System.out.println("\n🎵 Files in bucket '" + bucketName + "':");
        for (Blob blob : storage.list(bucketName).iterateAll()) {
            System.out.println("  - " + blob.getName() + " (" + blob.getSize() + " bytes)");
        }
    }
}