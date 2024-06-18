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

    private DefaultListModel<String> roomListModel;
    private JList<String> jList;
    private JFrame jFrame;

    public ServerChat() {
        this.roomList = new ArrayList<>();
        this.roomListModel = new DefaultListModel<>();
        this.jList = new JList<>(roomListModel);
        this.jFrame = new JFrame("Server - Clique em uma sala para fecha-la");

        initGUI();
    }

    private void initGUI() {
        this.jFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.jFrame.setSize(400, 300);
        this.jFrame.setLayout(new BorderLayout());

        JScrollPane scrollPane = new JScrollPane(jList);
        this.jFrame.add(scrollPane, BorderLayout.CENTER);

        jList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                String selectedRoom = jList.getSelectedValue();
                if (selectedRoom != null) {
                    closeRoom(selectedRoom);
                }
            }
        });

        this.jFrame.setVisible(true);
    }

    @Override
    public ArrayList<String> getRooms() {
        return roomList;
    }

    @Override
    public void createRoom(String roomName) {
        try {
            if (!roomList.contains(roomName)) {
                RoomChat room = new RoomChat(roomName);

                IRoomChat stub = (IRoomChat) UnicastRemoteObject.exportObject(room, 0);

                Registry registry = LocateRegistry.getRegistry("127.0.0.1", 2020);
                registry.rebind(roomName, stub);

                roomList.add(roomName);
                roomListModel.addElement(roomName);
            }
        } catch (RemoteException e) {
            System.err.println("Create room exception: " + e);
            e.printStackTrace();
        }
    }

    private void closeRoom(String roomName) {
        try {
            Registry registry = LocateRegistry.getRegistry("127.0.0.1", 2020);
            IRoomChat roomChat = (IRoomChat) registry.lookup(roomName);

            roomChat.closeRoom();

            roomList.remove(roomName);
            roomListModel.removeElement(roomName);
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
            System.err.println("Server exception: " + e);
            e.printStackTrace();
        }
    }
}
