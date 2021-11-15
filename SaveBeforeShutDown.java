package progetto_reti;

import java.io.File;
import java.io.FileWriter;
import java.nio.file.Files;
import java.util.ArrayList;

import org.json.simple.JSONObject;

public class SaveBeforeShutDown implements Runnable{
	ArrayList<User> allUsers;
	String userDirectoryPath;
	public SaveBeforeShutDown(ArrayList<User> allUsers,String userDirectoryPath) {
		this.allUsers=allUsers;
		this.userDirectoryPath=userDirectoryPath;
	}
	public void run() {
		System.out.println("sto salvando i dati utente...");
		File userNewDirectory= new File(userDirectoryPath+"\\"+System.currentTimeMillis());
		userNewDirectory.mkdir();
		for(int i=0;i<allUsers.size();i++) {
			try {
				File userFile= new File(userNewDirectory.getPath()+"\\"+allUsers.get(i).getName()+".json");
				userFile.createNewFile();
				FileWriter fileWriter= new FileWriter(userFile);
				fileWriter.write(allUsers.get(i).getJSON().toJSONString());
				fileWriter.close();
			}
			catch(Exception e) {
				System.out.println("errore nel salvataggio di: "+allUsers.get(i).getName());
				e.getStackTrace();
			}
		}
		System.out.println("finito di salvare");
	}

}
