package com.marginallyclever.robotOverlord.sixi3Interface.jogInterface;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.FlowLayout;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.vecmath.Matrix3d;
import javax.vecmath.Matrix4d;
import javax.vecmath.Vector3d;

import com.marginallyclever.convenience.MatrixHelper;
import com.marginallyclever.convenience.log.Log;
import com.marginallyclever.robotOverlord.robots.sixi3.JacobianNewtonRaphson;
import com.marginallyclever.robotOverlord.robots.sixi3.Sixi3IK;

public class CartesianDrivePanel extends JPanel {
	private static final long serialVersionUID = 1L;
	private ButtonGroup buttonGroup = new ButtonGroup();
	private JRadioButton x = makeRadioButton(buttonGroup,"X");
	private JRadioButton y = makeRadioButton(buttonGroup,"Y");
	private JRadioButton z = makeRadioButton(buttonGroup,"Z");
	private JRadioButton roll = makeRadioButton(buttonGroup,"roll");
	private JRadioButton pitch = makeRadioButton(buttonGroup,"pitch");
	private JRadioButton yaw = makeRadioButton(buttonGroup,"yaw");
	private JComboBox<String> frameOfReference;
	
	private Dial dial = new Dial();

	public CartesianDrivePanel(Sixi3IK sixi3) {
		super();

		frameOfReference = getFramesOfReference(sixi3);
		
		x.setSelected(true);
		
		dial.addActionListener( (e)-> onDialTurn(sixi3) );

		this.setBorder(BorderFactory.createTitledBorder("CartesianDrive"));
		this.setLayout(new GridBagLayout());
		
		GridBagConstraints c = new GridBagConstraints();
		c.gridx=0;
		c.gridy=0;
		c.weightx=1;
		c.weighty=0;
		c.gridheight=1;
		c.anchor=GridBagConstraints.NORTHWEST;

		c.gridwidth=2;

		JPanel referenceFrameSelection = new JPanel(new FlowLayout(SwingConstants.HORIZONTAL));
		referenceFrameSelection.add(new JLabel("Reference frame"));
		referenceFrameSelection.add(frameOfReference);
		
		c.fill = GridBagConstraints.HORIZONTAL;
		this.add(referenceFrameSelection,c);

		c.gridwidth=1;
		c.gridy++;
		this.add(x,c);
		c.gridy++;
		this.add(y,c);
		c.gridy++;
		this.add(z,c);
		c.gridy++;
		this.add(roll,c);
		c.gridy++;
		this.add(pitch,c);
		c.gridy++;
		this.add(yaw,c);
		c.gridy++;
		
		c.gridx=1;
		c.gridy=1;
		c.weightx=1;
		c.weighty=1;
		c.gridwidth=1;
		c.gridheight=6;
		c.fill = GridBagConstraints.BOTH;
		c.anchor = GridBagConstraints.EAST;
		dial.setPreferredSize(new Dimension(120,120));
		this.add(dial,c);
	}

	private JComboBox<String> getFramesOfReference(Sixi3IK sixi3) {
		JComboBox<String> FOR = new JComboBox<String>();
		FOR.addItem("World");
		FOR.addItem("First joint");
		FOR.addItem("End effector");
		
		return FOR;
	}

	private Matrix4d getFrameOfReferenceMatrix(Sixi3IK sixi3) {
		Matrix4d mFor;
		
		switch(frameOfReference.getSelectedIndex()) {
		case 0:
			mFor = MatrixHelper.createIdentityMatrix4();
			break;
		case 1:
			mFor = sixi3.getPoseWorld();
			mFor.mul(sixi3.getBone(0).getPose());
			break;
		case 2:
			mFor = sixi3.getEndEffector();
			break;
		default:
			throw new UnsupportedOperationException("frame of reference selection");
		}
		
		return mFor;
	}

	private JRadioButton makeRadioButton(ButtonGroup group, String label) {
		JRadioButton rb = new JRadioButton(label);
		rb.setActionCommand(label);
		group.add(rb);
		return rb;
	}
	
	private void onDialTurn(Sixi3IK sixi3) {
		double v_mm = dial.getChange()*0.1;
		Matrix4d m4 = sixi3.getEndEffectorTarget();
		Matrix4d mFor = getFrameOfReferenceMatrix(sixi3);
		moveEndEffectorInFrameOfReference(m4,mFor,v_mm);
		try {
			JacobianNewtonRaphson.iterate(sixi3,m4,20);
		} catch(Exception e) {
			// TODO deal with this more elegantly?
			Log.error("CartesianDrivePanel failed for move to "+m4+": "+e.getLocalizedMessage());
		}
	}
	
	private void moveEndEffectorInFrameOfReference(Matrix4d m4, Matrix4d mFor, double v_mm) {
		Vector3d p=new Vector3d();
		Matrix3d mA = new Matrix3d(); 
		m4.get(p);
		m4.get(mA);
		
		if(x.isSelected()) {
			Vector3d v = MatrixHelper.getXAxis(mFor);
			translateMatrix(m4,v,v_mm);
		} else if(y.isSelected()) {
			Vector3d v = MatrixHelper.getYAxis(mFor);
			translateMatrix(m4,v,v_mm);
		} else if(z.isSelected()) {
			Vector3d v = MatrixHelper.getZAxis(mFor);
			translateMatrix(m4,v,v_mm);
		} else {
			Matrix3d rot = new Matrix3d();
			Matrix3d mB = new Matrix3d();
			mFor.get(mB);
			if(roll.isSelected()) {
				rot.rotZ(v_mm);
			} else if(pitch.isSelected()) {
				rot.rotX(v_mm);
			} else if(yaw.isSelected()) {
				rot.rotY(v_mm);
			}
			Matrix3d mBi = new Matrix3d(mB);
			mBi.invert();
			mA.mul(mBi);
			mA.mul(rot);
			mA.mul(mB);
			
			m4.set(mA);
			m4.setTranslation(p);
		}
	}

	private void translateMatrix(Matrix4d m4, Vector3d v, double v_mm) {
		v.scale(v_mm);
		m4.m03 += v.x;
		m4.m13 += v.y;
		m4.m23 += v.z;
	}

	// TEST

	public static void main(String[] args) {
		Log.start();

		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (Exception e) {
		}

		JFrame frame = new JFrame("CartesianDrivePanel");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.add(new CartesianDrivePanel(new Sixi3IK()));
		frame.pack();
		frame.setVisible(true);
	}
}