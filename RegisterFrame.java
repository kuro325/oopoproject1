package test;
import javax.swing.*;
import java.awt.*;
import java.sql.*;
import javax.swing.border.LineBorder;

public class LoginFrame extends JFrame {
    public LoginFrame() {
        //Main Window
        setTitle("E-Parking System");
        setSize(1000,700);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        setLayout(new GridBagLayout());

        //Main Panel 
        JPanel main_panel = new JPanel();
        main_panel.setBackground(Color.WHITE);
        main_panel.setBorder(new LineBorder(Color.GRAY));
        main_panel.setPreferredSize(new Dimension(400,500));
        main_panel.setLayout(new BorderLayout());

        //Header Panel
        JPanel header_panel = new JPanel(new GridLayout(2,1));
        header_panel.setBackground(new Color(22,93,252));
        header_panel.setPreferredSize(new Dimension(400,70));

        JLabel header_title = new JLabel("E-Parking System", SwingConstants.CENTER);
        header_title.setForeground(Color.WHITE);
        header_title.setFont(new Font("SansSerif",Font.BOLD,30));

        JLabel header_title_2 = new JLabel("Login to your account", SwingConstants.CENTER);
        header_title_2.setFont(new Font("SansSerif",Font.BOLD,15));

        header_panel.add(header_title);
        header_panel.add(header_title_2);

        //Body Panel
        JPanel main_body = new JPanel();
        main_body.setBackground(Color.WHITE);
        main_body.setLayout(new BoxLayout(main_body, BoxLayout.Y_AXIS));
        main_body.setBorder(BorderFactory.createEmptyBorder(50, 30, 50, 30));

        //Username Panel
        JPanel email_panel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        email_panel.setBackground(Color.WHITE);

        JLabel email_label = new JLabel (" Email:         ");
        JTextField email_field = new JTextField();
        email_field.setPreferredSize(new Dimension(200,30));

        email_panel.add(email_label);
        email_panel.add(email_field);

        //Password Panel
        JPanel pass_panel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        pass_panel.setBackground(Color.WHITE);
        JLabel pass_label = new JLabel("Password:");    
        JPasswordField pass_field = new JPasswordField();
        pass_field.setPreferredSize(new Dimension(200,30));
        pass_panel.add(pass_label);
        pass_panel.add(pass_field);

        // Panel for Remember Me and Forgot Password
        JPanel options_panel = new JPanel(new BorderLayout());
        options_panel.setBackground(Color.WHITE);

        JCheckBox remember_checkbox = new JCheckBox("Remember Me");
        remember_checkbox.setBackground(Color.WHITE);
        options_panel.add(remember_checkbox, BorderLayout.WEST);

        JLabel forgot_label = new JLabel("<HTML><U>Forgot Password?</U></HTML>");
        forgot_label.setForeground(Color.BLUE);
        forgot_label.setCursor(new Cursor(Cursor.HAND_CURSOR));
        options_panel.add(forgot_label, BorderLayout.EAST);

        //Button Panel
        JPanel button_panel = new JPanel();
        button_panel.setBackground(Color.WHITE);
        button_panel.setLayout(new BoxLayout(button_panel, BoxLayout.Y_AXIS));

        JButton loginBtn = new JButton("Login");
        loginBtn.setBackground(new Color(22,93,252));
        loginBtn.setForeground(Color.WHITE);
        loginBtn.setPreferredSize(new Dimension(250,40));
        loginBtn.setMaximumSize(new Dimension(250,40));
        loginBtn.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel register_label = new JLabel("Don't have an account? Register here");
        register_label.setForeground(Color.BLUE);
        register_label.setCursor(new Cursor(Cursor.HAND_CURSOR));
        register_label.setAlignmentX(Component.CENTER_ALIGNMENT);

        button_panel.add(loginBtn);
        button_panel.add(Box.createVerticalStrut(10));
        button_panel.add(register_label);

        main_body.add(email_panel);
        main_body.add(Box.createVerticalStrut(20));
        main_body.add(pass_panel);
        main_body.add(Box.createVerticalStrut(10));
        main_body.add(options_panel);
        main_body.add(Box.createVerticalStrut(30));
        main_body.add(button_panel);

        main_panel.add(header_panel, BorderLayout.NORTH);
        main_panel.add(main_body, BorderLayout.CENTER);

        add(main_panel);

        setVisible(true);

        
       loginBtn.addActionListener(e -> {
    String email = email_field.getText();
    String password = new String(pass_field.getPassword());

    if(email.isEmpty() || password.isEmpty()) {
        JOptionPane.showMessageDialog(this, "Please enter email and password", "Error", JOptionPane.ERROR_MESSAGE);
        return;
    }

    try (Connection conn = DBConnection.getConnection()) { 
        String sql = "SELECT * FROM users WHERE email=? AND password=?";
        PreparedStatement pst = conn.prepareStatement(sql);
        pst.setString(1, email);
        pst.setString(2, password);
        ResultSet rs = pst.executeQuery();

        if(rs.next()) {
            JOptionPane.showMessageDialog(this, "Login successful!");
            new DashboardFrame().setVisible(true);
            this.dispose();
        } else {
            JOptionPane.showMessageDialog(this, "Invalid email or password", "Error", JOptionPane.ERROR_MESSAGE);
        }
    } catch(SQLException | ClassNotFoundException ex) {
        ex.printStackTrace();
        JOptionPane.showMessageDialog(this, "Database connection failed!", "Error", JOptionPane.ERROR_MESSAGE);
    }
});

        
        register_label.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                new RegisterFrame().setVisible(true);
            }
        });
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new LoginFrame().setVisible(true));
    }
}