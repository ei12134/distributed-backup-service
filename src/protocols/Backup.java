package protocols;

import java.nio.charset.StandardCharsets;
import utils.Constants;
import core.Dbs;
import database.LocalChunk;
import message.Message;

public class Backup {

	private String protocolVersion = Constants.DEFAULT_PROTOCOL_VERSION;

	public synchronized boolean storeChunk(Message message) {
		LocalChunk chunk = new LocalChunk(message.getFileID(),
				message.getChunkNo(), message.getReplicantionDegree(),
				message.getBody());
		return Dbs.peer.getDataBase().saveChunk(chunk);
	}

	public synchronized boolean putChunk(LocalChunk chunk) {

		// loop to re-send a chunk until attempting to reach the desired
		// replication degree
		int waitingTime = Constants.BACKUP_MIN_CONFIRMATION_WAIT;

		for (int i = Constants.BACKUP_MAX_BACKUP_RETRIES; i > 0; i--, waitingTime *= 2) {

			// go check the replication degree status
			int initialReplicationDegree = chunk.getReplicationDegree();
			int currentReplicationDegree = Dbs.peer.getDataBase()
					.getRemoteChunkPeersCount(chunk.getFileID(),
							chunk.getChunkNo())
					+ Dbs.peer.getDataBase().hasLocalChunk(chunk.getFileID(),
							chunk.getChunkNo());

			// the job is done
			if (currentReplicationDegree >= initialReplicationDegree) {
				return true;
			}

			try {
				String putChunkHeader = Message.PUTCHUNKheader(
						protocolVersion,
						Dbs.peer.peerID + "",
						chunk.getFileID(),
						chunk.getChunkNo() + "",
						chunk.getReplicationDegree() + "",
						Dbs.peer.getCrypto().encode(
								Dbs.peer.getCrypto().getSharedKeyString(),
								new String(new String(chunk.getData(),
										StandardCharsets.UTF_8))));

				byte[] putChunkMsg = Message.addMessageBody(
						putChunkHeader.getBytes(), chunk.getData());

				Dbs.peer.sendMulticastDataBackupChannel(putChunkMsg);
			} catch (Exception e) {
				e.printStackTrace();
			}

			// wait for messages
			try {
				Thread.sleep(waitingTime);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}

		// retry with default protocol
		if (protocolVersion.equals(Constants.ENHANCED_PROTOCOL_VERSION)) {
			setProtocolVersion(Constants.DEFAULT_PROTOCOL_VERSION);
			return putChunk(chunk);
		}

		return false;
	}

	public String getProtocolVersion() {
		return protocolVersion;
	}

	public void setProtocolVersion(String protocolVersion) {
		this.protocolVersion = protocolVersion;
	}
}
