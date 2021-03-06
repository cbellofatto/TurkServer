package edu.harvard.econcs.turkserver.server.gui;

import edu.harvard.econcs.turkserver.client.JTextFieldLimit;
import edu.harvard.econcs.turkserver.client.LobbyPanel;
import edu.harvard.econcs.turkserver.client.SortedListModel;
import edu.harvard.econcs.turkserver.server.ExperimentControllerImpl;
import edu.harvard.econcs.turkserver.server.HITWorkerImpl;
import edu.harvard.econcs.turkserver.server.Lobby;
import edu.harvard.econcs.turkserver.server.SessionServer;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Comparator;
import java.util.Set;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ListCellRenderer;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

import net.andrewmao.misc.Utils;

public class ServerPanel extends JPanel implements ActionListener {

	private static final long serialVersionUID = -5350221106754787412L;
	
	private static final String updateStatusCmd = "UpdateStatus";
	
	private static final String runningExpsText = "Running Experiments: ";
	private static final String doneExpsText = "Completed Experiments: ";
	
	private final SessionServer server;
	private final Lobby lobby;
	
	private JTextField statusMsg;
	
	private SortedListModel<HITWorkerImpl> userListModel;
	
	private DefaultListModel<ExperimentControllerImpl> runningExpModel;
	private JList<ExperimentControllerImpl> runningExpList;
	private DefaultListModel<ExperimentControllerImpl> doneExpModel;
	private JList<ExperimentControllerImpl> doneExpList;
	
	private JLabel currentUsers;		
	
	private JLabel runningExpsLabel;
	private JLabel doneExpsLabel;
	
	private Timer timeTicker;
	
	public ServerPanel(SessionServer host, Lobby lobby) {
		// Put lobby on left and experiments on right
		super(new GridLayout(1, 2));
		this.server = host;
		this.lobby = lobby;					
		
		// Lobby
		JPanel lobbyPanel = new JPanel();
		lobbyPanel.setLayout(new BoxLayout(lobbyPanel, BoxLayout.PAGE_AXIS));
		lobbyPanel.setBorder(BorderFactory.createTitledBorder("Lobby"));
		
		JPanel statusPanel = new JPanel();
		// Limit the height of this panel
		statusPanel.setMaximumSize(new Dimension(800, 100));
		statusPanel.setLayout(new BoxLayout(statusPanel, BoxLayout.LINE_AXIS));
		
		JLabel statusLabel = new JLabel("Message:");
		statusMsg = new JTextField(40);
		statusMsg.setDocument(new JTextFieldLimit(80));
		JButton updateStatusButton = new JButton("Update Status");
		updateStatusButton.setActionCommand(updateStatusCmd);
		updateStatusButton.addActionListener(this);								
		
		statusPanel.add(statusLabel);
		statusPanel.add(statusMsg);
		statusPanel.add(updateStatusButton);
		
		lobbyPanel.add(statusPanel);
		
		currentUsers = new JLabel();
		lobbyPanel.add(currentUsers);
		userListModel = new SortedListModel<HITWorkerImpl>(new UsernameComparator());
		JList<HITWorkerImpl> userList = new JList<>(userListModel);
		userList.setCellRenderer(new ServerLobbyCellRenderer());
		lobbyPanel.add(new JScrollPane(userList));
		
		// Experiments
		JPanel expPanel = new JPanel();						
		expPanel.setBorder(BorderFactory.createTitledBorder("Experiments"));
		expPanel.setLayout(new GridLayout(2, 1));
		
		// Running experiments
		JPanel runningExpPanel = new JPanel();
		runningExpPanel.setLayout(new BoxLayout(runningExpPanel, BoxLayout.PAGE_AXIS));
		runningExpPanel.setBorder(BorderFactory.createEtchedBorder());
		
		JButton btnInitiateShutdown = new JButton("Initiate Shutdown");
		btnInitiateShutdown.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				int selection = JOptionPane.showConfirmDialog(ServerPanel.this, 
						"This will block new experiments from starting, " +
						"wait for ones in progress to finish, and then shut down server. Proceed?",
						"Initiate Shutdown", JOptionPane.OK_CANCEL_OPTION);
				
				if (selection == JOptionPane.OK_OPTION ) {
					server.shutdown();
				}
			}
		});
		runningExpPanel.add(btnInitiateShutdown);
		
		runningExpsLabel = new JLabel(runningExpsText + 0);
		runningExpPanel.add(runningExpsLabel);
		
		runningExpModel = new DefaultListModel<ExperimentControllerImpl>();		
		runningExpList = new JList<ExperimentControllerImpl>(runningExpModel);
		runningExpList.setCellRenderer(new RunningExpCellRenderer());
		runningExpPanel.add(new JScrollPane(runningExpList));
		
		// Done experiments
		JPanel doneExpPanel = new JPanel();
		doneExpPanel.setLayout(new BoxLayout(doneExpPanel, BoxLayout.PAGE_AXIS));
		doneExpPanel.setBorder(BorderFactory.createEtchedBorder());
		
		doneExpsLabel = new JLabel(doneExpsText + 0);
		doneExpPanel.add(doneExpsLabel);
		
		doneExpModel = new DefaultListModel<ExperimentControllerImpl>();
		doneExpList = new JList<ExperimentControllerImpl>(doneExpModel);
		doneExpPanel.add(new JScrollPane(doneExpList));
		
		expPanel.add(runningExpPanel);
		expPanel.add(doneExpPanel);
				
		add(lobbyPanel);
		add(expPanel);
		
		timeTicker = new Timer(1000, this);
		timeTicker.start();
	}	
	
	@Override
	public void actionPerformed(ActionEvent e) {
		if( e.getActionCommand() != null && e.getActionCommand().equals(updateStatusCmd) ) {
			lobby.setMessage(statusMsg.getText());	
		}
		else if( e.getSource() == timeTicker && runningExpModel.size() > 0 ) {			
			// Only bother with this if there are running experiments
			runningExpList.repaint();
		}
	}

	public void updateLobbyModel() {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				Set<HITWorkerImpl> lobbyppl = lobby.getLobbyUsers();
				// Update user count
				currentUsers.setText("Users: " + lobbyppl.size());															
				// Update users list				
				userListModel.updateModel(lobbyppl);
			}			
		});		
	}
	
	private class ServerLobbyCellRenderer extends JLabel implements ListCellRenderer<HITWorkerImpl> {							
		private static final long serialVersionUID = -9092662058995935206L;

		@Override
		public Component getListCellRendererComponent(JList<? extends HITWorkerImpl> list, 
				HITWorkerImpl id, int index, boolean isSelected, boolean cellHasFocus) {			
			
			Object status = lobby.getStatus(id);			
			if( status != null) setIcon( (Boolean) status == true ? LobbyPanel.ready : LobbyPanel.notReady );			
			
			setText( id.getUsername() );
			// TODO render textual messages
			
			setEnabled(true); // not list.isEnabled()); otherwise the icon won't draw
			setFont(list.getFont());
			setOpaque(true);
			
			return (JLabel) this;
		}		
	}

	public void newExperiment(final ExperimentControllerImpl exp) {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {				
				runningExpsLabel.setText(runningExpsText + server.getExpsInProgress());
				runningExpModel.addElement(exp);				
			}			
		});
	}
	
	private class RunningExpCellRenderer extends JLabel implements ListCellRenderer<ExperimentControllerImpl> {
		private static final long serialVersionUID = 5708685323712954603L;

		@Override
		public Component getListCellRendererComponent(JList<? extends ExperimentControllerImpl> list,
				ExperimentControllerImpl exp,	int index, boolean isSelected, boolean cellHasFocus) {			
			
			setText( String.format("%s %s R:%d (%d)",
					Utils.paddedClockString(System.currentTimeMillis() - exp.getStartTime()), 
					exp.toString(), exp.getCurrentRound(), exp.getGroup().groupSize()
					));			
			
			setEnabled(true); // not list.isEnabled()); otherwise the icon won't draw
			setFont(list.getFont());
			setOpaque(true);
			
			return (JLabel) this;
		}
	
	}

	public void finishedExperiment(final ExperimentControllerImpl exp) {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				runningExpsLabel.setText(runningExpsText + server.getExpsInProgress());
				runningExpModel.removeElement(exp);
				
				doneExpsLabel.setText(doneExpsText + server.getExpsCompleted());
				doneExpModel.addElement(exp);
			}			
		});	
	}
	
	public class UsernameComparator implements Comparator<HITWorkerImpl> {	
		@Override
		public int compare(HITWorkerImpl o1, HITWorkerImpl o2) {
			String u1 = o1.getUsername();
			String u2 = o1.getUsername();

			if( u1 != null ) {
				int comp = u1.compareTo(u2);
				if( comp != 0 ) return comp;				
			}
			
			return o1.getHitId().compareTo(o2.getHitId());
		}	
	}
}
