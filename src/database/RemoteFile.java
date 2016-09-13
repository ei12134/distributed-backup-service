package database;

import java.io.Serializable;

public class RemoteFile implements Serializable {

	private static final long serialVersionUID = -2453778688572360599L;
	private String filePath;
	private String fileID;
	private int chunkCount;
	private String fileName;

	public RemoteFile(String filePath, String fileID, int chunkCount,
			String fileName) {
		this.filePath = filePath;
		this.fileID = fileID;
		this.setChunkCount(chunkCount);
		this.fileName = fileName;
	}

	public String getFilePath() {
		return filePath;
	}

	public void setFilePath(String filePath) {
		this.filePath = filePath;
	}

	public RemoteFile(String filePath) {
		this.filePath = filePath;
		this.fileID = "";
		this.setChunkCount(0);
		this.fileName = "";
	}

	@Override
	public boolean equals(Object other) {
		if (other == null)
			return false;
		if (other == this)
			return true;
		if (!(other instanceof Chunk))
			return false;
		RemoteFile rf = (RemoteFile) other;
		return (this.filePath.equals(rf.filePath));
	}

	public String getFileName() {
		return fileName;
	}

	public void setFileName(String fileName) {
		this.fileName = fileName;
	}

	public String getFileID() {
		return fileID;
	}

	public void setFileID(String fileID) {
		this.fileID = fileID;
	}

	public int getChunkCount() {
		return chunkCount;
	}

	public void setChunkCount(int chunkCount) {
		this.chunkCount = chunkCount;
	}
}