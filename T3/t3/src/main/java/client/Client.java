package client;

import StableMulticast.IStableMulticast;
import StableMulticast.StableMulticast;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class Client extends JFrame implements IStableMulticast {
    private StableMulticast stableMulticast;
    private String clientName;

    private JTextArea messageArea;
    private JTextField messageField;

    private Client(String clientName) {
        super(clientName);
        this.clientName = clientName;
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.setSize(400, 300);

        // Layout
        setLayout(new BorderLayout());

        // Text area to display messages
        messageArea = new JTextArea();
        messageArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(messageArea);
        add(scrollPane, BorderLayout.CENTER);

        // Panel for message input
        JPanel inputPanel = new JPanel();
        inputPanel.setLayout(new BorderLayout());
        add(inputPanel, BorderLayout.SOUTH);

        // Text field for message input
        messageField = new JTextField();
        inputPanel.add(messageField, BorderLayout.CENTER);

        // Add ActionListener to JTextField for Enter key
        messageField.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String msg = messageField.getText();
                sendMessage(msg);
                messageField.setText(""); // Clear the input field after sending
            }
        });

        // Button to send message
        JButton sendButton = new JButton("Send");
        sendButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String msg = messageField.getText();
                sendMessage(msg);
                messageField.setText(""); // Clear the input field after sending
            }
        });
        inputPanel.add(sendButton, BorderLayout.EAST);
    }

    private void init(String clientIp, Integer clientPort) {
        stableMulticast = new StableMulticast(clientIp, clientPort, this);
    }

    @Override
    public void deliver(String msg) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                messageArea.append(msg + "\n");
            }
        });
    }

    public void sendMessage(String msg) {
        stableMulticast.msend(msg);
    }

    public static void main(String[] args) {
        if (args.length < 2) {
            System.err.println("Usage: java Client <clientIp> <clientPort>");
            System.exit(1);
        }

        String clientIp = args[0];
        int clientPort = Integer.parseInt(args[1]);

        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                // Ask for client name
                String clientName = JOptionPane.showInputDialog(null, "Enter client name:");
                if (clientName == null || clientName.trim().isEmpty()) {
                    JOptionPane.showMessageDialog(null, "Client name cannot be empty. Exiting.");
                    System.exit(1);
                }

                // Create and display the client GUI
                Client client = new Client(clientName);
                client.init(clientIp, clientPort);
                client.setVisible(true);
            }
        });
    }
}
