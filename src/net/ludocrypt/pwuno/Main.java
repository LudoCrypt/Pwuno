package net.ludocrypt.pwuno;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import net.harawata.appdirs.AppDirs;
import net.harawata.appdirs.AppDirsFactory;
import net.ludocrypt.pwuno.ui.ClientUI;
import net.ludocrypt.pwuno.ui.ServerUI;

public class Main {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Uno!!!");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setSize(300, 200);
            JPanel panel = new JPanel();

            JButton serverButton = new JButton("Start Server");
            JButton clientButton = new JButton("Join Server");

            serverButton.addActionListener(e -> {
                String port = JOptionPane.showInputDialog(frame, "Open on Port:", "54555");
                if (port == null || port.isEmpty()) {
                    return;
                }
                try {
                    int portNumber = Integer.parseInt(port);
                    frame.dispose();
                    new ServerUI(portNumber);
                } catch (NumberFormatException e1) {
                    JOptionPane.showMessageDialog(frame, "Invalid port. Please enter a valid number.", "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                } catch (IOException e1) {
                    e1.printStackTrace();
                }

            });

            clientButton.addActionListener(e -> {
                String initialIP = "127.0.0.1:54555";
                File file = new File(configDir(), "pwunolastserver.");

                if (file.exists()) {
                    try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                        String ip = reader.readLine();
                        initialIP = ip;
                    } catch (IOException e1) {
                        e1.printStackTrace();
                    }
                }

                String ip = JOptionPane.showInputDialog(frame, "Enter Server IP:", initialIP);
                if (ip == null || ip.isEmpty()) {
                    return;
                }

                try {
                    Integer.parseInt(ip.substring(ip.indexOf(":") + 1, ip.length()));
                } catch (NumberFormatException e1) {
                    JOptionPane.showMessageDialog(frame, "Invalid ip. Missing port?", "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                new File(configDir()).mkdirs();
                try (FileWriter writer = new FileWriter(new File(configDir(), "pwunolastserver."))) {
                    writer.write(ip);
                } catch (IOException e1) {
                    e1.printStackTrace();
                }

                String name = JOptionPane.showInputDialog(frame, "Enter your name:");
                if (name == null || name.isEmpty()) {
                    return;
                }

                frame.dispose();
                try {
                    new ClientUI(ip, name);
                } catch (Exception e1) {
                    e1.printStackTrace();
                }
            });

            panel.add(serverButton);
            panel.add(clientButton);
            frame.add(panel);
            frame.setVisible(true);
        });
    }

    private static String configDir() {
        AppDirs appDirs = AppDirsFactory.getInstance();
        return appDirs.getUserConfigDir("Pwuno", "1.0.0", "LudoCrypt");
    }

}
