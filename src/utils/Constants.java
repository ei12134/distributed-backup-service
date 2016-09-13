package utils;

import java.io.File;

public class Constants {

	// general
	public static final String SUCCESS = "SUCCESS";
	public static final String FAILURE = "FAILURE";
	public static final int THREAD_SLEEP = 1000;
	public static final int TIME_WAIT = 400;
	public static final int TIME_TO_LIVE = 1;
	public static final int MAX_CHUNK_SIZE_BYTES = 64000;
	public static final int RMI_PORT = 1099;

	// files & folders
	public static final String CHUNKS_FOLDER = "chunks";
	public static final String RESTORED_FILES_FOLDER = "restores";
	public static final String DATABASE_FOLDER = "database";
	public static final String LOCAL_CHUNKS_DATABASE_FILE = DATABASE_FOLDER
			+ File.separator + "localChunksDb";
	public static final String REMOTE_CHUNKS_DATABASE_FILE = DATABASE_FOLDER
			+ File.separator + "remoteChunksDb";
	public static final String REMOTE_FILES_DATABASE_FILE = DATABASE_FOLDER
			+ File.separator + "remoteFilesDb";

	// common protocol constants
	public static final String CRLF = "\r\n";
	public static final String HEADER_TERMINATION = "\r\n\r\n";
	public static final String SPLITTER = " ";
	public static final String DEFAULT_PROTOCOL_VERSION = "1.0";
	public static final String ENHANCED_PROTOCOL_VERSION = "1.3";
	public static final String MAC = "0563ed54059f980dc18088f9a18ab7155362d8d209827718892eb0d5cd7b23c9";
	public static final String CHUNK_FILE_EXTENSION = ".chunk";
	public static final int MAX_DELAY_MILLISECONDS = 400;
	public static final int MAX_TCP_CONNECTION_WAIT_MILLISECONDS = 500;

	// backup protocol constants
	public static final int BACKUP_MIN_CONFIRMATION_WAIT = 1000;
	public static final int BACKUP_MAX_BACKUP_RETRIES = 5;
	public static final int BACKUP_MAX_CONFIRMATION_WAIT = BACKUP_MIN_CONFIRMATION_WAIT
			+ BACKUP_MIN_CONFIRMATION_WAIT
			* 2
			+ BACKUP_MIN_CONFIRMATION_WAIT
			* 4
			+ BACKUP_MIN_CONFIRMATION_WAIT
			* 8
			+ BACKUP_MIN_CONFIRMATION_WAIT
			* 16
			+ BACKUP_MIN_CONFIRMATION_WAIT
			* 4;

	// delete protocol constants
	public static final long DELETE_MIN_WAITING = 15000; // 15 seconds
	public static final long DELETE_MAX_WAITING = 960000; // 16 minutes

	public static enum protocolType {
		BACKUP, BACKUPENH, RESTORE, RESTOREENH, DELETE, DELETEENH, RECLAIM, RECLAIMENH, BACKUPMETADATA, RESTOREMETADATA
	};

	// message types
	public static final String PUT_CHUNK = "PUTCHUNK";
	public static final String GET_CHUNK = "GETCHUNK";
	public static final String DELETE = "DELETE";
	public static final String DELETEACK = "DELETEACK";
	public static final String REMOVED = "REMOVED";
	public static final String STORED = "STORED";
	public static final String CHUNK = "CHUNK";
}
