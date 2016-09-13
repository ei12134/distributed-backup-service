package database;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import core.Dbs;
import utils.Constants;

public class Database {

	private volatile ArrayList<Chunk> localChunks = new ArrayList<Chunk>();
	private volatile ArrayList<RemoteChunk> remoteChunks = new ArrayList<RemoteChunk>();
	private volatile ArrayList<RemoteFile> remoteFiles = new ArrayList<RemoteFile>();

	public Database() {
		loadLocalChunksDatabase();
		loadRemoteChunksDatabase();
		loadRemoteFilesDatabase();
	}

	@SuppressWarnings("unchecked")
	public synchronized void loadLocalChunksDatabase() {
		File file = new File(Dbs.peer.getFolder() + File.separator
				+ Constants.LOCAL_CHUNKS_DATABASE_FILE);
		file.getParentFile().mkdirs();
		try {
			file.createNewFile();
		} catch (IOException e) {
			e.printStackTrace();
		}

		if (file.exists() && file.isFile()) {
			FileInputStream f;

			try {
				f = new FileInputStream(Dbs.peer.getFolder() + File.separator
						+ Constants.LOCAL_CHUNKS_DATABASE_FILE);
				ObjectInputStream s = new ObjectInputStream(f);
				this.localChunks = (ArrayList<Chunk>) s.readObject();
				s.close();
			} catch (IOException | ClassNotFoundException e) {
				// System.out.println("Error opening the database file "
				// + file.getName());
				saveLocalChunksDatabase();
			}
		}
	}

	@SuppressWarnings("unchecked")
	public synchronized void loadRemoteChunksDatabase() {
		File file = new File(Dbs.peer.getFolder() + File.separator
				+ Constants.REMOTE_CHUNKS_DATABASE_FILE);
		file.getParentFile().mkdirs();
		try {
			file.createNewFile();
		} catch (IOException e1) {
			e1.printStackTrace();
		}

		if (file.exists() && file.isFile()) {
			FileInputStream f;

			try {
				f = new FileInputStream(Dbs.peer.getFolder() + File.separator
						+ Constants.REMOTE_CHUNKS_DATABASE_FILE);
				ObjectInputStream s = new ObjectInputStream(f);
				this.remoteChunks = (ArrayList<RemoteChunk>) s.readObject();
				s.close();
			} catch (IOException | ClassNotFoundException e) {
				// System.out.println("Error opening the database file "
				// + file.getName());
				saveRemoteChunksDatabase();
			}
		}
	}

	@SuppressWarnings("unchecked")
	public synchronized void loadRemoteFilesDatabase() {
		File file = new File(Dbs.peer.getFolder() + File.separator
				+ Constants.REMOTE_FILES_DATABASE_FILE);
		file.getParentFile().mkdirs();
		try {
			file.createNewFile();
		} catch (IOException e1) {
			e1.printStackTrace();
		}

		if (file.exists() && file.isFile()) {
			FileInputStream f;

			try {
				f = new FileInputStream(Dbs.peer.getFolder() + File.separator
						+ Constants.REMOTE_FILES_DATABASE_FILE);
				ObjectInputStream s = new ObjectInputStream(f);
				this.remoteFiles = (ArrayList<RemoteFile>) s.readObject();
				s.close();
			} catch (IOException | ClassNotFoundException e) {
				// System.out.println("Error opening the database file "
				// + file.getName());
				saveRemoteFilesDatabase();
			}
		}
	}

	public synchronized boolean saveLocalChunksDatabase() {
		String filePath = Dbs.peer.getFolder() + File.separator
				+ Constants.LOCAL_CHUNKS_DATABASE_FILE;
		try {
			FileOutputStream fos = new FileOutputStream(filePath);
			ObjectOutputStream oos = new ObjectOutputStream(fos);

			oos.writeObject(localChunks);
			oos.close();
			return true;
		} catch (IOException e) {
			return false;
		}
	}

	public synchronized boolean saveRemoteChunksDatabase() {
		String filePath = Dbs.peer.getFolder() + File.separator
				+ Constants.REMOTE_CHUNKS_DATABASE_FILE;
		try {
			FileOutputStream fos = new FileOutputStream(filePath);
			ObjectOutputStream oos = new ObjectOutputStream(fos);

			oos.writeObject(remoteChunks);
			oos.close();
			return true;
		} catch (IOException e) {
			return false;
		}
	}

	public synchronized boolean saveRemoteFilesDatabase() {
		String filePath = Dbs.peer.getFolder() + File.separator
				+ Constants.REMOTE_FILES_DATABASE_FILE;
		try {
			FileOutputStream fos = new FileOutputStream(filePath);
			ObjectOutputStream oos = new ObjectOutputStream(fos);

			oos.writeObject(remoteFiles);
			oos.close();
			return true;
		} catch (IOException e) {
			return false;
		}
	}

	public synchronized void sortLocalChunks() {
		Comparator<Chunk> c = new Comparator<Chunk>() {
			@Override
			public int compare(Chunk a, Chunk b) {
				if (a.getReplicationDegree() > b.getReplicationDegree()) {
					return -1;
				} else if (a.getReplicationDegree() < b.getReplicationDegree()) {
					return 1;
				}
				return 0;
			}
		};
		Collections.sort(localChunks, c);
	}

	public synchronized boolean hasChunk(Chunk chunk) {
		return localChunks.contains(chunk);
	}

	public synchronized boolean saveChunk(LocalChunk chunk) {

		String filePath = this.getChunkPath(chunk.getFileID(),
				chunk.getChunkNo());

		if (localChunks.contains(new Chunk(chunk.getFileID(), chunk
				.getChunkNo(), chunk.getReplicationDegree()))) {
			return false;
		}

		File file = new File(filePath);
		file.getParentFile().mkdirs();

		try {
			file.createNewFile();
		} catch (IOException e1) {
			e1.printStackTrace();
		}

		try {
			FileOutputStream fos = new FileOutputStream(filePath);
			ObjectOutputStream oos = new ObjectOutputStream(fos);

			oos.writeObject(chunk);
			oos.close();

			localChunks.add(new Chunk(chunk.getFileID(), chunk.getChunkNo(),
					chunk.getReplicationDegree()));

			saveLocalChunksDatabase();
			return true;

		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
	}

	public synchronized LocalChunk getChunk(String fileID, int chunkNo) {

		String filePath = this.getChunkPath(fileID, chunkNo);
		File file = new File(filePath);
		FileInputStream f;
		LocalChunk chunk = null;

		try {
			f = new FileInputStream(file);
			ObjectInputStream s;
			s = new ObjectInputStream(f);
			chunk = (LocalChunk) s.readObject();
			s.close();
		} catch (ClassNotFoundException | IOException e) {
			// e.printStackTrace();
		}

		return chunk;
	}

	public synchronized int getLocalChunkReplicationDegree(String fileID,
			int chunkNo) {

		Chunk c = new Chunk(fileID, chunkNo, 0);
		int i = localChunks.indexOf(c);

		if (i >= 0) {
			return localChunks.get(i).getReplicationDegree();
		} else {
			return 0;
		}
	}

	public synchronized int hasLocalChunk(String fileID, int chunkNo) {
		Chunk c = new Chunk(fileID, chunkNo);
		if (localChunks.contains(c)) {
			return 1;
		}
		return 0;
	}

	public synchronized int getRemoteChunkPeersCount(String fileID, int chunkNo) {
		RemoteChunk rc = new RemoteChunk(fileID, chunkNo);
		int i = remoteChunks.indexOf(rc);

		if (i >= 0) {
			return remoteChunks.get(i).getReplicationDegree();
		} else {
			return 0;
		}
	}

	public synchronized boolean fileChunkExists(String fileID, int chunkNo) {
		return this.localChunks.contains(this.getChunkPath(fileID, chunkNo));
	}

	public synchronized String getChunkPath(String fileID, int chunkNo) {
		return Dbs.peer.getFolder() + File.separator + Constants.CHUNKS_FOLDER
				+ File.separator + fileID + "_" + chunkNo
				+ Constants.CHUNK_FILE_EXTENSION;
	}

	public synchronized ArrayList<Chunk> getLocalChunks() {
		return localChunks;
	}

	public synchronized ArrayList<RemoteChunk> getRemoteChunks() {
		return remoteChunks;
	}

	public synchronized ArrayList<RemoteFile> getRemoteFiles() {
		return remoteFiles;
	}

	public synchronized RemoteFile getRemoteFileByFilePath(String filePath) {
		for (int i = 0; i < remoteFiles.size(); i++) {
			if (remoteFiles.get(i).getFilePath().equals(filePath)) {
				return remoteFiles.get(i);
			}
		}
		return null;
	}

	public synchronized RemoteFile getRemoteFileByFileID(String fileID) {
		for (int i = 0; i < remoteFiles.size(); i++) {
			if (remoteFiles.get(i).getFileID().equals(fileID)) {
				return remoteFiles.get(i);
			}
		}
		return null;
	}

	public synchronized void setFiles(ArrayList<Chunk> localChunks) {
		this.localChunks = localChunks;
	}

	public synchronized void addRemoteChunk(String fileID, int chunkNo,
			int peerID) {

		RemoteChunk rc = new RemoteChunk(fileID, chunkNo);
		int i = remoteChunks.indexOf(rc);

		if (i >= 0) {
			remoteChunks.get(i).addRemotePeer(peerID);
		} else {
			rc.addRemotePeer(peerID);
			remoteChunks.add(rc);
		}
		saveRemoteChunksDatabase();
	}

	public synchronized boolean addRemoteFile(String filePath, String fileID,
			int chunkCount, String fileName) {

		RemoteFile rf = new RemoteFile(filePath, fileID, chunkCount, fileName);
		int i = remoteFiles.indexOf(rf);

		if (i == -1) {
			remoteFiles.add(rf);
			saveRemoteFilesDatabase();
			return true;
		}
		return false;
	}

	public synchronized long removeLocalChunk(final String fileID,
			final int chunkNo) {

		long fileSize = 0;
		final File chunksFolder = new File(Dbs.peer.getFolder()
				+ File.separator + Constants.CHUNKS_FOLDER);
		final File[] chunkFiles = chunksFolder.listFiles(new FilenameFilter() {
			@Override
			public boolean accept(final File dir, final String name) {
				return name.matches(fileID + "_" + chunkNo + "\\.chunk");
			}
		});

		if (chunkFiles == null) {
			return 0;
		}

		for (final File file : chunkFiles) {

			long oldFileSize = (file.length() / 1000); // convert to 1000 bytes

			if (!file.delete()) {
				// System.err.println("Can't remove " + file.getAbsolutePath());
			} else {
				fileSize += oldFileSize;
			}
		}

		Chunk c = new Chunk(fileID, chunkNo);
		localChunks.remove(c);
		saveLocalChunksDatabase();

		return fileSize;
	}

	public synchronized boolean removeLocalFile(final String fileID) {
		final File chunksFolder = new File(Dbs.peer.getFolder()
				+ File.separator + Constants.CHUNKS_FOLDER);
		final File[] chunkFiles = chunksFolder.listFiles(new FilenameFilter() {
			@Override
			public boolean accept(final File dir, final String name) {
				return name.matches(fileID + "_([0-9])+\\.chunk");
			}
		});

		if (chunkFiles == null || chunkFiles.length == 0) {
			return false;
		}

		for (final File file : chunkFiles) {
			if (!file.delete()) {
				return false;
			}
		}

		for (int i = 0; i < localChunks.size(); i++) {
			if (localChunks.get(i).getFileID().equals(fileID)) {
				localChunks.remove(i);
				i--;
			}
		}

		saveLocalChunksDatabase();
		return true;
	}

	public synchronized boolean removeRemotePeer(String fileID, int chunkNo,
			int peerID) {

		RemoteChunk rc = new RemoteChunk(fileID, chunkNo);

		for (int i = 0; i < remoteChunks.size(); i++) {
			if (remoteChunks.get(i).equals(rc)) {
				remoteChunks.get(i).removeRemotePeer(peerID);
				saveRemoteChunksDatabase();
				return true;
			}
		}
		return false;
	}

	public synchronized boolean removeRemoteFileByPath(String filePath,
			boolean removeRemoteChunks) {

		String fileID = null;

		for (int i = 0; i < remoteFiles.size(); i++) {
			if (remoteFiles.get(i).getFilePath().equals(filePath)) {
				fileID = remoteFiles.get(i).getFileID();
				remoteFiles.remove(i);
				break;
			}
		}

		if (fileID == null) {
			return false;
		}

		if (removeRemoteChunks) {
			for (int i = 0; i < remoteChunks.size(); i++) {
				if (remoteChunks.get(i).getFileID().equals(fileID)) {
					remoteChunks.remove(i);
					i--;
				}
			}
			saveRemoteChunksDatabase();
		}

		saveRemoteFilesDatabase();
		return true;
	}

	public synchronized void removeRemoteFileByFileID(String fileID) {

		// boolean found = false;

		for (int i = 0; i < remoteFiles.size(); i++) {
			if (remoteFiles.get(i).getFileID().equals(fileID)) {
				remoteFiles.remove(i);
				// found = true;
				break;
			}
		}
		//
		// if (!found) {
		// return false;
		// }

		for (int i = 0; i < remoteChunks.size(); i++) {
			if (remoteChunks.get(i).getFileID().equals(fileID)) {
				remoteChunks.remove(i);
				i--;
			}
		}

		saveRemoteChunksDatabase();
		saveRemoteFilesDatabase();
		// return true;
	}
}
