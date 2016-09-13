package database;

import java.io.Serializable;
import java.util.ArrayList;

public class RemoteChunk extends Chunk implements Serializable {

	private static final long serialVersionUID = 1577402797149078701L;
	private volatile ArrayList<Integer> remotePeers = new ArrayList<Integer>();

	public RemoteChunk(String fileId, int chunkNo) {
		super(fileId, chunkNo);
	}

	@Override
	public boolean equals(Object other) {
		if (other == null)
			return false;
		if (other == this)
			return true;
		if (!(other instanceof RemoteChunk))
			return false;
		RemoteChunk rc = (RemoteChunk) other;
		return (this.fileID.equals(rc.fileID) && this.chunkNo == rc.chunkNo);
	}

	public synchronized int getReplicationDegree() {
		return remotePeers.size();
	}

	public synchronized boolean addRemotePeer(int remotePeer) {
		if (!remotePeers.contains(remotePeer)) {
			remotePeers.add(remotePeer);
			return true;
		}
		return false;
	}

	public synchronized void removeRemotePeer(int remotePeer) {
		for (int i = 0; i < remotePeers.size(); i++) {
			if (remotePeers.get(i) == remotePeer) {
				remotePeers.remove(i);
				break;
			}
		}
	}

	public synchronized ArrayList<Integer> getRemotePeers() {
		return remotePeers;
	}

	public synchronized void setRemotePeers(ArrayList<Integer> remotePeers) {
		this.remotePeers = remotePeers;
	}
}