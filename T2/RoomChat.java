import java.io.Serializable;
import java.rmi.RemoteException;
import java.util.HashMap;
import java.util.Map;

public class RoomChat implements IRoomChat, Serializable {

    private Map<String, IUserChat> userList;
    private String roomName;

    public RoomChat(String roomName) {
        this.roomName = roomName;
        this.userList = new HashMap<>();
    }

    @Override
    public void sendMsg(String usrName, String msg) {
//      TODO: try-catch may be removed after removing throws from method signature
        try {
            for (IUserChat u : userList.values()) {
                u.deliverMsg(usrName, msg);
            }
        } catch (Exception e) {
            System.err.println("Send msg exception: " + e);
            e.printStackTrace();
        }
    }

    @Override
    public void joinRoom(String usrName, IUserChat user) {
        userList.put(usrName, user);
        this.sendMsg("Sistema", usrName + " entrou na sala.");
    }

    @Override
    public void leaveRoom(String usrName) {
        userList.remove(usrName);
        this.sendMsg("Sistema", usrName + " saiu da sala.");
    }

    @Override
    public void closeRoom() {
        this.sendMsg("Sistema", "Sala fechada pelo servidor.");
        userList.clear();
    }

    @Override
    public String getRoomName() {
        return this.roomName;
    }

}
