package dispatchers;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;

import core.Dbs;
import database.Chunk;
import database.LocalChunk;
import message.Message;
import message.MessageException;
import protocols.Backup;
import utils.Constants;
import utils.Utils;

public class BackupDispatcher extends Dispatcher {

	private volatile ArrayList<Message> mdbInbox = new ArrayList<Message>();
	private volatile boolean faultToleranceWorker;

	public BackupDispatcher(String address, int port)
			throws UnknownHostException {
		super(address, port);
		this.faultToleranceWorker = false;
	}

	public synchronized boolean hasPutChunk(String fileID, int chunkNo) {
		for (int i = 0; i < mdbInbox.size(); i++) {
			if (mdbInbox.get(i).getFileID().equals(fileID)
					&& mdbInbox.get(i).getChunkNo() == chunkNo) {
				return true;
			}
		}
		return false;
	}

	public synchronized ArrayList<Message> getInbox() {
		return mdbInbox;
	}

	public synchronized boolean checkEmptyInbox() {
		return mdbInbox.isEmpty();
	}

	public synchronized void clearInbox() {
		mdbInbox.clear();
	}

	@Override
	public void processMessage(final byte[] trimedMsg,
			final InetAddress address, final int port) {

		try {

			Message message = new Message(trimedMsg);
			message.printMsg("[MDB RCVD] ");

			if (message.getMessageType().equals(Constants.PUT_CHUNK)) {

				mdbInbox.add(message);
				Backup backup = new Backup();
				boolean stored = false;

				if (Dbs.peer.getDataBase().hasChunk(
						new Chunk(message.getFileID(), message.getChunkNo(),
								message.getReplicantionDegree()))
						&& !faultToleranceWorker) {

					faultToleranceWorker = true;

					// if this peer already has the chunk it shall start the
					// backup fault tolerance check that monitors if the current
					// stored count for the chunk is below the desired
					// replication after all the PUTCHUNK retries have failed to
					// establish the desired replication degree goal (long
					// waiting time)
					Utils.randomDelay(Constants.BACKUP_MAX_CONFIRMATION_WAIT
							+ Constants.BACKUP_MIN_CONFIRMATION_WAIT);
					clearInbox();
					Utils.randomDelay(Constants.MAX_DELAY_MILLISECONDS,
							Constants.MAX_DELAY_MILLISECONDS * 2);

					if (!hasPutChunk(message.getFileID(), message.getChunkNo())) {
						clearInbox();

						final LocalChunk localChunk;
						localChunk = Dbs.peer.getDataBase().getChunk(
								message.getFileID(), message.getChunkNo());

						int currentReplicationDegree = Dbs.peer.getDataBase()
								.getRemoteChunkPeersCount(message.getFileID(),
										message.getChunkNo())
								+ Dbs.peer.getDataBase().hasLocalChunk(
										message.getFileID(),
										message.getChunkNo());

						int expectedReplicationDegree = Dbs.peer.getDataBase()
								.getLocalChunkReplicationDegree(
										message.getFileID(),
										message.getChunkNo());
						if (expectedReplicationDegree >= 0
								&& currentReplicationDegree < expectedReplicationDegree) {
							backup.putChunk(localChunk);
						}
					}
					faultToleranceWorker = false;
				} else {
					// a peer must never store the chunks of its own files
					if (Dbs.peer.getDataBase().getRemoteFileByFileID(
							message.getFileID()) != null) {
						return;
					}

					// send STORED after random delay uniformly
					// distributed between 0 and 400 m
					Utils.randomDelay(Constants.MAX_DELAY_MILLISECONDS);

					// backup enhancement version check
					if (Dbs.peer.getDataBase().getRemoteChunkPeersCount(
							message.getFileID(), message.getChunkNo()) < message
							.getReplicantionDegree()) {
						stored = backup.storeChunk(message);
					}

					if (stored) {
						String storedMsg = Message.STOREDheader(
								message.getVersion(), Dbs.peer.peerID + "",
								message.getFileID(), message.getChunkNo() + "");

						try {
							Dbs.peer.sendMulticastControlChannel(storedMsg
									.getBytes());
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
				}
			}
		} catch (MessageException e) {
			// System.out.println("[MC] Message dropped (" + e.getMessage() +
			// ")");
		}
	}
}
