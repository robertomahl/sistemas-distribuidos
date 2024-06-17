import javax.swing.*;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;

public class ServerChat implements IServerChat {

    private ArrayList<String> roomList;

    private JFrame frame = new JFrame("Server");

    public ServerChat() {
        this.roomList = new ArrayList<>();

        this.frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
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

            Registry registry = LocateRegistry.getRegistry("127.0.0.1", 2020);
            registry.rebind(roomName, room);

            roomList.add(roomName);

            show();
        }
    }

    private void closeRoom(String roomName) {
        try {
            Registry registry = LocateRegistry.getRegistry("127.0.0.1", 2020);
            IRoomChat roomChat = (IRoomChat) registry.lookup(roomName);

            roomChat.closeRoom();

            roomList.remove(roomName);
        } catch (Exception e) {
            System.err.println("Close room exception: " + e.toString());
            e.printStackTrace();
        }
    }

    private void show() {
        var list = new JList<>(this.roomList.toArray(new String[0]));

        list.setVisible(true);

        list.addListSelectionListener((e) -> {
            this.closeRoom(list.getSelectedValue());
        });

        frame.getContentPane().add(list);
    }

    public static void main(String[] args) {
        try {
            ServerChat server = new ServerChat();

            IServerChat stub = (IServerChat) UnicastRemoteObject.exportObject(server, 0);
            LocateRegistry.createRegistry(2020).rebind("Servidor", stub);

            server.createRoom("Padrao");

            server.show();
        } catch (Exception e) {
            System.err.println("Server exception: " + e.toString());
            e.printStackTrace();
        }

    }

}
