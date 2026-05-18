package com.library;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class MainApp extends JFrame {
    private JTable bookTable;
    private DefaultTableModel tableModel;
    
    private int currentUserId;
    private String currentUserName;
    private String currentUserRole;

    // 定義通用的時間格式化器，避免系統預設編碼造成的亂碼
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public MainApp(int userId, String name, String role) {
        this.currentUserId = userId;
        this.currentUserName = name;
        this.currentUserRole = role;
        initUI();
    }

    private void initUI() {
        setTitle("圖書館管理系統 Dashboard | 當前使用者: " + currentUserName);
        setSize(1080, 660);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        // 1. 左側邊導覽欄 (Sidebar)
        JPanel sidePanel = new JPanel();
        sidePanel.setLayout(new BoxLayout(sidePanel, BoxLayout.Y_AXIS));
        sidePanel.setBackground(new Color(245, 247, 250));
        sidePanel.setPreferredSize(new Dimension(240, 0));
        sidePanel.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, new Color(225, 230, 235)));

        JPanel userCard = new JPanel(new GridLayout(2, 1, 5, 5));
        userCard.setBackground(new Color(232, 244, 255));
        userCard.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        JLabel lblName = new JLabel("👤 成員: " + currentUserName);
        lblName.setFont(new Font("Microsoft JhengHei", Font.BOLD, 15));
        JLabel lblRole = new JLabel("權限層級: " + currentUserRole);
        userCard.add(lblName); userCard.add(lblRole);

        JButton btnBorrow = new JButton("📖 申請借閱書籍");
        JButton btnReturn = new JButton("↩️ 辦理歸還入庫");
        JButton btnMyRecords = new JButton("👤 我的個人借閱紀錄");
        JButton btnHistory = new JButton("🔍 全館歷史 SQL 日誌");
        JButton btnLogout = new JButton("🚪 登出帳戶系統");

        Dimension btnSize = new Dimension(210, 38);
        JButton[] buttons = {btnBorrow, btnReturn, btnMyRecords, btnHistory, btnLogout};
        for (JButton btn : buttons) {
            btn.setMaximumSize(btnSize);
            btn.setAlignmentX(Component.CENTER_ALIGNMENT);
            btn.putClientProperty("JButton.buttonType", "roundRect");
        }
        
        btnLogout.setBackground(new Color(255, 99, 71));
        btnLogout.setForeground(Color.WHITE);

        sidePanel.add(userCard); sidePanel.add(Box.createVerticalStrut(20));
        sidePanel.add(btnBorrow); sidePanel.add(Box.createVerticalStrut(10));
        sidePanel.add(btnReturn); sidePanel.add(Box.createVerticalStrut(10));
        sidePanel.add(btnMyRecords); sidePanel.add(Box.createVerticalStrut(10));
        sidePanel.add(btnHistory); sidePanel.add(Box.createVerticalGlue()); 
        sidePanel.add(btnLogout); sidePanel.add(Box.createVerticalStrut(20));
        add(sidePanel, BorderLayout.WEST);

        // 2. 右側數據表格面板
        String[] columns = {"書籍 SQL_ID", "書籍題名", "主要作者", "出版商", "出版年", "庫存狀態"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int r, int c) { return false; }
        };
        bookTable = new JTable(tableModel);
        bookTable.setRowHeight(30);
        bookTable.setShowVerticalLines(false);
        bookTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        
        refreshTableFromDatabase();

        JScrollPane scrollPane = new JScrollPane(bookTable);
        scrollPane.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        add(scrollPane, BorderLayout.CENTER);

        // 3. 功能實作

        // 申請借閱
        btnBorrow.addActionListener(e -> {
            int row = bookTable.getSelectedRow();
            if (row == -1) { JOptionPane.showMessageDialog(this, "請先選擇一本書籍。"); return; }
            int bookId = (int) bookTable.getValueAt(row, 0);
            String status = (String) bookTable.getValueAt(row, 5);

            if (status.contains("借出中")) {
                JOptionPane.showMessageDialog(this, "該書已被借出！", "提示", JOptionPane.WARNING_MESSAGE);
                return;
            }

            try (Connection conn = DatabaseManager.getConnection()) {
                conn.setAutoCommit(false);
                try {
                    try (PreparedStatement p1 = conn.prepareStatement("UPDATE books SET status = 'BORROWED' WHERE id = ?")) {
                        p1.setInt(1, bookId); p1.executeUpdate();
                    }
                    String insertSql = "INSERT INTO borrow_records (user_id, book_id, borrow_date, due_date, borrow_days, created_at) VALUES (?, ?, ?, ?, 7, ?)";
                    try (PreparedStatement p2 = conn.prepareStatement(insertSql)) {
                        p2.setInt(1, currentUserId);
                        p2.setInt(2, bookId);
                        p2.setTimestamp(3, Timestamp.valueOf(LocalDateTime.now()));
                        p2.setTimestamp(4, Timestamp.valueOf(LocalDateTime.now().plusDays(7)));
                        p2.setTimestamp(5, Timestamp.valueOf(LocalDateTime.now()));
                        p2.executeUpdate();
                    }
                    conn.commit();
                    refreshTableFromDatabase();
                    JOptionPane.showMessageDialog(this, "🎉 借閱手續完成，已即時同步資料庫！");
                } catch (Exception ex) { conn.rollback(); throw ex; }
            } catch (Exception ex) { ex.printStackTrace(); }
        });

        // 辦理歸還
        btnReturn.addActionListener(e -> {
            int row = bookTable.getSelectedRow();
            if (row == -1) return;
            int bookId = (int) bookTable.getValueAt(row, 0);
            String status = (String) bookTable.getValueAt(row, 5);

            if (status.contains("可借閱")) {
                JOptionPane.showMessageDialog(this, "此書在庫內，不需歸還。");
                return;
            }

            try (Connection conn = DatabaseManager.getConnection()) {
                conn.setAutoCommit(false);
                try {
                    try (PreparedStatement p1 = conn.prepareStatement("UPDATE books SET status = 'AVAILABLE' WHERE id = ?")) {
                        p1.setInt(1, bookId); p1.executeUpdate();
                    }
                    try (PreparedStatement p2 = conn.prepareStatement("UPDATE borrow_records SET return_date = ? WHERE book_id = ? AND return_date IS NULL")) {
                        p2.setTimestamp(1, Timestamp.valueOf(LocalDateTime.now()));
                        p2.setInt(2, bookId); p2.executeUpdate();
                    }
                    conn.commit();
                    refreshTableFromDatabase();
                    JOptionPane.showMessageDialog(this, "✅ 歸還成功！庫存已同步重置。");
                } catch (Exception ex) { conn.rollback(); throw ex; }
            } catch (Exception ex) { ex.printStackTrace(); }
        });

        // 我的個人借閱紀錄 (修復亂碼)
        btnMyRecords.addActionListener(e -> {
            StringBuilder sb = new StringBuilder();
            sb.append("👤 ").append(currentUserName).append(" 的專屬個人借還書軌跡：\n");
            sb.append("====================================================\n\n");

            String sql = "SELECT r.*, b.title FROM borrow_records r JOIN books b ON r.book_id = b.id WHERE r.user_id = ? ORDER BY r.borrow_date DESC";
            
            try (Connection conn = DatabaseManager.getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setInt(1, currentUserId);
                ResultSet rs = pstmt.executeQuery();

                boolean hasData = false;
                while (rs.next()) {
                    hasData = true;
                    sb.append("📖 書名: 《").append(rs.getString("title")).append("》 (書籍ID: ").append(rs.getInt("book_id")).append(")\n")
                      .append("   ↳ 借出時間: ").append(formatTimestamp(rs.getTimestamp("borrow_date"))).append("\n")
                      .append("   ↳ 應還日期: ").append(formatTimestamp(rs.getTimestamp("due_date"))).append("\n")
                      .append("   ↳ 狀態: ").append(rs.getTimestamp("return_date") != null ? "已歸還 (" + formatTimestamp(rs.getTimestamp("return_date")) + ")" : "❌ 仍在借閱中(未還)").append("\n")
                      .append("----------------------------------------------------------------------\n");
                }
                if (!hasData) sb.append("（您在資料庫中尚無任何借閱紀錄）");
            } catch (SQLException ex) { ex.printStackTrace(); }

            showTextPopup("個人借閱日誌查核中心", sb.toString());
        });

        // 全館歷史日誌 (修復亂碼)
        btnHistory.addActionListener(e -> {
            int row = bookTable.getSelectedRow();
            if (row == -1) { JOptionPane.showMessageDialog(this, "請先選取一本書籍。"); return; }
            int bookId = (int) bookTable.getValueAt(row, 0);
            String bookTitle = (String) bookTable.getValueAt(row, 1);

            StringBuilder sb = new StringBuilder();
            sb.append("📊 《").append(bookTitle).append("》全館借閱歷史日誌：\n");
            sb.append("====================================================\n\n");
            
            String sql = "SELECT r.*, u.name FROM borrow_records r JOIN users u ON r.user_id = u.id WHERE r.book_id = ? ORDER BY r.borrow_date DESC";
            try (Connection conn = DatabaseManager.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setInt(1, bookId);
                ResultSet rs = pstmt.executeQuery();
                while (rs.next()) {
                    sb.append("▸ 借閱人: ").append(rs.getString("name"))
                      .append("\n  借出時間: ").append(formatTimestamp(rs.getTimestamp("borrow_date")))
                      .append("\n  歸還時間: ").append(rs.getTimestamp("return_date") != null ? formatTimestamp(rs.getTimestamp("return_date")) : "❌ 借出中未還").append("\n")
                      .append("----------------------------------------------------\n");
                }
            } catch (SQLException ex) { ex.printStackTrace(); }

            showTextPopup("全館 SQL 借閱追蹤日誌", sb.toString());
        });

        // 登出帳戶系統
        btnLogout.addActionListener(e -> {
            int confirm = JOptionPane.showConfirmDialog(this, "確定要登出當前帳戶並切換使用者嗎？", "登出提示", JOptionPane.YES_NO_OPTION);
            if (confirm == JOptionPane.YES_OPTION) {
                this.dispose(); 
                new LoginFrame(); 
            }
        });

        setVisible(true);
    }

    // 解決時間符號亂碼：將 Timestamp 安全轉換為格式化字串
    private String formatTimestamp(Timestamp ts) {
        if (ts == null) return "無";
        return ts.toLocalDateTime().format(TIME_FORMATTER);
    }

    // 解決字型方塊亂碼：建立強制指定「微軟正黑體」的文字視窗
    private void showTextPopup(String title, String content) {
        JTextArea area = new JTextArea(content, 18, 48);
        area.setEditable(false);
        // 核心修正：強制指定微軟正黑體，防止特定編碼環境下出現正方形或問號亂碼
        area.setFont(new Font("Microsoft JhengHei", Font.PLAIN, 13));
        
        JScrollPane pane = new JScrollPane(area);
        JOptionPane.showMessageDialog(this, pane, title, JOptionPane.INFORMATION_MESSAGE);
    }

    private void refreshTableFromDatabase() {
        tableModel.setRowCount(0);
        String sql = "SELECT * FROM books ORDER BY id ASC";
        try (Connection conn = DatabaseManager.getConnection(); Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                tableModel.addRow(new Object[]{
                    rs.getInt("id"), rs.getString("title"), rs.getString("author"),
                    rs.getString("publisher"), rs.getInt("publish_year"),
                    "AVAILABLE".equals(rs.getString("status")) ? "🟢 在館（可借閱）" : "🔴 借出中（不可借）"
                });
            }
        } catch (SQLException e) { e.printStackTrace(); }
    }

    public static void main(String[] args) {
        try { com.formdev.flatlaf.FlatLightLaf.setup(); } catch (Exception e) {}
        DatabaseManager.initializeDatabase(); 
        SwingUtilities.invokeLater(() -> new LoginFrame());
    }
}