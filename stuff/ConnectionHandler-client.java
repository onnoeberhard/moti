package core.communication;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import core.Sobere;
import core.Logger.State;
import core.communication.PacketHandler.Packet;
import core.communication.PacketHandler.PacketType;

public class ConnectionHandler {

	Socket soc = null;
	Scanner in = null;
	PrintWriter out = null;
	ExecutorService exeservice = Executors.newSingleThreadExecutor();
	public PacketHandler packethandler = new PacketHandler();

	public ConnectionHandler() {
		try {
			soc = new Socket("127.0.0.1", 4444);
			in = new Scanner(soc.getInputStream());
			out = new PrintWriter(soc.getOutputStream(), true);

			Sobere.logger.log("Connection established", State.INFO);

			exeservice.submit(new recivor(in));

		} catch(Exception e) {
			Sobere.logger.log("keine Verbindung", State.ERROR);
			System.exit(2);
		}
	}

	public void send(Packet pack) {
		out.println(packethandler.ready_pack(pack));
	}


	public void disconnect () {
		if(soc != null) {
			try {
				soc.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	private class recivor implements Runnable {
		private Scanner s;
		@Override
		public void run() {
			while(true) {
				try {
					String gotten = s.nextLine();
					HandlePacket(packethandler.take_pack(gotten));
				} catch (Exception e) {
				}
			}
		}
		public recivor(Scanner input) {
			s = input;
		}
	}
}

