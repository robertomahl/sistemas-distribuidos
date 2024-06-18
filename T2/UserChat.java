import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import javax.swing.*;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;

public class UserChat extends JFrame implements IUserChat {

    private IServerChat serverStub;
    private IRoomChat currentRoomStub;
    private String userName;
    private JTextPane messageArea;
    private JTextField textField;
    private JPanel roomListPanel;
    private List<String> previousRoomList;
    private ScheduledExecutorService scheduler;
    private Map<String, Color> userColors; // Mapa para armazenar as cores dos usuários
    private JLabel roomNameLabel; // JLabel para exibir o nome da sala atual

    private static final String SERVER_IP_ADDRESS = "192.168.0.109";

    public UserChat(String userName) {
        this.userName = userName;
        this.previousRoomList = new ArrayList<>();
        this.userColors = new HashMap<>();
        initializeGUI();

        try {
            serverStub = (IServerChat) LocateRegistry.getRegistry(SERVER_IP_ADDRESS, 2020).lookup("Servidor");
            IUserChat stub = (IUserChat) UnicastRemoteObject.exportObject(this, 0);
            updateRoomList();
            startRoomListUpdater();
        } catch (Exception e) {
            System.err.println("UserChat exception: " + e);
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
        textField.setEditable(true);
        textField.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                try {
                    if (currentRoomStub != null) {
                        String message = textField.getText();
                        currentRoomStub.sendMsg(userName, message);
                        appendMessage(userName, message, false); // Exibe a mensagem localmente
                        textField.setText("");
                    }
                } catch (RemoteException ex) {
                    ex.printStackTrace();
                }
            }
        });

        JPanel messagePanel = new JPanel(new BorderLayout());
        messagePanel.add(messageScrollPane, BorderLayout.CENTER);
        messagePanel.add(textField, BorderLayout.SOUTH);

        JPanel navbar = new JPanel();
        navbar.setLayout(new FlowLayout(FlowLayout.LEFT));

        JButton addRoomButton = new JButton("Add Room");
        addRoomButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String roomName = JOptionPane.showInputDialog(UserChat.this, "Enter room name:");
                if (roomName != null && !roomName.isEmpty()) {
                    System.out.println(roomName);
                    try {
                        serverStub.createRoom(roomName);
                        updateRoomList();
                    } catch (RemoteException ex) {
                        ex.printStackTrace();
                    }
                }
            }
        });
        navbar.add(addRoomButton);

        JButton exitButton = new JButton("Leave");
        exitButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                leaveRoom(); // Ao sair, deixe a sala atual
            }
        });
        navbar.add(exitButton);
        roomNameLabel = new JLabel("Room Name: ");
        roomNameLabel.setHorizontalAlignment(SwingConstants.CENTER);
        navbar.add(roomNameLabel, BorderLayout.NORTH);

        roomListPanel = new JPanel();
        roomListPanel.setLayout(new BoxLayout(roomListPanel, BoxLayout.Y_AXIS));

        JScrollPane roomScrollPane = new JScrollPane(roomListPanel);

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, roomScrollPane, messagePanel);
        splitPane.setDividerLocation(150);

        getContentPane().add(splitPane, BorderLayout.CENTER);
        getContentPane().add(navbar, BorderLayout.NORTH);
    }

    private void startRoomListUpdater() {
        scheduler = Executors.newScheduledThreadPool(1);
        scheduler.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                updateRoomList();
            }
        }, 0, 1, TimeUnit.SECONDS);
    }

    private synchronized void updateRoomList() {
        try {
            List<String> rooms = serverStub.getRooms();
            if (!rooms.equals(previousRoomList)) {
                previousRoomList = new ArrayList<>(rooms);
                SwingUtilities.invokeLater(() -> {
                    roomListPanel.removeAll();
                    for (String roomName : rooms) {
                        JButton roomButton = new JButton(roomName);
                        roomButton.setAlignmentX(Component.LEFT_ALIGNMENT);
                        roomButton.addActionListener(e -> joinRoom(roomName));
                        roomListPanel.add(roomButton);
                    }
                    roomListPanel.revalidate();
                    roomListPanel.repaint();
                });
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    private void joinRoom(String roomName) {
        try {
            if (currentRoomStub != null) {
                currentRoomStub.leaveRoom(userName); // Primeiro, deixe a sala atual, se houver
            }
            currentRoomStub = (IRoomChat) LocateRegistry.getRegistry(SERVER_IP_ADDRESS, 2020).lookup(roomName);
            currentRoomStub.joinRoom(userName, this); // Junte-se à nova sala
            textField.setEditable(true);
            messageArea.setText("");
            roomNameLabel.setText("Room Name: " + roomName);
            appendMessage("Joined room: ", roomName, true);
        } catch (Exception e) {
            System.err.println("Room exception: " + e.toString());
            e.printStackTrace();
        }
    }

    public void leaveRoom() {
        try {
            if (currentRoomStub != null) {
                currentRoomStub.leaveRoom(userName); // Deixe a sala atual
                currentRoomStub = null; // Limpe a referência ao stub da sala atual
                textField.setEditable(false);
                messageArea.setText("");
                roomNameLabel.setText("Room Name: ");
            }
        } catch (Exception e) {
            System.err.println("Error leaving room: " + e.toString());
            e.printStackTrace();
        }
    }

    private void appendMessage(String senderName, String message, boolean special) {
        StyledDocument doc = messageArea.getStyledDocument();
        SimpleAttributeSet set = new SimpleAttributeSet();

        Color color = getUserColor(senderName); // Obtém a cor do usuário
        StyleConstants.setForeground(set, color);

        if (special) {
            StyleConstants.setItalic(set, true);
        }

        try {
            doc.insertString(doc.getLength(), senderName + ": " + message + "\n", set);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private Color getUserColor(String userName) {
        // Gera uma cor com base no nome do usuário
        int hash = userName.hashCode();
        float hue = (float) Math.abs(hash % 360) / 360;
        return Color.getHSBColor(hue, 0.5f, 0.9f);
    }

    @Override
    public void deliverMsg(String senderName, String msg) {
        // Verifica se a mensagem não vem do próprio usuário
        if (!senderName.equals(userName)) {
            appendMessage(senderName, msg, false);
        }
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
