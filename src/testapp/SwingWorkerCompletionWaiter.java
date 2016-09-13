package testapp;

import rmi.RMIResult;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.net.URL;

public class SwingWorkerCompletionWaiter implements PropertyChangeListener {
    private JDialog dialog;
    private JLabel label;
    private JList<String> reportList;
    private int counter;
    private GridBagLayout gridBagLayout;
    private JButton closeButton;

    public SwingWorkerCompletionWaiter(JDialog dialog, String labelText) {
        this.dialog = dialog;
        this.dialog.setTitle("Peer working");
        URL url = this.getClass().getResource("/ajax-loader.gif");
        if (url != null) {
            ImageIcon imageIcon = new ImageIcon(url);
            label = new JLabel(labelText, imageIcon, JLabel.CENTER);
        } else {
            label = new JLabel(labelText, JLabel.CENTER);
        }

        this.gridBagLayout = new GridBagLayout();
        this.dialog.setLayout(gridBagLayout);
        this.counter = 1;

        this.dialog.add(label);
        GridBagConstraints labelConstraints = new GridBagConstraints();
        labelConstraints.gridy = 0;
        labelConstraints.fill = GridBagConstraints.BOTH;
        labelConstraints.insets = new Insets(6, 6, 6, 6);
        this.gridBagLayout.setConstraints(label, labelConstraints);

        reportList = new JList<String>(new DefaultListModel<String>());
        JScrollPane scrollPane = new JScrollPane(reportList);
        GridBagConstraints reportListConstraints = new GridBagConstraints();
        reportListConstraints.gridy = 1;
        reportListConstraints.ipady = 200;
        reportListConstraints.weightx = 1.0;
        reportListConstraints.fill = GridBagConstraints.HORIZONTAL;
        reportListConstraints.insets = new Insets(6, 6, 6, 6);
        this.dialog.add(scrollPane);
        this.gridBagLayout.setConstraints(scrollPane, reportListConstraints);

        this.closeButton = new JButton("Close");
        this.closeButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent arg0) {
                closeDialog();
            }
        });
        this.closeButton.setEnabled(false);
        GridBagConstraints buttonConstraints = new GridBagConstraints();
        buttonConstraints.gridy = 2;
        buttonConstraints.fill = GridBagConstraints.CENTER;

        buttonConstraints.insets = new Insets(6, 6, 6, 6);
        this.gridBagLayout.setConstraints(closeButton, buttonConstraints);
        this.dialog.add(closeButton);

        this.dialog.pack();
        this.dialog.setSize(480, 380);
        this.dialog.setResizable(false);
        this.dialog.setLocationRelativeTo(null);
        this.dialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
    }

    public void propertyChange(PropertyChangeEvent event) {
        if ("iteration".equals(event.getPropertyName())) {
            label.setText((String) event.getNewValue());
            this.dialog.add(label);
            this.dialog.repaint();
        } else if ("state".equals(event.getPropertyName())
                && SwingWorker.StateValue.DONE == event.getNewValue()) {
            this.closeButton.setEnabled(true);
            URL url = this.getClass().getResource("/loader-still.png");
            if (url != null) {
                ImageIcon stillIcon = new ImageIcon(url);
                label.setIcon(stillIcon);
            }
        }
    }

    public void closeDialog() {
        dialog.setVisible(false);
        dialog.dispose();
    }

    public void showResult(RMIResult result) {
        ((DefaultListModel<String>) reportList.getModel()).addElement("[ "
                + counter++ + " ]   " + result.toString());
        reportList.repaint();
    }
}
