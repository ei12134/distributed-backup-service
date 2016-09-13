package testapp;

import java.awt.Component;
import java.awt.Desktop;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.LinkedList;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ImageIcon;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFormattedTextField;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRootPane;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.ScrollPaneConstants;
import javax.swing.SpinnerModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;

import database.RemoteFile;
import rmi.RMIInterface;
import rmi.RMIResult;
import utils.Constants.protocolType;

public class PeerActionTester extends JDialog {
	private static final long serialVersionUID = 1L;

	private JPanel contentPane;
	private String restoresFolder;
	private RMIInterface rmiInterface;
	private JButton btnChooseFile;
	private JTextField textFieldFilepath;
	private JButton btnDelete;
	private JButton btnRestore;
	private JButton btnRestoresFolder;
	private JButton btnReclaim;
	private JLabel labelReclaim;
	private JTextField textFieldReclaim;
	private JCheckBox checkboxHiddenFiles;
	private JFileChooser fileChooser;
	private JSpinner spinner;
	private ImageIcon loading;
	private JTable tableFiles;
	private DefaultTableModel tableModel;
	private File[] selectedFiles;

	public PeerActionTester(final JFrame parentFrame,
			RMIInterface rmiPeerInterface, String peer_id)
			throws RemoteException {
		super(parentFrame, "Connected to Peer " + peer_id, true);
		this.rmiInterface = rmiPeerInterface;
		this.restoresFolder = rmiInterface.getRestoresFolder();
		this.fileChooser = new JFileChooser();
		this.fileChooser.setMultiSelectionEnabled(true);
		this.selectedFiles = new File[0];

		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (Exception e) {
			e.printStackTrace();
		}

		setResizable(false);
		setSize(640, 550);
		setLocationRelativeTo(null);
		contentPane = new JPanel();
		contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
		setContentPane(contentPane);
		contentPane.setLayout(null);

		btnChooseFile = new JButton("Select file(s)");
		btnChooseFile.setToolTipText("Select file(s) to backup");
		btnChooseFile.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				fileChooser.setFileHidingEnabled(!checkboxHiddenFiles
						.isSelected());

				int returnValue = fileChooser.showOpenDialog(null);

				if (returnValue == JFileChooser.APPROVE_OPTION) {
					selectedFiles = fileChooser.getSelectedFiles();
					String textFieldFilepathText = "";
					for (File f : selectedFiles) {
						textFieldFilepathText += f.getPath() + "  ";
					}
					textFieldFilepath.setText(textFieldFilepathText);
				}
			}
		});
		btnChooseFile.setBounds(12, 24, 125, 24);
		contentPane.add(btnChooseFile);

		textFieldFilepath = new JTextField();
		textFieldFilepath.setEditable(false);
		textFieldFilepath.setBounds(146, 24, 325, 24);
		contentPane.add(textFieldFilepath);
		textFieldFilepath.setColumns(10);

		checkboxHiddenFiles = new JCheckBox("Show hidden files");
		checkboxHiddenFiles.setBounds(476, 24, 256, 24);
		contentPane.add(checkboxHiddenFiles);

		JLabel labelReplicationDegree = new JLabel("Replication degree:");
		labelReplicationDegree.setBounds(112, 66, 148, 24);
		contentPane.add(labelReplicationDegree);

		spinner = createSpinner();
		spinner.setBounds(274, 66, 64, 24);
		contentPane.add(spinner);

		JButton btnBackup = new JButton("Backup");
		btnBackup.setToolTipText("Backup the selected file(s)");
		btnBackup.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {

				ArrayList<String> selectedFilesPaths = new ArrayList<String>();

				for (int i = 0; i < selectedFiles.length; i++)
					if (selectedFiles[i].exists()
							&& !selectedFiles[i].isDirectory()) {
						String path = selectedFiles[i].getPath();
						for (int x = tableModel.getRowCount() - 1; x > -1; x--) {
							if (path.equals(tableModel.getValueAt(x, 0)
									.toString())) {
								JOptionPane.showMessageDialog(
										PeerActionTester.this, "The file \""
												+ path
												+ "\" was already backed up.",
										"Backup error",
										JOptionPane.ERROR_MESSAGE);
								return;
							}
						}
						selectedFilesPaths.add(selectedFiles[i].getPath());
					}

				if (selectedFilesPaths.size() > 0) {
					setPanelEnabled(contentPane, false);
					int replicationDegree = (Integer) spinner.getValue();
					JDialog dialog = new JDialog(parentFrame, true);

					SwingWorkerCompletionWaiter swing = new SwingWorkerCompletionWaiter(
							dialog, " Backing up file " + 0 + " out of "
									+ selectedFiles.length);

					ActionTask task = new ActionTask(
							new LinkedList<RMIResult>(), selectedFilesPaths,
							replicationDegree, rmiInterface, protocolType
									.valueOf("BACKUP"), swing);

					task.execute();
					dialog.setVisible(true);
					updateTable();
					setPanelEnabled(contentPane, true);
				} else {
					JOptionPane.showMessageDialog(PeerActionTester.this,
							"No valid files to backup were selected.",
							"Backup error", JOptionPane.ERROR_MESSAGE);
				}
			}
		});
		btnBackup.setBounds(362, 66, 120, 24);
		contentPane.add(btnBackup);

		JSeparator topSeparator = new JSeparator();
		topSeparator.setBounds(12, 102, 614, 12);
		contentPane.add(topSeparator);

		tableModel = new DefaultTableModel() {
			private static final long serialVersionUID = 1L;

			@Override
			public boolean isCellEditable(int row, int column) {
				return false;
			}
		};

		tableFiles = new JTable(tableModel);
		DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
		centerRenderer.setHorizontalAlignment(JLabel.CENTER);
		DefaultTableCellRenderer leftCellRenderer = new DefaultTableCellRenderer();
		leftCellRenderer.setHorizontalAlignment(JLabel.LEFT);

		tableModel.addColumn("Path");
		tableModel.addColumn("Number of chunks");

		TableColumn pathColumn = tableFiles.getColumnModel().getColumn(0);
		pathColumn.setCellRenderer(leftCellRenderer);
		pathColumn.setMinWidth(250);
		TableColumn chunksColumn = tableFiles.getColumnModel().getColumn(1);
		chunksColumn.setCellRenderer(centerRenderer);
		chunksColumn.setPreferredWidth(8);
		tableFiles.setRowHeight(24);

		JScrollPane scrollPane = new JScrollPane(tableFiles);
		scrollPane
				.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		scrollPane.setBounds(12, 114, 614, 188);
		contentPane.add(scrollPane);

		btnDelete = new JButton("Delete");
		btnDelete.setToolTipText("Delete the selected table files");
		btnDelete.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				int[] selectedRows = tableFiles.getSelectedRows();

				if (selectedRows.length > 0) {
					ArrayList<String> filePaths = new ArrayList<String>();
					for (int i = 0; i < selectedRows.length; i++) {
						String filePath = (String) tableFiles.getValueAt(
								selectedRows[i], 0);
						filePaths.add(filePath);
					}

					setPanelEnabled(contentPane, false);
					int replicationDegree = (Integer) spinner.getValue();
					JDialog dialog = new JDialog(parentFrame, true);

					SwingWorkerCompletionWaiter swing = new SwingWorkerCompletionWaiter(
							dialog, " Deleting file " + 0 + " out of "
									+ selectedFiles.length);

					ActionTask task = new ActionTask(
							new LinkedList<RMIResult>(), filePaths,
							replicationDegree, rmiInterface, protocolType
									.valueOf("DELETE"), swing);

					task.execute();
					dialog.setVisible(true);

					updateTable();
					setPanelEnabled(contentPane, true);

					// JOptionPane.showMessageDialog(PeerActionTester.this,
					// result);
				} else {
					JOptionPane.showMessageDialog(PeerActionTester.this,
							"At least one file must be selected for deletion.",
							"Delete error", JOptionPane.ERROR_MESSAGE);
				}
			}
		});
		btnDelete.setBounds(41, 310, 144, 24);
		btnDelete.setEnabled(false);
		contentPane.add(btnDelete);

		btnRestore = new JButton("Restore");
		btnRestore.setToolTipText("Restore the selected table files");
		btnRestore.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {

				int[] selectedRows = tableFiles.getSelectedRows();

				if (selectedRows.length > 0) {
					ArrayList<String> filePaths = new ArrayList<String>();
					for (int i = 0; i < selectedRows.length; i++) {
						String filePath = (String) tableFiles.getValueAt(
								selectedRows[i], 0);
						filePaths.add(filePath);
					}

					setPanelEnabled(contentPane, false);
					int replicationDegree = (Integer) spinner.getValue();
					JDialog dialog = new JDialog(parentFrame, true);

					SwingWorkerCompletionWaiter swing = new SwingWorkerCompletionWaiter(
							dialog, " Restoring file " + 0 + " out of "
									+ selectedFiles.length);

					ActionTask task = new ActionTask(
							new LinkedList<RMIResult>(), filePaths,
							replicationDegree, rmiInterface, protocolType
									.valueOf("RESTORE"), swing);

					task.execute();
					dialog.setVisible(true);
					updateTable();
					setPanelEnabled(contentPane, true);

				} else {
					JOptionPane
							.showMessageDialog(
									PeerActionTester.this,
									"At least one file must be selected for restoration.",
									"Restore error", JOptionPane.ERROR_MESSAGE);
				}
			}
		});
		btnRestore.setBounds(248, 310, 144, 24);
		btnRestore.setEnabled(false);
		contentPane.add(btnRestore);
		btnRestoresFolder = new JButton("Restores folder");

		btnRestoresFolder.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				try {
					Desktop desktop = Desktop.getDesktop();
					File f = new File(restoresFolder);
					if (f.exists() && f.isDirectory()) {
						desktop.open(f);
					} else {
						JOptionPane.showMessageDialog(PeerActionTester.this,
								"Invalid peer restores folder path.",
								"Restores folder error",
								JOptionPane.ERROR_MESSAGE);
					}
				} catch (IOException e1) {
					e1.printStackTrace();
				}
			}
		});

		btnRestoresFolder.setBounds(455, 310, 144, 24);
		btnRestoresFolder
				.setToolTipText("Open the connected peer restores folder");
		btnRestoresFolder.setEnabled(true);
		contentPane.add(btnRestoresFolder);

		JSeparator middleSeparator = new JSeparator();
		middleSeparator.setBounds(12, 348, 614, 12);
		contentPane.add(middleSeparator);

		labelReclaim = new JLabel("Space to reclaim (KBytes):");
		labelReclaim.setBounds(80, 360, 210, 24);
		contentPane.add(labelReclaim);

		textFieldReclaim = new JTextField();
		textFieldReclaim.setBounds(280, 360, 120, 24);
		textFieldReclaim.setColumns(3);
		textFieldReclaim.setToolTipText("Set the amount of KBytes to free");
		textFieldReclaim.setText("0");
		textFieldReclaim.setHorizontalAlignment(JLabel.CENTER);
		contentPane.add(textFieldReclaim);

		btnReclaim = new JButton("Reclaim");
		btnReclaim.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				try {
					try {
						int spaceToReclaim = Integer.parseInt(textFieldReclaim
								.getText());
						if (spaceToReclaim <= 0) {
							throw (new NumberFormatException());
						}

						RMIResult result = rmiInterface
								.reclaimSpace(spaceToReclaim);

						JOptionPane.showMessageDialog(PeerActionTester.this,
								result);
					} catch (RemoteException ex) {
						JOptionPane.showMessageDialog(PeerActionTester.this,
								ex.getMessage());
					}
				} catch (NumberFormatException ex) {
					JOptionPane
							.showMessageDialog(
									PeerActionTester.this,
									"The space to reclaim value must be a valid positive integer.",
									"Reclaim error", JOptionPane.ERROR_MESSAGE);
				}
			}
		});
		btnReclaim.setBounds(427, 360, 120, 24);
		contentPane.add(btnReclaim);

		JSeparator bottomSeparator = new JSeparator();
		bottomSeparator.setBounds(12, 398, 614, 12);
		contentPane.add(bottomSeparator);

		JButton btnBackupMetadata = new JButton("Backup metadata");
		btnBackupMetadata.setToolTipText("Backup peer metadata");
		btnBackupMetadata.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				setPanelEnabled(contentPane, false);
				int replicationDegree = (Integer) spinner.getValue();
				JDialog dialog = new JDialog(parentFrame, true);

				SwingWorkerCompletionWaiter swing = new SwingWorkerCompletionWaiter(
						dialog, "  Backing up metadata...");

				ArrayList<String> selectedFilesPaths = new ArrayList<String>();
				selectedFilesPaths.add("invalid_path");
				ActionTask task = new ActionTask(new LinkedList<RMIResult>(),
						selectedFilesPaths, replicationDegree, rmiInterface,
						protocolType.valueOf("BACKUPMETADATA"), swing);

				task.execute();
				dialog.setVisible(true);
				updateTable();
				setPanelEnabled(contentPane, true);
			}
		});
		btnBackupMetadata.setBounds(110, 416, 180, 24);
		contentPane.add(btnBackupMetadata);

		JButton btnRestoreMetadata = new JButton("Restore metadata");
		btnRestoreMetadata.setToolTipText("Restore peer metadata");
		btnRestoreMetadata.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				setPanelEnabled(contentPane, false);
				int replicationDegree = (Integer) spinner.getValue();
				JDialog dialog = new JDialog(parentFrame, true);

				SwingWorkerCompletionWaiter swing = new SwingWorkerCompletionWaiter(
						dialog, "  Restoring metadata...");

				ArrayList<String> selectedFilesPaths = new ArrayList<String>();
				selectedFilesPaths.add("invalid_path");
				ActionTask task = new ActionTask(new LinkedList<RMIResult>(),
						selectedFilesPaths, replicationDegree, rmiInterface,
						protocolType.valueOf("RESTOREMETADATA"), swing);

				task.execute();
				dialog.setVisible(true);
				updateTable();
				setPanelEnabled(contentPane, true);
			}
		});
		btnRestoreMetadata.setBounds(320, 416, 180, 24);
		contentPane.add(btnRestoreMetadata);

		JSeparator lastBottomSeparator = new JSeparator();
		lastBottomSeparator.setBounds(12, 458, 614, 12);
		contentPane.add(lastBottomSeparator);

		JButton btnDisconnect = new JButton("Disconnect");
		btnDisconnect.setToolTipText("Disconnect from the peer");
		btnDisconnect.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				dispose();
			}
		});
		btnDisconnect.setBounds(260, 476, 120, 24);
		contentPane.add(btnDisconnect);
		updateTable();
		setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
		this.setVisible(true);
	}

	public static JSpinner createSpinner() {
		JSpinner spinner = new JSpinner(new SpinnerNumberModel(1, 1, 999, 1));
		spinner.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
				JSpinner spinner = (JSpinner) e.getSource();
				SpinnerModel spinnerModel = spinner.getModel();
				System.out.println(spinnerModel.getValue());
			}
		});

		JComponent editor = spinner.getEditor();
		JFormattedTextField tf = ((JSpinner.DefaultEditor) editor)
				.getTextField();
		tf.setColumns(3);
		return spinner;
	}

	void setLoading() {
		loading = new ImageIcon("/res/gifs/ajax-loader.gif");
		contentPane.add(new JLabel("loading... ", loading, JLabel.CENTER));
	}

	void setPanelEnabled(JPanel panel, Boolean isEnabled) {
		panel.setEnabled(isEnabled);
		Component[] components = panel.getComponents();

		for (int i = 0; i < components.length; i++) {
			if (components[i].getClass().getName() == "javax.swing.JPanel") {
				setPanelEnabled((JPanel) components[i], isEnabled);
			}

			components[i].setEnabled(isEnabled);
		}
		repaint();
	}

	void updateTable() {

		for (int i = tableModel.getRowCount() - 1; i > -1; i--) {
			tableModel.removeRow(i);
		}

		ArrayList<RemoteFile> remoteFiles;
		try {
			remoteFiles = rmiInterface.getRemoteFiles();
			for (RemoteFile rf : remoteFiles) {

				tableModel.addRow(new Object[] { rf.getFilePath(),
						rf.getChunkCount() });
			}
		} catch (RemoteException e) {
			e.printStackTrace();
		}

		if (tableModel.getRowCount() > 0) {
			btnDelete.setEnabled(true);
			btnRestore.setEnabled(true);
		} else {
			btnDelete.setEnabled(false);
			btnRestore.setEnabled(false);
		}
	}

	// Exit when the ESCAPE key is pressed
	@Override
	protected JRootPane createRootPane() {
		JRootPane rootPane = new JRootPane();
		KeyStroke stroke = KeyStroke.getKeyStroke("ESCAPE");
		Action actionListener = new AbstractAction() {
			private static final long serialVersionUID = -6263573854878451452L;

			public void actionPerformed(ActionEvent actionEvent) {
				dispose();
			}
		};
		InputMap inputMap = rootPane
				.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
		inputMap.put(stroke, "ESCAPE");
		rootPane.getActionMap().put("ESCAPE", actionListener);

		return rootPane;
	}

	// public class ProgressFrame {
	//
	// private String title;
	// private int current;
	// private int total;
	// private JLabel label;
	// private JDialog frame;
	//
	// public ProgressFrame(JFrame parentFrame, String title, int current,
	// int total) {
	// setTitle(title);
	// setCurrent(current);
	// setTotal(total);
	// frame = new JDialog(parentFrame, getTitle(), true);
	// URL url = this.getClass().getResource("/ajax-loader.gif");
	// ImageIcon imageIcon = new ImageIcon(url);
	// label = new JLabel(" " + title + " file " + current + "/" + total,
	// imageIcon, JLabel.CENTER);
	//
	// frame.add(label);
	// frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
	// frame.setSize(320, 240);
	// frame.setLocationRelativeTo(null);
	// frame.setAlwaysOnTop(true);
	// frame.setVisible(true);
	// frame.toFront();
	// frame.repaint();
	// }
	//
	// public void close() {
	// frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
	// frame.dispose();
	// }
	//
	// public String getTitle() {
	// return title;
	// }
	//
	// public void setTitle(String title) {
	// this.title = title;
	// }
	//
	// public int getCurrent() {
	// return current;
	// }
	//
	// public void setCurrent(int current) {
	// this.current = current;
	// }
	//
	// public int getTotal() {
	// return total;
	// }
	//
	// public void setTotal(int total) {
	// this.total = total;
	// }
	// }
}
