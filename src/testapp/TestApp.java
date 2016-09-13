package testapp;

import java.io.IOException;
import java.rmi.ConnectException;
import java.rmi.NotBoundException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import rmi.RMIInterface;
import utils.Constants;
import utils.Constants.protocolType;

public class TestApp {

	private static String peerAp;
	private static String filePath;
	private static protocolType protocol;
	private static int spaceToReclaim;
	private static int replicationDegree;
	private static Registry registry = null;

	public TestApp() {
	}

	private static boolean parseArgs(String[] args) {
		try {
			if (args.length >= 3) {
				if (args.length == 3) {
					peerAp = args[0];
					protocol = protocolType.valueOf(args[1]);

					switch (protocol) {
					case RESTORE:
						filePath = args[2];
						break;
					case DELETE:
						filePath = args[2];
						break;
					case RECLAIM:
						spaceToReclaim = Integer.parseInt(args[2]);
						break;
					default:
						return false;
					}
					return true;
				} else if (args.length == 4) { // Backup
					peerAp = args[0];
					protocol = protocolType.valueOf(args[1]);
					if (protocol != Constants.protocolType.BACKUP
							&& protocol != Constants.protocolType.BACKUPENH) {
						return false;
					}
					filePath = args[2];
					replicationDegree = Integer.parseInt(args[3]);
					return true;
				} else
					return false;
			} else {
				return false;
			}
		} catch (NumberFormatException e) {
			System.err.println("Error converting a string to an integer.");
			return false;
		} catch (IllegalArgumentException e) {
			System.err.println("Invalid protocol");
			return false;
		}
	}

	public static void main(String[] args) throws IOException {

		if (!parseArgs(args)) {
			System.err
					.println("Usage: java TestApp <peer_ap> <sub_protocol> <opnd_1> <opnd_2>");
			System.exit(1);
		}

		// client only uses the RMI interface
		registry = LocateRegistry.getRegistry();
		RMIInterface rmiInterface = null;

		try {
			rmiInterface = (RMIInterface) registry.lookup(peerAp);
		} catch (NotBoundException e) {
			System.err.println("Error connecting to the remote RMI interface");
			System.exit(1);
			// e.printStackTrace();
		}

		String result = "The remote RMI server did not respond";

		try {
			switch (protocol) {
			case BACKUP:
				result = rmiInterface.backupFile(filePath, replicationDegree,
						Constants.DEFAULT_PROTOCOL_VERSION).getMessage();
				break;
			case DELETE:
				result = rmiInterface.deleteFile(filePath,
						Constants.DEFAULT_PROTOCOL_VERSION).getMessage();
				break;
			case RESTORE:
				result = rmiInterface.restoreFile(filePath,
						Constants.DEFAULT_PROTOCOL_VERSION).getMessage();
				break;
			case RECLAIM:
				result = rmiInterface.reclaimSpace(spaceToReclaim).getMessage();
				break;
			default:
				break;
			}

			System.out.println("[TestApp] " + result);

		} catch (ConnectException e) {
			System.err.println("[TestApp] " + "Error: " + result);
		}

	}
}
