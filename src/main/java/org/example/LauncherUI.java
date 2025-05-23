package org.example;

import org.example.client.ChatLauncherUI;
import org.example.server.ServerDemo;

import javax.swing.*;
import java.awt.*;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class LauncherUI extends JFrame {
    public LauncherUI() {
        initUI();
    }

    private void initUI() {
        setTitle("Chat Application Launcher");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(300, 200);
        setLocationRelativeTo(null);

        JPanel panel = new JPanel(new GridLayout(2, 1, 10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JButton serverBtn = new JButton("Start Server");
        JButton clientBtn = new JButton("Start Client");

        // Style buttons
        styleButton(serverBtn, new Color(76, 175, 80));
        styleButton(clientBtn, new Color(33, 150, 243));

        serverBtn.addActionListener(e -> startServer());
        clientBtn.addActionListener(e -> startClient());

        panel.add(serverBtn);
        panel.add(clientBtn);
        add(panel);
    }

    private void styleButton(JButton button, Color color) {
        button.setFont(new Font("Segoe UI", Font.BOLD, 14));
        button.setBackground(color);
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }

    private void startServer() {
        try {
            // Check if server is already running
            Registry registry = LocateRegistry.getRegistry("localhost", 55545);
            registry.lookup("ChatService"); // Check if service exists
            JOptionPane.showMessageDialog(this, "Server is already running!");
        } catch (Exception ex) {
            // Start server in background thread
            new Thread(() -> {
                ServerDemo.startServer();
                JOptionPane.showMessageDialog(this, "Server started successfully!");
            }).start();
        }
    }

    private void startClient() {
        SwingUtilities.invokeLater(() -> {
            new ChatLauncherUI().setVisible(true);
        });
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            LauncherUI launcher = new LauncherUI();
            launcher.setVisible(true);
        });
    }
}