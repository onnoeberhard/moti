package core.communication;

import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import core.Server;
import core.Game.OnlinePlayer;
import core.Game.PlayerTask;
import core.Logger.State;
import core.communication.PacketHandler.Packet;
import core.communication.PacketHandler.PacketType;

public class ConnectionHandler {
	public PacketHandler packethandler = new PacketHandler();

	public ConnectionHandler() {
		try {
			@SuppressWarnings("resource")
			final ServerSocket soc = new ServerSocket(4444);
			final ExecutorService exeservice = Executors.newCachedThreadPool();

			exeservice.submit(new Runnable() {
				@Override
				public void run() {
					while (true) {
						Socket client = null;
						try {
							client = soc.accept();

							Server.logger.log("Client verbunden mit ip: " + client.getInetAddress(), State.INFO);

							Connector c = new Connector(client);
							Future<?> task = exeservice.submit(c);
							c.task = task;

						} catch(Exception e) {
							e.printStackTrace();
						}
					}
				}
			});
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(2);
		}		
	}



	


	public class Connector implements Runnable {
		public Socket client;
		public Scanner     in;
		public PrintWriter out;

		private Future<?> task;
		private boolean versionok = false;

		public String playerkey = "";

		private long lastlivesig = 0L;
		private boolean pingpacketsend = false;

		public Connector(Socket soc) {
			client = soc;
			try {
				in  = new Scanner( client.getInputStream() );
				out = new PrintWriter( client.getOutputStream(), true );
			} catch (Exception e) {}
		}
		@Override
		public void run() {
			try {
				while(!client.isClosed()) {
					try {
						HandlePacket( packethandler.take_pack(in.nextLine()) , this);
						resetPingCounter();
					} catch (Exception e) {}

				}
				if(client.isClosed()) {
					Server.playermanager.removePlayerbyKey(playerkey);
					Server.logger.log("Connection to Player lost!" , State.WARNING);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		public void resetPingCounter () {
			lastlivesig = System.currentTimeMillis();
			pingpacketsend = false;
		}

		public void isThere() {
			if((System.currentTimeMillis() - lastlivesig) / 1000 > 10 & !pingpacketsend) {//länger als 10 sec nichts gehört -> sende ping pack
				sendPacket(packethandler.createPacket(PacketType.PING, ""));
				Server.logger.log("Player is not sending! Send ping Packet", State.WARNING);
				pingpacketsend = true;
			} else if((System.currentTimeMillis() - lastlivesig) / 1000 > 17) {//länger als 17 sec nicht gehört trenne verbindung
				Server.logger.log("Player Timed out", State.WARNING);
				disconnect();
			}
		}

		public void disconnect(String reson) {
			sendPacket(packethandler.createPacket(PacketType.DISCONNECT, reson));
			CharSequence ip = client.getInetAddress() + "";
			try {client.close();} catch(Exception e) {}
			Server.logger.log("Player Disconnected: " + ip + " Reson: " + reson, State.INFO);
			cancel();
		}

		public void disconnect() {
			disconnect("");
		}

		public void versionisok() {
			versionok = true;
		}

		public boolean isversionok() {
			return versionok;
		}

		public void sendPacket(Packet packet) {
			out.println(Server.conhand.packethandler.ready_pack(packet));
		}

		public void setFuture(Future<?> t) {
			task = t;
		}

		public void cancel() {
			task.cancel(true);
		}
	}
}

