package com.musiclibrary;

// Inheritance
public class AdvancedMusicPlayer extends MusicPlayer {

    // Method Overriding
    @Override
    protected void beforePlay() {
        System.out.println("🔥 Advanced buffering & sound enhancement...");
    }

    @Override
    protected void play(String localFilePath, String songTitle) {
        System.out.println("🎧 Using Advanced Music Player...");
        super.play(localFilePath, songTitle); // call parent method
    }

    public void showEqualizer() {
        System.out.println("🎚️ Equalizer settings enabled.");
    }
}