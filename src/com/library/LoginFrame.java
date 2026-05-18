package com.library;

import javax.swing.*;
import java.awt.*;
import java.sql.*;

public class LoginFrame extends JFrame {
    private JTextField txtUser = new JTextField();
    private JPasswordField txtPwd = new JPasswordField();

    public LoginFrame() {
        setTitle("圖書館管理系統 - 安全驗證");
        setSize(420, 290);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setResizable(false);

        JPanel mainPanel = new JPanel(new BorderLayout(10, 15));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(25, 30, 25, 30));

        JLabel lblTitle = new JLabel("LIBRARY LOGIN", JLabel.CENTER);
        lblTitle.setFont(new Font("Microsoft JhengHei", Font.BOLD, 24));
        lblTitle.setForeground(new Color(30, 144, 255));
        mainPanel.add(lblTitle, BorderLayout.NORTH);

        JPanel formPanel = new JPanel(new GridLayout(4, 1, 5, 5));
        formPanel.add(new JLabel("請輸入學號帳號 (例: A12345678) :"));
        txtUser.setPreferredSize(new Dimension(0, 30));
        formPanel.add(txtUser);
        formPanel.add(new JLabel("輸入認證密碼 :"));
        formPanel.add(txtPwd);
        mainPanel.add(formPanel, BorderLayout.CENTER);

        JButton btnLogin = new JButton("資料庫安全驗證");
        btnLogin.setFont(new Font("Microsoft JhengHei", Font.BOLD, 14));
        btnLogin.setPreferredSize(new Dimension(0, 38));
        btnLogin.setBackground(new Color(30, 144, 255));
        btnLogin.setForeground(Color.WHITE);
        btnLogin.putClientProperty("JButton.buttonType", "roundRect");

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
                        JOptionPane.showMessageDialog(this, "該帳戶已被系統違規停權，拒絕登入！", "權限遭拒", JOptionPane.WARNING_MESSAGE);
                        return;
                    }
                    int dbUserId = rs.getInt("id");
                    String dbUserName = rs.getString("name");
                    String dbRoleLevel = rs.getString("role_level");

                    dispose(); 
                    new MainApp(dbUserId, dbUserName, dbRoleLevel); 
                } else {
                    JOptionPane.showMessageDialog(this, "學號或密碼錯誤，請重新輸入。", "驗證失敗", JOptionPane.ERROR_MESSAGE);
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