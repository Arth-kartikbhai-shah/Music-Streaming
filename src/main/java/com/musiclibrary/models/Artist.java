package com.musiclibrary.models;

public class Artist {
    public int id;
    public String name;
    public String genre;
    public String bio;

    public Artist(int id, String name, String genre, String bio) {
        this.id = id;
        this.name = name;
        this.genre = genre;
        this.bio = bio;
    }

    @Override
    public String toString() {
        return String.format("[%d] %s | Genre: %s", id, name, genre);
    }
}