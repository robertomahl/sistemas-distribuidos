import java.rmi.Remote;
import java.rmi.RemoteException;

public interface IRoomChat extends Remote {

    public void sendMsg(String usrName, String msg);

    public void joinRoom(String usrName, IUserChat user);

    public void leaveRoom(String usrName);

    public void closeRoom();

    public String getRoomName();

}
