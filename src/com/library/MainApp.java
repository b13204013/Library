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

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public MainApp(int userId, String name, String role) {
        this.currentUserId = userId;
        this.currentUserName = name;
        this.currentUserRole = role;
        initUI();
        
        // 使用者逾期未還智能提醒機制
        // 登入成功後，在背景自動向 SQL 進行查核
        SwingUtilities.invokeLater(() -> checkOverdueReminder());
    }

    private void initUI() {
        setTitle("圖書館智慧管理系統 | 當前核心層: " + currentUserRole + " (" + currentUserName + ")");
        setSize(1120, 680);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        // ==========================================
        // 1. 還原 Figma 
        // ==========================================
        JPanel sidePanel = new JPanel();
        sidePanel.setLayout(new BoxLayout(sidePanel, BoxLayout.Y_AXIS));
        sidePanel.setBackground(new Color(248, 249, 250)); 
        sidePanel.setPreferredSize(new Dimension(250, 0));
        sidePanel.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, new Color(230, 235, 240)));

        // 使用者個人狀態
        JPanel userCard = new JPanel(new GridLayout(2, 1, 5, 5));
        userCard.setBackground(new Color(235, 243, 255)); // 科技輕量藍
        userCard.setBorder(BorderFactory.createEmptyBorder(15, 20, 15, 20));
        JLabel lblName = new JLabel("成員: " + currentUserName);
        lblName.setFont(new Font("Microsoft JhengHei", Font.BOLD, 15));
        JLabel lblRole = new JLabel("身份權限: " + (("B13204013".equals(currentUserRole)||"B13204043".equals(currentUserRole)||"R13945041".equals(currentUserRole)) ? "系統最高管理員" : "一般成員"));
        lblRole.setFont(new Font("Microsoft JhengHei", Font.PLAIN, 12));
        userCard.add(lblName); userCard.add(lblRole);

        // 基礎使用者功能按鈕
        JButton btnBorrow = new JButton("申請線上借閱");
        JButton btnReturn = new JButton("辦理圖書歸還");
        JButton btnMyRecords = new JButton("我的個人借閱紀錄");
        JButton btnHistory = new JButton("書籍歷史紀錄");
        
        
        JButton btnAdminAddBook = new JButton("管理者：新書入庫填報");
        JButton btnAdminStats = new JButton("管理者：全館營運數據");

        JButton btnLogout = new JButton("登出");

        
        Dimension btnSize = new Dimension(220, 38);
        JButton[] allButtons = {btnBorrow, btnReturn, btnMyRecords, btnHistory, btnAdminAddBook, btnAdminStats, btnLogout};
        for (JButton btn : allButtons) {
            btn.setMaximumSize(btnSize);
            btn.setAlignmentX(Component.CENTER_ALIGNMENT);
            btn.setFont(new Font("Microsoft JhengHei", Font.BOLD, 13));
            btn.putClientProperty("JButton.buttonType", "roundRect"); 
        }

       
        btnLogout.setBackground(new Color(255, 90, 95)); 
        btnLogout.setForeground(Color.WHITE);
        btnAdminAddBook.setBackground(new Color(50, 50, 50)); 
        btnAdminAddBook.setForeground(Color.WHITE);
        btnAdminStats.setBackground(new Color(50, 50, 50));
        btnAdminStats.setForeground(Color.WHITE);

        // 動態權限排版組裝
        sidePanel.add(userCard); sidePanel.add(Box.createVerticalStrut(20));
        sidePanel.add(btnBorrow); sidePanel.add(Box.createVerticalStrut(10));
        sidePanel.add(btnReturn); sidePanel.add(Box.createVerticalStrut(10));
        sidePanel.add(btnMyRecords); sidePanel.add(Box.createVerticalStrut(10));
        sidePanel.add(btnHistory); sidePanel.add(Box.createVerticalStrut(15));
        
        // 
        if ("B13204013".equals(currentUserRole)||"B13204043".equals(currentUserRole)||"R13945041".equals(currentUserRole)) {
            JSeparator sep = new JSeparator();
            sep.setMaximumSize(new Dimension(220, 2));
            sidePanel.add(sep); sidePanel.add(Box.createVerticalStrut(15));
            sidePanel.add(btnAdminAddBook); sidePanel.add(Box.createVerticalStrut(10));
            sidePanel.add(btnAdminStats);
        }

        sidePanel.add(Box.createVerticalGlue()); 
        sidePanel.add(btnLogout); sidePanel.add(Box.createVerticalStrut(20));
        add(sidePanel, BorderLayout.WEST);

        // ==========================================
        // 右側數據表格
        // ==========================================
        String[] columns = {"書籍 SQL_ID", "圖書題名", "主要作者", "出版商", "出版年", "即時庫存狀態"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int r, int c) { return false; }
        };
        bookTable = new JTable(tableModel);
        bookTable.setRowHeight(32);
        bookTable.setShowVerticalLines(false); 
        bookTable.setFont(new Font("Microsoft JhengHei", Font.PLAIN, 13));
        bookTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        
        refreshTableFromDatabase();

        JScrollPane scrollPane = new JScrollPane(bookTable);
        scrollPane.setBorder(BorderFactory.createEmptyBorder(15, 20, 15, 20));
        scrollPane.getViewport().setBackground(Color.WHITE);
        add(scrollPane, BorderLayout.CENTER);

        // ==========================================
        // 3. 事件控制器邏輯 (與後端實體資料庫異動連動)
        // ==========================================

        // 申請借閱
        btnBorrow.addActionListener(e -> {
            int row = bookTable.getSelectedRow();
            if (row == -1) { JOptionPane.showMessageDialog(this, "請先在表格中選取一本書籍。"); return; }
            int bookId = (int) bookTable.getValueAt(row, 0);
            String status = (String) bookTable.getValueAt(row, 5);

            if (status.contains("借出中")) {
                JOptionPane.showMessageDialog(this, "⚠️ 該書目前在庫狀態為借出中，無法重複申辦！", "借閱中止", JOptionPane.WARNING_MESSAGE);
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
                    JOptionPane.showMessageDialog(this, "借閱登記成功！");
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
                JOptionPane.showMessageDialog(this, "書籍安全存放於館內，毋須歸還。");
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
                    JOptionPane.showMessageDialog(this, "還書成功！");
                } catch (Exception ex) { conn.rollback(); throw ex; }
            } catch (Exception ex) { ex.printStackTrace(); }
        });

        // 個人借閱紀錄
        btnMyRecords.addActionListener(e -> {
            StringBuilder sb = new StringBuilder();
            sb.append("👤 ").append(currentUserName).append(" 的個人借還書歷史紀錄：\n");
            sb.append("====================================================\n\n");

            String sql = "SELECT r.*, b.title FROM borrow_records r JOIN books b ON r.book_id = b.id WHERE r.user_id = ? ORDER BY r.borrow_date DESC";
            try (Connection conn = DatabaseManager.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setInt(1, currentUserId);
                ResultSet rs = pstmt.executeQuery();
                boolean hasData = false;
                while (rs.next()) {
                    hasData = true;
                    Timestamp dDate = rs.getTimestamp("due_date");
                    Timestamp rDate = rs.getTimestamp("return_date");
                    
                    // 智能標記：如果未歸還且超期，加上特殊警告符號
                    String alertMark = "";
                    if (rDate == null && dDate != null && dDate.before(new java.util.Date())) {
                        alertMark = " 🔴 [已逾期！請儘速歸還]";
                    }

                    sb.append("書名: 《").append(rs.getString("title")).append("》\n")
                      .append("		借出時間: ").append(formatTimestamp(rs.getTimestamp("borrow_date"))).append("\n")
                      .append("		應還日期: ").append(formatTimestamp(dDate)).append("\n")
                      .append("		歸還狀態: ").append(rDate != null ? "已於 [" + formatTimestamp(rDate) + "] 歸還上架" : "❌ 仍在借閱中" + alertMark).append("\n")
                      .append("----------------------------------------------------------------------\n");
                }
                if (!hasData) sb.append("（目前查無借閱紀錄）");
            } catch (SQLException ex) { ex.printStackTrace(); }
            showTextPopup("個人借閱紀錄", sb.toString());
        });

        // [功能四] 全館歷史日誌聯查
        btnHistory.addActionListener(e -> {
            int row = bookTable.getSelectedRow();
            if (row == -1) { JOptionPane.showMessageDialog(this, "請先選擇一本書籍追蹤日誌。"); return; }
            int bookId = (int) bookTable.getValueAt(row, 0);
            String bookTitle = (String) bookTable.getValueAt(row, 1);

            StringBuilder sb = new StringBuilder();
            sb.append("《").append(bookTitle).append("》全館書籍借閱歷史紀錄：\n");
            sb.append("====================================================\n\n");
            
            String sql = "SELECT r.*, u.name FROM borrow_records r JOIN users u ON r.user_id = u.id WHERE r.book_id = ? ORDER BY r.borrow_date DESC";
            try (Connection conn = DatabaseManager.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setInt(1, bookId);
                ResultSet rs = pstmt.executeQuery();
                while (rs.next()) {
                    sb.append("借閱者: ").append(rs.getString("name"))
                      .append("\n	借出時間: ").append(formatTimestamp(rs.getTimestamp("borrow_date")))
                      .append("\n	歸還狀況: ").append(rs.getTimestamp("return_date") != null ? formatTimestamp(rs.getTimestamp("return_date")) : "❌ 借出中（未歸還）").append("\n")
                      .append("----------------------------------------------------\n");
                }
            } catch (SQLException ex) { ex.printStackTrace(); }
            showTextPopup("全館圖書流向日誌", sb.toString());
        });

        // 【🔥 需求二實作：管理者之新書入庫填報功能】
        btnAdminAddBook.addActionListener(e -> {
            JTextField tTitle = new JTextField();
            JTextField tAuthor = new JTextField();
            JTextField tPublisher = new JTextField();
            JTextField tYear = new JTextField();
            
            // 打造圓滑高質感輸入面板
            tTitle.putClientProperty("JComponent.roundRect", true);
            tAuthor.putClientProperty("JComponent.roundRect", true);
            tPublisher.putClientProperty("JComponent.roundRect", true);
            tYear.putClientProperty("JComponent.roundRect", true);

            Object[] message = {
                "圖書標題 / 題名:", tTitle,
                "主要作者:", tAuthor,
                "出版商名稱:", tPublisher,
                "出版年份 (數字):", tYear
            };

            int option = JOptionPane.showConfirmDialog(this, message, "最高管理層：新書採購實體寫入", JOptionPane.OK_CANCEL_OPTION);
            if (option == JOptionPane.OK_OPTION) {
                String title = tTitle.getText().trim();
                String author = tAuthor.getText().trim();
                String publisher = tPublisher.getText().trim();
                String yearStr = tYear.getText().trim();

                if (title.isEmpty() || author.isEmpty() || publisher.isEmpty() || yearStr.isEmpty()) {
                    JOptionPane.showMessageDialog(this, "❌ 欄位不可留白，採購中止！", "錯誤提示", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                try {
                    int year = Integer.parseInt(yearStr);
                    String insertBookSql = "INSERT INTO books (title, author, publisher, publish_year, status) VALUES (?, ?, ?, ?, 'AVAILABLE')";
                    try (Connection conn = DatabaseManager.getConnection();
                         PreparedStatement pstmt = conn.prepareStatement(insertBookSql)) {
                        pstmt.setString(1, title);
                        pstmt.setString(2, author);
                        pstmt.setString(3, publisher);
                        pstmt.setInt(4, year);
                        pstmt.executeUpdate();
                        
                        refreshTableFromDatabase(); // 核心同步：UI 瞬間多出最新一列
                        JOptionPane.showMessageDialog(this, "成功！新書已指派自增主鍵，永久保存至資料庫中。");
                    }
                } catch (NumberFormatException ex) {
                    JOptionPane.showMessageDialog(this, "❌ 出版年份必須為純數字！", "型態錯誤", JOptionPane.ERROR_MESSAGE);
                } catch (SQLException ex) { ex.printStackTrace(); }
            }
        });

        // 【🔥 需求二實作：管理者之全館營運智慧大數據統計】
        btnAdminStats.addActionListener(e -> {
            String sTotalBooks = "SELECT COUNT(*) FROM books";
            String sBorrowedBooks = "SELECT COUNT(*) FROM books WHERE status = 'BORROWED'";
            String sTotalRecords = "SELECT COUNT(*) FROM borrow_records";
            String sOverdueCount = "SELECT COUNT(*) FROM borrow_records WHERE return_date IS NULL AND due_date < CURRENT_TIMESTAMP()";

            try (Connection conn = DatabaseManager.getConnection(); Statement stmt = conn.createStatement()) {
                int totalBooks = 0, borrowedBooks = 0, totalRecords = 0, overdueCount = 0;
                
                ResultSet r1 = stmt.executeQuery(sTotalBooks); if (r1.next()) totalBooks = r1.getInt(1);
                ResultSet r2 = stmt.executeQuery(sBorrowedBooks); if (r2.next()) borrowedBooks = r2.getInt(1);
                ResultSet r3 = stmt.executeQuery(sTotalRecords); if (r3.next()) totalRecords = r3.getInt(1);
                ResultSet r4 = stmt.executeQuery(sOverdueCount); if (r4.next()) overdueCount = r4.getInt(1);

                String statsContent = "圖書館全館實時營運大數據報告：\n" +
                        "=========================================\n\n" +
                        "		館藏實體書籍總數 : " + totalBooks + " 冊\n" +
                        "		當前全館借出在外 : " + borrowedBooks + " 冊\n" +
                        "		歷史借還日誌總計 : " + totalRecords + " 筆軌跡\n" +
                        "		全館逾期未歸還數 : " + overdueCount + " 筆 (系統已執行催還程序)\n\n" +
                        "=========================================\n" +
                        "核心儲存引擎: H2 Database Engine\n" +
                        "狀態更新時間: " + LocalDateTime.now().format(TIME_FORMATTER);

                showTextPopup("全館數據決策中心", statsContent);
            } catch (SQLException ex) { ex.printStackTrace(); }
        });

        // 安全登出
        btnLogout.addActionListener(e -> {
            int confirm = JOptionPane.showConfirmDialog(this, "確定要登出，切換其他使用者身分嗎？", "切換確認", JOptionPane.YES_NO_OPTION);
            if (confirm == JOptionPane.YES_OPTION) {
                this.dispose(); 
                new LoginFrame(); 
            }
        });

        setVisible(true);
    }

    // 【🔥 需求一邏輯：登入時的智能催還提醒機制】
    private void checkOverdueReminder() {
        // 利用 SQL 語法：找出 return_date 為 NULL 且 due_date（應還時間）小於目前的系統時間
        String checkSql = "SELECT COUNT(*) FROM borrow_records WHERE user_id = ? AND return_date IS NULL AND due_date < CURRENT_TIMESTAMP()";
        try (Connection conn = DatabaseManager.getConnection(); PreparedStatement pstmt = conn.prepareStatement(checkSql)) {
            pstmt.setInt(1, currentUserId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next() && rs.getInt(1) > 0) {
                int count = rs.getInt(1);
                // 震懾人心的高質感警告提示，完全符合指引
                JOptionPane.showMessageDialog(this, 
                    "尊敬的 " + currentUserName + " 您好：\n\n" +
                    "系統偵測到您帳戶內目前有 [ " + count + " 筆 ] 圖書已超過應歸還期限！\n" +
                    "請點擊側邊欄【個人借閱紀錄】查看詳細清單，並請儘速辦理歸還，以免影響您的借閱權限。",
                    "⚠️ 帳戶逾期未歸還催還通知", 
                    JOptionPane.WARNING_MESSAGE);
            }
        } catch (SQLException ex) { ex.printStackTrace(); }
    }

    private String formatTimestamp(Timestamp ts) {
        if (ts == null) return "無";
        return ts.toLocalDateTime().format(TIME_FORMATTER);
    }

    private void showTextPopup(String title, String content) {
        JTextArea area = new JTextArea(content, 18, 48);
        area.setEditable(false);
        area.setFont(new Font("Microsoft JhengHei", Font.PLAIN, 13));
        JScrollPane pane = new JScrollPane(area);
        pane.setBorder(BorderFactory.createEmptyBorder());
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