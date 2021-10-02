package com.marginallyclever.robotOverlord.robotArmInterface.marlinInterface;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;

import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.UIManager;

import com.marginallyclever.communications.NetworkSession;
import com.marginallyclever.communications.NetworkSessionEvent;
import com.marginallyclever.communications.NetworkSessionListener;
import com.marginallyclever.convenience.log.Log;

public class TextInterfaceToNetworkSession extends JPanel implements NetworkSessionListener {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1032123255711692874L;
	private TextInterfaceWithHistory myInterface = new TextInterfaceWithHistory();
	private ChooseConnectionPanel myConnectionChoice = new ChooseConnectionPanel();
	private NetworkSession mySession;

	public TextInterfaceToNetworkSession() {
		super();

		this.setBorder(BorderFactory.createTitledBorder("TextInterfaceToNetworkSession"));
		setLayout(new BorderLayout());
		
		add(myConnectionChoice,BorderLayout.NORTH);
		add(myInterface,BorderLayout.CENTER);
		
		myInterface.setEnabled(false);
		myInterface.addActionListener( (evt) -> {
			if(mySession==null) return;
			
			String str = evt.getActionCommand();
			str = str.toUpperCase();
			if(!str.endsWith("\n")) str+="\n";
			
			try {
				mySession.sendMessage(str);
			} catch (Exception e1) {
				JOptionPane.showMessageDialog(this,e1.getLocalizedMessage(),"Error",JOptionPane.ERROR_MESSAGE);
			}
		});
		myConnectionChoice.addActionListener((e)->{
			switch(e.getID()) {
			case ChooseConnectionPanel.CONNECTION_OPENED: 
				setNetworkSession(myConnectionChoice.getNetworkSession());
				break;
			case ChooseConnectionPanel.CONNECTION_CLOSED:
				setNetworkSession(null);
				break;
			}
			
			notifyListeners(e);
		});
	}
	
	private void setNetworkSession(NetworkSession session) {
		if(mySession!=null) mySession.removeListener(this);
		mySession = session;
		if(mySession!=null) mySession.addListener(this);
		
		myConnectionChoice.setNetworkSession(session);
		myInterface.setEnabled(mySession!=null);
	}

	public void sendCommand(String str) {
		myInterface.sendCommand(str);
	}
	
	public String getCommand() {
		return myInterface.getCommand();
	}

	public void setCommand(String str) {
		myInterface.setCommand(str);
	}
	
	@Override
	public void networkSessionEvent(NetworkSessionEvent evt) {
		if(evt.flag == NetworkSessionEvent.DATA_AVAILABLE) {
			myInterface.addToHistory(mySession.getName(),((String)evt.data).trim());
		}
	}
	
	// OBSERVER PATTERN
	
	private ArrayList<ActionListener> listeners = new ArrayList<ActionListener>();
	public void addActionListener(ActionListener a) {
		listeners.add(a);
	}
	
	public void removeActionListener(ActionListener a) {
		listeners.remove(a);
	}
	
	private void notifyListeners(ActionEvent e) {
		for( ActionListener a : listeners ) {
			a.actionPerformed(e);
		}
	}

	public void addNetworkSessionListener(NetworkSessionListener a) {
		mySession.addListener(a);
	}
	
	public void removeNetworkSessionListener(NetworkSessionListener a) {
		mySession.removeListener(a);
	}

	// TEST 
	
	public static void main(String[] args) {
		Log.start();
		JFrame frame = new JFrame("TextInterfaceToNetworkSession");
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch(Exception e) {}
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setPreferredSize(new Dimension(600, 400));
		frame.add(new TextInterfaceToNetworkSession());
		frame.pack();
		frame.setVisible(true);
	}

	public boolean getIsConnected() { 
		return (mySession!=null && mySession.isOpen());
	}
}