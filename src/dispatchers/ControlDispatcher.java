package dispatchers;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import protocols.Backup;
import core.Dbs;
import database.LocalChunk;
import utils.Constants;
import utils.Utils;
import message.Message;
import message.MessageException;

public class ControlDispatcher extends Dispatcher {

	private volatile ArrayList<Message> deleteAcksInbox = new ArrayList<Message>();

	public ControlDispatcher(String address, int port)
			throws UnknownHostException {
		super(address, port);
	}

	public synchronized ArrayList<Message> getInbox() {
		return deleteAcksInbox;
	}

	public synchronized int getDeleteAcks(String fileID) {
		return deleteAcksInbox.size();
	}

	public synchronized void clearInbox() {
		deleteAcksInbox.clear();
	}

	@Override
	public void processMessage(final byte[] trimedMsg,
			final InetAddress address, final int port) {

		try {
			Message msg = new Message(trimedMsg);
			msg.printMsg("[MC RCVD] ");

			switch (msg.getMessageType()) {

			case Constants.STORED:
				Dbs.peer.getDataBase().addRemoteChunk(msg.getFileID(),
						msg.getChunkNo(), msg.getSenderID());
				break;

			case Constants.GET_CHUNK:
				LocalChunk lc = null;

				// retrieve the chunk and send it through the MDR TCP socket
				lc = Dbs.peer.getDataBase().getChunk(msg.getFileID(),
						msg.getChunkNo());

				if (lc != null) {
					try {
						String chunkMsgHeader = Message.CHUNKheader(
								Constants.DEFAULT_PROTOCOL_VERSION,
								Dbs.peer.peerID + "",
								lc.getFileID(),
								lc.getChunkNo() + "",
								Dbs.peer.getCrypto().encode(
										Dbs.peer.getCrypto()
												.getSharedKeyString(),
										new String(new String(lc.getData(),
												StandardCharsets.UTF_8))));
						byte[] chunkMsg = Message.addMessageBody(
								chunkMsgHeader.getBytes(), lc.getData());

						SocketChannel socketChannel = SocketChannel.open();
						socketChannel.connect(new InetSocketAddress(address,
								msg.getPort()));
						ByteBuffer bb = ByteBuffer.wrap(chunkMsg);
						socketChannel.write(bb);
						socketChannel.close();
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
				break;

			case Constants.DELETE:
				if (Dbs.peer.getDataBase().removeLocalFile(msg.getFileID())) {
					Dbs.peer.getDataBase().removeRemoteFileByFileID(
							msg.getFileID());

					String deleteAck = Message.DELETEACKheader(
							Constants.DEFAULT_PROTOCOL_VERSION, Dbs.peer.peerID
									+ "", msg.getFileID());
					try {
						Dbs.peer.controlChannel.sendMulticast(deleteAck
								.getBytes());
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
				break;

			case Constants.DELETEACK:
				deleteAcksInbox.add(msg);
				break;

			case Constants.REMOVED:
				// update its local count of this chunk
				if (!Dbs.peer.getDataBase().removeRemotePeer(msg.getFileID(),
						msg.getChunkNo(), msg.getSenderID())) {
					break;
				}

				Dbs.peer.backupChannel.clearInbox();

				// if this count drops below the desired replication
				// degree of that chunk, it shall initiate the chunk
				// backup sub-protocol after a random delay uniformly
				// distributed between 0 and 400 milliseconds
				// if during this delay, a peer receives a PUTCHUNK message for
				// the same file chunk, it should back off and restrain from
				// starting yet another backup sub-protocol for that file chunk.
				Utils.randomDelay(Constants.MAX_DELAY_MILLISECONDS * 2);

				if (!Dbs.peer.backupChannel.hasPutChunk(msg.getFileID(),
						msg.getChunkNo())) {
					final LocalChunk localChunk;
					localChunk = Dbs.peer.getDataBase().getChunk(
							msg.getFileID(), msg.getChunkNo());

					int currentReplicationDegree = Dbs.peer.getDataBase()
							.getRemoteChunkPeersCount(msg.getFileID(),
									msg.getChunkNo())
							+ Dbs.peer.getDataBase().hasLocalChunk(
									msg.getFileID(), msg.getChunkNo());

					int expectedReplicationDegree = Dbs.peer.getDataBase()
							.getLocalChunkReplicationDegree(msg.getFileID(),
									msg.getChunkNo());

					if (expectedReplicationDegree >= 0
							&& currentReplicationDegree < expectedReplicationDegree) {
						Backup backup = new Backup();
						backup.putChunk(localChunk);
					}
				} else {
					Dbs.peer.backupChannel.clearInbox();
				}
				break;
			default:
				break;
			}

		} catch (MessageException e) {
			// System.out.println("[MC] Message dropped (" + e.getMessage() +
			// ")");
		}

	}
}
