package com.library;

import javax.swing.*;
import java.awt.*;
import java.sql.*;

public class LoginFrame extends JFrame {
    private JTextField txtUser = new JTextField();
    private JPasswordField txtPwd = new JPasswordField();

    public LoginFrame() {
        setTitle("圖書館管理系統 - 帳戶認證");
        setSize(400, 310);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setResizable(false);

        // 主面板：白色網頁簡約風格
        JPanel mainPanel = new JPanel(new BorderLayout(10, 15));
        mainPanel.setBackground(Color.WHITE);
        mainPanel.setBorder(BorderFactory.createEmptyBorder(30, 35, 30, 35));

        JLabel lblTitle = new JLabel("LIBRARY PORTAL", JLabel.CENTER);
        lblTitle.setFont(new Font("Microsoft JhengHei", Font.BOLD, 22));
        lblTitle.setForeground(new Color(40, 44, 52)); // 質感深灰
        mainPanel.add(lblTitle, BorderLayout.NORTH);

        JPanel formPanel = new JPanel(new GridLayout(4, 1, 5, 5));
        formPanel.setBackground(Color.WHITE);
        
        JLabel lblU = new JLabel("使用者帳號 / 學號");
        lblU.setFont(new Font("Microsoft JhengHei", Font.BOLD, 12));
        lblU.setForeground(Color.GRAY);
        formPanel.add(lblU);
        
        txtUser.setPreferredSize(new Dimension(0, 32));
        txtUser.putClientProperty("JComponent.roundRect", true); // 平滑圓角框
        formPanel.add(txtUser);
        
        JLabel lblP = new JLabel("認證密碼");
        lblP.setFont(new Font("Microsoft JhengHei", Font.BOLD, 12));
        lblP.setForeground(Color.GRAY);
        formPanel.add(lblP);
        
        txtPwd.putClientProperty("JComponent.roundRect", true);
        formPanel.add(txtPwd);
        mainPanel.add(formPanel, BorderLayout.CENTER);

        JButton btnLogin = new JButton("登入");
        btnLogin.setFont(new Font("Microsoft JhengHei", Font.BOLD, 13));
        btnLogin.setPreferredSize(new Dimension(0, 40));
        btnLogin.setBackground(new Color(10, 108, 255)); // 科技亮藍
        btnLogin.setForeground(Color.WHITE);
        btnLogin.putClientProperty("JButton.buttonType", "roundRect"); // 按鈕極致圓角

        btnLogin.addActionListener(e -> {
            String sno = txtUser.getText().trim();
            String pwd = new String(txtPwd.getPassword());

            String sql = "SELECT * FROM users WHERE student_no = ? AND password = ?";
            try (Connection conn = DatabaseManager.getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(sql)) {
                
                pstmt.setString(1, sno);
                pstmt.setString(2, pwd);
                ResultSet rs = pstmt.executeQuery();

                if (rs.next()) {
                    if ("SUSPENDED".equals(rs.getString("status"))) {
                        JOptionPane.showMessageDialog(this, "該帳戶已被系統違規停權！", "拒絕存取", JOptionPane.WARNING_MESSAGE);
                        return;
                    }
                    int dbUserId = rs.getInt("id");
                    String dbUserName = rs.getString("name");
                    String dbRoleLevel = rs.getString("role_level");

                    dispose(); 
                    new MainApp(dbUserId, dbUserName, dbRoleLevel); // 開啟系統主艙
                } else {
                    JOptionPane.showMessageDialog(this, "帳號或密碼錯誤，請重新確認。", "驗證失敗", JOptionPane.ERROR_MESSAGE);
                }
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
        });

        mainPanel.add(btnLogin, BorderLayout.SOUTH);
        add(mainPanel);
        setVisible(true);
    }
}