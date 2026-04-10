package com.musiclibrary;

import com.musiclibrary.models.*;
import java.io.IOException;
import java.sql.SQLException;
import java.util.*;

public class MusicApp {
    private DatabaseManager db;
    private StorageManager storage;
    private MusicPlayer player;
    private Scanner scanner;

    private static final String BUCKET_NAME = "music-app-flexi-storage";

    public MusicApp() throws IOException {
        db = new DatabaseManager();
        storage = new StorageManager(BUCKET_NAME);
        player = new AdvancedMusicPlayer();
        scanner = new Scanner(System.in);
    }

    public void start() throws SQLException, IOException {
        db.connect();
        printBanner();
        boolean running = true;
        while (running) {
            printMainMenu();
            String choice = scanner.nextLine().trim();
            switch (choice) {
                case "1" -> searchSongs();
                case "2" -> searchArtists();
                case "3" -> managePlaylists();
                case "4" -> playSong();
                case "5" -> uploadSong();
                case "6" -> storage.listSongs();
                case "0" -> running = false;
                default  -> System.out.println("❌ Invalid option.");
            }
        }
        player.stop();
        db.disconnect();
        System.out.println("\n👋 Goodbye!");
    }

    private void printBanner() {
        System.out.println("""
            ╔══════════════════════════════════════╗
            ║   🎵  CLOUD MUSIC LIBRARY  🎵        ║
            ║   Powered by Google Cloud + Java      ║
            ╚══════════════════════════════════════╝
            """);
    }

    private void printMainMenu() {
        System.out.print("""
            ─────────────────────────────────
             MAIN MENU
            ─────────────────────────────────
             1. Search Songs
             2. Search Artists
             3. Manage Playlists
             4. Play a Song 🎵
             5. Upload Song to Cloud Storage
             6. List Cloud Storage Files
             0. Exit
            ─────────────────────────────────
            Enter choice: """);
    }

    private void searchSongs() throws SQLException {
        System.out.print("🔍 Enter song title keyword: ");
        String keyword = scanner.nextLine().trim();
        List<Song> songs = db.searchSongs(keyword);
        if (songs.isEmpty()) {
            System.out.println("No songs found for: " + keyword);
        } else {
            System.out.println("\n🎵 Results (" + songs.size() + " found):");
            songs.forEach(System.out::println);
        }
    }

    private void searchArtists() throws SQLException {
        System.out.print("🔍 Enter artist name keyword: ");
        String keyword = scanner.nextLine().trim();
        List<Artist> artists = db.searchArtists(keyword);
        if (artists.isEmpty()) {
            System.out.println("No artists found for: " + keyword);
            return;
        }
        System.out.println("\n🎤 Artists (" + artists.size() + " found):");
        artists.forEach(System.out::println);

        System.out.print("\nView songs for artist ID (or ENTER to skip): ");
        String input = scanner.nextLine().trim();
        if (!input.isEmpty()) {
            int artistId = Integer.parseInt(input);
            List<Song> songs = db.getSongsByArtist(artistId);
            if (songs.isEmpty()) System.out.println("No songs found for this artist.");
            else songs.forEach(System.out::println);
        }
    }

    private void managePlaylists() throws SQLException, IOException {
        System.out.print("""
            ─────────────────────────────────
             PLAYLISTS
            ─────────────────────────────────
             1. View All Playlists
             2. Create New Playlist
             3. Add Song to Playlist
             4. View Playlist Songs
             5. Remove Song from Playlist
             6. Play Playlist 🎵
             0. Back
            ─────────────────────────────────
            Enter choice: """);
        String choice = scanner.nextLine().trim();
        switch (choice) {
            case "1" -> {
                List<Playlist> playlists = db.getAllPlaylists();
                if (playlists.isEmpty()) System.out.println("No playlists yet.");
                else playlists.forEach(System.out::println);
            }
            case "2" -> {
                System.out.print("📝 Playlist name: ");
                String name = scanner.nextLine().trim();
                int id = db.createPlaylist(name);
                System.out.println("✅ Created playlist \"" + name + "\" with ID: " + id);
            }
            case "3" -> {
                System.out.print("Playlist ID: ");
                int pid = Integer.parseInt(scanner.nextLine().trim());
                System.out.print("Song ID to add: ");
                int sid = Integer.parseInt(scanner.nextLine().trim());
                db.addSongToPlaylist(pid, sid);
                System.out.println("✅ Song added to playlist!");
            }
            case "4" -> {
                System.out.print("Playlist ID: ");
                int pid = Integer.parseInt(scanner.nextLine().trim());
                List<Song> songs = db.getPlaylistSongs(pid);
                if (songs.isEmpty()) System.out.println("Playlist is empty.");
                else songs.forEach(System.out::println);
            }
            case "5" -> {
                System.out.print("Playlist ID: ");
                int pid = Integer.parseInt(scanner.nextLine().trim());
                System.out.print("Song ID to remove: ");
                int sid = Integer.parseInt(scanner.nextLine().trim());
                db.removeFromPlaylist(pid, sid);
                System.out.println("✅ Song removed from playlist.");
            }
            case "6" -> playPlaylist();
        }
    }

    private void playSong() throws SQLException, IOException {
        System.out.print("🔍 Enter song title keyword to find: ");
        String keyword = scanner.nextLine().trim();
        List<Song> songs = db.searchSongs(keyword);

        if (songs.isEmpty()) {
            System.out.println("No songs found for: " + keyword);
            return;
        }

        System.out.println("\n🎵 Found:");
        songs.forEach(System.out::println);

        System.out.print("\nEnter Song ID to play: ");
        int songId = Integer.parseInt(scanner.nextLine().trim());

        Song selected = songs.stream()
            .filter(s -> s.id == songId)
            .findFirst()
            .orElse(null);

        if (selected == null) {
            System.out.println("❌ Song ID not found in results.");
            return;
        }

        if (selected.gcsUrl == null || selected.gcsUrl.isBlank()) {
            System.out.println("❌ This song has no file in cloud storage.");
            return;
        }

        String localPath = storage.downloadToTemp(selected.gcsUrl);
        player.play(localPath, selected.title + " - " + selected.artistName);

        System.out.println("Press ENTER to stop playback...");
        scanner.nextLine();
        player.stop();
    }

    private void playPlaylist() throws SQLException, IOException {
        List<Playlist> playlists = db.getAllPlaylists();
        if (playlists.isEmpty()) {
            System.out.println("No playlists yet.");
            return;
        }
        System.out.println("\n📋 Your playlists:");
        playlists.forEach(System.out::println);

        System.out.print("Enter Playlist ID to play: ");
        int pid = Integer.parseInt(scanner.nextLine().trim());
        List<Song> songs = db.getPlaylistSongs(pid);

        if (songs.isEmpty()) {
            System.out.println("Playlist is empty.");
            return;
        }

        System.out.println("\n▶️  Playing playlist (" + songs.size() + " songs)...");
        System.out.println("Press ENTER after each song to play the next, or type 'q' to quit.\n");

        for (Song song : songs) {
            if (song.gcsUrl == null || song.gcsUrl.isBlank()) {
                System.out.println("⚠️  Skipping \"" + song.title + "\" — no file in cloud storage.");
                continue;
            }
            String localPath = storage.downloadToTemp(song.gcsUrl);
            player.play(localPath, song.title + " - " + song.artistName);

            System.out.print("Press ENTER for next song (or 'q' to stop): ");
            String input = scanner.nextLine().trim();
            player.stop();
            if (input.equalsIgnoreCase("q")) break;
        }
        System.out.println("✅ Playlist finished.");
    }

    private void uploadSong() throws IOException {
        System.out.print("📁 Local file path to MP3: ");
        String localPath = scanner.nextLine().trim();
        System.out.print("☁️  Destination filename in GCS (e.g. my-song.mp3): ");
        String destName = scanner.nextLine().trim();
        String gcsUrl = storage.uploadSong(localPath, destName);
        System.out.println("GCS URL: " + gcsUrl);
        System.out.println("You can now add this song to the database using its GCS URL.");
    }
}