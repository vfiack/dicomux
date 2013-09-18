import java.io.OutputStream;
import java.net.Socket;


public class SocketTest {
	public static void main(String[] args) throws Exception {
		Socket s = new Socket("hux14", 9090);
		OutputStream os = s.getOutputStream();
		System.out.println("socket ouverte");
		System.in.read();
		os.write(0);
		os.flush();
		System.out.println("flush ok");
	}
}
