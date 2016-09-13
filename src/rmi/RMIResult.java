package rmi;

import java.io.Serializable;

public class RMIResult implements Serializable {

	private static final long serialVersionUID = 5229695236831912799L;
	private boolean success;
	private String msg;

	public RMIResult(boolean success, String msg) {
		this.success = success;
		this.msg = msg;
	}

	public boolean successful() {
		return this.success;
	}

	public String getMessage() {
		return this.msg;
	}

	public String toString() {
		return (this.msg + ".");
	}
}
