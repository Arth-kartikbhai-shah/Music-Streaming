package com.musiclibrary;

import com.musiclibrary.models.*;
import java.sql.*;
import java.util.*;

public class DatabaseManager {
    private Connection conn;

    // ── Replace with your Cloud SQL public IP and credentials ─────────────────
    private static final String DB_URL  = "jdbc:mysql://127.0.0.1:3307/musiclibrary?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
    private static final String DB_USER = "root";
    private static final String DB_PASS = "YOUR_PASSWORD";

    public void connect() throws SQLException {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            throw new SQLException("MySQL JDBC Driver not found.", e);
        }
        conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
        System.out.println("✅ Connected to Cloud SQL (MySQL)");
    }

    public void disconnect() throws SQLException {
        if (conn != null && !conn.isClosed()) conn.close();
    }

    // ─── SEARCH SONGS ────────────────────────────────────────────────────────
    public List<Song> searchSongs(String keyword) throws SQLException {
        List<Song> results = new ArrayList<>();
        String sql = """
            SELECT s.id, s.title, a.name AS artist, s.album,
                   s.duration_seconds, s.release_year, s.gcs_url
            FROM songs s
            JOIN artists a ON s.artist_id = a.id
            WHERE LOWER(s.title) LIKE LOWER(?)
            ORDER BY s.title
            """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, "%" + keyword + "%");
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                results.add(new Song(
                    rs.getInt("id"), rs.getString("title"), rs.getString("artist"),
                    rs.getString("album"), rs.getInt("duration_seconds"),
                    rs.getInt("release_year"), rs.getString("gcs_url")
                ));
            }
        }
        return results;
    }

    // ─── SEARCH ARTISTS ──────────────────────────────────────────────────────
    public List<Artist> searchArtists(String keyword) throws SQLException {
        List<Artist> results = new ArrayList<>();
        String sql = "SELECT * FROM artists WHERE LOWER(name) LIKE LOWER(?) ORDER BY name";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, "%" + keyword + "%");
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                results.add(new Artist(
                    rs.getInt("id"), rs.getString("name"),
                    rs.getString("genre"), rs.getString("bio")
                ));
            }
        }
        return results;
    }

    // ─── GET SONGS BY ARTIST ─────────────────────────────────────────────────
    public List<Song> getSongsByArtist(int artistId) throws SQLException {
        List<Song> results = new ArrayList<>();
        String sql = """
            SELECT s.id, s.title, a.name AS artist, s.album,
                   s.duration_seconds, s.release_year, s.gcs_url
            FROM songs s
            JOIN artists a ON s.artist_id = a.id
            WHERE a.id = ?
            ORDER BY s.title
            """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, artistId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                results.add(new Song(
                    rs.getInt("id"), rs.getString("title"), rs.getString("artist"),
                    rs.getString("album"), rs.getInt("duration_seconds"),
                    rs.getInt("release_year"), rs.getString("gcs_url")
                ));
            }
        }
        return results;
    }

    // ─── ADD SONG ─────────────────────────────────────────────────────────────
    public void addSong(String title, int artistId, String album,
                        int duration, int year, String gcsUrl) throws SQLException {
        String sql = "INSERT INTO songs (title, artist_id, album, duration_seconds, release_year, gcs_url) VALUES (?,?,?,?,?,?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, title);
            ps.setInt(2, artistId);
            ps.setString(3, album);
            ps.setInt(4, duration);
            ps.setInt(5, year);
            ps.setString(6, gcsUrl);
            ps.executeUpdate();
        }
    }

    // ─── CREATE PLAYLIST ─────────────────────────────────────────────────────
    public int createPlaylist(String name) throws SQLException {
        String sql = "INSERT INTO playlists (name) VALUES (?)";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, name);
            ps.executeUpdate();
            ResultSet keys = ps.getGeneratedKeys();
            if (keys.next()) return keys.getInt(1);
        }
        return -1;
    }

    // ─── GET ALL PLAYLISTS ────────────────────────────────────────────────────
    public List<Playlist> getAllPlaylists() throws SQLException {
        List<Playlist> results = new ArrayList<>();
        String sql = "SELECT * FROM playlists ORDER BY created_at DESC";
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                results.add(new Playlist(rs.getInt("id"), rs.getString("name")));
            }
        }
        return results;
    }

    // ─── ADD SONG TO PLAYLIST ─────────────────────────────────────────────────
    public void addSongToPlaylist(int playlistId, int songId) throws SQLException {
        int pos = 1;
        String countSql = "SELECT COUNT(*) FROM playlist_songs WHERE playlist_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(countSql)) {
            ps.setInt(1, playlistId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) pos = rs.getInt(1) + 1;
        }
        String sql = "INSERT IGNORE INTO playlist_songs (playlist_id, song_id, position) VALUES (?,?,?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, playlistId);
            ps.setInt(2, songId);
            ps.setInt(3, pos);
            ps.executeUpdate();
        }
    }

    // ─── GET PLAYLIST SONGS ───────────────────────────────────────────────────
    public List<Song> getPlaylistSongs(int playlistId) throws SQLException {
        List<Song> results = new ArrayList<>();
        String sql = """
            SELECT s.id, s.title, a.name AS artist, s.album,
                   s.duration_seconds, s.release_year, s.gcs_url
            FROM playlist_songs ps
            JOIN songs s ON ps.song_id = s.id
            JOIN artists a ON s.artist_id = a.id
            WHERE ps.playlist_id = ?
            ORDER BY ps.position
            """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, playlistId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                results.add(new Song(
                    rs.getInt("id"), rs.getString("title"), rs.getString("artist"),
                    rs.getString("album"), rs.getInt("duration_seconds"),
                    rs.getInt("release_year"), rs.getString("gcs_url")
                ));
            }
        }
        return results;
    }

    // ─── REMOVE FROM PLAYLIST ─────────────────────────────────────────────────
    public void removeFromPlaylist(int playlistId, int songId) throws SQLException {
        String sql = "DELETE FROM playlist_songs WHERE playlist_id = ? AND song_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, playlistId);
            ps.setInt(2, songId);
            ps.executeUpdate();
        }
    }
}
