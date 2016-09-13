package dispatchers;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.UnknownHostException;
//import java.nio.charset.StandardCharsets;
import java.util.Arrays;

//import core.Dbs;

import utils.Constants;

public abstract class Dispatcher implements Runnable {

	protected volatile MulticastSocket socket;
	protected volatile Thread multicastThread;
	protected InetAddress multicastGroup;
	protected int multicastPort;

	public Dispatcher(String address, int port) throws UnknownHostException {
		this.multicastGroup = InetAddress.getByName(address);
		this.multicastPort = port;
	}

	public synchronized void start() throws IOException {
		if (this.socket != null && !socket.isClosed()) {
			return;
		}
		this.socket = new MulticastSocket(this.multicastPort);
		this.socket.joinGroup(this.multicastGroup);
		this.socket.setTimeToLive(Constants.TIME_TO_LIVE);

		this.multicastThread = new Thread(this);
		this.multicastThread.start();
	}

	public synchronized void stop() throws IOException {
		if (this.socket != null && socket.isClosed())
			return;
		this.socket.close();
		try {
			multicastThread.join();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	/*
	 * receive messages
	 */
	public void run() {
		byte[] buffer = new byte[64500];
		DatagramPacket multicastPacket = new DatagramPacket(buffer,
				buffer.length);

		while (!socket.isClosed()) {
			try {
				// wait for a message
				this.socket.receive(multicastPacket);
				final byte[] messageToProcess = Arrays.copyOfRange(
						multicastPacket.getData(), 0,
						multicastPacket.getLength());
				final InetAddress address = multicastPacket.getAddress();
				final int port = multicastPacket.getPort();

				// create a worker thread to process the message
				Thread worker = new Thread() {
					@Override
					public void run() {
						try {
							processMessage(messageToProcess, address, port);
						} catch (Exception e) {
							e.printStackTrace();
						}
					};
				};
				worker.start();

			} catch (IOException e) {
				if (socket.isClosed()) {
					return;
				}
				e.printStackTrace();
			}
		}
	}

	/*
	 * send messages
	 */
	public synchronized void sendMulticast(byte[] message) throws Exception {
		DatagramPacket multicastPacket = new DatagramPacket(message,
				message.length, multicastGroup, multicastPort);
		this.socket.send(multicastPacket);
	}

	protected abstract void processMessage(final byte[] trimedMsg,
			final InetAddress address, final int port);

	protected synchronized byte[] getDatagramBytes(
			final DatagramPacket multicastPacket) {
		return Arrays.copyOfRange(multicastPacket.getData(), 0,
				multicastPacket.getLength());
	}
}
