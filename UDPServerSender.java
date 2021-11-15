package progetto_reti;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;

public class UDPServerSender implements Runnable {
	DatagramSocket datasocket;
	String name;
	int port;
	public UDPServerSender(DatagramSocket datasocket,String name, int port) {
		this.datasocket=datasocket;
		this.name=name;
		this.port=port;
	}
	public void run() {
		name.getBytes();
		InetAddress address = null;
		try {
			address = InetAddress.getByName("127.0.0.1");
		} catch (UnknownHostException e1) {
			System.out.println("problemi con l'inidirizzo inet udp del server");
			e1.printStackTrace();
		}
		DatagramPacket packet= new DatagramPacket(name.getBytes(),name.length(),address,port);
		try {
			datasocket.send(packet);
		} catch (IOException e) {
			System.out.println("non sono riuscito ad inviare il paccheto di "+name);
			e.printStackTrace();
		}
	}

}
