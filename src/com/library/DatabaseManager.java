package com.library;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.List;

public class DatabaseManager {
	private static final String DB_URL = "jdbc:h2:./library_db;AUTO_SERVER=TRUE";
    private static final String USER = "sa";
    private static final String PASSWORD = "";

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DB_URL, USER, PASSWORD);
    }

    public static void initializeDatabase() {
        try (Connection conn = getConnection(); Statement stmt = conn.createStatement()) {
            // 1. Users 表
            stmt.execute("CREATE TABLE IF NOT EXISTS users (" +
                    "id INT AUTO_INCREMENT PRIMARY KEY, " +
                    "student_no VARCHAR(50) UNIQUE, " +
                    "name VARCHAR(100), " +
                    "password VARCHAR(100), " +
                    "role_level VARCHAR(50), " +
                    "status VARCHAR(50), " +
                    "created_at VARCHAR(50))");

            // 2. Books 表
            stmt.execute("CREATE TABLE IF NOT EXISTS books (" +
                    "id INT AUTO_INCREMENT PRIMARY KEY, " +
                    "title VARCHAR(255), " +
                    "author VARCHAR(255), " +
                    "publisher VARCHAR(255), " +
                    "publish_year INT, " +
                    "status VARCHAR(50) DEFAULT 'AVAILABLE')");

            // 3. Borrow_records 表
            stmt.execute("CREATE TABLE IF NOT EXISTS borrow_records (" +
                    "record_id INT AUTO_INCREMENT PRIMARY KEY, " +
                    "user_id INT, " +
                    "book_id INT, " +
                    "borrow_date TIMESTAMP, " +
                    "due_date TIMESTAMP, " +
                    "return_date TIMESTAMP, " +
                    "borrow_days INT, " +
                    "created_at TIMESTAMP, " +
                    "FOREIGN KEY(user_id) REFERENCES users(id), " +
                    "FOREIGN KEY(book_id) REFERENCES books(id))");

            // 檢查是否需要匯入初始 JSON
            ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM users");
            if (rs.next() && rs.getInt(1) == 0) {
                importJsonData(conn);
            }
            
            // 資料庫中沒有管理員，自動生成一個
            try (PreparedStatement checkAdmin = conn.prepareStatement("SELECT COUNT(*) FROM users WHERE role_level = 'ADMIN'")) {
                ResultSet rsAdmin = checkAdmin.executeQuery();
                if (rsAdmin.next() && rsAdmin.getInt(1) == 0) {
                    stmt.execute("INSERT INTO users (student_no, name, password, role_level, status, created_at) " +
                            "VALUES ('B13204013', '系統管理員', 'B13204013', 'ADMIN', 'ACTIVE', '2026-01-01 00:00:00')");
                    System.out.println("ℹ️ 已自動為系統配置一組管理員測試帳號 [帳號: B13204013 / 密碼: B13204013]");
                    
                    stmt.execute("INSERT INTO users (student_no, name, password, role_level, status, created_at) " +
                            "VALUES ('B13204043', '系統管理員', 'B13204043', 'ADMIN', 'ACTIVE', '2026-01-01 00:00:00')");
                    System.out.println("ℹ️ 已自動為系統配置一組管理員測試帳號 [帳號: B13204043 / 密碼: B13204043]");
                    
                    stmt.execute("INSERT INTO users (student_no, name, password, role_level, status, created_at) " +
                            "VALUES ('R13945041', '系統管理員', 'R13945041', 'ADMIN', 'ACTIVE', '2026-01-01 00:00:00')");
                    System.out.println("ℹ️ 已自動為系統配置一組管理員測試帳號 [帳號: R13945041 / 密碼: R13945041]");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void importJsonData(Connection conn) throws Exception {
        Gson gson = new Gson();
        System.out.println("系統首次啟動：正在以 UTF-8 編碼解析 250 筆原始 JSON 並導入 SQL 資料庫...");

        // 匯入 Users.json
        List<User> jsonUsers;
        try (InputStreamReader isr = new InputStreamReader(new FileInputStream("Users.json"), StandardCharsets.UTF_8)) {
            jsonUsers = gson.fromJson(isr, new TypeToken<List<User>>(){}.getType());
        }
        String userSql = "INSERT INTO users (student_no, name, password, role_level, status, created_at) VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement pstmt = conn.prepareStatement(userSql)) {
            for (User u : jsonUsers) {
                pstmt.setString(1, u.student_no); pstmt.setString(2, u.name);
                pstmt.setString(3, u.password); pstmt.setString(4, u.role_level);
                pstmt.setString(5, u.status); pstmt.setString(6, u.created_at);
                pstmt.addBatch();
            }
            pstmt.executeBatch();
        }

        // 匯入 Books.json
        List<Book> jsonBooks;
        try (InputStreamReader isr = new InputStreamReader(new FileInputStream("Books.json"), StandardCharsets.UTF_8)) {
            jsonBooks = gson.fromJson(isr, new TypeToken<List<Book>>(){}.getType());
        }
        String bookSql = "INSERT INTO books (title, author, publisher, publish_year, status) VALUES (?, ?, ?, ?, 'AVAILABLE')";
        try (PreparedStatement pstmt = conn.prepareStatement(bookSql)) {
            for (Book b : jsonBooks) {
                pstmt.setString(1, b.題名);
                pstmt.setString(2, (b.作者 != null && !b.作者.isEmpty()) ? b.作者.get(0) : "未知作者");
                pstmt.setString(3, b.出版者); pstmt.setInt(4, b.出版年);
                pstmt.addBatch();
            }
            pstmt.executeBatch();
        }

        // 匯入 Borrow_records.json
        List<BorrowRecord> jsonRecords;
        try (InputStreamReader isr = new InputStreamReader(new FileInputStream("Borrow_records.json"), StandardCharsets.UTF_8)) {
            jsonRecords = gson.fromJson(isr, new TypeToken<List<BorrowRecord>>(){}.getType());
        }
        String recordSql = "INSERT INTO borrow_records (user_id, book_id, borrow_date, due_date, return_date, borrow_days, created_at) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement pstmt = conn.prepareStatement(recordSql)) {
            for (BorrowRecord r : jsonRecords) {
                pstmt.setInt(1, r.user_id);
                pstmt.setInt(2, r.book_id);
                pstmt.setTimestamp(3, parseRelativeTimestamp(r.borrow_date));
                pstmt.setTimestamp(4, parseRelativeTimestamp(r.due_date));
                pstmt.setTimestamp(5, parseRelativeTimestamp(r.return_date));
                pstmt.setInt(6, r.borrow_days);
                pstmt.setTimestamp(7, parseRelativeTimestamp(r.created_at));
                pstmt.addBatch();

                if (r.return_date == null) {
                    try (Statement stmt = conn.createStatement()) {
                        stmt.executeUpdate("UPDATE books SET status = 'BORROWED' WHERE id = " + r.book_id);
                    }
                }
            }
            pstmt.executeBatch();
        }
        System.out.println("✅ 資料庫持久化與相對時間解析全部成功！");
    }

    public static Timestamp parseRelativeTimestamp(String offsetStr) {
        if (offsetStr == null || offsetStr.isEmpty()) return null;
        try {
            int days = Integer.parseInt(offsetStr.replaceAll("[^0-9-]", ""));
            LocalDateTime targetTime = LocalDateTime.now().plusDays(days);
            return Timestamp.valueOf(targetTime);
        } catch (Exception e) { return null; }
    }
}