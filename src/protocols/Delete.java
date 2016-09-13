package protocols;

import utils.Constants;
import message.Message;
import core.Dbs;
import database.RemoteFile;

public class Delete {

	public int sendDelete(final RemoteFile remoteFile, String protocolVersion) {
		try {
			byte[] deleteMsg = Message.DELETEheader(protocolVersion,
					Dbs.peer.peerID + "", remoteFile.getFileID()).getBytes();

			Dbs.peer.controlChannel.clearInbox();
			Dbs.peer.controlChannel.sendMulticast(deleteMsg);
			Thread.sleep(Constants.MAX_DELAY_MILLISECONDS * 2);

			int deleteAcks = Dbs.peer.controlChannel.getDeleteAcks(remoteFile
					.getFileID());

			Dbs.peer.getDataBase().removeRemoteFileByPath(
					remoteFile.getFilePath(), true);

			return deleteAcks;

		} catch (Exception e) {
			return 0;
		}
	}

	public int sendDeleteByFileID(String fileId, String filePath,
			String protocolVersion) {
		try {
			byte[] deleteMsg = Message.DELETEheader(protocolVersion,
					Dbs.peer.peerID + "", fileId).getBytes();

			Dbs.peer.controlChannel.clearInbox();
			Dbs.peer.controlChannel.sendMulticast(deleteMsg);
			Thread.sleep(Constants.MAX_DELAY_MILLISECONDS * 2);

			int deleteAcks = Dbs.peer.controlChannel.getDeleteAcks(fileId);

			Dbs.peer.getDataBase().removeRemoteFileByPath(filePath, true);

			return deleteAcks;

		} catch (Exception e) {
			return 0;
		}
	}
}
