import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.util.Map;
import java.util.HashMap;

public class RoomChat implements IRoomChat {

    private Map<String, IUserChat> userList;
    private String roomName;

    public RoomChat(String roomName) {

        this.roomName = roomName;
        this.userList = new HashMap<>();
    }

    @Override
    public void sendMsg(String usrName, String msg) throws RemoteException {
        for (IUserChat u : userList.values()) {
            u.deliverMsg(usrName, msg);
        }
    }

    @Override
    public void joinRoom(String usrName, IUserChat user) {
        userList.put(usrName, user);
//        TODO: enviar mensagem
    }

    @Override
    public void leaveRoom(String usrName) {
        userList.remove(usrName);
//        TODO: enviar mensagem
    }

    @Override
    public void closeRoom() {
//        TODO: enviar mensagem
        userList.clear();
    }

    @Override
    public String getRoomName() {
        return this.roomName;
    }

}
