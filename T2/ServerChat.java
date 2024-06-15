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
//        TODO
        return null;
    }

    @Override
    public void createRoom(String roomName) {
//        TODO
    }

    @Override
    public String sayHello() throws RemoteException {
        return "Hello";
    }

    public static void main(String[] args) {
        try {
            ServerChat obj = new ServerChat();
            IServerChat stub = (IServerChat) UnicastRemoteObject.exportObject(obj, 0);

            LocateRegistry.getRegistry("127.0.0.1", 2020).bind("Servidor", stub);

            System.err.println("Server ready");
        } catch (Exception e) {
            System.err.println("Server exception: " + e.toString());
            e.printStackTrace();
        }

    }

}
