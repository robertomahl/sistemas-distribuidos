import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Map;
import java.util.HashMap;

public class RoomChat implements IRoomChat {

    private Map<String, IUserChat> userList;

    public RoomChat() {
        this.userList = new HashMap<>();
    }

    @Override
    public void sendMsg(String usrName, String msg) {
//        TODO
    }

    @Override
    public void joinRoom(String usrName, IUserChat user) {
//        TODO
    }

    @Override
    public void leaveRoom(String usrName) {
//        TODO
    }

    @Override
    public void closeRoom() {
//        TODO
    }

    @Override
    public String getRoomName() {
//        TODO
        return "";
    }

    public static void main(String[] args) {
        try {
            IServerChat stub = (IServerChat) LocateRegistry.getRegistry("127.0.0.1", 2020).lookup("Servidor");
            String response = stub.sayHello();
            System.out.println("response: " + response);
        } catch (Exception e) {
            System.err.println("Room exception: " + e.toString());
            e.printStackTrace();
        }
    }

}
