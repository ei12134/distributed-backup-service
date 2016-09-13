package rmi;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.ArrayList;

import database.RemoteFile;

public interface RMIInterface extends Remote {
	RMIResult backupFile(String fileName, int replicationDegree,
			String protocolVersion) throws RemoteException;

	RMIResult restoreFile(String fileName, String protocolVersion)
			throws RemoteException;

	RMIResult deleteFile(String fileName, String protocolVersion)
			throws RemoteException;

	RMIResult reclaimSpace(long spaceToReclaim) throws RemoteException;

	ArrayList<RemoteFile> getRemoteFiles() throws RemoteException;

	String getRestoresFolder() throws RemoteException;

	RMIResult backupMetadata() throws RemoteException;

	RMIResult restoreMetadata() throws RemoteException;
}
