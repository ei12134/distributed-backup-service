package core;

public class Dbs {

	public static String mcAddress;
	public static int mcPort;
	public static String mdbAddress;
	public static int mdbPort;

	public static volatile Peer peer;

	public static void main(String[] args) {

		if (args.length == 5) {
			try {
				int peerID = Integer.parseInt(args[0]);
				mcAddress = args[1];
				mcPort = Integer.parseInt(args[2]);
				mdbAddress = args[3];
				mdbPort = Integer.parseInt(args[4]);
				
				Dbs.peer = new Peer(peerID);
				Dbs.peer.initDatabase();
				Dbs.peer.initCrypto();
				Dbs.peer.startConnections();

			} catch (NumberFormatException e) {
				System.err.println("Error: Invalid argument.");
				System.exit(1);
			} catch (Exception e) {
				System.err.println("Error: " + e.getMessage());
				System.exit(1);
			}
		} else
			System.out
					.println("Usage: java Dbs <peerId> <mcAddress> <mcPort> <mdbAddress> <mdbPort>");
	}
}
