import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.RemoteException;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.*;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;
import java.util.List;

public class UserChat extends JFrame implements IUserChat {

    private IServerChat serverStub;
    private IRoomChat currentRoomStub;
    private String userName;
    private JTextPane messageArea;
    private JTextField textField;
    private JPanel roomListPanel;

    public UserChat(String userName) {
        this.userName = userName;
        initializeGUI();

        try {
            serverStub = (IServerChat) LocateRegistry.getRegistry("127.0.0.1", 2020).lookup("Servidor");
            updateRoomList();
        } catch (Exception e) {
            System.err.println("UserChat exception: " + e.toString());
            e.printStackTrace();
        }
    }

    private void initializeGUI() {
        setTitle("User Chat - " + userName);
        setSize(600, 400);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        messageArea = new JTextPane();
        messageArea.setEditable(false);
        JScrollPane messageScrollPane = new JScrollPane(messageArea);

        textField = new JTextField();
        textField.setEditable(false);
        textField.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                try {
                    if (currentRoomStub != null) {
                        currentRoomStub.sendMsg(userName, textField.getText());
                        textField.setText("");
                    }
                } catch (RemoteException ex) {
                    ex.printStackTrace();
                }
            }
        });

        JPanel navbar = new JPanel();
        navbar.setLayout(new FlowLayout(FlowLayout.LEFT)); // Set layout for buttons

        JButton addRoomButton = new JButton("Add Room");
        addRoomButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String roomName = JOptionPane.showInputDialog(UserChat.this, "Enter room name:");
                if (roomName != null && !roomName.isEmpty()) {
                  System.out.println(roomName);
                    try {
                        serverStub.createRoom(roomName);
                    } catch (RemoteException ex) {
                        throw new RuntimeException(ex);
                    }
                }
            }
        });
        navbar.add(addRoomButton);

        JButton exitButton = new JButton("Exit");
        exitButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    currentRoomStub.leaveRoom(userName);
                } catch (RemoteException ex) {
                    throw new RuntimeException(ex);
                }
            }
        });
        navbar.add(exitButton);

        roomListPanel = new JPanel();
        roomListPanel.setLayout(new FlowLayout(FlowLayout.LEFT)); // Set layout for buttons

        JScrollPane roomScrollPane = new JScrollPane(roomListPanel);

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, roomScrollPane, messageScrollPane);
        splitPane.setDividerLocation(150);

        getContentPane().add(splitPane, BorderLayout.CENTER);
        getContentPane().add(textField, BorderLayout.SOUTH);
        getContentPane().add(roomScrollPane, BorderLayout.WEST);
        getContentPane().add(navbar, BorderLayout.NORTH);

    }

    private void updateRoomList() {
        try {
            List<String> rooms = serverStub.getRooms();
            System.out.println(rooms);
            roomListPanel.removeAll(); // Clear existing buttons

            for (String roomName : rooms) {
                JButton roomButton = new JButton(roomName);
                roomButton.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        joinRoom(roomName);
                    }
                });
                roomListPanel.add(roomButton);
            }

            roomListPanel.revalidate(); // Refresh the panel layout
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    private void joinRoom(String roomName) {
        try {
            if (currentRoomStub != null) {
                currentRoomStub.leaveRoom(userName);
            }
            currentRoomStub = (IRoomChat) LocateRegistry.getRegistry("127.0.0.1", 2020).lookup(roomName);
            currentRoomStub.joinRoom(userName, this);
            textField.setEditable(true);
            messageArea.setText("");
            appendMessage("Joined room: " + roomName, true);
        } catch (Exception e) {
            System.err.println("Room exception: " + e.toString());
            e.printStackTrace();
        }
    }

    private void appendMessage(String message, boolean special) {
        StyledDocument doc = messageArea.getStyledDocument();
        SimpleAttributeSet set = new SimpleAttributeSet();
        if (special) {
            StyleConstants.setForeground(set, Color.BLUE);
            StyleConstants.setItalic(set, true);
        }
        try {
            doc.insertString(doc.getLength(), message + "\n", set);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void deliverMsg(String senderName, String msg) throws RemoteException {
        appendMessage(senderName + ": " + msg, false);
    }

    public static void main(String[] args) {
        if (args.length != 1) {
            System.err.println("Usage: java UserChat <username>");
            return;
        }
        String userName = args[0];
        SwingUtilities.invokeLater(() -> {
            UserChat client = new UserChat(userName);
            client.setVisible(true);
        });
    }
}
