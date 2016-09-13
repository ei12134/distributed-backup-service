package testapp;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.LinkedList;
import javax.swing.SwingWorker;
import rmi.RMIInterface;
import rmi.RMIResult;
import utils.Constants;
import utils.Constants.protocolType;

public class ActionTask extends
		SwingWorker<LinkedList<RMIResult>, ArrayList<String>> {

	private LinkedList<RMIResult> rmiResults;
	private ArrayList<String> filesPaths;
	private RMIInterface rmiInterface;
	private int replicationDegree;
	private protocolType operation;
	private SwingWorkerCompletionWaiter swing;

	ActionTask(LinkedList<RMIResult> rmiResults, ArrayList<String> filesPaths,
			int replicationDegree, RMIInterface rmiInterface,
			protocolType operation, SwingWorkerCompletionWaiter swing) {
		this.rmiResults = rmiResults;
		this.filesPaths = filesPaths;
		this.replicationDegree = replicationDegree;
		this.rmiInterface = rmiInterface;
		this.operation = operation;

		this.swing = swing;
		this.addPropertyChangeListener(this.swing);
	}

	@Override
	public LinkedList<RMIResult> doInBackground() {
		for (int i = 0; i < filesPaths.size() && !isCancelled(); i++) {
			try {
				RMIResult result = null;
				switch (operation) {
				case BACKUP:
					firePropertyChange("iteration", "", "  Backing up file "
							+ (i + 1) + " out of " + filesPaths.size());
					result = rmiInterface.backupFile(filesPaths.get(i),
							replicationDegree,
							Constants.DEFAULT_PROTOCOL_VERSION);
					rmiResults.add(result);
					break;
				case DELETE:
					firePropertyChange("iteration", "", "  Deleting file "
							+ (i + 1) + " out of " + filesPaths.size());
					result = rmiInterface.deleteFile(filesPaths.get(i),
							Constants.DEFAULT_PROTOCOL_VERSION);
					rmiResults.add(result);
					break;
				case RESTORE:
					firePropertyChange("iteration", "", "  Restoring file "
							+ (i + 1) + " out of " + filesPaths.size());
					result = rmiInterface.restoreFile(filesPaths.get(i),
							Constants.DEFAULT_PROTOCOL_VERSION);
					rmiResults.add(result);
					break;
				case BACKUPMETADATA:
					firePropertyChange("iteration", "",
							"  Backing up metadata...");
					result = rmiInterface.backupMetadata();
					rmiResults.add(result);
					break;
				case RESTOREMETADATA:
					firePropertyChange("iteration", "",
							"  Restoring metadata...");
					result = rmiInterface.restoreMetadata();
					rmiResults.add(result);
					break;
				default:
					break;
				}
				if (result != null)
					this.swing.showResult(result);
			} catch (RemoteException e) {
				e.printStackTrace();
			}
		}
		return rmiResults;
	}
}
