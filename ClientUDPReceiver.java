package progetto_reti;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;

public class ClientUDPReceiver implements Runnable {
	DatagramSocket datagramSocket;
	boolean flagto=true;
	byte[] ba=new byte[40];
	DatagramPacket dataPacket= new DatagramPacket(ba, 40);
	public ClientUDPReceiver(DatagramSocket datagramSocket) {
		this.datagramSocket=datagramSocket;
	}
	public void run() {
		while(flagto) {
			try {
				datagramSocket.setSoTimeout(200);
				flagto=false;
			} catch (SocketException e) {
				flagto=true;
				System.out.println("non riesco a settare il timeout udp, riprovo");
				e.printStackTrace();
			}
		}
		while(true) {
			try {
				datagramSocket.receive(dataPacket);		
				System.out.println(new String(ba)+" ti ha sfidato! Hai 16 secondi per decidere"
						+ "Inserisci (yes nickname_sfidante) per accettare o (no nickname_sfidante) per rifiutare, "
						+ "oppure continua tranquillamente a navigare il menu");
			} 
			catch(Exception e) {
				try {
					Thread.sleep(3000);
				} catch (InterruptedException e1) {
					break;
				}
			}
		}
	}

}
