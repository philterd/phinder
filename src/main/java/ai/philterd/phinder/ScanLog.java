/*
 * Copyright 2026 Philterd, LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ai.philterd.phinder;

import java.io.File;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a log of a scan to track processed files and their xxHash64 hashes using an H2 database.
 */
public class ScanLog implements AutoCloseable {

    private final Connection connection;

    public ScanLog(final File databaseFile) throws SQLException {
        try {
            Class.forName("org.h2.Driver");
        } catch (final ClassNotFoundException e) {
            throw new SQLException("H2 driver not found", e);
        }
        final String url = "jdbc:h2:" + databaseFile.getAbsolutePath();
        this.connection = DriverManager.getConnection(url, "sa", "");
        initializeDatabase();
    }

    private void initializeDatabase() throws SQLException {
        try (final Statement stmt = connection.createStatement()) {
            stmt.execute("CREATE TABLE IF NOT EXISTS scanned_paths (path VARCHAR(4096) PRIMARY KEY)");
            stmt.execute("CREATE TABLE IF NOT EXISTS file_hashes (file_path VARCHAR(4096) PRIMARY KEY, hash VARCHAR(64))");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_scanned_paths_path ON scanned_paths(path)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_file_hashes_file_path ON file_hashes(file_path)");
        }
    }

    public List<String> getScannedPaths() throws SQLException {
        final List<String> paths = new ArrayList<>();
        try (final Statement stmt = connection.createStatement();
             final ResultSet rs = stmt.executeQuery("SELECT path FROM scanned_paths")) {
            while (rs.next()) {
                paths.add(rs.getString("path"));
            }
        }
        return paths;
    }

    public void addScannedPath(final String path) throws SQLException {
        final String sql = "MERGE INTO scanned_paths (path) KEY (path) VALUES (?)";
        try (final PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, path);
            pstmt.executeUpdate();
        }
    }

    public void putFileHash(final String filePath, final String hash) throws SQLException {
        final String sql = "MERGE INTO file_hashes (file_path, hash) KEY (file_path) VALUES (?, ?)";
        try (final PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, filePath);
            pstmt.setString(2, hash);
            pstmt.executeUpdate();
        }
    }

    public void clean() throws SQLException {
        try (final Statement stmt = connection.createStatement()) {
            stmt.execute("TRUNCATE TABLE scanned_paths");
            stmt.execute("TRUNCATE TABLE file_hashes");
        }
    }

    public String getFileHash(final String filePath) throws SQLException {
        final String sql = "SELECT hash FROM file_hashes WHERE file_path = ?";
        try (final PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, filePath);
            try (final ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("hash");
                }
            }
        }
        return null;
    }

    @Override
    public void close() throws SQLException {
        if (connection != null && !connection.isClosed()) {
            connection.close();
        }
    }
}
