package rmi;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Arrays;
import core.Dbs;
import database.LocalChunk;
import database.RemoteFile;
import protocols.Backup;
import protocols.Delete;
import protocols.ReclaimSpace;
import protocols.Restore;
import utils.Constants;
import utils.Utils;

public class RMIService implements RMIInterface {

	@Override
	public RMIResult backupFile(String filePath, int replicationDegree,
			String protocolVersion) throws RemoteException {

		System.out
				.println("[Peer "
						+ Dbs.peer.peerID
						+ "] [RMI Request] "
						+ (protocolVersion
								.equals(Constants.DEFAULT_PROTOCOL_VERSION) ? "BACKUP"
								: "BACKUPENH") + " " + filePath);

		if (replicationDegree <= 0) {
			return new RMIResult(false, replicationDegree
					+ " - non positive replication degree");
		}

		try {
			File f = new File(filePath);
			if (f.exists() && f.isFile()) {
				Path path = Paths.get(filePath);
				String fileID = Utils.sha256Sum(f.getName() + f.lastModified()
						+ f.length());

				Dbs.peer.printInfoMessage("Encrypting the file before sending it through the LAN network...");
				byte[] data = Dbs.peer.getCrypto().encrypt(
						Files.readAllBytes(path),
						Dbs.peer.getCrypto().getPeerPublicKey());

				Backup backup = new Backup();
				// divides the file in 64 kilobytes chunks
				int chunkNo = 0;

				// add remote file so that this peer doesn't store its own
				// chunks
				Dbs.peer.getDataBase().addRemoteFile(filePath, fileID, chunkNo,
						f.getName());

				for (int i = 0; i < data.length; i += Constants.MAX_CHUNK_SIZE_BYTES, chunkNo++) {
					LocalChunk chunkToBackup = null;

					if ((i + Constants.MAX_CHUNK_SIZE_BYTES) > data.length) {
						int temp = data.length - i;

						chunkToBackup = new LocalChunk(fileID, chunkNo,
								replicationDegree, Arrays.copyOfRange(data, i,
										i + temp));
					} else {
						chunkToBackup = new LocalChunk(fileID, chunkNo,
								replicationDegree, Arrays.copyOfRange(data, i,
										i + Constants.MAX_CHUNK_SIZE_BYTES));

						if ((i + Constants.MAX_CHUNK_SIZE_BYTES) == data.length) {
							chunkToBackup = new LocalChunk(fileID, chunkNo,
									replicationDegree, new byte[0]);
						}
					}
					if (!backup.putChunk(chunkToBackup)) {
						// update remote file chunkNo count
						Dbs.peer.getDataBase().removeRemoteFileByPath(filePath,
								false);
						Dbs.peer.getDataBase().addRemoteFile(filePath, fileID,
								chunkNo, f.getName());
						return new RMIResult(false, "File " + '"' + f.getName()
								+ '"' + " backup failed");
					}
				}
				// update remote file chunkNo count
				Dbs.peer.getDataBase().removeRemoteFileByPath(filePath, false);
				Dbs.peer.getDataBase().addRemoteFile(filePath, fileID, chunkNo,
						f.getName());

			} else {
				return new RMIResult(false, "File " + '"' + f.getName() + '"'
						+ " not found");
			}
			return new RMIResult(true, "File " + '"' + f.getName() + '"'
					+ " backup complete");
		} catch (Exception e) {
			e.printStackTrace();
			return new RMIResult(false, "Unknown backup error");
		}
	}

	public RMIResult restoreFile(String filePath, String protocolVersion)
			throws RemoteException {

		System.out
				.println("[Peer "
						+ Dbs.peer.peerID
						+ "] [RMI Request] "
						+ (protocolVersion
								.equals(Constants.DEFAULT_PROTOCOL_VERSION) ? "RESTORE"
								: "RESTOREENH") + " " + filePath);

		RemoteFile rf = Dbs.peer.getDataBase()
				.getRemoteFileByFilePath(filePath);

		String returnMsg = "";

		if (rf == null) {
			return new RMIResult(false, "File " + '"' + filePath + '"'
					+ " not found");
		}

		if (rf != null) {
			returnMsg = "File " + '"' + rf.getFileName() + '"' + " restored";
			String fileID = rf.getFileID();

			String restoreFilePath = Dbs.peer.getFolder() + File.separator
					+ Constants.RESTORED_FILES_FOLDER + File.separator
					+ rf.getFileName();
			File file = new File(restoreFilePath);
			file.getParentFile().mkdirs();

			try {
				file.createNewFile();
			} catch (IOException e1) {
				e1.printStackTrace();
			}

			byte[] data = new byte[0];

			Restore restore = new Restore();

			for (int chunkNo = 0; chunkNo < rf.getChunkCount(); chunkNo++) {
				byte[] chunkData = restore.getChunk(fileID, chunkNo,
						protocolVersion);
				data = Utils.append(data, chunkData);
				if (chunkData.length == 0 && chunkNo < rf.getChunkCount() - 1) {
					return new RMIResult(true, '"' + rf.getFileName() + '"'
							+ " failed to get chunk no." + chunkNo);
				}
			}

			try {
				byte[] plainTextData = Dbs.peer.getCrypto().decrypt(data,
						Dbs.peer.getCrypto().getPeerPrivateKey());
				FileOutputStream fos = new FileOutputStream(restoreFilePath);
				fos.write(plainTextData);
				fos.flush();
				fos.close();
			} catch (Exception e) {
				return new RMIResult(false, e.getMessage());
				// e.printStackTrace();
			}
		}
		return new RMIResult(true, returnMsg);
	}

	@Override
	public RMIResult deleteFile(String filePath, String protocolVersion)
			throws RemoteException {

		System.out
				.println("[Peer "
						+ Dbs.peer.peerID
						+ "] [RMI Request] "
						+ (protocolVersion
								.equals(Constants.DEFAULT_PROTOCOL_VERSION) ? "DELETE"
								: "DELETEENH") + " " + filePath);

		final RemoteFile remoteFile = Dbs.peer.getDataBase()
				.getRemoteFileByFilePath(filePath);

		if (remoteFile == null) {
			return new RMIResult(false, "File " + '"' + filePath + '"'
					+ " not found");
		}

		final Delete delete = new Delete();

		Thread worker = new Thread() {
			@Override
			public void run() {
				try {
					long waitingTime = Constants.DELETE_MIN_WAITING;
					int replicationDegree = Dbs.peer
							.getDataBase()
							.getRemoteChunkPeersCount(remoteFile.getFileID(), 0);

					do {
						replicationDegree -= delete.sendDelete(remoteFile,
								Constants.DEFAULT_PROTOCOL_VERSION);
						Thread.sleep(waitingTime);
						waitingTime *= 2;
					} while (waitingTime < Constants.DELETE_MAX_WAITING
							&& replicationDegree > 0);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			};
		};
		worker.start();

		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		return new RMIResult(true, '"' + remoteFile.getFileName() + '"'
				+ " delete requests sent");
	}

	@Override
	public RMIResult reclaimSpace(final long spaceToReclaim)
			throws RemoteException {
		System.out.println("[Peer " + Dbs.peer.peerID
				+ "] [RMI Request] RECLAIM " + spaceToReclaim + " KBytes");
		ReclaimSpace rs = new ReclaimSpace();
		return new RMIResult(true, rs.reclaimSpace(spaceToReclaim)
				+ " KBytes freed");
	}

	@Override
	public ArrayList<RemoteFile> getRemoteFiles() {
		return Dbs.peer.getDataBase().getRemoteFiles();
	}

	@Override
	public String getRestoresFolder() {
		File folder = new File(Dbs.peer.getFolder() + File.separator
				+ Constants.RESTORED_FILES_FOLDER);
		if (!folder.exists()) {
			folder.mkdir();
		}
		return folder.getAbsolutePath();
	}

	@Override
	public RMIResult backupMetadata() throws RemoteException {

		System.out.println("[Peer " + Dbs.peer.peerID
				+ "] [RMI Request] BACKUP peer metadata");

		ArrayList<String> metaDataFilePaths = new ArrayList<String>();
		metaDataFilePaths.add(Dbs.peer.getFolder() + File.separator
				+ Constants.LOCAL_CHUNKS_DATABASE_FILE);
		metaDataFilePaths.add(Dbs.peer.getFolder() + File.separator
				+ Constants.REMOTE_CHUNKS_DATABASE_FILE);
		metaDataFilePaths.add(Dbs.peer.getFolder() + File.separator
				+ Constants.REMOTE_FILES_DATABASE_FILE);

		int replicationDegree = 1;

		for (String filePath : metaDataFilePaths) {
			try {
				File f = new File(filePath);
				if (f.exists() && f.isFile()) {
					Path path = Paths.get(filePath);
					String fileID = Utils.sha256Sum("p" + Dbs.peer.peerID + "_"
							+ f.getName());

					Delete delete = new Delete();
					delete.sendDeleteByFileID(fileID, filePath,
							Constants.DEFAULT_PROTOCOL_VERSION);

					Dbs.peer.printInfoMessage("Encrypting the file before sending it through the LAN network...");
					byte[] data = Dbs.peer.getCrypto().encryptAES(
							Files.readAllBytes(path),
							Dbs.peer.getCrypto().getSharedKey());

					Backup backup = new Backup();

					// divides the file in 64 kilobytes chunks
					int chunkNo = 0;

					// add remote file so that this peer doesn't store its own
					// chunks
					Dbs.peer.getDataBase().addRemoteFile(filePath, fileID,
							chunkNo, f.getName());

					for (int i = 0; i < data.length; i += Constants.MAX_CHUNK_SIZE_BYTES, chunkNo++) {
						LocalChunk chunkToBackup = null;

						if ((i + Constants.MAX_CHUNK_SIZE_BYTES) > data.length) {
							int temp = data.length - i;

							chunkToBackup = new LocalChunk(fileID, chunkNo,
									replicationDegree, Arrays.copyOfRange(data,
											i, i + temp));
						} else {
							chunkToBackup = new LocalChunk(fileID, chunkNo,
									replicationDegree, Arrays.copyOfRange(data,
											i,
											i + Constants.MAX_CHUNK_SIZE_BYTES));

							if ((i + Constants.MAX_CHUNK_SIZE_BYTES) == data.length) {
								chunkToBackup = new LocalChunk(fileID, chunkNo,
										replicationDegree, new byte[0]);
							}
						}
						if (!backup.putChunk(chunkToBackup)) {
							Dbs.peer.getDataBase().removeRemoteFileByPath(
									filePath, true);
							return new RMIResult(false, "File " + '"'
									+ f.getName() + '"' + " backup failed");
						}
					}

					Dbs.peer.getDataBase().removeRemoteFileByPath(filePath,
							true);

				} else {
					return new RMIResult(false, "File " + '"' + f.getName()
							+ '"' + " not found");
				}

			} catch (Exception e) {
				return new RMIResult(false, "Unknown backup error");
			}
		}
		return new RMIResult(true, "Metadata backup complete");
	}

	@Override
	public RMIResult restoreMetadata() throws RemoteException {

		System.out.println("[Peer " + Dbs.peer.peerID
				+ "] [RMI Request] RESTORE peer metadata");

		ArrayList<String> fileNames = new ArrayList<String>();
		fileNames.add("localChunksDb");
		fileNames.add("remoteChunksDb");
		fileNames.add("remoteFilesDb");

		for (String fileName : fileNames) {

			String fileID = Utils.sha256Sum("p" + Dbs.peer.peerID + "_"
					+ fileName);

			String restoreFilePath = Dbs.peer.getFolder() + File.separator
					+ Constants.RESTORED_FILES_FOLDER + File.separator
					+ fileName;
			File file = new File(restoreFilePath);
			file.getParentFile().mkdirs();

			try {
				file.createNewFile();
			} catch (IOException e1) {
				e1.printStackTrace();
			}

			byte[] data = new byte[0];

			Restore restore = new Restore();

			for (int chunkNo = 0; chunkNo < (Integer.MAX_VALUE); chunkNo++) {
				byte[] chunkData = restore.getChunk(fileID, chunkNo,
						Constants.DEFAULT_PROTOCOL_VERSION);
				data = Utils.append(data, chunkData);
				if (chunkData.length == 0 && chunkNo < Integer.MAX_VALUE - 1) {
					break;
				}
			}

			if (data.length == 0) {
				return new RMIResult(false, "Metadata restoration failed");
			}

			try {
				byte[] plainTextData = Dbs.peer.getCrypto().decryptAES(data,
						Dbs.peer.getCrypto().getSharedKey());
				FileOutputStream fos = new FileOutputStream(restoreFilePath);
				fos.write(plainTextData);
				fos.flush();
				fos.close();
			} catch (Exception e) {
				e.printStackTrace();
				return new RMIResult(false, "Metadata restoration failed");
			}
		}

		try {
			if (Dbs.peer.restoreDatabaseFiles()) {
				return new RMIResult(true, "Metadata restore complete");
			} else {
				return new RMIResult(false, "Metadata restoration failed");
			}
		} catch (IOException e) {
			e.printStackTrace();
			return new RMIResult(false, "Metadata restoration failed");
		}
	}
}
