package testapp;

import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.URL;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ImageIcon;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRootPane;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;

import rmi.RMIInterface;

public class TestAppGUI extends JFrame {
	private static final long serialVersionUID = -1L;
	private JPanel contentPane;
	private JTextField peerIdTextField;

	public TestAppGUI() {

		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (Exception e) {
			e.printStackTrace();
		}
		// ----------------------------------------------------------
		setResizable(false);
		setTitle("Peer TestApp");
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setSize(250, 310);
		setLocationRelativeTo(null);
		contentPane = new JPanel();
		contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
		setContentPane(contentPane);
		contentPane.setLayout(null);
		peerIdTextField = new JTextField();
		peerIdTextField.setBounds(50, 112, 144, 20);
		peerIdTextField.setColumns(10);
		peerIdTextField.setHorizontalAlignment(JLabel.CENTER);
		peerIdTextField.setText("1");
		contentPane.add(peerIdTextField);

		JLabel peerIdLabel = new JLabel("Peer id:");
		peerIdLabel.setBounds(50, 84, 76, 15);
		contentPane.add(peerIdLabel);

		JButton connectButton = new JButton("Connect");
		getRootPane().setDefaultButton(connectButton); // ENTER key to connect
		connectButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				try {
					int peerId = Integer.parseInt(peerIdTextField.getText());
					try {
						// setVisible(false);
						startTest(peerId + "");
					} catch (RemoteException e1) {
						JOptionPane.showMessageDialog(TestAppGUI.this,
								e1.getMessage());
					} catch (NotBoundException e2) {
						JOptionPane
								.showMessageDialog(
										TestAppGUI.this,
										"A NotBoundException was thrown because an attempt\nwas made to lookup or unbind in the registry a name\nthat has no associated binding.");
					}
					// setVisible(true);
				} catch (NumberFormatException err) {
					JOptionPane.showMessageDialog(TestAppGUI.this,
							"The peer id must be an integer!");
				}
			}
		});
		connectButton.setBounds(65, 210, 120, 24);
		contentPane.add(connectButton);
	}

	// login Here ------------------------------------------
	private void startTest(String peerId) throws RemoteException,
			NotBoundException {

		// client only uses the RMI interface
		Registry registry = LocateRegistry.getRegistry();
		RMIInterface rmiInterface = (RMIInterface) registry.lookup(peerId);
		new PeerActionTester(TestAppGUI.this, rmiInterface, peerId);
	}

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

	public void setIcon() {
		URL urlPath = getClass().getResource("/icon.png");
		if (urlPath != null) {
			ImageIcon imageIcon = new ImageIcon(getClass().getResource(
					"/icon.png"));
			this.setIconImage(imageIcon.getImage());
		}
	}

	// -----------------------------------------------------
	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					System.setProperty("awt.useSystemAAFontSettings", "on");
					System.setProperty("swing.aatext", "true");
					TestAppGUI frame = new TestAppGUI();
					frame.setIcon();
					frame.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}
}
