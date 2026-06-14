package com.library;

import javax.swing.*;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;

public class MainApp extends JFrame {
	
    public static void main(String[] args) {
        try { 
            com.formdev.flatlaf.FlatLightLaf.setup(); 
        } catch (Exception e) {}
        DatabaseManager.initializeDatabase(); 
        SwingUtilities.invokeLater(() -> new LoginFrame());
    }

    private JPanel cardsPanel;
    private JScrollPane scrollPane;
    private JComboBox<String> cbSearchType;
    private JTextField txtSearchKeywords;
    private JButton btnSearch;
    
    private int currentUserId;
    private String currentUserName;
    private String currentUserRole;

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private static final Color MORANDI_BG = new Color(240, 244, 248);       
    private static final Color MORANDI_CARD_BG = new Color(230, 237, 245);  
    private static final Color MORANDI_PRIMARY = new Color(90, 115, 142);   
    private static final Color MORANDI_TEXT = new Color(52, 73, 94);        
    private static final Color MORANDI_CHARCOAL = new Color(43, 48, 59);    

    public MainApp(int userId, String name, String role) {
        this.currentUserId = userId;
        this.currentUserName = name;
        this.currentUserRole = role;
        initUI();
    }

    private void initUI() {
        setTitle("圖書館智慧管理系統");
        setSize(1150, 850); 
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        getContentPane().setBackground(MORANDI_BG);
        setLayout(new BorderLayout());

        // ==================== 左側導覽功能面板 ====================
        JPanel sidePanel = new JPanel();
        sidePanel.setLayout(new BoxLayout(sidePanel, BoxLayout.Y_AXIS));
        sidePanel.setBackground(Color.WHITE);
        sidePanel.setPreferredSize(new Dimension(270, 0));
        sidePanel.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, new Color(215, 222, 230)));

        JPanel userCard = new JPanel(new GridLayout(2, 1, 4, 4));
        userCard.setBackground(MORANDI_CARD_BG);
        userCard.setBorder(BorderFactory.createEmptyBorder(18, 20, 18, 20));
        
        JLabel lblName = new JLabel("成員: " + currentUserName);
        lblName.setFont(new Font("Microsoft JhengHei", Font.BOLD, 15));
        lblName.setForeground(MORANDI_TEXT);
        
        JLabel lblRole = new JLabel("權限: " + ("ADMIN".equals(currentUserRole) ? "系統管理員" : ("VIP".equals(currentUserRole) ? "VIP 尊榮成員" : "普通成員")));
        lblRole.setFont(new Font("Microsoft JhengHei", Font.BOLD, 13));
        lblRole.setForeground("ADMIN".equals(currentUserRole) ? new Color(194, 122, 44) : ("VIP".equals(currentUserRole) ? new Color(110, 84, 149) : Color.GRAY));
        userCard.add(lblName); userCard.add(lblRole);

        JButton btnMyRecords = new JButton("個人借閱紀錄");
        JButton btnBookHistory = new JButton("書籍借還歷史"); 
        JButton btnAllHistory = new JButton("全館借還紀錄"); 
        JButton btnStudentSearch = new JButton("個別使用者借還紀錄"); 
        JButton btnUserStatus = new JButton("使用者帳號停權與復權");
        JButton btnAdminAddBook = new JButton("新增圖書上架填報"); 
        JButton btnLogout = new JButton("登出");

        Dimension btnSize = new Dimension(230, 38);
        JButton[] allButtons = {btnMyRecords, btnBookHistory, btnAllHistory, btnStudentSearch, btnUserStatus, btnAdminAddBook, btnLogout};
        for (JButton btn : allButtons) {
            btn.setMaximumSize(btnSize);
            btn.setAlignmentX(Component.CENTER_ALIGNMENT);
            btn.setFont(new Font("Microsoft JhengHei", Font.BOLD, 13));
            btn.setFocusPainted(false);
            btn.putClientProperty("JButton.buttonType", "roundRect");
        }

        btnMyRecords.setBackground(MORANDI_PRIMARY); btnMyRecords.setForeground(Color.WHITE);
        btnBookHistory.setBackground(MORANDI_PRIMARY); btnBookHistory.setForeground(Color.WHITE);
        btnLogout.setBackground(new Color(230, 126, 115)); btnLogout.setForeground(Color.WHITE);
        
        Color adminBtnColor = MORANDI_CHARCOAL;
        btnAllHistory.setBackground(adminBtnColor); btnAllHistory.setForeground(Color.WHITE);
        btnStudentSearch.setBackground(adminBtnColor); btnStudentSearch.setForeground(Color.WHITE);
        btnUserStatus.setBackground(adminBtnColor); btnUserStatus.setForeground(Color.WHITE);
        btnAdminAddBook.setBackground(adminBtnColor); btnAdminAddBook.setForeground(Color.WHITE);

        sidePanel.add(userCard); sidePanel.add(Box.createVerticalStrut(15));
        if (!"ADMIN".equals(this.currentUserRole)) {
            sidePanel.add(btnMyRecords); 
            sidePanel.add(Box.createVerticalStrut(10));
        }
        sidePanel.add(btnBookHistory); sidePanel.add(Box.createVerticalStrut(10));
        
        if ("ADMIN".equals(this.currentUserRole)) {
            JSeparator sep = new JSeparator();
            sep.setMaximumSize(new Dimension(230, 2));
            sidePanel.add(sep); sidePanel.add(Box.createVerticalStrut(15));
            sidePanel.add(btnAllHistory); sidePanel.add(Box.createVerticalStrut(10));
            sidePanel.add(btnStudentSearch); sidePanel.add(Box.createVerticalStrut(10));
            sidePanel.add(btnUserStatus); sidePanel.add(Box.createVerticalStrut(10));
            sidePanel.add(btnAdminAddBook);
        }

        sidePanel.add(Box.createVerticalGlue());
        sidePanel.add(btnLogout); sidePanel.add(Box.createVerticalStrut(25));
        add(sidePanel, BorderLayout.WEST);

        // ==================== 右側上方智慧搜尋面板 ====================
        JPanel rightMainPanel = new JPanel(new BorderLayout());
        rightMainPanel.setBackground(MORANDI_BG);

        JPanel searchBarPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 15));
        searchBarPanel.setBackground(Color.WHITE);
        searchBarPanel.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(220, 225, 230)));

        JLabel lblSearchHint = new JLabel("智慧館藏檢索:");
        lblSearchHint.setFont(new Font("Microsoft JhengHei", Font.BOLD, 14));
        lblSearchHint.setForeground(MORANDI_TEXT);

        cbSearchType = new JComboBox<>(new String[]{"依書名搜尋", "依作者搜尋", "依出版者搜尋"});
        cbSearchType.setFont(new Font("Microsoft JhengHei", Font.PLAIN, 13));
        
        txtSearchKeywords = new JTextField(25);
        txtSearchKeywords.setFont(new Font("Microsoft JhengHei", Font.PLAIN, 14)); 

        btnSearch = new JButton("檢索");
        btnSearch.setFont(new Font("Microsoft JhengHei", Font.BOLD, 13));
        btnSearch.setBackground(MORANDI_PRIMARY);
        btnSearch.setForeground(Color.WHITE);
        btnSearch.putClientProperty("JButton.buttonType", "roundRect");

        searchBarPanel.add(lblSearchHint); searchBarPanel.add(cbSearchType);
        searchBarPanel.add(txtSearchKeywords); searchBarPanel.add(btnSearch);
        rightMainPanel.add(searchBarPanel, BorderLayout.NORTH);

        // ==================== 右側中央圖書卡片面板 ====================
        cardsPanel = new JPanel();
        cardsPanel.setLayout(new GridLayout(0, 1, 0, 15)); 
        cardsPanel.setBackground(MORANDI_BG);
        cardsPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        JPanel scrollWrapper = new JPanel(new BorderLayout());
        scrollWrapper.setBackground(MORANDI_BG);
        scrollWrapper.add(cardsPanel, BorderLayout.NORTH);

        scrollPane = new JScrollPane(scrollWrapper);
        scrollPane.setBorder(null);
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16); 
        rightMainPanel.add(scrollPane, BorderLayout.CENTER);
        
        add(rightMainPanel, BorderLayout.CENTER);

        refreshBookCards("", "");

        btnSearch.addActionListener(e -> {
            String typeStr = (String) cbSearchType.getSelectedItem();
            String keyword = txtSearchKeywords.getText().trim();
            if (keyword.isEmpty()) { refreshBookCards("", ""); return; }
            String column = "title";
            if ("依作者搜尋".equals(typeStr)) column = "author";
            else if ("依出版者搜尋".equals(typeStr)) column = "publisher";
            refreshBookCards(column, keyword);
        });

       
        btnAllHistory.addActionListener(e -> {
            StringBuilder sb = new StringBuilder();
            sb.append("全館借還紀錄：\n==================================================\n\n");
            String sql = "SELECT r.*, u.name as uname, u.student_no, b.title FROM borrow_records r JOIN users u ON r.user_id = u.id JOIN books b ON r.book_id = b.id ORDER BY r.borrow_date DESC";
            try (Connection conn = DatabaseManager.getConnection(); Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
                int count = 1;
                while (rs.next()) {
                    sb.append(count++).append(". [借閱者]: ").append(rs.getString("uname")).append(" (").append(rs.getString("student_no")).append(")\n")
                      .append("   [書籍名稱]: 《").append(rs.getString("title")).append("》\n")
                      .append("   [借出時間]: ").append(formatTimestamp(rs.getTimestamp("borrow_date"))).append("\n")
                      .append("   [應還時間]: ").append(formatTimestamp(rs.getTimestamp("due_date"))).append("\n")
                      .append("   [歸還時間]: ").append(rs.getTimestamp("return_date") != null ? formatTimestamp(rs.getTimestamp("return_date")) : "尚未歸還 (借出中)").append("\n");
                    
                    String dbReturnDateStr = rs.getString("return_date");
                    if (dbReturnDateStr != null) {
                        sb.append("   歸還日期: ").append(dbReturnDateStr).append("\n");
                    }
                    sb.append("--------------------------------------------------\n");
                }
            } catch (Exception ex) { ex.printStackTrace(); }
            showTextPopup("全館借還紀錄", sb.toString());
        });

        // 管理員不能停權管理員
        btnUserStatus.addActionListener(e -> {
            String queryInput = JOptionPane.showInputDialog(this, "請輸入使用者姓名或帳號：");
            if (queryInput == null || queryInput.trim().isEmpty()) return;
            
            ArrayList<String> userList = new ArrayList<>();
            ArrayList<String> userSnoList = new ArrayList<>();
            ArrayList<String> userStatusList = new ArrayList<>();
            ArrayList<String> userRoleList = new ArrayList<>();
            
            // 使用 TRIM 確保搜尋不受空格影響，檢查欄位名稱是否為 name, student_no
            String findSql = "SELECT name, student_no, status, role_level FROM users WHERE name LIKE ? OR student_no LIKE ?";

            try (Connection conn = DatabaseManager.getConnection(); 
                 PreparedStatement pstmt = conn.prepareStatement(findSql)) {
                
                String lk = "%" + queryInput.trim() + "%";
                pstmt.setString(1, lk); 
                pstmt.setString(2, lk);
                
                ResultSet rs = pstmt.executeQuery();
                while (rs.next()) {
                    userList.add("讀者: " + rs.getString("name") + " (" + rs.getString("student_no") + ") [" + rs.getString("status") + "]");
                    userSnoList.add(rs.getString("student_no"));
                    userStatusList.add(rs.getString("status"));
                    userRoleList.add(rs.getString("role_level")); 
                }
            } catch (Exception ex) { 
                ex.printStackTrace(); 
                JOptionPane.showMessageDialog(this, "查詢失敗: " + ex.getMessage());
                return;
            }
            
            if (userList.isEmpty()) {
                JOptionPane.showMessageDialog(this, "找不到任何符合該姓名或帳號的使用者！", "查無結果", JOptionPane.WARNING_MESSAGE);
                return;
            }
            
            String selectedUser = (String) JOptionPane.showInputDialog(this, "請選擇使用者：", "選擇用戶",
                    JOptionPane.QUESTION_MESSAGE, null, userList.toArray(), userList.get(0));
            
            if (selectedUser != null) {
                int index = userList.indexOf(selectedUser);
                String targetSno = userSnoList.get(index);
                String currentStatus = userStatusList.get(index);
                String targetRoleLevel = userRoleList.get(index);
                
             // 不能停權自己
                if (targetSno.equals(this.currentUserId)) { 
                    JOptionPane.showMessageDialog(this, "您不能對自己的帳號進行停權操作！", "限制", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                
                // 以角色等級判斷 (假設 "ADMIN" 或特定的 Level 代表管理員)
                // 請根據你資料庫裡存的值，例如 "ADMIN" 或 "1" 等做調整
                if ("ADMIN".equals(targetRoleLevel)) {
                    JOptionPane.showMessageDialog(this, "無法對管理員帳號進行停權/復權管制！", "控制失敗", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                
                // 狀態變更
                String[] options = {"ACTIVE (啟用)", "SUSPENDED (停權)"};
                int initialChoice = "ACTIVE".equals(currentStatus) ? 0 : 1;
                int choice = JOptionPane.showOptionDialog(this, "目前狀態: " + currentStatus + "\n請選取變更後的狀態:", "狀態控制",
                        0, JOptionPane.QUESTION_MESSAGE, null, options, options[initialChoice]);
                
                if (choice != -1) {
                    String newStatus = (choice == 0) ? "ACTIVE" : "SUSPENDED";
                    try (Connection conn = DatabaseManager.getConnection(); 
                         PreparedStatement up = conn.prepareStatement("UPDATE users SET status = ? WHERE student_no = ?")) {
                        
                        up.setString(1, newStatus); 
                        up.setString(2, targetSno);
                        int rows = up.executeUpdate();
                        
                        if (rows > 0) {
                            JOptionPane.showMessageDialog(this, "狀態調整成功！用戶 [" + targetSno + "] 已變更為 " + newStatus);
                        } else {
                            JOptionPane.showMessageDialog(this, "更新失敗：找不到該帳號紀錄。");
                        }
                    } catch (Exception ex) { 
                        ex.printStackTrace(); 
                        JOptionPane.showMessageDialog(this, "系統錯誤: " + ex.getMessage());
                    }
                }
            }
        });

        btnStudentSearch.addActionListener(e -> {
            String target = JOptionPane.showInputDialog(this, "請輸入要查詢的使用者姓名或帳號：");
            if (target == null || target.trim().isEmpty()) return;
            StringBuilder sb = new StringBuilder("使用者 [" + target + "] 個別借還記錄：\n==================================================\n\n");
            try (Connection conn = DatabaseManager.getConnection(); PreparedStatement pstmt = conn.prepareStatement("SELECT r.*, b.title, u.name, u.student_no FROM borrow_records r JOIN users u ON r.user_id = u.id JOIN books b ON r.book_id = b.id WHERE u.name LIKE ? OR u.student_no LIKE ? ORDER BY r.borrow_date DESC")) {
                String likeKeyword = "%" + target.trim() + "%";
                pstmt.setString(1, likeKeyword); pstmt.setString(2, likeKeyword);
                ResultSet rs = pstmt.executeQuery();
                int count = 1;
                while (rs.next()) {
                    sb.append(count++).append(".  使用者: ").append(rs.getString("name")).append(" (").append(rs.getString("student_no")).append(")\n")
                      .append("    書籍: 《").append(rs.getString("title")).append("》\n")
                      .append("    借出時間: ").append(formatTimestamp(rs.getTimestamp("borrow_date"))).append("\n")
                      .append("    應還時間: ").append(formatTimestamp(rs.getTimestamp("due_date"))).append("\n")
                      .append("    歸還時間: ").append(rs.getTimestamp("return_date") != null ? formatTimestamp(rs.getTimestamp("return_date")) : "尚未歸還 (借出中)").append("\n--------------------------------------------------\n");
                }
            } catch (Exception ex) { ex.printStackTrace(); }
            showTextPopup("使用者個別借還書紀錄查詢結果", sb.toString());
        });

        btnAdminAddBook.addActionListener(e -> {
            JTextField tTitle = new JTextField(); tTitle.setFont(new Font("Microsoft JhengHei", Font.PLAIN, 13));
            JTextField tAuthor = new JTextField(); tAuthor.setFont(new Font("Microsoft JhengHei", Font.PLAIN, 13));
            JTextField tPub = new JTextField(); tPub.setFont(new Font("Microsoft JhengHei", Font.PLAIN, 13));
            JTextField tYear = new JTextField();
            Object[] message = {"書籍標題:", tTitle, "原著作者:", tAuthor, "出版機構:", tPub, "出版年份:", tYear};
            if (JOptionPane.showConfirmDialog(this, message, "新書填報入庫", JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION) {
                try (Connection conn = DatabaseManager.getConnection(); PreparedStatement pstmt = conn.prepareStatement("INSERT INTO books (title, author, publisher, publish_year, status) VALUES (?, ?, ?, ?, 'AVAILABLE')")) {
                    pstmt.setString(1, tTitle.getText().trim()); pstmt.setString(2, tAuthor.getText().trim()); 
                    pstmt.setString(3, tPub.getText().trim()); pstmt.setInt(4, Integer.parseInt(tYear.getText().trim())); 
                    pstmt.executeUpdate(); 
                    refreshBookCards("", "");
                } catch (Exception ex) { ex.printStackTrace(); }
            }
        });

        btnMyRecords.addActionListener(e -> {
            StringBuilder sb = new StringBuilder("個人借還紀錄：\n==================================================\n\n");
            try (Connection conn = DatabaseManager.getConnection(); PreparedStatement pstmt = conn.prepareStatement("SELECT r.*, b.title FROM borrow_records r JOIN books b ON r.book_id = b.id WHERE r.user_id = ? ORDER BY r.borrow_date DESC")) {
                pstmt.setInt(1, currentUserId); ResultSet rs = pstmt.executeQuery();
                int count = 1;
                while (rs.next()) {
                    sb.append(count++).append(".  書籍: 《").append(rs.getString("title")).append("》\n")
                      .append("    借出時間: ").append(formatTimestamp(rs.getTimestamp("borrow_date"))).append("\n")
                      .append("    歸還時間: ").append(rs.getTimestamp("return_date") != null ? formatTimestamp(rs.getTimestamp("return_date")) : "尚未歸還 (借出中)").append("\n--------------------------------------------------\n");
                }
            } catch (Exception ex) { ex.printStackTrace(); }
            showTextPopup("個人紀錄", sb.toString());
        });

        btnBookHistory.addActionListener(e -> {
            String btitle = JOptionPane.showInputDialog(this, "請輸入書籍名稱：");
            if (btitle == null || btitle.trim().isEmpty()) return;
            StringBuilder sb = new StringBuilder("書籍借還歷史：\n==================================================\n\n");
            try (Connection conn = DatabaseManager.getConnection(); PreparedStatement pstmt = conn.prepareStatement("SELECT r.*, u.name, b.title FROM borrow_records r JOIN books b ON r.book_id = b.id JOIN users u ON r.user_id = u.id WHERE b.title LIKE ? ORDER BY r.borrow_date DESC")) {
                pstmt.setString(1, "%" + btitle.trim() + "%"); ResultSet rs = pstmt.executeQuery();
                int count = 1;
                while (rs.next()) {
                    sb.append(count++).append(".  書名: 《").append(rs.getString("title")).append("》\n")
                      .append("    使用者: ").append(rs.getString("name")).append("\n")
                      .append("    借出時間: ").append(formatTimestamp(rs.getTimestamp("borrow_date"))).append("\n")
                      .append("    歸還時間: ").append(rs.getTimestamp("return_date") != null ? formatTimestamp(rs.getTimestamp("return_date")) : "尚未歸還").append("\n--------------------------------------------------\n");
                }
            } catch (Exception ex) { ex.printStackTrace(); }
            showTextPopup("書籍歷史", sb.toString());
        });

        btnLogout.addActionListener(e -> { this.dispose(); new LoginFrame(); });
        
        SwingUtilities.invokeLater(() -> {
            checkOverdueAndReminders();
            checkReservationNotifications(); // 
        });
        refreshBookCards("", ""); // 初始化畫面
        setVisible(true);
    }

    
    private void refreshBookCards(String searchColumn, String keyword) {
        cardsPanel.removeAll(); 
        
        // 先獲取當前使用者未歸還的書籍與是否過期
        ArrayList<Integer> myBorrowedIds = new ArrayList<>();
        ArrayList<Integer> myOverdueIds = new ArrayList<>();
        String checkMySql = "SELECT book_id, due_date FROM borrow_records WHERE user_id = ? AND return_date IS NULL";
        try (Connection conn = DatabaseManager.getConnection(); PreparedStatement pstmt = conn.prepareStatement(checkMySql)) {
            pstmt.setInt(1, currentUserId);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                int bId = rs.getInt("book_id");
                myBorrowedIds.add(bId);
                if (rs.getTimestamp("due_date").toLocalDateTime().isBefore(LocalDateTime.now())) {
                    myOverdueIds.add(bId);
                }
            }
        } catch (Exception e) { e.printStackTrace(); }

        String sql = "SELECT * FROM books";
        boolean hasCondition = !searchColumn.isEmpty() && !keyword.isEmpty();

        if (hasCondition) {
            sql += " WHERE " + searchColumn + " LIKE ?";
        }

        // 2. 嚴謹的排序拼接 (使用 myBorrowedIds)
        if (myBorrowedIds != null && !myBorrowedIds.isEmpty()) {
            // 產生 ID 字串 (例如: 1, 5, 8)
            String ids = myBorrowedIds.stream()
                                      .map(String::valueOf)
                                      .collect(java.util.stream.Collectors.joining(","));
            
            // 組合排序語句 (注意前面要有空格)
            sql += " ORDER BY CASE WHEN id IN (" + ids + ") THEN 1 ELSE 2 END ASC, id ASC";
        } else {
            // 沒有借閱紀錄時，直接按 ID 排序
            sql += " ORDER BY id ASC";
        }
        
        try (Connection conn = DatabaseManager.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            if (!searchColumn.isEmpty() && !keyword.isEmpty()) { pstmt.setString(1, "%" + keyword + "%"); }
            ResultSet rs = pstmt.executeQuery();
            boolean hasData = false;
            
            while (rs.next()) {
                hasData = true;
                int id = rs.getInt("id"); 
                String title = rs.getString("title"); 
                String author = rs.getString("author");
                String publisher = rs.getString("publisher"); 
                int year = rs.getInt("publish_year");
                String rawStatus = rs.getString("status"); 

                // 解析複合型預約狀態 (例如 BORROWED_RES:2)
                boolean isBorrowed = rawStatus.startsWith("BORROWED");
                boolean isAvailable = "AVAILABLE".equals(rawStatus);

                JPanel card = new JPanel(new BorderLayout(15, 10));
                card.setBackground(Color.WHITE);
                card.setBorder(BorderFactory.createCompoundBorder(
                    new LineBorder(new Color(220, 228, 235), 1, true), 
                    BorderFactory.createEmptyBorder(14, 20, 14, 20)
                ));
                card.setPreferredSize(new Dimension(820, 115)); 

                JPanel infoPanel = new JPanel(new GridLayout(3, 1, 2, 2)); 
                infoPanel.setBackground(Color.WHITE);
                
                // 在書目中明確標出個人借閱狀態
                String tagPrefix = "";
                if (myBorrowedIds.contains(id)) {
                    if (myOverdueIds.contains(id)) {
                        tagPrefix = "<font color='#c0392b'>[您的借閱已逾期！]</font> ";
                    } else {
                        tagPrefix = "<font color='#2980b9'>[您正借閱此書]</font> ";
                    }
                }

                JLabel lblTitle = new JLabel("<html><b>" + tagPrefix + "《 " + title + " 》</b></html>");
                lblTitle.setFont(new Font("Microsoft JhengHei", Font.BOLD, 15));
                lblTitle.setForeground(MORANDI_TEXT);
                
                JLabel lblAuthor = new JLabel("作者: " + author + "  |  出版者: " + publisher + " (" + year + ")");
                lblAuthor.setFont(new Font("Microsoft JhengHei", Font.PLAIN, 12));
                lblAuthor.setForeground(Color.GRAY);
                
                JLabel lblStatus = new JLabel("狀態: " + (isAvailable ? "可在館內借閱" : "已被借出外借中"));
                lblStatus.setFont(new Font("Microsoft JhengHei", Font.BOLD, 12));
                lblStatus.setForeground(isAvailable ? new Color(103, 164, 126) : new Color(192, 108, 108));

                infoPanel.add(lblTitle); infoPanel.add(lblAuthor); infoPanel.add(lblStatus);
                card.add(infoPanel, BorderLayout.CENTER);

                JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 18));
                btnPanel.setBackground(Color.WHITE);

                JButton btnAction = new JButton();
                btnAction.setFont(new Font("Microsoft JhengHei", Font.BOLD, 12));
                btnAction.putClientProperty("JButton.buttonType", "roundRect");
                
                boolean isAdmin = "ADMIN".equals(this.currentUserRole);

                if (isAvailable) {
                    if (!isAdmin) {
                        // 一般使用者：顯示借閱按鈕
                        btnAction.setText("辦理借閱");
                        btnAction.setBackground(MORANDI_PRIMARY); 
                        btnAction.setForeground(Color.WHITE);
                        btnAction.addActionListener(e -> handleBorrow(id));
                        btnPanel.add(btnAction);
                    }
                } else {
                    // 書籍外借中
                    if (myBorrowedIds.contains(id)) {
                        if (!isAdmin) {
                            // 一般使用者：顯示歸還按鈕
                            btnAction.setText("辦理歸還");
                            btnAction.setBackground(new Color(238, 240, 245)); 
                            btnAction.setForeground(MORANDI_TEXT);
                            btnAction.addActionListener(e -> handleReturn(id, rawStatus));
                            btnPanel.add(btnAction);
                        }
                    } else {
                        // 書籍被他人借走：僅限一般使用者顯示預約按鈕
                        if (!isAdmin) {
                            JButton btnReserve = new JButton("線上預約書籍");
                            btnReserve.setFont(new Font("Microsoft JhengHei", Font.BOLD, 12));
                            btnReserve.setBackground(new Color(110, 137, 166)); 
                            btnReserve.setForeground(Color.WHITE);
                            btnReserve.putClientProperty("JButton.buttonType", "roundRect");
                            btnReserve.addActionListener(e -> handleReserve(id, rawStatus));
                            btnPanel.add(btnReserve);
                        }
                    }
                }

                // 管理員下架管制 - 若被借出（isBorrowed）則禁用按鈕並提示
                if ("ADMIN".equals(this.currentUserRole)) {
                    JButton btnDelete = new JButton("下架書籍");
                    btnDelete.setFont(new Font("Microsoft JhengHei", Font.BOLD, 12));
                    btnDelete.setBackground(new Color(230, 126, 115)); 
                    btnDelete.setForeground(Color.WHITE);
                    btnDelete.putClientProperty("JButton.buttonType", "roundRect");
                    
                    // 若外借中，鎖定按鈕
                    if (isBorrowed) {
                        btnDelete.setEnabled(false);
                        btnDelete.setToolTipText("書籍外借中，暫時無法下架");
                    }
                    
                    btnDelete.addActionListener(e -> {
                        if (JOptionPane.showConfirmDialog(this, "確定要下架《" + title + "》嗎？", "確認下架", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
                            try {
                                // 執行刪除：即便不是外借中的書，也應清理其歷史借閱紀錄，保持資料庫整潔
                                try (Connection conn1 = DatabaseManager.getConnection();
                                     PreparedStatement p1 = conn1.prepareStatement("DELETE FROM borrow_records WHERE book_id = ?")) {
                                    p1.setInt(1, id);
                                    p1.executeUpdate();
                                }
                                
                                try (Connection conn2 = DatabaseManager.getConnection();
                                     PreparedStatement p2 = conn2.prepareStatement("DELETE FROM books WHERE id = ?")) {
                                    p2.setInt(1, id);
                                    p2.executeUpdate();
                                }
                                
                                JOptionPane.showMessageDialog(this, "書籍已下架，相關歷史紀錄已清理。");
                                refreshBookCards("", "");
                                
                            } catch (Exception ex) {
                                ex.printStackTrace();
                                JOptionPane.showMessageDialog(this, "下架失敗: " + ex.getMessage());
                            }
                        }
                    });
                    btnPanel.add(btnDelete);
                }
                    
                card.add(btnPanel, BorderLayout.EAST);
                cardsPanel.add(card); 
            }
            
            if(!hasData) {
                JLabel lblNoData = new JLabel("查無任何符合條件的館藏書籍。", JLabel.CENTER);
                lblNoData.setFont(new Font("Microsoft JhengHei", Font.BOLD, 16));
                lblNoData.setForeground(Color.GRAY);
                cardsPanel.add(lblNoData);
            }
            
        } catch (SQLException e) { e.printStackTrace(); }
        
        cardsPanel.revalidate();
        cardsPanel.repaint();
    }

    private void handleBorrow(int bookId) {
    	String checkSql = "SELECT COUNT(*) FROM borrow_records WHERE user_id = ? AND return_date IS NULL AND due_date < NOW()";
        try (Connection conn = DatabaseManager.getConnection(); 
             PreparedStatement pstmt = conn.prepareStatement(checkSql)) {
            pstmt.setInt(1, currentUserId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next() && rs.getInt(1) > 0) {
                JOptionPane.showMessageDialog(this, "借閱失敗：您有書籍已逾期且未繳清罰款，請先完成歸還與罰款結清！");
                return; // 嚴格阻擋借閱
            }
        } catch (Exception ex) { ex.printStackTrace(); return; }
    	
    	String countSql = "SELECT COUNT(*) FROM borrow_records WHERE user_id = ? AND return_date IS NULL";
        try (Connection conn = DatabaseManager.getConnection(); 
             PreparedStatement pstmt = conn.prepareStatement(countSql)) {
            
            pstmt.setInt(1, currentUserId); // 假設 currentUserId 是字串，若為 int 請改用 setInt
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                int currentCount = rs.getInt(1);
                if (currentCount >= 5) { // 設定上限為 5 本，可自行調整
                    JOptionPane.showMessageDialog(this, "借閱失敗：您已達同時借閱上限 (5本)！");
                    return; // 中斷後續借閱流程
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "查詢借閱狀態失敗: " + ex.getMessage());
            return;
        }
    	
    	String[] daysOptions;
    	int[] daysMapping;
    	

    	if ("VIP".equals(this.currentUserRole)) {
    	    // VIP 可以借 1, 3, 7, 14, 28 天
    	    daysOptions = new String[]{"1 天", "3 天", "7 天", "14 天", "28 天"};
    	    daysMapping = new int[]{1, 3, 7, 14, 28};
    	} else {
    	    // 一般用戶 (USER) 只能借 1, 3, 7, 14 天
    	    daysOptions = new String[]{"1 天", "3 天", "7 天", "14 天"};
    	    daysMapping = new int[]{1, 3, 7, 14};
    	}

    	// 2. 顯示選擇對話框
    	int choice = JOptionPane.showOptionDialog(this, "請選擇借閱期限：", "期限選擇", 
    	             JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE, null, daysOptions, daysOptions[0]);

    	if (choice == -1) return; // 使用者按了取消

    	// 3. 根據選擇取得天數 (不再需要複雜的 ?: 巢狀判斷)
    	int days = daysMapping[choice];
    	
        try (Connection conn = DatabaseManager.getConnection()) {
            conn.setAutoCommit(false);
            try {
                try (PreparedStatement p1 = conn.prepareStatement("UPDATE books SET status = 'BORROWED' WHERE id = ?")) { 
                    p1.setInt(1, bookId); p1.executeUpdate(); 
                }
                try (PreparedStatement p2 = conn.prepareStatement("INSERT INTO borrow_records (user_id, book_id, borrow_date, due_date, borrow_days, created_at) VALUES (?, ?, ?, ?, ?, ?)")) {
                    p2.setInt(1, currentUserId); p2.setInt(2, bookId); p2.setTimestamp(3, Timestamp.valueOf(LocalDateTime.now()));
                    p2.setTimestamp(4, Timestamp.valueOf(LocalDateTime.now().plusDays(days))); p2.setInt(5, days); p2.setTimestamp(6, Timestamp.valueOf(LocalDateTime.now()));
                    p2.executeUpdate();
                }
                conn.commit();
                JOptionPane.showMessageDialog(this, "借閱成功！");
                // 成功才刷新
                refreshBookCards("", "");
            } catch (Exception ex) { 
                conn.rollback(); 
                throw ex; // 拋出讓外層捕捉
            }
        } catch (Exception ex) { 
            ex.printStackTrace(); 
            JOptionPane.showMessageDialog(this, "借閱失敗: " + ex.getMessage());
        }
    }

    private void handleReturn(int bookId, String currentRawStatus) {
        // 使用 try-with-resources 確保連線自動關閉
        try (Connection conn = DatabaseManager.getConnection()) {
            conn.setAutoCommit(false); // 確保交易完整性
            try {
                // 1. 處理書籍狀態：如果原本有預約標記，則轉移，否則設為 AVAILABLE
                String nextStatus = "AVAILABLE";
                if (currentRawStatus != null && currentRawStatus.startsWith("BORROWED_RES:")) {
                    String resUserId = currentRawStatus.substring(13);
                    nextStatus = "AVAILABLE_RES:" + resUserId;
                }

                // 更新書籍狀態
                try (PreparedStatement p1 = conn.prepareStatement("UPDATE books SET status = ? WHERE id = ?")) { 
                    p1.setString(1, nextStatus); 
                    p1.setInt(2, bookId); 
                    p1.executeUpdate(); 
                }

                // 2. 更新 borrow_records：標記歸還時間 (直接存入時間字串)
                String nowStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                
                try (PreparedStatement p2 = conn.prepareStatement(
                        "UPDATE borrow_records SET return_date = ? WHERE book_id = ? AND user_id = ? AND return_date IS NULL")) {
                    p2.setString(1, nowStr);
                    p2.setInt(2, bookId);
                    p2.setInt(3, currentUserId);
                    
                    int rows = p2.executeUpdate();
                    if (rows == 0) {
                        throw new SQLException("找不到該書籍的未歸還記錄，請確認借閱狀態。");
                    }
                }

                conn.commit(); // 確認所有操作
                refreshBookCards("", ""); // 【強制同步】刷新介面
                JOptionPane.showMessageDialog(this, "書籍已成功歸還！");
                
            } catch (Exception ex) { 
                conn.rollback(); // 若發生錯誤則還原
                throw ex; 
            }
        } catch (Exception ex) { 
            ex.printStackTrace(); 
            JOptionPane.showMessageDialog(this, "歸還失敗: " + ex.getMessage(), "錯誤", JOptionPane.ERROR_MESSAGE);
        }
    }

    // 將預約者 ID 寫入 status 欄位中 (格式為 BORROWED_RES:用戶ID)
    private void handleReserve(int bookId, String currentRawStatus) {
        if (currentRawStatus.contains("RES:")) {
            JOptionPane.showMessageDialog(this, "此書籍已被其他讀者預約，請等候空位！", "提示", JOptionPane.WARNING_MESSAGE);
            return;
        }
        try (Connection conn = DatabaseManager.getConnection()) {
            String newStatus = "BORROWED_RES:" + currentUserId;
            try (PreparedStatement pstmt = conn.prepareStatement("UPDATE books SET status = ? WHERE id = ?")) {
                pstmt.setString(1, newStatus); pstmt.setInt(2, bookId);
                pstmt.executeUpdate();
                JOptionPane.showMessageDialog(this, "預約成功！當書籍被歸還後，下次登入您會收到通知！");
                refreshBookCards("", "");
            }
        } catch (Exception ex) { ex.printStackTrace(); }
    }

    // 登入時掃描有無符合當前使用者的 AVAILABLE_RES 預約到貨標記
    private void checkReservationNotifications() {
        String targetTag = "AVAILABLE_RES:" + currentUserId;
        String querySql = "SELECT id, title FROM books WHERE status = ?";
        try (Connection conn = DatabaseManager.getConnection(); PreparedStatement pstmt = conn.prepareStatement(querySql)) {
            pstmt.setString(1, targetTag);
            ResultSet rs = pstmt.executeQuery();
            StringBuilder sb = new StringBuilder("【圖書館預約書籍到書通知】\n-------------------------------\n");
            boolean hasNotice = false;
            ArrayList<Integer> clearList = new ArrayList<>();
            
            while (rs.next()) {
                hasNotice = true;
                sb.append("您預約的書籍《").append(rs.getString("title")).append("》已經歸還入庫，現在可以辦理借閱！\n");
                clearList.add(rs.getInt("id"));
            }
            
            if (hasNotice) {
                JOptionPane.showMessageDialog(this, sb.toString(), "預約書籍到書提醒", JOptionPane.INFORMATION_MESSAGE);
                // 提醒後將狀態恢復成常規的 AVAILABLE
                for (int bId : clearList) {
                    try (PreparedStatement up = conn.prepareStatement("UPDATE books SET status = 'AVAILABLE' WHERE id = ?")) {
                        up.setInt(1, bId); up.executeUpdate();
                    }
                }
                refreshBookCards("", "");
            }
        } catch (Exception ex) { ex.printStackTrace(); }
    }

    private void checkOverdueAndReminders() {
        String sql = "SELECT r.*, b.title FROM borrow_records r JOIN books b ON r.book_id = b.id WHERE r.user_id = ? AND r.return_date IS NULL";
        try (Connection conn = DatabaseManager.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, currentUserId); // 若為字串則用 setString
            ResultSet rs = pstmt.executeQuery();
            StringBuilder msg = new StringBuilder("到期提醒\n-------------------------------\n");
            boolean trigger = false;
            int totalFine = 0; // 累積罰款

            while (rs.next()) {
                LocalDateTime dueDate = rs.getTimestamp("due_date").toLocalDateTime();
                long left = ChronoUnit.DAYS.between(LocalDateTime.now(), dueDate);
                
                if (dueDate.isBefore(LocalDateTime.now())) {
                    trigger = true;
                    long overdueDays = Math.abs(left);
                    int fine = (int) (overdueDays * 50); 
                    totalFine += fine;
                    msg.append("【已逾期】").append(overdueDays).append(" 天 (罰款 $").append(fine)
                       .append(")：《").append(rs.getString("title")).append("》\n");
                } else if (left <= 3) {
                    trigger = true;
                    msg.append("【剩餘 ").append(left).append(" 天】：《").append(rs.getString("title")).append("》\n");
                }
            }
            if (trigger) {
                msg.append("\n總計待繳罰款：$").append(totalFine);
                JOptionPane.showMessageDialog(this, msg.toString(), "提醒", JOptionPane.WARNING_MESSAGE);
            }
        } catch (Exception ex) { ex.printStackTrace(); }
    }

    private String formatTimestamp(Timestamp ts) { return ts == null ? "無" : ts.toLocalDateTime().format(TIME_FORMATTER); }
    private void showTextPopup(String title, String content) {
        JTextArea area = new JTextArea(content, 22, 60); area.setEditable(false);
        area.setFont(new Font("Microsoft JhengHei", Font.PLAIN, 13));
        JOptionPane.showMessageDialog(this, new JScrollPane(area), title, JOptionPane.INFORMATION_MESSAGE);
    }
}
