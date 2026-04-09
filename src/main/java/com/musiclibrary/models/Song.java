package com.musiclibrary.models;

public class Song {
    public int id;
    public String title;
    public String artistName;
    public String album;
    public int durationSeconds;
    public int releaseYear;
    public String gcsUrl;

    public Song(int id, String title, String artistName, String album,
                int durationSeconds, int releaseYear, String gcsUrl) {
        this.id = id;
        this.title = title;
        this.artistName = artistName;
        this.album = album;
        this.durationSeconds = durationSeconds;
        this.releaseYear = releaseYear;
        this.gcsUrl = gcsUrl;
    }

    @Override
    public String toString() {
        int mins = durationSeconds / 60;
        int secs = durationSeconds % 60;
        return String.format("[%d] \"%s\" by %s | Album: %s | %d:%02d | %d | %s",
            id, title, artistName, album, mins, secs, releaseYear, gcsUrl);
    }
}