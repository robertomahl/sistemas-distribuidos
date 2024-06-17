import java.awt.BorderLayout;
import java.io.Serializable;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import javax.swing.DefaultListModel;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JScrollPane;

public class ServerChat implements IServerChat, Serializable {

    private ArrayList<String> roomList;

    private DefaultListModel<String> listModel;
    private JList<String> roomJList;
    private JFrame frame;

    public ServerChat() {
        this.roomList = new ArrayList<>();
        this.listModel = new DefaultListModel<>();
        this.roomJList = new JList<>(listModel);
        this.frame = new JFrame("Server");

        initGUI();
    }

    private void initGUI() {
        this.frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.frame.setSize(400, 300);
        this.frame.setLayout(new BorderLayout());

        JScrollPane scrollPane = new JScrollPane(roomJList);
        this.frame.add(scrollPane, BorderLayout.CENTER);

        roomJList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                String selectedRoom = roomJList.getSelectedValue();
                if (selectedRoom != null) {
                    closeRoom(selectedRoom);
                }
            }
        });

        this.frame.setVisible(true);
    }

    @Override
    public ArrayList<String> getRooms() {
        return roomList;
    }

    @Override
    public void createRoom(String roomName) throws RemoteException {
        if (!roomList.contains(roomName)) {
            RoomChat room = new RoomChat(roomName);

            IRoomChat stub = (IRoomChat) UnicastRemoteObject.exportObject(room, 0);

            Registry registry = LocateRegistry.getRegistry("127.0.0.1", 2020);
            registry.rebind(roomName, stub);

            roomList.add(roomName);
            listModel.addElement(roomName);
        }
    }

    private void closeRoom(String roomName) {
        try {
            Registry registry = LocateRegistry.getRegistry("127.0.0.1", 2020);
            IRoomChat roomChat = (IRoomChat) registry.lookup(roomName);

            roomChat.closeRoom();

            roomList.remove(roomName);
            listModel.removeElement(roomName);
        } catch (Exception e) {
            System.err.println("Close room exception: " + e);
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        try {
            ServerChat server = new ServerChat();

            IServerChat stub = (IServerChat) UnicastRemoteObject.exportObject(server, 0);
            LocateRegistry.createRegistry(2020).rebind("Servidor", stub);
        } catch (Exception e) {
            System.err.println("Server exception: " + e.toString());
            e.printStackTrace();
        }
    }
}
