package core;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.rmi.ConnectException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import database.Database;
import dispatchers.BackupDispatcher;
import dispatchers.ControlDispatcher;
import rmi.RMIInterface;
import rmi.RMIService;
import utils.Constants;
import utils.Encryption;
import static java.nio.file.StandardCopyOption.*;

public class Peer {

	public final int peerID;

	private volatile Database database;
	private volatile Encryption crypto;
	private static Registry rmiRegistry = null;
	private static RMIInterface rmiInterface = null;
	public volatile ControlDispatcher controlChannel;
	public volatile BackupDispatcher backupChannel;
	public String remoteObjectName;

	public Peer(int peerID) {
		this.peerID = peerID;
		this.remoteObjectName = String.valueOf(peerID);
	}

	public String getFolder() {
		return "p" + peerID + "_" + "files";
	}

	public void initCrypto() throws Exception {
		printInfoMessage("Starting encryption scheme");
		this.crypto = new Encryption();
	}

	public void initDatabase() {
		this.database = new Database();

		printInfoMessage("Local chunks database has "
				+ database.getLocalChunks().size() + " locally backup chunks");
		printInfoMessage("Remote chunks database has "
				+ database.getRemoteChunks().size() + " remote backup chunks");
		printInfoMessage("Remote files database has "
				+ database.getRemoteFiles().size() + " remote backup files");
	}

	public boolean startConnections() {

		try {
			startControlChannel(Dbs.mcAddress, Dbs.mcPort);
			startBackUpChannel(Dbs.mdbAddress, Dbs.mdbPort);
		} catch (IOException e) {
			printErrorMessage(e.getMessage());
			e.printStackTrace();
			return false;
		}

		try {
			Thread.sleep(100);
		} catch (InterruptedException e) {
			printErrorMessage(e.getMessage());
			return false;
		}

		try {
			startRMIService();
		} catch (RemoteException e) {
			printErrorMessage(e.getLocalizedMessage());
			return false;
		}

		System.out.println("\n[Peer " + peerID + "] ONLINE\n");

		return true;
	}

	public void startRMIService() throws RemoteException {

		RMIService rmiService = new RMIService();

		// start RMI objects
		rmiInterface = (RMIInterface) UnicastRemoteObject.exportObject(
				rmiService, 0);

		// get the registry
		try {
			rmiRegistry = LocateRegistry.getRegistry();

			// this call will throw an exception if the registry does not
			// already exist
			rmiRegistry.list();
		} catch (RemoteException e) {
			e.getMessage();
			rmiRegistry = LocateRegistry.createRegistry(Constants.RMI_PORT);
		}

		// bind the remote object's stub in the registry
		try {
			rmiRegistry.rebind(remoteObjectName, rmiInterface);
			printInfoMessage("RMI server started with the object name: "
					+ remoteObjectName);
		} catch (ConnectException e) {
			printErrorMessage(e.getMessage());
		}
	}

	public void startControlChannel(String mcAddress, int mcPort)
			throws IOException {
		this.controlChannel = new ControlDispatcher(mcAddress, mcPort);
		this.controlChannel.start();
		printInfoMessage("MC control channel is now online at " + mcAddress
				+ ":" + mcPort);
	}

	public void startBackUpChannel(String mdbAddress, int mdbPort)
			throws IOException {
		this.backupChannel = new BackupDispatcher(mdbAddress, mdbPort);
		this.backupChannel.start();
		printInfoMessage("MDB data backup channel is now online at "
				+ mdbAddress + ":" + mdbPort);
	}

	public synchronized void sendMulticastControlChannel(byte[] msg)
			throws Exception {
		this.controlChannel.sendMulticast(msg);
	}

	public synchronized void sendMulticastDataBackupChannel(byte[] msg)
			throws Exception {
		this.backupChannel.sendMulticast(msg);
	}

	public void stopControlChannel() throws IOException {
		if (this.controlChannel != null) {
			this.controlChannel.stop();
			printInfoMessage("MC control channel is now offline");
		}
	}

	public void stopBackUpChannel() throws IOException {
		if (this.backupChannel != null) {
			this.backupChannel.stop();
			printInfoMessage("MDB data backup channel is now offline");
		}
	}

	public synchronized Database getDataBase() {
		return database;
	}

	public void setDataBase(Database dataBase) {
		this.database = dataBase;
	}

	public synchronized void printInfoMessage(String message) {
		System.out.println("[Peer " + peerID + "] " + message);
	}

	public synchronized void printErrorMessage(String message) {
		System.err.println("[Peer " + peerID + "] Error: " + message);
	}

	public synchronized boolean restoreDatabaseFiles() throws IOException {
		stopBackUpChannel();
		stopControlChannel();

		Path targetLocalChunksDatabse = FileSystems.getDefault().getPath(
				Dbs.peer.getFolder() + File.separator
						+ Constants.LOCAL_CHUNKS_DATABASE_FILE);
		Path targetRemoteChunksDatabase = FileSystems.getDefault().getPath(
				Dbs.peer.getFolder() + File.separator
						+ Constants.REMOTE_CHUNKS_DATABASE_FILE);
		Path targetRemoteFilesDatabase = FileSystems.getDefault().getPath(
				Dbs.peer.getFolder() + File.separator
						+ Constants.REMOTE_FILES_DATABASE_FILE);

		Path sourceLocalChunksDatabse = FileSystems.getDefault().getPath(
				Dbs.peer.getFolder() + File.separator
						+ Constants.RESTORED_FILES_FOLDER + File.separator
						+ "localChunksDb");
		Path sourceRemoteChunksDatabase = FileSystems.getDefault().getPath(
				Dbs.peer.getFolder() + File.separator
						+ Constants.RESTORED_FILES_FOLDER + File.separator
						+ "remoteChunksDb");
		Path sourceRemoteFilesDatabase = FileSystems.getDefault().getPath(
				Dbs.peer.getFolder() + File.separator
						+ Constants.RESTORED_FILES_FOLDER + File.separator
						+ "remoteFilesDb");

		Files.move(sourceLocalChunksDatabse, targetLocalChunksDatabse,
				REPLACE_EXISTING);
		Files.move(sourceRemoteChunksDatabase, targetRemoteChunksDatabase,
				REPLACE_EXISTING);
		Files.move(sourceRemoteFilesDatabase, targetRemoteFilesDatabase,
				REPLACE_EXISTING);

		initDatabase();
		startControlChannel(Dbs.mcAddress, Dbs.mcPort);
		startBackUpChannel(Dbs.mdbAddress, Dbs.mdbPort);

		return true;
	}

	public Encryption getCrypto() {
		return crypto;
	}
}
