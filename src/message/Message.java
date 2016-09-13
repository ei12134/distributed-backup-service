package message;

import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import core.Dbs;
import utils.Constants;
import utils.Utils;

public class Message {
	private String messageType;
	private String version; // 1.0 but could be other check again
	private int senderID; // all ASCII digits
	private String fileID; // sha256 hash string
	private int chunkNo; // starts at 0 | only digits | no longer than 6 chars
	private int replicantionDeg; // only one digit
	private String mac; // message authentication code sha256 hash string
	private int port; // TCP port for restoring chunks
	private byte[] body; // body is a byte array of data

	public Message(byte[] message) throws MessageException {
		String header = this.getHeader(message);

		// removes all extra spaces from header string
		String oneSpaceHeader = header
				.replaceAll(Constants.SPLITTER + "+", " ");
		String[] tempHeaderValues = oneSpaceHeader.trim().split(
				Constants.SPLITTER);

		// get messageType
		this.messageType = tempHeaderValues[0].toUpperCase();
		switch (this.messageType) {
		case Constants.PUT_CHUNK:
			// length size
			if (tempHeaderValues.length != 7)
				throw new MessageException(
						"PUTCHUNK must have exactly 7 args, but "
								+ tempHeaderValues.length
								+ " args where found!");

			this.version = this.getVersion(tempHeaderValues[1]);
			this.senderID = this.getServerId(tempHeaderValues[2]);
			this.fileID = this.getFileId(tempHeaderValues[3]);
			this.chunkNo = this.getChunkNo(tempHeaderValues[4]);
			this.replicantionDeg = this.getReplicationDeg(tempHeaderValues[5]);
			this.setBody(message);
			this.mac = this.getMac(tempHeaderValues[6]);
			break;
		case Constants.STORED:
			// length size
			if (tempHeaderValues.length != 5)
				throw new MessageException(
						"STORED must have exactly 5 args, but "
								+ tempHeaderValues.length
								+ " args where found!");

			this.version = this.getVersion(tempHeaderValues[1]);
			this.senderID = this.getServerId(tempHeaderValues[2]);
			this.fileID = this.getFileId(tempHeaderValues[3]);
			this.chunkNo = this.getChunkNo(tempHeaderValues[4]);

			break;
		case Constants.GET_CHUNK:
			// length size
			if (tempHeaderValues.length != 6)
				throw new MessageException(
						"GETCHUNK must have exactly 5 args, but "
								+ tempHeaderValues.length
								+ " args where found!");

			this.version = this.getVersion(tempHeaderValues[1]);
			this.senderID = this.getServerId(tempHeaderValues[2]);
			this.fileID = this.getFileId(tempHeaderValues[3]);
			this.chunkNo = this.getChunkNo(tempHeaderValues[4]);
			this.port = this.getPort(tempHeaderValues[5]);
			break;
		case Constants.CHUNK:
			// length size
			if (tempHeaderValues.length != 6)
				throw new MessageException(
						"CHUNK must have exactly 6 args, but "
								+ tempHeaderValues.length
								+ " args where found!");

			this.version = this.getVersion(tempHeaderValues[1]);
			this.senderID = this.getServerId(tempHeaderValues[2]);
			this.fileID = this.getFileId(tempHeaderValues[3]);
			this.chunkNo = this.getChunkNo(tempHeaderValues[4]);
			this.setBody(message);

			this.mac = this.getMac(tempHeaderValues[5]);

			break;
		case Constants.DELETE:
		case Constants.DELETEACK:
			// length size
			if (tempHeaderValues.length != 4)
				throw new MessageException(
						"CHUNK must have exactly 4 args, but "
								+ tempHeaderValues.length
								+ " args where found!");
			this.version = this.getVersion(tempHeaderValues[1]);
			this.senderID = this.getServerId(tempHeaderValues[2]);
			this.fileID = this.getFileId(tempHeaderValues[3]);
			break;
		case Constants.REMOVED:
			// length size
			if (tempHeaderValues.length != 5)
				throw new MessageException(
						"CHUNK must have exactly 5 args, but "
								+ tempHeaderValues.length
								+ " args where found!");

			this.version = this.getVersion(tempHeaderValues[1]);
			this.senderID = this.getServerId(tempHeaderValues[2]);
			this.fileID = this.getFileId(tempHeaderValues[3]);
			this.chunkNo = this.getChunkNo(tempHeaderValues[4]);
			break;
		default:
			throw new MessageException("Invalid message Type ("
					+ tempHeaderValues[0].toUpperCase() + ")!");
		}
	}

	public void printMsg(String prefix) {

		prefix = "[Peer " + Dbs.peer.peerID + "] " + prefix;

		switch (messageType) {

		case Constants.PUT_CHUNK:
			System.out.println(prefix + messageType + " " + version + " "
					+ senderID + " " + fileID + " " + chunkNo + " "
					+ replicantionDeg + " " + mac + " " + body.length
					+ " bytes");
			break;
		case Constants.STORED:
			System.out.println(prefix + messageType + " " + version + " "
					+ senderID + " " + fileID + " " + chunkNo);
			break;
		case Constants.GET_CHUNK:
			System.out.println(prefix + messageType + " " + version + " "
					+ senderID + " " + fileID + " " + chunkNo + " " + port);
			break;
		case Constants.CHUNK:
			System.out.println(prefix + messageType + " " + version + " "
					+ senderID + " " + fileID + " " + chunkNo + " " + mac + " "
					+ body.length + " bytes");
			break;
		case Constants.DELETE:
			System.out.println(prefix + messageType + " " + version + " "
					+ senderID + " " + fileID);
			break;
		case Constants.DELETEACK:
			System.out.println(prefix + messageType + " " + version + " "
					+ senderID + " " + fileID);
			break;
		case Constants.REMOVED:
			System.out.println(prefix + messageType + " " + version + " "
					+ senderID + " " + fileID + " " + chunkNo);
			break;
		default:
			System.out.println(prefix + "unknown message type");
			break;
		}
	}

	@Override
	public boolean equals(Object other) {
		if (other == null)
			return false;
		if (other == this)
			return true;
		if (!(other instanceof Message))
			return false;
		Message msg = (Message) other;
		return (this.fileID.equals(msg.getFileID()) && this.chunkNo == msg
				.getChunkNo());
	}

	// getMessageType
	public String getMessageType() {
		return messageType;
	}

	// getVersion
	public String getVersion() {
		return version;
	}

	// getSenderId
	public int getSenderID() {
		return senderID;
	}

	// getFileId
	public String getFileID() {
		return fileID;
	}

	// getChunkNo
	public int getChunkNo() {
		return chunkNo;
	}

	// getReplicationDeg
	public int getReplicantionDegree() {
		return replicantionDeg;
	}

	public byte[] getBody() {
		return body;
	}

	public String getMac() {
		return mac;
	}

	public int getPort() {
		return port;
	}

	public static synchronized byte[] addMessageBody(byte[] header, byte[] body) {
		byte[] destiny = new byte[header.length + body.length];
		System.arraycopy(header, 0, destiny, 0, header.length);
		System.arraycopy(body, 0, destiny, header.length, body.length);
		return destiny;
	}

	public static synchronized String PUTCHUNKheader(String version,
			String senderId, String fileId, String chunkNo, String repDeg,
			String mac) {
		return Constants.PUT_CHUNK + Constants.SPLITTER + version
				+ Constants.SPLITTER + senderId + Constants.SPLITTER + fileId
				+ Constants.SPLITTER + chunkNo + Constants.SPLITTER + repDeg
				+ Constants.SPLITTER + mac + Constants.HEADER_TERMINATION;
	}

	public static synchronized String STOREDheader(String version,
			String senderId, String fileId, String chunkNo) {
		return Constants.STORED + Constants.SPLITTER + version
				+ Constants.SPLITTER + senderId + Constants.SPLITTER + fileId
				+ Constants.SPLITTER + chunkNo + Constants.HEADER_TERMINATION;
	}

	public static synchronized String GETCHUNKheader(String version,
			String senderId, String fileId, String chunkNo, int port) {
		return Constants.GET_CHUNK + Constants.SPLITTER + version
				+ Constants.SPLITTER + senderId + Constants.SPLITTER + fileId
				+ Constants.SPLITTER + chunkNo + Constants.SPLITTER + port
				+ Constants.HEADER_TERMINATION;
	}

	public static synchronized String CHUNKheader(String version,
			String senderId, String fileId, String chunkNo, String mac) {
		return Constants.CHUNK + Constants.SPLITTER + version
				+ Constants.SPLITTER + senderId + Constants.SPLITTER + fileId
				+ Constants.SPLITTER + chunkNo + Constants.SPLITTER + mac
				+ Constants.HEADER_TERMINATION;
	}

	public static synchronized String DELETEheader(String version,
			String senderId, String fileId) {
		return Constants.DELETE + Constants.SPLITTER + version
				+ Constants.SPLITTER + senderId + Constants.SPLITTER + fileId
				+ Constants.HEADER_TERMINATION;
	}

	public static synchronized String DELETEACKheader(String version,
			String senderId, String fileId) {
		return Constants.DELETEACK + Constants.SPLITTER + version
				+ Constants.SPLITTER + senderId + Constants.SPLITTER + fileId
				+ Constants.HEADER_TERMINATION;
	}

	public static synchronized String REMOVEDheader(String Version,
			String SenderId, String FileId, String ChunkNo) {
		return Constants.REMOVED + Constants.SPLITTER + Version
				+ Constants.SPLITTER + SenderId + Constants.SPLITTER + FileId
				+ Constants.SPLITTER + ChunkNo + Constants.HEADER_TERMINATION;
	}

	private String getHeader(byte[] message) throws MessageException {
		byte[] pattern = Constants.HEADER_TERMINATION.getBytes();

		int index = Utils.knuthMorrisPratt(message, pattern);

		if (index <= 0)
			throw new MessageException("Invalid Header finalizer!");

		byte[] res = new byte[index];
		System.arraycopy(message, 0, res, 0, index);
		String header = new String(res);
		return header;
	}

	private void setBody(byte[] message) throws MessageException {
		byte[] pattern = Constants.HEADER_TERMINATION.getBytes();

		int index = Utils.knuthMorrisPratt(message, pattern);

		if (index <= 0)
			throw new MessageException("Invalid Header finalizer!");

		index += pattern.length;
		if (index > message.length)
			throw new MessageException("Error getting the message body!");

		if (index == message.length) {
			this.body = new byte[0];// empty body
		} else {
			this.body = new byte[message.length - index];
			System.arraycopy(message, index, this.body, 0, message.length
					- index);

		}
	}

	private int getReplicationDeg(String data) throws MessageException {
		int replic = 0;
		try {
			replic = Integer.parseInt(data);
			if (replic < 0 || replic > 9)
				throw new MessageException(
						"ServerId arg isn't a valid single digit (" + data
								+ ")!");
		} catch (NumberFormatException e) {
			throw new MessageException("ServerId arg isn't valid (" + data
					+ ")!");
		}
		return replic;
	}

	private int getChunkNo(String data) throws MessageException {
		int chunkNo = 0;
		try {
			chunkNo = Integer.parseInt(data);
			if (chunkNo < 0 || chunkNo > 999999)
				throw new MessageException(
						"ChunkNo arg does not have the right size (" + data
								+ ")!");
		} catch (NumberFormatException e) {
			throw new MessageException("ChunkNo arg isn't valid (" + data
					+ ")!");
		}
		return chunkNo;
	}

	private int getPort(String data) throws MessageException {
		int port = 0;
		try {
			port = Integer.parseInt(data);
			if (port < 49152 || chunkNo > 65535)
				throw new MessageException("TCP port is not withing range ("
						+ data + ")!");
		} catch (NumberFormatException e) {
			throw new MessageException("Invalid port arg (" + data + ")!");
		}
		return port;
	}

	private String getFileId(String data) throws MessageException {
		Pattern pattern = Pattern.compile("[0-9a-fA-F]{64}");
		Matcher matcher = pattern.matcher(data);
		if (!matcher.matches())
			throw new MessageException(
					"FileId arg isn't a valid SHA-256 string (" + data + ")!");
		return data;
	}

	// check if the message was from the server that sent the message
	private int getServerId(String data) throws MessageException {
		int id = 0;
		try {
			id = Integer.parseInt(data);
			if (id == Dbs.peer.peerID)
				throw new MessageException(
						"this message was sent by this server!");
		} catch (NumberFormatException e) {
			throw new MessageException("invalid serverId (" + data + ")!");
		}
		return id;
	}

	private String getVersion(String data) throws MessageException {
		// Pattern pattern = Pattern.compile("\\d+\\.\\d+");
		Pattern pattern = Pattern.compile("[1].[03]");
		Matcher matcher = pattern.matcher(data);
		if (!matcher.matches())
			throw new MessageException("invalid version format (" + data + ")!");
		return data;
	}

	private String getMac(String data) throws MessageException {
		Pattern pattern = Pattern
				.compile("^(?:[A-Za-z0-9+/]{4})*(?:[A-Za-z0-9+/]{2}==|[A-Za-z0-9+/]{3}=)?");
		Matcher matcher = pattern.matcher(data);
		if (!matcher.matches())
			throw new MessageException(
					"HMAC arg isn't a valid Base64 Data Encoding (" + data
							+ ")!");
		try {
			if (!Dbs.peer
					.getCrypto()
					.encode(Dbs.peer.getCrypto().getSharedKeyString(),
							new String(body, StandardCharsets.UTF_8))
					.equals(data))
				throw new MessageException(
						"MAC arg doesn't match the data checksum");
		} catch (Exception e) {
			throw new MessageException(
					"MAC arg doesn't match the data checksum");
		}

		return data;
	}
}
