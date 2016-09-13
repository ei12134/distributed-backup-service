package database;

import java.io.Serializable;

public class Chunk implements Serializable {

    private static final long serialVersionUID = -5303272977516356133L;
    protected String fileID;
    protected int chunkNo;
    protected int replicationDegree;

    public Chunk(String fileID, int chunkNo, int replicationDegree) {
        this.setFileID(fileID);
        this.setChunkNo(chunkNo);
        this.replicationDegree = replicationDegree;
    }

    public Chunk(String fileID, int chunkNo) {
        this.setFileID(fileID);
        this.setChunkNo(chunkNo);
    }

    public int getReplicationDegree() {
        return replicationDegree;
    }

    public void setReplicationDegree(int replicationDegree) {
        this.replicationDegree = replicationDegree;
    }

    @Override
    public boolean equals(Object other) {
        if (other == null)
            return false;
        if (other == this)
            return true;
        if (!(other instanceof Chunk))
            return false;
        Chunk c = (Chunk) other;
        return (this.fileID.equals(c.fileID) && this.chunkNo == c.chunkNo);
    }

    public String getFileID() {
        return fileID;
    }

    public void setFileID(String fileID) {
        this.fileID = fileID;
    }

    public int getChunkNo() {
        return chunkNo;
    }

    public void setChunkNo(int chunkNo) {
        this.chunkNo = chunkNo;
    }
}
