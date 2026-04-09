package com.musiclibrary;

import java.io.*;

public class MusicPlayer {
    private Process process;
    private volatile boolean playing = false;

    public void play(String localFilePath, String songTitle) {
        stop(); // stop any currently playing song

        System.out.println("▶️  Now playing: " + songTitle);

        try {
            ProcessBuilder pb;
            String os = System.getProperty("os.name").toLowerCase();

            if (os.contains("linux")) {
                if (commandExists("mpv")) {
                    pb = new ProcessBuilder("mpv", "--no-video", "--really-quiet", localFilePath);
                } else if (commandExists("ffplay")) {
                    pb = new ProcessBuilder("ffplay", "-nodisp", "-autoexit", "-loglevel", "quiet", localFilePath);
                } else if (commandExists("vlc")) {
                    pb = new ProcessBuilder("vlc", "--intf", "dummy", "--play-and-exit", localFilePath);
                } else if (commandExists("mplayer")) {
                    pb = new ProcessBuilder("mplayer", "-really-quiet", localFilePath);
                } else {
                    System.out.println("❌ No audio player found. Run: sudo pacman -S mpv");
                    return;
                }
            } else if (os.contains("mac")) {
                pb = new ProcessBuilder("afplay", localFilePath);
            } else {
                pb = new ProcessBuilder("cmd", "/c", "start", "", localFilePath);
            }

            pb.redirectErrorStream(true);
            process = pb.start();
            playing = true;

            // Drain output so the process doesn't block
            new Thread(() -> {
                try {
                    process.getInputStream().transferTo(OutputStream.nullOutputStream());
                } catch (IOException ignored) {}
            }).start();

        } catch (IOException e) {
            System.out.println("❌ Error starting player: " + e.getMessage());
        }
    }

    public void stop() {
        if (process != null && process.isAlive()) {
            process.destroy();
            try { process.waitFor(); } catch (InterruptedException ignored) {}
        }
        playing = false;
    }

    public boolean isPlaying() {
        return playing && process != null && process.isAlive();
    }

    private boolean commandExists(String cmd) {
        try {
            Process p = new ProcessBuilder("which", cmd).start();
            return p.waitFor() == 0;
        } catch (Exception e) {
            return false;
        }
    }
}