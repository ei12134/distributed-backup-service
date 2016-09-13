package database;

import java.io.Serializable;

public class LocalChunk extends Chunk implements Serializable {

	private static final long serialVersionUID = 5726005962856729855L;
	private byte[] data;

	public LocalChunk(String fileId, int chunkNo, int replicationDegree,
			byte[] data) {
		super(fileId, chunkNo, replicationDegree);
		this.data = data;
	}

	public byte[] getData() {
		return data;
	}

	public void setData(byte[] data) {
		this.data = data;
	}
}
