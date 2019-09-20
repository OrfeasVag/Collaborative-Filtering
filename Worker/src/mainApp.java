import java.net.Socket;

//Worker
public class mainApp {
	public static void main(String[] args) {
		System.out.println("Starting Worker . . .");
		openClient();
	}

	private static void openClient() {
		Socket requestSocket = null;
		try {
			requestSocket = new Socket("127.0.0.1", 4200); // Server's ip,port

		} catch (Exception e) {
			System.out.println("Error -- openClient.");
		}
		Worker worker = new Worker(requestSocket);
		worker.initialize();


	}

}