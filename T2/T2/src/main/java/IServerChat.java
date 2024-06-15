import java.util.ArrayList;

public interface IServerChat extends java.rmi.Remote {
    
    public ArrayList<String> getRooms() throws java.rmi.RemoteException;

    public void createRoom(String roomName) throws java.rmi.RemoteException;
    
}