package progetto_reti;

public class MainTestServer {
	public static void main(String[] args) {
		Server server= new Server("C:\\Users\\nicco\\Desktop\\progetto reti\\esempio cartella per server");
		System.out.println("inizio test server");
		server.run();
	}
}
