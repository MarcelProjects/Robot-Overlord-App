package com.marginallyclever.robotOverlord.sixi2Robot;

import javax.swing.JPanel;
import javax.vecmath.Vector3f;

import com.jogamp.opengl.GL2;
import com.marginallyclever.communications.NetworkConnection;
import com.marginallyclever.convenience.MathHelper;
import com.marginallyclever.robotOverlord.*;
import com.marginallyclever.robotOverlord.material.Material;
import com.marginallyclever.robotOverlord.sixi2Robot.tool.*;
import com.marginallyclever.robotOverlord.world.World;
import com.marginallyclever.robotOverlord.model.Model;
import com.marginallyclever.robotOverlord.model.ModelFactory;
import com.marginallyclever.robotOverlord.robot.Robot;
import com.marginallyclever.robotOverlord.robot.RobotKeyframe;

import java.io.BufferedReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.Reader;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Iterator;


/**
 * Robot Overlord simulation of Sixi 6DOF robot arm.
 * 
 * @author Dan Royer <dan @ marinallyclever.com>
 */
public class Sixi2Robot
extends Robot {
	/**
	 * 
	 */
	private static final long serialVersionUID = -3644731265897692399L;

	private final static String hello = "HELLO WORLD! I AM ARM6 #";
	private final static String ROBOT_NAME = "Sixi 2 6DOF arm";
	
	// machine dimensions from design software
	public final static double FLOOR_ADJUST = 5.150f;
	public final static double FLOOR_TO_SHOULDER_MODEL = 8.140f;
	public final static double FLOOR_TO_SHOULDER = 13.44;
	public final static double SHOULDER_TO_ELBOW_Y = 0;
	public final static double SHOULDER_TO_ELBOW_Z = 44.55;
	public final static double ELBOW_TO_ULNA_Y = -28.805f;
	public final static double ELBOW_TO_ULNA_Z = 4.7201f;
	public final static double ULNA_TO_WRIST_Y = -11.800f;
	public final static double ULNA_TO_WRIST_Z = 0;
	public final static double ELBOW_TO_WRIST_Y = ELBOW_TO_ULNA_Y + ULNA_TO_WRIST_Y;
	public final static double ELBOW_TO_WRIST_Z = ELBOW_TO_ULNA_Z + ULNA_TO_WRIST_Z;
	public final static double WRIST_TO_TOOL_Z = 3.9527+5;

	public final static double SHOULDER_TO_ELBOW = Math.sqrt(SHOULDER_TO_ELBOW_Z*SHOULDER_TO_ELBOW_Z + SHOULDER_TO_ELBOW_Y*SHOULDER_TO_ELBOW_Y);
	public final static double ELBOW_TO_WRIST = Math.sqrt(ELBOW_TO_WRIST_Z*ELBOW_TO_WRIST_Z + ELBOW_TO_WRIST_Y*ELBOW_TO_WRIST_Y); 

	// joint limits
	public final static float MIN_ANGLE_0 = -90;
	public final static float MAX_ANGLE_0 =  90;
	public final static float MIN_ANGLE_1 = -90;
	public final static float MAX_ANGLE_1 =  90;
	public final static float MIN_ANGLE_2 = -90;
	public final static float MAX_ANGLE_2 =  90;
	public final static float MIN_ANGLE_3 = -90;
	public final static float MAX_ANGLE_3 =  90;
	public final static float MIN_ANGLE_4 = -90;
	public final static float MAX_ANGLE_4 =  90;
	public final static float MIN_ANGLE_5 = -90;
	public final static float MAX_ANGLE_5 =  90;
	
	
	// model files
	//private Model floorModel    = null;
	private Model anchorModel   = null;
	private Model shoulderModel = null;
	private Model bicepModel    = null;
	//private Model elbowModel    = null;
	private Model forearmModel  = null;
	private Model tuningForkModel  = null;
	private Model picassoBoxModel    = null;
	private Model handModel     = null;

	//private Material floorMat		= null;
	private Material anchorMat		= null;
	private Material shoulderMat	= null;
	private Material bicepMat		= null;
	//private Material elbowMat		= null;
	private Material forearmMat		= null;
	private Material tuningForkMat		= null;
	private Material picassoBoxMat		= null;
	private Material handMat		= null;

	// machine ID
	private long robotUID;
	
	// currently attached tool
	private Sixi2Tool tool = null;
	
	// collision volumes
	private Cylinder [] volumes = new Cylinder[6];

	// motion state of the arm right now
	private Sixi2RobotKeyframe motionNow = new Sixi2RobotKeyframe();
	// motion state in the near future, if valid.
	private Sixi2RobotKeyframe motionFuture = new Sixi2RobotKeyframe();
		
	// keyboard history
	private float aDir = 0.0f;
	private float bDir = 0.0f;
	private float cDir = 0.0f;
	private float dDir = 0.0f;
	private float eDir = 0.0f;
	private float fDir = 0.0f;

	private float xDir = 0.0f;
	private float yDir = 0.0f;
	private float zDir = 0.0f;
	private float uDir = 0.0f;
	private float vDir = 0.0f;
	private float wDir = 0.0f;

	// machine logic states
	private boolean armMoved 		= false;
	private boolean isPortConfirmed	= false;
	private double stepSize			= 2;
	private double feedRate			= 25;
	private double acceleration     = 5;

	// visual debugging
	private boolean showDebug=false;

	// gui
	protected transient Sixi2RobotControlPanel armPanel=null;
	
	public Sixi2Robot() {
		super();

		setDisplayName(ROBOT_NAME);
		
		// set up bounding volumes
		for(int i=0;i<volumes.length;++i) {
			volumes[i] = new Cylinder();
		}
		volumes[0].setRadius(3.2f);
		volumes[1].setRadius(3.0f*0.575f);
		volumes[2].setRadius(2.2f);
		volumes[3].setRadius(1.15f);
		volumes[4].setRadius(1.2f);
		volumes[5].setRadius(1.0f*0.575f);
		
		rotateBase(0,0);
		motionNow.set(motionFuture);
		motionNow.checkAngleLimits();
		motionFuture.checkAngleLimits();
		
		setToHomePosition();
		setupMaterials();
		
		tool = new Sixi2ToolGripper();
		tool.attachTo(this);
	}
	
	protected void setupMaterials() {
		//floorMat	= new Material();
		anchorMat	= new Material();
		shoulderMat	= new Material();
		bicepMat	= new Material();
		//elbowMat	= new Material();
		forearmMat	= new Material();
		tuningForkMat = new Material();
		picassoBoxMat	= new Material();
		handMat		= new Material();

		//floorMat   .setDiffuseColor(1.0f,0.0f,0.0f,1);
		float r=1;
		float g= 217f/255f;
		float b= 33f/255f;
		anchorMat  .setDiffuseColor(r,g,b,1);
		shoulderMat.setDiffuseColor(r,g,b,1);
		bicepMat   .setDiffuseColor(r,g,b,1);
		//elbowMat   .setDiffuseColor(r,g,b,1);
		forearmMat .setDiffuseColor(r,g,b,1);
		tuningForkMat   .setDiffuseColor(r,g,b,1);
		picassoBoxMat   .setDiffuseColor(r,g,b,1);
		handMat    .setDiffuseColor(r,g,b,1);

		//floorMat   .setShininess(10);
		anchorMat  .setShininess(10);
		shoulderMat.setShininess(10);
		bicepMat   .setShininess(10);
		//elbowMat   .setShininess(10);
		forearmMat .setShininess(10);
		tuningForkMat   .setShininess(10);
		picassoBoxMat   .setShininess(10);
		handMat    .setShininess(10);
	}
	
	@Override
	protected void loadModels(GL2 gl2) {
		try {
			//floorModel      = ModelFactory.createModelFromFilename("/Sixi2/floor.stl",0.1f);
			anchorModel     = ModelFactory.createModelFromFilename("/Sixi2/anchor.stl",0.1f);
			shoulderModel   = ModelFactory.createModelFromFilename("/Sixi2/shoulder.stl",0.1f);
			bicepModel      = ModelFactory.createModelFromFilename("/Sixi2/bicep.stl",0.1f);
			//elbowModel      = ModelFactory.createModelFromFilename("/Sixi2/elbow.stl",0.1f);
			forearmModel    = ModelFactory.createModelFromFilename("/Sixi2/forearm.stl",0.1f);
			tuningForkModel = ModelFactory.createModelFromFilename("/Sixi2/tuningFork.stl",0.1f);
			picassoBoxModel = ModelFactory.createModelFromFilename("/Sixi2/picassoBox.stl",0.1f);
			handModel       = ModelFactory.createModelFromFilename("/Sixi2/hand.stl",0.1f);
			
			bicepModel  .adjustOrigin(new Vector3f(-1.82f, 9, 0));
			//elbowModel  .adjustOrigin(new Vector3f(0, 0, 0));
			forearmModel.adjustOrigin(new Vector3f(0, (float)ELBOW_TO_WRIST_Z, (float)ELBOW_TO_WRIST_Y));
			tuningForkModel  .adjustOrigin(new Vector3f(0, 0, 0));
			picassoBoxModel  .adjustOrigin(new Vector3f(0, 0, 0));
			handModel   .adjustOrigin(new Vector3f(0, 0, 0));
			
			System.out.println("Sixi 2 loaded OK");
		} catch(Exception e) {
			e.printStackTrace();
		}
	}

    private void readObject(ObjectInputStream inputStream)
            throws IOException, ClassNotFoundException
    {
        inputStream.defaultReadObject();
    }

	@Override
	public ArrayList<JPanel> getContextPanel(RobotOverlord gui) {
		ArrayList<JPanel> list = super.getContextPanel(gui);
		
		//if(armPanel == null) 
			armPanel = new Sixi2RobotControlPanel(gui,this);
		list.add(armPanel);
		
		//updateGUI();

		ArrayList<JPanel> toolList = tool.getContextPanel(gui);
		Iterator<JPanel> iter = toolList.iterator();
		while(iter.hasNext()) {
			list.add(iter.next());
		}
		
		return list;
	}
	
	public boolean isPortConfirmed() {
		return isPortConfirmed;
	}
	
	private void enableFK() {		
		xDir=0;
		yDir=0;
		zDir=0;
		uDir=0;
		vDir=0;
		wDir=0;
	}
	
	private void disableFK() {	
		aDir=0;
		bDir=0;
		cDir=0;
		dDir=0;
		eDir=0;
		fDir=0;
	}

	/**
	 * 
	 * @param arg0 any value >= 0
	 */
	public void setStepSize(double arg0) {
		if(arg0<0) return;
		stepSize=arg0;
	}
	
	public double getStepSize() {
		return stepSize;
	}

	/**
	 * 
	 * @param arg0 any value >= 0
	 */
	public void setFeedRate(double arg0) {
		if(arg0<0) return;
		feedRate=arg0;
	}
	
	public double getFeedRate() {
		return feedRate;
	}

	public void setAcceleration(double arg0) {
		if(arg0<0) return;
		acceleration = arg0;
	}
	public double getAcceleration() {
		return acceleration;
	}
	
	public void moveA(float dir) {
		aDir=dir;
		enableFK();
	}

	public void moveB(float dir) {
		bDir=dir;
		enableFK();
	}

	public void moveC(float dir) {
		cDir=dir;
		enableFK();
	}

	public void moveD(float dir) {
		dDir=dir;
		enableFK();
	}

	public void moveE(float dir) {
		eDir=dir;
		enableFK();
	}

	public void moveF(float dir) {
		fDir=dir;
		enableFK();
	}

	public void moveX(float dir) {
		xDir=dir;
		disableFK();
	}

	public void moveY(float dir) {
		yDir=dir;
		disableFK();
	}

	public void moveZ(float dir) {
		zDir=dir;
		disableFK();
	}

	public void moveU(float dir) {
		uDir=dir;
		disableFK();
	}

	public void moveV(float dir) {
		vDir=dir;
		disableFK();
	}

	public void moveW(float dir) {
		wDir=dir;
		disableFK();
	}

	public void toggleDebug() {
		showDebug=!showDebug;
	}
	
	public void findHome() {
		// send home command
		if(connection!=null) this.sendLineToRobot("G28");
		setToHomePosition();
		updateGUI();
	}

	protected void setToHomePosition() {
		motionNow.angle0=0f;
		motionNow.angle1=0f;
		motionNow.angle2=0f;
		motionNow.angle3=0f;
		motionNow.angle4=0f;
		motionNow.angle5=0f;
		motionNow.forwardKinematics(false,null);
		//motionNow.inverseKinematics();
		motionFuture.set(motionNow);
		//forwardKinematics(motionFuture,false,null);
		//inverseKinematics(motionFuture);
	}
	
	/**
	 * update the desired finger location
	 * @param delta the time since the last update.  Typically ~1/30s
	 */
	protected void updateIK(float delta) {
		boolean changed=false;
		motionFuture.fingerPosition.set(motionNow.fingerPosition);
		final float vel=(float)stepSize;
		float dp = vel;// * delta;

		float dX=motionFuture.fingerPosition.x;
		float dY=motionFuture.fingerPosition.y;
		float dZ=motionFuture.fingerPosition.z;
		
		if (xDir!=0) {
			dX += xDir * dp;
			changed=true;
			xDir=0;
		}		
		if (yDir!=0) {
			dY += yDir * dp;
			changed=true;
			yDir=0;
		}
		if (zDir!=0) {
			dZ += zDir * dp;
			changed=true;
			zDir=0;
		}
		// rotations
		float ru=motionFuture.ikU;
		float rv=motionFuture.ikV;
		float rw=motionFuture.ikW;
		boolean hasTurned=false;

		if (uDir!=0) {
			ru += uDir * dp;
			changed=true;
			hasTurned=true;
			uDir=0;
		}
		if (vDir!=0) {
			rv += vDir * dp;
			changed=true;
			hasTurned=true;
			vDir=0;
		}
		if (wDir!=0) {
			rw += wDir * dp;
			changed=true;
			hasTurned=true;
			wDir=0;
		}


		if(hasTurned) {
			// On a 3-axis robot when homed the forward axis of the finger tip is pointing downward.
			// More complex arms start from the same assumption.
			motionFuture.ikU=ru;
			motionFuture.ikV=rv;
			motionFuture.ikW=rw;

			// Rotating around itself has no effect, so just skip it
			//Vector3f result = MathHelper.rotateAroundAxis(World.forward,World.forward,(float)Math.toRadians(motionFuture.ikU));
			Vector3f result = new Vector3f(World.forward);

			result = MathHelper.rotateAroundAxis(result     ,World.right  ,(float)Math.toRadians(motionFuture.ikV));
			result = MathHelper.rotateAroundAxis(result     ,World.up     ,(float)Math.toRadians(motionFuture.ikW));
			motionFuture.fingerForward.set(result);

			result = MathHelper.rotateAroundAxis(World.right,World.forward,(float)Math.toRadians(motionFuture.ikU));
			result = MathHelper.rotateAroundAxis(result     ,World.right  ,(float)Math.toRadians(motionFuture.ikV));
			result = MathHelper.rotateAroundAxis(result     ,World.up     ,(float)Math.toRadians(motionFuture.ikW));
			motionFuture.fingerRight.set(result);
		}
		
		//if(changed==true && motionFuture.movePermitted()) {
		if(changed) {
			motionFuture.fingerPosition.x = dX;
			motionFuture.fingerPosition.y = dY;
			motionFuture.fingerPosition.z = dZ;
			if(!motionFuture.inverseKinematics(false,null)) {
				return;
			}
			if(motionFuture.checkAngleLimits()) {
			//if(motionNow.fingerPosition.epsilonEquals(motionFuture.fingerPosition,0.1f) == false) {
				armMoved=true;

				sendChangeToRealMachine();
				//if(!this.isPortConfirmed()) {
					// live data from the sensors will update motionNow, so only do this if we're unconnected.
					motionNow.set(motionFuture);
				//}
				updateGUI();
			} else {
				motionFuture.set(motionNow);
			}
		}
	}
	
	protected void updateFK(float delta) {
		boolean changed=false;
		float velcd=(float)stepSize; // * delta
		float velabe=(float)stepSize; // * delta

		motionFuture.set(motionNow);
		
		float d0 = motionFuture.angle0;
		float d1 = motionFuture.angle1;
		float d2 = motionFuture.angle2;
		float d3 = motionFuture.angle3;
		float d4 = motionFuture.angle4;
		float d5 = motionFuture.angle5;

		if (fDir!=0) {
			d0 += velabe * fDir;
			changed=true;
			fDir=0;
		}
		
		if (eDir!=0) {
			d1 += velabe * eDir;
			changed=true;
			eDir=0;
		}
		
		if (dDir!=0) {
			d2 += velcd * dDir;
			changed=true;
			dDir=0;
		}

		if (cDir!=0) {
			d3 += velcd * cDir;
			changed=true;
			cDir=0;
		}
		
		if(bDir!=0) {
			d4 += velabe * bDir;
			changed=true;
			bDir=0;
		}
		
		if(aDir!=0) {
			d5 += velabe * aDir;
			changed=true;
			aDir=0;
		}
		

		if(changed) {
			motionFuture.angle5=d5;
			motionFuture.angle4=d4;
			motionFuture.angle3=d3;
			motionFuture.angle2=d2;
			motionFuture.angle1=d1;
			motionFuture.angle0=d0;
			if(motionFuture.checkAngleLimits()) {
				motionFuture.forwardKinematics(false,null);
				armMoved=true;
				
				sendChangeToRealMachine();
				//if(!this.isPortConfirmed()) {
					// live data from the sensors will update motionNow, so only do this if we're unconnected.
					motionNow.set(motionFuture);
				//}
				updateGUI();
			} else {
				motionFuture.set(motionNow);
			}
		}
	}

	protected float roundOff(float v) {
		float SCALE = 1000.0f;
		
		return Math.round(v*SCALE)/SCALE;
	}
	
	public void updateGUI() {
		if(armPanel.drivenIndex!=-1) {
			return;
		}
		armPanel.drivenIndex=1;
		
		Vector3f v = new Vector3f();
		v.set(motionNow.fingerPosition);
		v.add(getPosition());
		armPanel.xPos.setText(Float.toString(roundOff(v.x)));
		armPanel.yPos.setText(Float.toString(roundOff(v.y)));
		armPanel.zPos.setText(Float.toString(roundOff(v.z)));
		armPanel.uPos.setText(Float.toString(roundOff(motionNow.ikU)));
		armPanel.vPos.setText(Float.toString(roundOff(motionNow.ikV)));
		armPanel.wPos.setText(Float.toString(roundOff(motionNow.ikW)));

		armPanel.fk0.setValue((int)motionNow.angle0);	armPanel.angle0.setText(Float.toString(roundOff(motionNow.angle0)));
		armPanel.fk1.setValue((int)motionNow.angle1);	armPanel.angle1.setText(Float.toString(roundOff(motionNow.angle1)));
		armPanel.fk2.setValue((int)motionNow.angle2);	armPanel.angle2.setText(Float.toString(roundOff(motionNow.angle2)));
		armPanel.fk3.setValue((int)motionNow.angle3);	armPanel.angle3.setText(Float.toString(roundOff(motionNow.angle3)));
		armPanel.fk4.setValue((int)motionNow.angle4);	armPanel.angle4.setText(Float.toString(roundOff(motionNow.angle4)));
		armPanel.fk5.setValue((int)motionNow.angle5);	armPanel.angle5.setText(Float.toString(roundOff(motionNow.angle5)));
		
		if( tool != null ) tool.updateGUI();

		armPanel.drivenIndex=-1;
	}

	protected void sendChangeToRealMachine() {
		if(!isPortConfirmed) return;
		
		String str="";
		if(motionFuture.angle0!=motionNow.angle0) str+=" X"+roundOff(motionFuture.angle0);
		if(motionFuture.angle1!=motionNow.angle1) str+=" Y"+roundOff(motionFuture.angle1);
		if(motionFuture.angle2!=motionNow.angle2) str+=" Z"+roundOff(motionFuture.angle2);
		if(motionFuture.angle3!=motionNow.angle3) str+=" U"+roundOff(motionFuture.angle3);
		if(motionFuture.angle4!=motionNow.angle4) str+=" V"+roundOff(motionFuture.angle4);
		if(motionFuture.angle5!=motionNow.angle5) str+=" W"+roundOff(motionFuture.angle5);
		if(str.length()>0) {
			str+=" F"+roundOff((float)feedRate) + " A"+roundOff((float)acceleration);
			System.out.println(str);
			this.sendLineToRobot("G0"+str);
		}
	}
	
	@Override
	public void prepareMove(float delta) {
		super.prepareMove(delta);
		updateIK(delta);
		updateFK(delta);
		if(tool != null) tool.update(delta);
	}

	@Override
	public void finalizeMove() {
		if(getKeyframeSize()>0&&animationSpeed!=0) {
			motionFuture.set((Sixi2RobotKeyframe)getKeyframeNow());

			//System.out.println(keyframe_index+":"+keyframe_t+"\t"+motionFuture.fingerRight);
		}
		
		// copy motion_future to motion_now
		motionNow.set(motionFuture);
		
		if(armMoved) {
			if( this.isReadyToReceive ) {
				armMoved=false;
			}
		}
	}

	/*
	LineControlPoint lcp = new LineControlPoint();
	public void testLineControlPointRender(GL2 gl2) {
		
		lcp.position.p0.x=0;
		lcp.position.p0.y=0;
		lcp.position.p0.z=0;

		lcp.position.p3.x=10;
		lcp.position.p3.y=10;
		lcp.position.p3.z=10;

		lcp.position.p1.x+=Math.random()-0.5;
		lcp.position.p1.y+=Math.random()-0.5;
		lcp.position.p1.z+=Math.random()-0.5;
		lcp.position.p1.x=Math.min(Math.max(lcp.position.p1.x, -10), 10);
		lcp.position.p1.y=Math.min(Math.max(lcp.position.p1.y, -10), 10);
		lcp.position.p1.z=Math.min(Math.max(lcp.position.p1.z, -10), 10);
		
		lcp.position.p2.x+=Math.random()-0.5;
		lcp.position.p2.y+=Math.random()-0.5;
		lcp.position.p2.z+=Math.random()-0.5;
		lcp.position.p2.x=Math.min(Math.max(lcp.position.p2.x, 0), 20);
		lcp.position.p2.y=Math.min(Math.max(lcp.position.p2.y, 0), 20);
		lcp.position.p2.z=Math.min(Math.max(lcp.position.p2.z, 0), 20);

		lcp.render(gl2);
	}
	*/
	@Override
	public void render(GL2 gl2) {
		if(!isModelLoaded) {
			this.keyframeAdd();
			this.keyframeAdd();
			this.keyframeAdd();
			this.keyframeAdd();
			this.keyframeAdd();

			Sixi2RobotKeyframe k;
			
			k = (Sixi2RobotKeyframe)getKeyframe(0);
			k.forwardKinematics(false, null);
			//keyframes.set(0,k);
			
			k = (Sixi2RobotKeyframe)getKeyframe(1);
			k.forwardKinematics(false, null);
			k.fingerPosition.z-=10;
			k.inverseKinematics(false, null);
			//keyframes.set(0,k);
			
			k = (Sixi2RobotKeyframe)getKeyframe(2);
			k.forwardKinematics(false, null);
			k.fingerPosition.y+=10;
			k.fingerPosition.z-=10;
			k.inverseKinematics(false, null);
			//keyframes.set(0,k);
			
			k = (Sixi2RobotKeyframe)getKeyframe(3);
			k.forwardKinematics(false, null);
			k.fingerPosition.y+=10;
			k.inverseKinematics(false, null);
			//keyframes.set(0,k);
			
			k = (Sixi2RobotKeyframe)getKeyframe(4);
			k.forwardKinematics(false, null);	 													//keyframes.set(0,k);
		}

		super.render(gl2);

		//motionNow.inverseKinematics(false,null);
		
		//testLineControlPointRender(gl2);
		
		gl2.glPushMatrix();
			// TODO rotate model
			Vector3f p = getPosition();
			gl2.glTranslatef(p.x, p.y, p.z);

			//gl2.glTranslated(motionNow.base.x,motionNow.base.y,motionNow.base.z+FLOOR_ADJUST);	
			
			gl2.glPushMatrix();
			renderModels(gl2);
			gl2.glPopMatrix();
			
			if(showDebug) {
				gl2.glDisable(GL2.GL_DEPTH_TEST);
				boolean lightOn= gl2.glIsEnabled(GL2.GL_LIGHTING);
				boolean matCoOn= gl2.glIsEnabled(GL2.GL_COLOR_MATERIAL);
				gl2.glDisable(GL2.GL_LIGHTING);
				gl2.glDisable(GL2.GL_COLOR_MATERIAL);
				
				gl2.glPushMatrix();	
				motionNow.forwardKinematics(true,gl2);
				gl2.glPopMatrix();

				gl2.glPushMatrix();
				motionNow.inverseKinematics(true,gl2);
				gl2.glPopMatrix();
				
				if(lightOn) gl2.glEnable(GL2.GL_LIGHTING);
				if(matCoOn) gl2.glEnable(GL2.GL_COLOR_MATERIAL);
				gl2.glEnable(GL2.GL_DEPTH_TEST);
			}
		gl2.glPopMatrix();
	}
	
	/**
	 * Draw the physical model according to the angle values in the motionNow state.
	 * @param gl2 the openGL render context
	 */
	protected void renderModels(GL2 gl2) {
		// anchor
		gl2.glPushMatrix();
		gl2.glTranslated(0, 0, FLOOR_ADJUST);
		gl2.glRotated(90,1,0,0);
		anchorMat.render(gl2);
		anchorModel.render(gl2);
		gl2.glPopMatrix();

		// shoulder

		gl2.glTranslated( 0, 0, FLOOR_TO_SHOULDER_MODEL);
		gl2.glRotated(-90-motionNow.angle0,0,0,1);
		gl2.glRotated(90,1,0,0);
		//shoulderMat.setSpecularColor(0, 0, 0, 1);
		//shoulderMat.setDiffuseColor(1, 0, 0, 1);
		shoulderMat.render(gl2);
		shoulderModel.render(gl2);
		
		// bicep

		double blen=FLOOR_TO_SHOULDER_MODEL-FLOOR_TO_SHOULDER;
		gl2.glTranslated(0, -blen,0);
		gl2.glRotated(motionNow.angle1, 1, 0, 0);
		bicepMat.render(gl2);
		bicepModel.render(gl2);
		
		// elbow
		gl2.glTranslated(0, SHOULDER_TO_ELBOW_Z, 0);
		gl2.glRotated(180,1,0,0);
		gl2.glRotated(180,0,0,1);
		gl2.glRotated(-motionNow.angle2,1,0,0);
		forearmMat.render(gl2);
		forearmModel.render(gl2);
		
		// tuning fork
		gl2.glTranslated(0,ELBOW_TO_WRIST_Z,ELBOW_TO_WRIST_Y);
		gl2.glRotated(motionNow.angle3,0,0,1);
		tuningForkMat.render(gl2);
		tuningForkModel.render(gl2);

		// picassoBox
		gl2.glRotated(motionNow.angle4,1,0,0);
		picassoBoxMat.render(gl2);
		picassoBoxModel.render(gl2);

		// hand
		gl2.glRotated(motionNow.angle5,0,0,1);
		handMat.render(gl2);
		handModel.render(gl2);
		/*
		// tool
		if(tool!=null) {
			gl2.glTranslated(0,0,WRIST_TO_TOOL_Z);
			gl2.glRotated(90, 0, 1, 0);
			// tool has its own material.
			//tool.render(gl2);
		}*/
	}
	protected void drawBounds(GL2 gl2) {
		throw new UnsupportedOperationException();
	}
	
	private double parseNumber(String str) {
		float f=0;
		try {
			f = Float.parseFloat(str);
		}
		catch(Exception e) {
			e.printStackTrace();
		}
		
		return f;
	}

	public void setModeAbsolute() {
		if(connection!=null) this.sendLineToRobot("G90");
	}
	
	public void setModeRelative() {
		if(connection!=null) this.sendLineToRobot("G91");
	}
	
	@Override
	// override this method to check that the software is connected to the right type of robot.
	public void dataAvailable(NetworkConnection arg0,String line) {
		if(line.contains(hello)) {
			isPortConfirmed=true;
			//finalizeMove();
			setModeAbsolute();
			this.sendLineToRobot("R1");
			
			String uidString=line.substring(hello.length()).trim();
			System.out.println(">>> UID="+uidString);
			try {
				long uid = Long.parseLong(uidString);
				if(uid==0) {
					robotUID = getNewRobotUID();
				} else {
					robotUID = uid;
				}
				armPanel.setUID(robotUID);
			}
			catch(Exception e) {
				e.printStackTrace();
			}

			setDisplayName(ROBOT_NAME+" #"+robotUID);
		}
		
		if( isPortConfirmed ) {
			if(line.startsWith("A") && !line.startsWith("As")) {
				String items[] = line.split(" ");
				try {
					if(items.length>=5) {
						for(int i=0;i<items.length;++i) {
							if(items[i].startsWith("A")) {
								float v = (float)parseNumber(items[i].substring(1));
								if(motionFuture.angle5 != v) {
									motionFuture.angle5 = v;
									armPanel.angle5.setText(Float.toString(roundOff(v)));
								}
							} else if(items[i].startsWith("B")) {
								float v = (float)parseNumber(items[i].substring(1));
								if(motionFuture.angle4 != v) {
									motionFuture.angle4 = v;
									armPanel.angle4.setText(Float.toString(roundOff(v)));
								}
							} else if(items[i].startsWith("C")) {
								float v = (float)parseNumber(items[i].substring(1));
								if(motionFuture.angle3 != v) {
									motionFuture.angle3 = v;
									armPanel.angle3.setText(Float.toString(roundOff(v)));
								}
							} else if(items[i].startsWith("D")) {
								float v = (float)parseNumber(items[i].substring(1));
								if(motionFuture.angle2 != v) {
									motionFuture.angle2 = v;
									armPanel.angle2.setText(Float.toString(roundOff(v)));
								}
							} else if(items[i].startsWith("E")) {
								float v = (float)parseNumber(items[i].substring(1));
								if(motionFuture.angle1 != v) {
									motionFuture.angle1 = v;
									armPanel.angle1.setText(Float.toString(roundOff(v)));
								}
							}
						}
						
						motionFuture.forwardKinematics(false,null);
						motionNow.set(motionFuture);
						updateGUI();
					}
				} catch(java.lang.NumberFormatException e) {
					System.out.print("*** "+line);
				}
			} else {
				System.out.print("*** "+line);
			}
		}
	}

	public void moveBase(Vector3f dp) {
		motionFuture.anchorPosition.set(dp);
	}
	
	public void rotateBase(double pan,double tilt) {
		motionFuture.basePan=pan;
		motionFuture.baseTilt=tilt;
		
		pan = Math.toRadians(pan);
		tilt = Math.toRadians(tilt);
		
		motionFuture.baseForward.y = (float)Math.sin(pan) * (float)Math.cos(tilt);
		motionFuture.baseForward.x = (float)Math.cos(pan) * (float)Math.cos(tilt);
		motionFuture.baseForward.z =                        (float)Math.sin(tilt);
		motionFuture.baseForward.normalize();
		
		motionFuture.baseUp.set(0,0,1);
	
		motionFuture.baseRight.cross(motionFuture.baseForward, motionFuture.baseUp);
		motionFuture.baseRight.normalize();
		motionFuture.baseUp.cross(motionFuture.baseRight, motionFuture.baseForward);
		motionFuture.baseUp.normalize();
	}
	
	public BoundingVolume [] getBoundingVolumes() {
		// shoulder joint
		Vector3f t1=new Vector3f(motionFuture.baseRight);
		t1.scale(volumes[0].getRadius()/2);
		t1.add(motionFuture.shoulder);
		Vector3f t2=new Vector3f(motionFuture.baseRight);
		t2.scale(-volumes[0].getRadius()/2);
		t2.add(motionFuture.shoulder);
		volumes[0].SetP1(getWorldCoordinatesFor(t1));
		volumes[0].SetP2(getWorldCoordinatesFor(t2));
		// bicep
		volumes[1].SetP1(getWorldCoordinatesFor(motionFuture.shoulder));
		volumes[1].SetP2(getWorldCoordinatesFor(motionFuture.elbow));
		// elbow
		t1.set(motionFuture.baseRight);
		t1.scale(volumes[0].getRadius()/2);
		t1.add(motionFuture.elbow);
		t2.set(motionFuture.baseRight);
		t2.scale(-volumes[0].getRadius()/2);
		t2.add(motionFuture.elbow);
		volumes[2].SetP1(getWorldCoordinatesFor(t1));
		volumes[2].SetP2(getWorldCoordinatesFor(t2));
		// ulna
		volumes[3].SetP1(getWorldCoordinatesFor(motionFuture.elbow));
		volumes[3].SetP2(getWorldCoordinatesFor(motionFuture.wrist));
		// wrist
		t1.set(motionFuture.baseRight);
		t1.scale(volumes[0].getRadius()/2);
		t1.add(motionFuture.wrist);
		t2.set(motionFuture.baseRight);
		t2.scale(-volumes[0].getRadius()/2);
		t2.add(motionFuture.wrist);
		volumes[4].SetP1(getWorldCoordinatesFor(t1));
		volumes[4].SetP2(getWorldCoordinatesFor(t2));
		// finger
		volumes[5].SetP1(getWorldCoordinatesFor(motionFuture.wrist));
		volumes[5].SetP2(getWorldCoordinatesFor(motionFuture.fingerPosition));
		
		return volumes;
	}
	
	Vector3f getWorldCoordinatesFor(Vector3f in) {
		Vector3f out = new Vector3f(motionFuture.anchorPosition);
		
		Vector3f tempx = new Vector3f(motionFuture.baseForward);
		tempx.scale(in.x);
		out.add(tempx);

		Vector3f tempy = new Vector3f(motionFuture.baseRight);
		tempy.scale(-in.y);
		out.add(tempy);

		Vector3f tempz = new Vector3f(motionFuture.baseUp);
		tempz.scale(in.z);
		out.add(tempz);
				
		return out;
	}

	/**
	 * Query the web server for a new robot UID.  
	 * @return the new UID if successful.  0 on failure.
	 * @see <a href='http://www.exampledepot.com/egs/java.net/Post.html'>http://www.exampledepot.com/egs/java.net/Post.html</a>
	 */
	private long getNewRobotUID() {
		long new_uid = 0;

		try {
			// Send data
			URL url = new URL("https://marginallyclever.com/evil_minion_getuid.php");
			URLConnection conn = url.openConnection();
			try (
                    final InputStream connectionInputStream = conn.getInputStream();
                    final Reader inputStreamReader = new InputStreamReader(connectionInputStream, StandardCharsets.UTF_8);
                    final BufferedReader rd = new BufferedReader(inputStreamReader)
					) {
				String line = rd.readLine();
				new_uid = Long.parseLong(line);
			}
		} catch (Exception e) {
			e.printStackTrace();
			return 0;
		}

		// did read go ok?
		if (new_uid != 0) {
			// make sure a topLevelMachinesPreferenceNode node is created
			// tell the robot it's new UID.
			this.sendLineToRobot("UID " + new_uid);
		}
		return new_uid;
	}
	
	// TODO check for collisions with http://geomalgorithms.com/a07-_distance.html#dist3D_Segment_to_Segment ?
	public boolean movePermitted(Sixi2RobotKeyframe keyframe) {
		// don't hit floor?
		// don't hit ceiling?

		// check far limit
		// seems doable
		if(!keyframe.inverseKinematics(false,null)) return false;
		// angle are good?
		if(!keyframe.checkAngleLimits()) return false;

		// OK
		return true;
	}
	
	/**
	 * Generate a table of FK angles and matching IK values for training a neural network.
	 */
	//@Test
	public void generateBigData() {
		Sixi2RobotKeyframe keyframe = new Sixi2RobotKeyframe();
		float a0,a1,a2,a3,a4,a5;
		float px,py,pz,iku,ikv,ikw;
		final float stepSize = 15f;
		int totalRecords=0;
		
		try {
			//DataOutputStream writer = new DataOutputStream(new FileOutputStream("FK2IK.csv"));
			FileWriter writer = new FileWriter("FK2IK.csv");

			for(a0=MIN_ANGLE_0;a0<MAX_ANGLE_0;a0+=stepSize) {
				for(a1=MIN_ANGLE_1;a1<MAX_ANGLE_1;a1+=stepSize) {
					for(a2=MIN_ANGLE_2;a2<MAX_ANGLE_2;a2+=stepSize) {
						for(a3=MIN_ANGLE_3;a3<MAX_ANGLE_3;a3+=stepSize) {
							for(a4=MIN_ANGLE_4;a4<MAX_ANGLE_4;a4+=stepSize) {
								for(a5=MIN_ANGLE_5;a5<MAX_ANGLE_5;a5+=stepSize) {
									keyframe.angle0=a0;
									keyframe.angle1=a1;
									keyframe.angle2=a2;
									keyframe.angle3=a3;
									keyframe.angle4=a4;
									keyframe.angle5=a5;
									keyframe.forwardKinematics(false,null);
									px = keyframe.fingerPosition.getX();
									py = keyframe.fingerPosition.getY();
									pz = keyframe.fingerPosition.getZ();
									iku = keyframe.ikU;
									ikv = keyframe.ikV;
									ikw = keyframe.ikW;

									StringBuilder sb = new StringBuilder();
									sb.append(a0).append(",");
									sb.append(a1).append(",");
									sb.append(a2).append(",");
									sb.append(a3).append(",");
									sb.append(a4).append(",");
									sb.append(a5).append(",");
									
									sb.append(px).append(",");
									sb.append(py).append(",");
									sb.append(pz).append(",");
									sb.append(iku).append(",");
									sb.append(ikv).append(",");
									sb.append(ikw).append("\n");
									writer.append(sb.toString());
/*
									writer.writeFloat(a0);
									writer.writeFloat(a1);
									writer.writeFloat(a2);
									writer.writeFloat(a3);
									writer.writeFloat(a4);
									writer.writeFloat(a5);
									writer.writeFloat(px);
									writer.writeFloat(py);
									writer.writeFloat(pz);
									writer.writeFloat(iku);
									writer.writeFloat(ikv);
									writer.writeFloat(ikw);*/
									++totalRecords;
								}
								try {
									Thread.sleep(1);
								} catch (InterruptedException e) {}
							}
							System.out.println(a0+"\t"+a1+"\t"+a2+"\t"+a3);
						}
					}
				}
				int progress = (int)(10000.0f*(a0-MIN_ANGLE_0)/(MAX_ANGLE_0-MIN_ANGLE_0));
				System.out.println("** "+((float)progress/100.0f)+"% **");
			}
			System.out.println("== Done ("+totalRecords+" total records. ==");
			writer.flush();
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void setFKAxis(int axis,float angle) {
		if(armPanel.drivenIndex!=-1) return;
		
		//System.out.println("setFKAxis "+axis);
		float dir;
		//	System.out.println("angle="+angle+"\tfuture="+motionFuture.angle0+"\tdir="+dir);
		switch(axis) {
		case 0: dir=angle-motionFuture.angle0; fDir=dir;  break;
		case 1: dir=angle-motionFuture.angle1; eDir=dir;  break;
		case 2: dir=angle-motionFuture.angle2; dDir=dir;  break;
		case 3: dir=angle-motionFuture.angle3; cDir=dir;  break;
		case 4: dir=angle-motionFuture.angle4; bDir=dir;  break;
		case 5: dir=angle-motionFuture.angle5; aDir=dir;  break;
		}
		
		  
		enableFK();
	}
	
	@Override
	public RobotKeyframe createKeyframe() {
		return new Sixi2RobotKeyframe();
	}
}