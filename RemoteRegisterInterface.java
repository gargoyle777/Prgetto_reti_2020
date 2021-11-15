package progetto_reti;

import java.rmi.Remote;
import java.rmi.RemoteException;


public interface RemoteRegisterInterface extends Remote {
public String register(String username, String password)throws RemoteException;
}
