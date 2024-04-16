/**
 * Luana e Roberto
 */

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.Dimension;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;

import java.awt.BorderLayout;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.JTextField;

import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;

public class ChatClient {

    String serverAddress;
    Scanner in;
    PrintWriter out;
    JFrame frame = new JFrame("Chatter");
    JTextField textField = new JTextField(50);
    JTextPane messageArea = new JTextPane();

    public ChatClient(String serverAddress) {
        this.serverAddress = serverAddress;

        textField.setEditable(false);
        messageArea.setEditable(false);
        messageArea.setPreferredSize(new Dimension(200, 400));
        frame.getContentPane().add(textField, BorderLayout.SOUTH);
        frame.getContentPane().add(new JScrollPane(messageArea), BorderLayout.CENTER);
        frame.pack();

        textField.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                out.println(textField.getText());
                textField.setText("");
            }
        });
    }

    private String getName() {
        return JOptionPane.showInputDialog(frame, "Choose a screen name:", "Screen name selection",
                JOptionPane.PLAIN_MESSAGE);
    }

    private void run() throws IOException {
        try {
            var socket = new Socket(serverAddress, 59001);
            in = new Scanner(socket.getInputStream());
            out = new PrintWriter(socket.getOutputStream(), true);

            while (in.hasNextLine()) {
                var line = in.nextLine();
                if (line.startsWith("SUBMITNAME")) {
                    out.println(getName());
                } else if (line.startsWith("NAMEACCEPTED")) {
                    this.frame.setTitle("Chatter - " + line.substring(13));
                    textField.setEditable(true);
                } else if (line.startsWith("MESSAGE")) {
                    appendMessage(messageArea, line.substring(8), false);
                } else if (line.startsWith("SMESSAGE")) {
                    appendMessage(messageArea, line.substring(9), true);
                }
            }
        } finally {
            frame.setVisible(false);
            frame.dispose();
        }
    }

    private void appendMessage(JTextPane textArea, String message, boolean special) {
        StyledDocument doc = textArea.getStyledDocument();
        SimpleAttributeSet set = new SimpleAttributeSet();
        if (special) {
            StyleConstants.setForeground(set, java.awt.Color.BLUE);
            StyleConstants.setItalic(set, true);
        }
        try {
            doc.insertString(doc.getLength(), message + "\n", set);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }    

    public static void main(String[] args) throws Exception {
        if (args.length != 1) {
            System.err.println("Pass the server IP as the sole command line argument");
            return;
        }
        var client = new ChatClient(args[0]);
        client.frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        client.frame.setVisible(true);
        client.run();
    }
}
