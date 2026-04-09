package com.musiclibrary.models;

import java.util.List;

public class Playlist {
    public int id;
    public String name;
    public List<Song> songs;

    public Playlist(int id, String name) {
        this.id = id;
        this.name = name;
    }

    @Override
    public String toString() {
        return String.format("[%d] %s", id, name);
    }
}