import java.util.ArrayList;

public class ServerChat implements IServerChat {

    private ArrayList<String> roomList;

    public ServerChat() {
        this.roomList = new ArrayList<String>();
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

    public static void main(String[] args) throws Exception {
    }

}
