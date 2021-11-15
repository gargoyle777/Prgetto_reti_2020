package progetto_reti;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import org.json.simple.JSONObject;


public class Server implements Runnable{ 
	String directoryPath;
	public Server(String directoryPath){			//directory in input
		this.directoryPath=directoryPath;
	}
	
	public Attachment login(String name, String password,ArrayList<User> allUsers) {
		for(int i=0;i<allUsers.size();i++) {
			if(allUsers.get(i).equals(name)) {
				if(allUsers.get(i).checkstatus())	return new Attachment(allUsers.get(i),"utente gia' loggato");
				if(allUsers.get(i).checkPSW(password)) {
					allUsers.get(i).setLogStatus();
					return new Attachment(allUsers.get(i),"successo");
				}
				else return new Attachment("password errata");
			}
		}
		return new Attachment("nome inesistente");
	}
	
	public void logout(Attachment attach) {
		attach.setOutput("-LOGOUT-");
		attach.userloggedoff();
	}
	
	public void addFriend(Attachment attach, String name,ArrayList<User> allUsers) {
		attach.setOutput("nome non trovato");
		if(attach.attachedUser().equals(name)) {
			attach.setOutput("non puoi aggiungere te stesso");
		}
		else {
			for(int i=0;i<allUsers.size();i++) {
				if(allUsers.get(i).getName().equals(name)) {
					if(attach.attachedUser().addFriend(name)) {
						attach.setOutput("amico aggiunto");
					}
					else {
						attach.setOutput("amico gia' in lista");
					}
					i=allUsers.size();
				}
			}	
		}
	}
	
	public void friendList(Attachment attach) {
		attach.setOutput(attach.attachedUser().getFriendListJSON());
	}
	
	public void score(Attachment attach)	{
		attach.setOutput(Integer.toString(attach.attachedUser().getScore()));
	}
	
	public void top(Attachment attach,ArrayList<User> allUsers) {		
		ArrayList<String> target= attach.attachedUser().getFriendList();
		target.add(attach.attachedUser().getName());
		ArrayList<User> utentiamici= new ArrayList<User>();
		Iterator<User> uIter=allUsers.iterator();
		User user;
		User tmp1;
		StringBuilder opBuilder=new StringBuilder();
		int p=0;
		while(uIter.hasNext() && p!=target.size()) {			//ottimizzabile
			user=uIter.next();
			if(target.contains(user.getName())) {
				for(int k=0;k<utentiamici.size();k++) {
					if(utentiamici.get(k).getScore()<user.getScore()) {
						tmp1=utentiamici.get(k);
						utentiamici.add(k,user);
						user=tmp1;
					}
				}
				utentiamici.add(user);
				p++;
			}
		}
		opBuilder.append("{"
				+ "\"chart\":[");
		for(int i=0;i<utentiamici.size();i++) {
			opBuilder.append(""
					+ "{\"name\":\""+utentiamici.get(i).getName()+"\",\"score\":\""+utentiamici.get(i).getScore()+"\"}");
			if(i!=utentiamici.size()-1)	opBuilder.append(",");
		}
		opBuilder.append("]"
				+ "}");
		attach.setOutput(opBuilder.toString());
	}	

	public void challenge(SelectionKey key,  String challenged,ArrayList<User> allUsers,DatagramSocket serverUDP) {
		Attachment attach= (Attachment) key.attachment();
		if(!attach.attachedUser().checkFriend(challenged)) {		
			attach.setOutput("-0");
		}
		else {
			int i;
			for(i=0;i<allUsers.size();i++) {
				if(allUsers.get(i).equals(challenged)) {
					if(allUsers.get(i).isNotWaiting()) {		
						if(allUsers.get(i).checkstatus()) {
							allUsers.get(i).addChallenger(attach.attachedUser().getName());
							attach.attachedUser().createChallenge(challenged,key);
							UDPServerSender udp= new UDPServerSender(serverUDP,attach.attachedUser().getName(),allUsers.get(i).getUDPPort());
							Thread udpthread= new Thread(udp);
							udpthread.start();
							i=allUsers.size();
						}
						else {
							attach.setOutput("-4");
						}
					}
					else {
						attach.setOutput("-1");
					}
				}
			}
		}
	}

	public void acceptChallenge(SelectionKey key, String challenger,ArrayList<User> allUsers,Lock dictionaryLock,Random random, ArrayList<String> dictionary) {
		Attachment attach= (Attachment) key.attachment();
		if(attach.attachedUser().checkChalleger(challenger)) {
			for(int i=0;i<allUsers.size();i++) {				
				if(allUsers.get(i).equals(challenger)) {		
					attach.attachedUser().removeChallenger(challenger);
					attach.attachedUser().setWaitingTranslator();
					attach.attachedUser().registerChallenge(allUsers.get(i).getOngoingChallenge());
					allUsers.get(i).setWaitingTranslator();
					allUsers.get(i).acceptedChallenge(key);
					Translator translator=new Translator(allUsers.get(i).getOngoingChallenge(),dictionary,dictionaryLock,random);		
					Thread threadTranslator= new Thread(translator);
					threadTranslator.start();
					break;
				}
			}
		}
		else	attach.setOutput("-1:"+challenger+" non ti ha sfidato o la sfida è scaduta");
	}
	
	public void denyChallenge(Attachment attach, String challenger,ArrayList<User> allUsers) {
		if(attach.attachedUser().checkChalleger(challenger)) {
			for(int i=0;i<allUsers.size();i++) {				
				if(allUsers.get(i).equals(challenger)) {		
					allUsers.get(i).declinedChallenge();
					attach.attachedUser().removeChallenger(challenger);
					attach.setOutput("sfida di "+challenger+"  rifiutata");
					i=allUsers.size();
				}
			}
		}
		else	attach.setOutput(challenger+" non ti ha sfidato o la sfida è scaduta");
	}
	
	public void	answer(Attachment attach,String answer) {
		Challenge cinfo= attach.attachedUser().getOngoingChallenge();
		if(cinfo.checkTimeOver())	cinfo.challengeEnd(attach);
		else	cinfo.answer(attach,answer);
	}


	public void run() {
		Thread saver=null;
		long timestampsave=System.currentTimeMillis();
		int command=-1;
		File[] allUserFile = null;
		DatagramSocket serverUDP= null;
		File statesDirectory;
		File dictionaryFile;
		ArrayList<User> allUsers= new ArrayList<User>();
		Selector selector;
		ArrayList<String> dictionary=new ArrayList<String>();		
		Lock dictionaryLock= new ReentrantLock();
		Random random= new Random();
		statesDirectory= new File(directoryPath+"\\states");				//sezione di acquisizione dati
		dictionaryFile= new File(directoryPath+"\\dictionary.txt");					//windows only
		JSONParser parser= new JSONParser();
		File[] allSavedStates= statesDirectory.listFiles();
		boolean stopDirSearch= false;
		boolean stopAquisition= false;
		int dirCounter= 1;
		File lastUpdate= null;
		try {			//controllo la presenza di directory
			lastUpdate=allSavedStates[0];
		}
		catch(IndexOutOfBoundsException e) {
			System.out.println("nessun file di stato salvato");
			stopDirSearch=true;
			stopAquisition=true;
		}
		while(!stopDirSearch) {
			try {
				if(Long.parseLong(lastUpdate.getName()) < Long.parseLong(allSavedStates[dirCounter].getName())) {		//ricerco ultimo stato disponibile
					lastUpdate=allSavedStates[dirCounter];
					dirCounter++;
					}
				}
			catch(IndexOutOfBoundsException e) {
				break;
			}				
		}
		if(!stopAquisition) {
			allUserFile=lastUpdate.listFiles();
		}
		int usercounter=0;
		while(!stopAquisition) {
			try {		
				Object obj = parser.parse(new FileReader(allUserFile[usercounter]));
				User tmp=new User((JSONObject) obj);
				allUsers.add(tmp);
				usercounter++;
			}
			catch(IndexOutOfBoundsException e) {
				stopAquisition=true;
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				System.out.println("errore input output durante acquisizione dati utenti");
				e.printStackTrace();
			} catch (ParseException e) {
				System.out.println("errore di parsing durante acquisizione dati utenti");
				e.printStackTrace();
			}
			
		}		//fine acquisizione dati persistenti su utenti
			BufferedReader br = null;
			try {
				br = new BufferedReader(new FileReader(dictionaryFile));
			} 
			catch (FileNotFoundException e) {
				System.out.println("non c'e' il file contenente le parole!!");
				e.printStackTrace();
			} 
			String parola; 
			try {
				while ((parola= br.readLine())!= null) {
					dictionary.add(parola);
				}
			} 
			catch (IOException e) {
				e.printStackTrace();
			} 		//fine acquisizione dizionario
		boolean flagrmi=false;
		while(!flagrmi) {	
			try {
				RemoteRegister registratore = new RemoteRegister(allUsers);
				LocateRegistry.createRegistry(5001);		//porta di prova
				Registry r=LocateRegistry.getRegistry(5001);
				r.rebind("registrazione", registratore); 
				System.out.println("Registrazione tramite rmi disponibile");
				flagrmi=true;
			}
			catch(Exception e) {
				e.printStackTrace();
			}	//fine inizializzazione oggetto per registrazione rmi
		}
		SaveBeforeShutDown dataSaver= new SaveBeforeShutDown(allUsers,directoryPath+"\\states");		//salvo i dati quando esco
		Runtime.getRuntime().addShutdownHook(new Thread(dataSaver));
		try{
			serverUDP= new DatagramSocket();
			System.out.println("udp lato server pronta");
		}
		catch(Exception e){
			e.printStackTrace();
			System.out.println("non ho inizializzato udp");
		}
		try{											// avvio selettore
			ServerSocketChannel serverChannel=ServerSocketChannel.open();
			ServerSocket ss=serverChannel.socket();
			InetSocketAddress address= new InetSocketAddress(7775);
			ss.bind(address);
			serverChannel.configureBlocking(false);
			selector=Selector.open();
			serverChannel.register(selector,SelectionKey.OP_ACCEPT);
		}
		catch(IOException e) {
			e.printStackTrace();
			return;
		}
		while(true) {		//cntrl c da terminale per uscire
			try {
				selector.select();
			}
			catch(IOException e){
				e.printStackTrace();
				break;
			}
			Set<SelectionKey> readyKeys=selector.selectedKeys();
			Iterator<SelectionKey> iterator= readyKeys.iterator();
			while(iterator.hasNext()) {
				SelectionKey key=iterator.next();
				iterator.remove();
				SocketChannel client;		
				try {
					if(key.isAcceptable()) {
						ServerSocketChannel server= (ServerSocketChannel) key.channel();
						client=server.accept();
						System.out.println("accetta connessione da: "+client);
						client.configureBlocking(false);
						client.register(selector, SelectionKey.OP_READ, new Attachment("NOTLOGGED"));
					}
					else if(key.isReadable()) {
						client=(SocketChannel) key.channel();
						ByteBuffer input=(ByteBuffer) ByteBuffer.allocate(1000);
						int r=0;
						while(r<80)	r=+client.read(input);		
						String msg= new String(input.array());
						Attachment attachment=(Attachment) key.attachment();
						String[] splittedInput=msg.split(":");
							command= Integer.parseInt(splittedInput[0]);
						if((command>0) && (attachment.attachedUser()==null)){		//user non loggato che non chiede il login (non dovrebbe essere possibile a meno di bug)
							attachment.setOutput("prima è richiesto il login");
							client.register(selector, SelectionKey.OP_WRITE,attachment);	
						}
						else {
							switch(command){
								case 0:		//login
									try {
										client.register(selector, SelectionKey.OP_WRITE,login(splittedInput[1],splittedInput[2],allUsers));
									}
									catch(ArrayIndexOutOfBoundsException e) {
										attachment.setOutput("nickname o password mancanti");
									}
									break;
								case 1:	//logOut
									logout(attachment);
									client.register(selector, SelectionKey.OP_WRITE,attachment); 
									break;
								case 2:	//AddFriend
									try {
										addFriend(attachment,splittedInput[1],allUsers);
									}
									catch(ArrayIndexOutOfBoundsException e) {
										attachment.setOutput("nome amico da aggiungere non presente nel messaggio");
									}
									client.register(selector, SelectionKey.OP_WRITE,attachment); 
									break;
								case 3:	//FriendList
									friendList(attachment);
									client.register(selector, SelectionKey.OP_WRITE,attachment); 
									break;
								case 4:	//Challenge		
									challenge(key, splittedInput[1],allUsers,serverUDP);
									client.register(selector, SelectionKey.OP_WRITE,attachment);
									break;	
								case 5:	//Score
									score(attachment);
									client.register(selector, SelectionKey.OP_WRITE,attachment); 
									break;
								case 6:	//Top
									top(attachment,allUsers);
									client.register(selector, SelectionKey.OP_WRITE,attachment); 
									break;
								case 7:	//risposta alla sfida
									answer(attachment,splittedInput[1]);
									client.register(selector, SelectionKey.OP_WRITE,attachment);
									break;
								case 8:	//acceptChallenge
									acceptChallenge(key,splittedInput[1],allUsers,dictionaryLock,random,dictionary);
									client.register(selector, SelectionKey.OP_WRITE,attachment);
									break;
								case 9:	//denyChallenge
									denyChallenge(attachment,splittedInput[1],allUsers);
									client.register(selector, SelectionKey.OP_WRITE,attachment); 
									break;
								case 10:	//comunicazione porta udp
									attachment.attachedUser().setUDPPort(Integer.parseInt(splittedInput[1].trim()));
									System.out.println(splittedInput[1]);
									attachment.setOutput("--");
									client.register(selector, SelectionKey.OP_WRITE,attachment); 
									break;
								default:
									System.out.println(msg+" da "+key);
									attachment.setOutput("-ERROR-");
									client.register(selector, SelectionKey.OP_WRITE,attachment); 
							}
						}
					}
					else if(key.isWritable()) {	
						client=(SocketChannel) key.channel();		
						Attachment attachment= (Attachment) key.attachment();
						try {
							if(attachment.attachedUser().isWaitingOtherPlayer()) {		//flag a 6
								ByteBuffer output=(ByteBuffer) attachment.getOutput();
								while(output.hasRemaining())	client.write(output);
								attachment.attachedUser().setWaitingTimeend();//aspetta la fine challenge
								client.register(selector,SelectionKey.OP_WRITE,attachment);
							}
							if(attachment.attachedUser().isWaitingTimeend()) {		//3
								if(attachment.attachedUser().getOngoingChallenge().checkTimeOver()) {
									attachment.attachedUser().getOngoingChallenge().challengeEnd(attachment);
									client.register(selector,SelectionKey.OP_WRITE,attachment);
								}
								else {
									client.register(selector,SelectionKey.OP_WRITE,attachment);
								}
							}
							if(attachment.attachedUser().isWaitingTranslator()) {			//flag a 5
								client.register(selector,SelectionKey.OP_WRITE,attachment);		
							}
							if(attachment.attachedUser().isDeclined()) {		//flag a 2
								attachment.attachedUser().unsetflagChallengeStatus();
								attachment.setOutput("sfida rifiutata!");
							}	
							if(attachment.attachedUser().isWaiting()) {		//flag a 1,controllo tempo;
								if(!attachment.attachedUser().checkTimeChallenge()) {		//tempo scaduto, il metodo resetta la flag
									for(int i=0;i<allUsers.size();i++) {
										if(allUsers.get(i).equals(attachment.attachedUser().whoamichallenging())) {
											allUsers.get(i).removeChallenger(attachment.attachedUser().getName());
										}
									}
									attachment.setOutput("tempo scaduto");
									attachment.attachedUser().unsetflagChallengeStatus();
								}
								else	client.register(selector,SelectionKey.OP_WRITE,attachment);		//sta ancora aspettando una risposta
							}
							if(attachment.attachedUser().isChallenging()) {		//flag a 4
								ByteBuffer output=(ByteBuffer) attachment.getOutput();
								while(output.hasRemaining())	client.write(output);
								client.register(selector,SelectionKey.OP_READ,attachment);
							}
							if(attachment.attachedUser().isNotWaiting()) {			//flag a 0
								ByteBuffer output=(ByteBuffer) attachment.getOutput();
								while(output.hasRemaining())	client.write(output);
								client.register(selector,SelectionKey.OP_READ,attachment);
							}
						}
						catch(NullPointerException e) {
							ByteBuffer output=(ByteBuffer) attachment.getOutput();
							while(output.hasRemaining())	client.write(output);
							client.register(selector,SelectionKey.OP_READ,attachment);
						}
					}
				}
				catch(IOException e) {			//quando il client chiude il programma
					System.out.println("chiusa connessione con: "+key);
					key.cancel();
					try {
						((Attachment) key.attachment()).attachedUser().logout();
						key.channel().close();						
					}
					catch(Exception ex) {}
				}
			}
			if((System.currentTimeMillis()-timestampsave)>10000) {
				try {
					if(!saver.isAlive()) {
						saver=new Thread(dataSaver);
						saver.start();
						timestampsave=System.currentTimeMillis();
					}
				}
				catch(Exception e) {		//per la prima volta
					saver=new Thread(dataSaver);
					saver.start();
					timestampsave=System.currentTimeMillis();
				}
			}
		}
	}
}
