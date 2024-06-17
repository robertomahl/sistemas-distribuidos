import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;

public class ServerChat implements IServerChat {

    private ArrayList<String> roomList;

    public ServerChat() {
        this.roomList = new ArrayList<>();
    }

    @Override
    public ArrayList<String> getRooms() {
        return roomList;
    }

    @Override
    public void createRoom(String roomName) throws RemoteException {
        if (!roomList.contains(roomName)) {
            RoomChat room = new RoomChat(roomName);

            LocateRegistry.getRegistry().rebind(roomName, room);

            roomList.add(roomName);
        }
    }

    /*
    Registry registry = LocateRegistry.getRegistry(); // poderia passar host e port
    ServerChat server = (ServerChat) registry.lookup("Servidor");
     */

    public static void main(String[] args) {
        try {
            ServerChat server = new ServerChat();
            IServerChat stub = (IServerChat) UnicastRemoteObject.exportObject(server, 0);

            LocateRegistry.createRegistry(2020).rebind("Servidor", stub);

            System.err.println("Server ready");
        } catch (Exception e) {
            System.err.println("Server exception: " + e.toString());
            e.printStackTrace();
        }

    }

}
