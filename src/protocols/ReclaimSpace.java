package protocols;

import java.util.ArrayList;

import core.Dbs;
import database.Chunk;
import utils.Constants;
import message.Message;

public class ReclaimSpace {

	public synchronized long reclaimSpace(final long spaceToReclaim) {

		// sort local chunks by descending replication degree
		Dbs.peer.getDataBase().sortLocalChunks();
		ArrayList<Chunk> localChunks = Dbs.peer.getDataBase().getLocalChunks();
		long reclaimedKBytes = 0;

		while (reclaimedKBytes < spaceToReclaim && localChunks.size() > 0) {
			Chunk c = localChunks.get(0);
			long chunkSize = Dbs.peer.getDataBase().removeLocalChunk(
					c.getFileID(), c.getChunkNo());
			if (chunkSize > 0) {
				sendRemoved(c.getFileID(), c.getChunkNo());
				reclaimedKBytes += chunkSize;
				localChunks.remove(c);
			}
		}

		return reclaimedKBytes;
	}

	public void sendRemoved(String fileID, int chunkNo) {
		String removedMsg = Message.REMOVEDheader(
				Constants.DEFAULT_PROTOCOL_VERSION, Dbs.peer.peerID + "",
				fileID, chunkNo + "");

		try {
			Dbs.peer.controlChannel.sendMulticast(removedMsg.getBytes());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
