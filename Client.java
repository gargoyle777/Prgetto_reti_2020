package progetto_reti;

import java.nio.channels.*;
import java.io.IOException;
import java.net.*;
import java.util.Iterator;
import java.util.Scanner;
import java.rmi.*;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.nio.ByteBuffer;

public class Client implements Runnable{
	private String username=null;
	private String psw=null;
	Scanner scanner=new Scanner(System.in);
	SocketAddress addressTCP = new InetSocketAddress("127.0.0.1", 7775); 
	SocketAddress addressUDP = new InetSocketAddress("127.0.0.1", 7778); 
	SocketChannel socketChannel;
	DatagramSocket datagramSocket;
	boolean flagL=false;
	boolean flagExit=false;
	boolean flagConnection=false;
	boolean flagUDP=false;
	long lag;
	public Client() {
		}
	
	public boolean login(String username, String psw, int porta) throws IOException {		
		String request= new String("0:"+username+":"+psw);
		ByteBuffer bytereq;
		ByteBuffer bbresponse=ByteBuffer.allocate(100);
		bytereq=ByteBuffer.wrap(fixLength(request).getBytes());
		socketChannel.write(bytereq); 
		socketChannel.read(bbresponse);
		String response=new String(bbresponse.array());
		if(response.contains("successo")) {
			System.out.println("login effettuato, connesione udp per le sfide in corso. porta: "+porta);
			bytereq=ByteBuffer.wrap(fixLength(("10:"+porta)).getBytes());		
			flagUDP=false;
			while(!flagUDP) {
				try {
					bbresponse=ByteBuffer.allocate(30);
					lag=System.currentTimeMillis();
					socketChannel.write(bytereq);
					socketChannel.read(bbresponse);
					lag=System.currentTimeMillis()-lag;
					if(new String(bbresponse.array()).contains("--")) {
						flagUDP=true;
					}
					else {
						System.out.println("risposta inaspettata nel settare l'udp: "+new String(bbresponse.array()));
					}
				} catch (IOException e) {
					System.out.println("errore nella comuicazione di inizio sessione, riprovo");
					e.printStackTrace();
				} 
			}
			System.out.println("lag nella comunicazione udp: "+lag);
			return true;
		}
		else {
			System.out.println(response);
			return false;
		}	
	}
	
	public String fixLength(String original) {
		String fixed=original+":";
		while(fixed.length()<80) {
			fixed=fixed+"o";
		}
		return fixed;
	}
	
	public boolean logout() throws IOException{
		System.out.println("logout in corso");
		ByteBuffer bytereq= ByteBuffer.wrap(fixLength("1:").getBytes());
		socketChannel.write(bytereq); 
		ByteBuffer bbresponse=ByteBuffer.allocate(25);
		socketChannel.read(bbresponse);
		if(new String(bbresponse.array()).contains("-LOGOUT-")) {
			return false;
		}
		else {
			System.out.println("errore nel logout");
			return true;
		}
	}
	
	public void addFriend(String nickToAdd) throws IOException {
		System.out.println("inoltro la richiesta di aggiungere "+nickToAdd);
		ByteBuffer bytereq= ByteBuffer.wrap(fixLength(("2:"+nickToAdd)).getBytes());
		socketChannel.write(bytereq);
		ByteBuffer bbresponse=ByteBuffer.allocate(1000);
		socketChannel.read(bbresponse);
		System.out.println(new String(bbresponse.array()));
	}
	
	public void friendList() throws IOException {
		System.out.println("richiesta della lista amici in corso");
		ByteBuffer bytereq= ByteBuffer.wrap(fixLength("3:").getBytes());
		socketChannel.write(bytereq);
		ByteBuffer bbresponse=ByteBuffer.allocate(4000);
		socketChannel.read(bbresponse);
		JSONParser parser= new JSONParser();
		JSONObject jsonobj;
		boolean flagfriend=false;
		try {
			jsonobj = (JSONObject) parser.parse((new String(bbresponse.array()).trim()));
			JSONArray rawfriendlist= (JSONArray) jsonobj.get("friendlist");
			Iterator<String> iterator= rawfriendlist.iterator();
			while(iterator.hasNext()) {
				System.out.println(iterator.next());
				flagfriend=true;
			}
		} 
		catch (ParseException e) {
			System.out.println("errore di parsing, dal server ho ricevuto:"+new String(bbresponse.array())); 
		}
		if(!flagfriend) {
			System.out.println("non hai ancora amici!");
		}
	}
	
	public void challenge(String nickChallenged) throws IOException{
		System.out.println("inoltro la sfida a: "+nickChallenged);
		ByteBuffer bytereq= ByteBuffer.wrap(fixLength(("4:"+nickChallenged)).getBytes());
		socketChannel.write(bytereq); 
		System.out.println("sfida inviata correttamente, resta in attesa");		
		ByteBuffer bbresponse=ByteBuffer.allocate(100);
		socketChannel.read(bbresponse);			
		String response= new String(bbresponse.array());
		String[] serveranswer=response.trim().split(":");
		serveranswer[0]=serveranswer[0];
		switch(serveranswer[0]) {
		case "-0":
			System.out.println(nickChallenged+" non è nella tua lista amici");
			break;
		case "-1":
			System.out.println(nickChallenged+" sta sfidando qualcuno");
			break;
		case "-2":
			System.out.println(nickChallenged+" ha rifiutato la tua sfida");
			break;
		case "-5":
			System.out.println("errore nel setup, sfida annullata");
			break;
		case "-4":
			System.out.println(nickChallenged+" non è online");
			break;
		case "-3":
			gameChallenge(serveranswer[1]);
			break;
		default:
			System.out.println(response);
		}
	}
	
	public void personalScore() throws IOException {
		System.out.println("richiesta punteggio in corso");
		ByteBuffer bytereq= ByteBuffer.wrap(fixLength("5:").getBytes());
		socketChannel.write(bytereq);
		ByteBuffer bbresponse=ByteBuffer.allocate(400);
		socketChannel.read(bbresponse);
		System.out.println(new String(bbresponse.array()));
	}
	
	public void top() throws IOException{
		System.out.println("richiesta della classifica in corso");
		ByteBuffer bytereq= ByteBuffer.wrap(fixLength("6:").getBytes());
		socketChannel.write(bytereq);
		ByteBuffer bbresponse=ByteBuffer.allocate(1000);
		socketChannel.read(bbresponse);
		JSONParser parser= new JSONParser();
		Object objanswer;
		JSONObject jsonobj;
		JSONObject jsonuser;
		try {
			objanswer= parser.parse((new String(bbresponse.array())).trim());
			jsonobj=(JSONObject) objanswer;
			JSONArray rawchart= (JSONArray) jsonobj.get("chart");
			Iterator<JSONObject> iterator= rawchart.iterator();
			while(iterator.hasNext()) {
				jsonuser=iterator.next();
				System.out.println(jsonuser.get("name")+" con punteggio: "+jsonuser.get("score"));
			}
		}
		catch (ParseException e) {
			System.out.println("errore di parsing, dal server ho ricevuto: "+new String(bbresponse.array())); 
		}
	}
	
	public void acceptChallenge(String name) throws IOException{
		System.out.println("accetto la sfida di "+name);
		ByteBuffer bytereq= ByteBuffer.wrap(fixLength(("8:"+name)).getBytes());
		socketChannel.write(bytereq);			
		ByteBuffer bbresponse=ByteBuffer.allocate(100);
		socketChannel.read(bbresponse);
		String[] serveranswer=new String(bbresponse.array()).split(":");
		if(serveranswer[0].equals("-1"))	System.out.println(serveranswer[1]);		//sfida scaduta o mai esistita
		if(serveranswer[0].equals("-3"))	gameChallenge(serveranswer[1]);
		if(serveranswer[0].trim().equals("-5"))	System.out.println("errore di setup, sfida annullata");
	}
	
	public void gameChallenge(String firstword) {
		ByteBuffer bbinput=ByteBuffer.allocate(200);
		String tmpanswer,tmpreceived;
		String[] tmpsplitted = new String[2];
		tmpsplitted[0]="30L";
		System.out.println("Ha inizio la sfida!");
		System.out.println("parola 1/6: "+firstword);
		tmpanswer=scanner.nextLine();
		try {
			ByteBuffer tmpbb=ByteBuffer.wrap(fixLength(("7:"+tmpanswer)).getBytes());
			socketChannel.write(tmpbb);
		} catch (IOException e) {
			e.printStackTrace();
		}
		int i=2;
		while(true) {
			try {
				bbinput=ByteBuffer.allocate(200);
				socketChannel.read(bbinput);
				tmpreceived=new String(bbinput.array());
				tmpsplitted=tmpreceived.split(":");
				if(tmpsplitted[0].equals("-2")) {
					break;
				}
				if(tmpsplitted[0].equals("-1")) {
					break;
				}
				System.out.println("parola "+i+"/6: "+new String(bbinput.array()));
				tmpanswer=scanner.nextLine();
				socketChannel.write(ByteBuffer.wrap(fixLength(("7:"+tmpanswer)).getBytes()));
				i++;
			}
			catch(Exception e) {
				e.printStackTrace();
				break;
			}
		}
		if(tmpsplitted[0].equals("-2")) {
			System.out.println(tmpsplitted[1]);
		}
		if(tmpsplitted[0].equals("-1")) {
			System.out.println(tmpsplitted[1]);
			bbinput=ByteBuffer.allocate(200);
			try {
				socketChannel.read(bbinput);
			} catch (IOException e) {
				e.printStackTrace();
			}
			tmpreceived=new String(bbinput.array());
			tmpsplitted=tmpreceived.split(":");
			System.out.println(tmpsplitted[1]);
		}
	}
	
	public void denyChallenge(String name) throws IOException{
		System.out.println("rifiuto la sfida di "+name);
		ByteBuffer bytereq= ByteBuffer.wrap(fixLength("9:"+name).getBytes());
		socketChannel.write(bytereq);
		ByteBuffer bbresponse=ByteBuffer.allocate(100);
		socketChannel.read(bbresponse);
		System.out.println(new String(bbresponse.array()));
	}
	
	public void register(String username, String psw){
		RemoteRegisterInterface ssregister;
		Remote remoteobj;
		try {
			Registry r= LocateRegistry.getRegistry(5001); 
			remoteobj= r.lookup("registrazione");
			ssregister= (RemoteRegisterInterface) remoteobj;
			System.out.println(ssregister.register(username,psw));
		}
		catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	public void run() {
		System.out.println("client avviato");
		while(!flagConnection) {		
			try{
				socketChannel = SocketChannel.open();
				socketChannel.connect(addressTCP);		
				flagConnection=true;
			}
			catch(Exception e) {
				System.out.println("connessione al server fallita, riprovo");
				try {
					Thread.sleep(3000);
				} catch (InterruptedException e1) {
					e1.printStackTrace();
				}
			}
			flagConnection=socketChannel.isConnected();
		}
		int porta=4000;
		boolean flagDoor=false;
		while(!flagDoor) {
			try {
				datagramSocket=new DatagramSocket(porta);
				flagDoor=true;
			}
			catch(BindException e) {
				System.out.println("la porta "+porta+" e' occupata, riprovo");
				porta++;
			}
			catch (SocketException e) {
				System.out.println("creazione udp per sfide fallita, riprovo");
				e.printStackTrace();
			}
		}
		ClientUDPReceiver udphandler= new ClientUDPReceiver(datagramSocket);
		Thread udphandlerThread= new Thread(udphandler);
		udphandlerThread.start();
		System.out.println("Benvenuto su Word Quizzle");
		System.out.println("per uscire in qualunque momento chiudi semplicemente il programma");
		System.out.println("ritardo nello scambio iniziale in tcp: "+lag);
		String menuEntry;
		String[] splittedEntry;
		String tmp;
		while(true) {
			System.out.println("vuoi loggare (L) o registrarti (R)?");
			menuEntry=scanner.nextLine();
			if(menuEntry.equalsIgnoreCase("L")) {
				try {
					System.out.println("inserisci il tuo username(non c'e' differenza tra maiuscole e minuscole):");
					username=scanner.nextLine();
					System.out.println("inserisci la tua password:");
					psw=scanner.nextLine();
					flagL=login(username.toLowerCase(),psw,porta); 
				}
				catch (IOException e) {
					System.out.println("errore di trassmissione, riprova");
				}
			}
			if(menuEntry.equalsIgnoreCase("R")) {
				System.out.println("inserisci il nickname (non c'e' differenza tra maiuscole e minuscole)");
				username=scanner.nextLine();
				System.out.println("inserisci la password:");
				psw=scanner.nextLine();
				register(username,psw);
			}
			if(menuEntry.equalsIgnoreCase("X")){
				flagL=false;
				try {
					socketChannel.close();
					udphandlerThread.interrupt();
					datagramSocket.close();
				} catch (IOException e) {
					System.out.println("errore di IO nella chiusura del collegamento, chiudo comunque");
				}
			}
			while(flagL) {
				System.out.println("cosa vuoi fare: aggiungere un amico (A), vedere la tua lista amici (F), sfidare un amico (C),"
						+"/n"+ " vedere il tuo punteggio (S), vedere la classifica (T) o fare il logout (O)");
				menuEntry=scanner.nextLine().toLowerCase();
				splittedEntry=menuEntry.split(" ");
				try {
					switch(splittedEntry[0].trim()) {
					case "a":
						System.out.println("chi vuoi aggiungere?");
						tmp=scanner.nextLine();					
						addFriend(tmp.toLowerCase());
						break;
					case "f":
						friendList();
						break;
					case "c":
						if(!splittedEntry[1].equalsIgnoreCase(username)) {
							challenge(splittedEntry[1].toLowerCase());
						}
						else System.out.println("non puoi sfidare te stesso");
						break;
					case "s":
						personalScore();
						break;
					case "t":
						top();
						break;
					case "o":		
						flagL=logout();
						break;
					case "yes":
						acceptChallenge(splittedEntry[1].toLowerCase());
						break;
					case "no":
						denyChallenge(splittedEntry[1].toLowerCase());
						break;
					default:
						System.out.println("comando non riconosciuto, riprova");
					}
				}
				catch(IndexOutOfBoundsException e) {
					System.out.println("manca parte del comando");
				}
				catch(IOException e) {
					System.out.println("errore di input output, riprova");
				}
			}
		}
	}
}
