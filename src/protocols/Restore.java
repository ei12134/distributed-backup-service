package protocols;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Arrays;

import utils.Constants;
import utils.Utils;
import message.Message;
import core.Dbs;

public class Restore {

	private ServerSocketChannel serverSocketChannel = null;
	private SocketChannel socketChannel = null;

	public boolean initServerSocket(int port) {
		try {
			serverSocketChannel = ServerSocketChannel.open();
			serverSocketChannel.socket().bind(new InetSocketAddress(port));
			serverSocketChannel.configureBlocking(false);
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}

	public void closeServerSocket() {
		try {
			if (socketChannel != null) {
				socketChannel.shutdownInput();
				socketChannel.shutdownOutput();
				socketChannel.close();
			}
			if (serverSocketChannel != null) {
				serverSocketChannel.close();
			}

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public synchronized byte[] getChunk(String fileID, int chunkNo,
			String protocolVersion) {

		int port = 0;
		boolean serverSocketReady = false;

		for (int i = 0; i < 3; i++) {
			port = Utils.randomPort();
			serverSocketReady = initServerSocket(port);
			if (serverSocketReady) {
				break;
			}
		}

		if (!serverSocketReady) {
			return new byte[0];
		}

		try {
			String getChunk = Message.GETCHUNKheader(protocolVersion,
					Dbs.peer.peerID + "", fileID, chunkNo + "", port);
			Dbs.peer.sendMulticastControlChannel(getChunk.getBytes());
			Thread.sleep(Constants.MAX_TCP_CONNECTION_WAIT_MILLISECONDS);
			socketChannel = serverSocketChannel.accept();

			if (socketChannel == null) {
				closeServerSocket();
				return new byte[0];
			}
			ByteBuffer buf = ByteBuffer.allocate(65000);
			int result = 0;
			int bytesRead = 0;

			while ((result = socketChannel.read(buf)) != -1) {
				bytesRead += result;
			}

			byte[] msg = new byte[bytesRead];
			msg = Arrays.copyOfRange(buf.array(), 0, bytesRead);
			Message chunkMsg = new Message(msg);
			chunkMsg.printMsg("[TCP] ");
			closeServerSocket();
			return chunkMsg.getBody();
		} catch (Exception e) {
			closeServerSocket();
			// e.printStackTrace();
			return new byte[0];
		}
	}
}
