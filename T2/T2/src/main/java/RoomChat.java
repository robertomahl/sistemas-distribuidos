import java.util.Map;
import java.util.HashMap;

public class RoomChat implements IRoomChat {

    private Map<String, IUserChat> userList;

    public RoomChat() {
        this.userList = new HashMap<String, IUserChat>();
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

    public static void main(String[] args) throws Exception {
    }

}