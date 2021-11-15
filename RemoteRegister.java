package progetto_reti;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;


public class RemoteRegister extends UnicastRemoteObject implements RemoteRegisterInterface {
	
	private static final long serialVersionUID = 1L;		//default
	ArrayList<User> allUser;
	public RemoteRegister(ArrayList<User> allUser) throws RemoteException{
		super();
		this.allUser=allUser;
	}
	@Override
	public synchronized String register(String username, String password) throws RemoteException{ //evito di inserire due username uguali in contemporanea
		username=username.toLowerCase();
		for(int i=0;i<allUser.size();i++) {
			if(allUser.get(i).equals(username)) {
				return "nome gia' in uso";
			}
		}
		allUser.add(new User(username,password));
		return "registrazione eseguita";
	}

}
