package core;

public class DbsTest {

	public static int serverId;
	public static String mcAddress;
	public static int mcPort;
	public static String mdbAddress;
	public static int mdbPort;

	public static volatile Peer peer;

	public static void main(String[] args) throws Exception {

		Dbs.mcAddress = "225.4.5.6";
		Dbs.mcPort = 4000;
		Dbs.mdbAddress = "225.4.5.6";
		Dbs.mdbPort = 4001;

		if (!(args.length > 0)) {
			System.out.println("Usage: java DbsTest <peerId>");
			System.exit(1);
		}
		try {
			Dbs.peer = new Peer(Integer.parseInt(args[0]));
			Dbs.peer.initDatabase();
			Dbs.peer.initCrypto();
			Dbs.peer.startConnections();

		} catch (Exception e) {
			System.out.println("Error: " + e.getMessage());
			System.exit(1);
		}
	}
}
