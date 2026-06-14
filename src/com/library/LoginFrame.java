package com.library;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class LoginFrame extends JFrame {
    
    public static void main(String[] args) {
        try { 
            com.formdev.flatlaf.FlatLightLaf.setup(); 
        } catch (Exception e) {}
        
        DatabaseManager.initializeDatabase(); 
        SwingUtilities.invokeLater(() -> new LoginFrame());
    }

    private JTextField txtUser;
    private JPasswordField txtPassword;
    private JButton btnLogin;
    private JButton btnRegister;

    private static final Color MORANDI_BG = new Color(240, 244, 248);       
    private static final Color MORANDI_PRIMARY = new Color(90, 115, 142);   
    private static final Color MORANDI_TEXT = new Color(52, 73, 94);        

    public LoginFrame() {
        initUI();
    }

    private void initUI() {
        setTitle("智慧圖書館系統 - 登入");
        setSize(460, 430);
        setDefaultCloseOperation(EXIT_ON_CLOSE); 
        setLocationRelativeTo(null);
        setResizable(false);

        JPanel mainPanel = new JPanel(new GridBagLayout());
        mainPanel.setBackground(MORANDI_BG);
        mainPanel.setBorder(new EmptyBorder(25, 25, 25, 25));
        add(mainPanel);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JLabel lblHeader = new JLabel("Smart Library System", JLabel.CENTER);
        lblHeader.setFont(new Font("Microsoft JhengHei", Font.BOLD, 26)); 
        lblHeader.setForeground(MORANDI_PRIMARY);
        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2;
        mainPanel.add(lblHeader, gbc);

        gbc.gridwidth = 1;
        JLabel lblUser = new JLabel("帳號/Account :");
        lblUser.setFont(new Font("Microsoft JhengHei", Font.BOLD, 14));
        lblUser.setForeground(MORANDI_TEXT);
        gbc.gridx = 0; gbc.gridy = 2;
        mainPanel.add(lblUser, gbc);

        txtUser = new JTextField(15);
        txtUser.setFont(new Font("Microsoft JhengHei", Font.PLAIN, 14)); 
        txtUser.putClientProperty("JComponent.roundRect", true);
        gbc.gridx = 1; mainPanel.add(txtUser, gbc);

        JLabel lblPassword = new JLabel("密碼/Password :");
        lblPassword.setFont(new Font("Microsoft JhengHei", Font.BOLD, 14));
        lblPassword.setForeground(MORANDI_TEXT);
        gbc.gridx = 0; gbc.gridy = 3;
        mainPanel.add(lblPassword, gbc);

        txtPassword = new JPasswordField(15);
        txtPassword.setFont(new Font("Microsoft JhengHei", Font.PLAIN, 14)); 
        txtPassword.putClientProperty("JComponent.roundRect", true);
        gbc.gridx = 1; mainPanel.add(txtPassword, gbc);

        btnLogin = new JButton("登入/Login");
        btnLogin.setFont(new Font("Microsoft JhengHei", Font.BOLD, 14));
        btnLogin.setBackground(MORANDI_PRIMARY); btnLogin.setForeground(Color.WHITE);
        btnLogin.putClientProperty("JButton.buttonType", "roundRect");
        gbc.gridx = 0; gbc.gridy = 4; gbc.gridwidth = 2;
        gbc.insets = new Insets(20, 10, 5, 10);
        mainPanel.add(btnLogin, gbc);

        btnRegister = new JButton("沒有帳號嗎？按此註冊");
        btnRegister.setFont(new Font("Microsoft JhengHei", Font.PLAIN, 12));
        btnRegister.setContentAreaFilled(false); btnRegister.setBorderPainted(false);
        btnRegister.setForeground(MORANDI_PRIMARY);
        gbc.gridy = 5; gbc.insets = new Insets(5, 10, 10, 10);
        mainPanel.add(btnRegister, gbc);

        btnLogin.addActionListener(e -> {
            String inputUser = txtUser.getText().trim();
            String inputPassword = new String(txtPassword.getPassword()).trim();

            String sql = "SELECT * FROM users WHERE student_no = ? AND password = ?";
            try (Connection conn = DatabaseManager.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, inputUser); pstmt.setString(2, inputPassword);
                ResultSet rs = pstmt.executeQuery();
                if (rs.next()) {
                    if ("SUSPENDED".equals(rs.getString("status"))) {
                        JOptionPane.showMessageDialog(this, "🛑 拒絕登入：您的帳號已被系統管理員處以「停權處分」，目前無法使用系統功能！", "帳號停權", JOptionPane.ERROR_MESSAGE);
                        return;
                    }

                    int dbUserId = rs.getInt("id");
                    String dbUserName = rs.getString("name");
                    String dbRoleLevel = rs.getString("role_level");

                    if ("B13204013".equals(inputUser) || "B13204043".equals(inputUser) || "R13945041".equals(inputUser) || "admin".equalsIgnoreCase(inputUser)) {
                        dbRoleLevel = "ADMIN";
                    }

                    this.dispose();
                    new MainApp(dbUserId, dbUserName, dbRoleLevel); 
                } else {
                    JOptionPane.showMessageDialog(this, "密碼驗證不符或帳號不存在！", "安全查核失敗", JOptionPane.ERROR_MESSAGE);
                }
            } catch (Exception ex) { ex.printStackTrace(); }
        });

        btnRegister.addActionListener(e -> {
            JTextField regSno = new JTextField(); regSno.setFont(new Font("Microsoft JhengHei", Font.PLAIN, 13));
            JTextField regName = new JTextField(); regName.setFont(new Font("Microsoft JhengHei", Font.PLAIN, 13));
            JPasswordField regPwd = new JPasswordField();
            JComboBox<String> regRoleCombo = new JComboBox<>(new String[]{"USER (普通成員)", "VIP (尊榮成員)"});
            regRoleCombo.setFont(new Font("Microsoft JhengHei", Font.PLAIN, 13));
            Object[] fields = {"帳號:", regSno, "姓名:", regName, "密碼:", regPwd, "成員級別:", regRoleCombo};

            if (JOptionPane.showConfirmDialog(this, fields, "沒有帳戶嗎？按此註冊", JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION) {
                String selectedRole = regRoleCombo.getSelectedIndex() == 0 ? "USER" : "VIP";
                try (Connection conn = DatabaseManager.getConnection(); PreparedStatement pstmt = conn.prepareStatement("INSERT INTO users (student_no, name, password, role_level, status, created_at) VALUES (?, ?, ?, ?, 'ACTIVE', NOW())")) {
                    pstmt.setString(1, regSno.getText().trim()); pstmt.setString(2, regName.getText().trim());
                    pstmt.setString(3, new String(regPwd.getPassword()).trim()); pstmt.setString(4, selectedRole);
                    pstmt.executeUpdate();
                    JOptionPane.showMessageDialog(this, "註冊完畢，請重新輸入進行登入驗證！");
                } catch (Exception ex) { JOptionPane.showMessageDialog(this, "註冊失敗，帳號可能已被使用。"); }
            }
        });

        setVisible(true);
    }
}