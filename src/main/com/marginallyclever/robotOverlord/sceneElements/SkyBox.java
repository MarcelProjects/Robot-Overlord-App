package com.marginallyclever.robotOverlord.sceneElements;

import javax.vecmath.Matrix4d;
import javax.vecmath.Vector3d;

import com.jogamp.opengl.GL2;
import com.marginallyclever.robotOverlord.RobotOverlord;
import com.marginallyclever.robotOverlord.Viewport;
import com.marginallyclever.convenience.MatrixHelper;
import com.marginallyclever.robotOverlord.Entity;
import com.marginallyclever.robotOverlord.PoseEntity;
import com.marginallyclever.robotOverlord.uiExposedTypes.TextureEntity;

public class SkyBox extends Entity {
	/**
	 * 
	 */
	private static final long serialVersionUID = 7218495889495845836L;
	protected transient boolean areSkyboxTexturesLoaded=false;
	protected transient TextureEntity skyboxtextureZPos = new TextureEntity("/skybox/cube-x-pos.png");
	protected transient TextureEntity skyboxtextureXPos = new TextureEntity("/skybox/cube-x-neg.png");
	protected transient TextureEntity skyboxtextureXNeg = new TextureEntity("/skybox/cube-y-pos.png");
	protected transient TextureEntity skyboxtextureYPos = new TextureEntity("/skybox/cube-y-neg.png");
	protected transient TextureEntity skyboxtextureYNeg = new TextureEntity("/skybox/cube-z-pos.png");
	protected transient TextureEntity skyboxtextureZNeg = new TextureEntity("/skybox/cube-z-neg.png");

	public SkyBox() {
		super();
		setName("Skybox");
		
		skyboxtextureXPos.setName("XPos");
		skyboxtextureXNeg.setName("XNeg");
		skyboxtextureYPos.setName("YPos");
		skyboxtextureYNeg.setName("YNeg");
		skyboxtextureZPos.setName("ZPos");
		skyboxtextureZNeg.setName("ZNeg");
		
		addChild(skyboxtextureXPos);
		addChild(skyboxtextureXNeg);
		addChild(skyboxtextureYPos);
		addChild(skyboxtextureYNeg);
		addChild(skyboxtextureZPos);
		addChild(skyboxtextureZNeg);
	}
	
	// Draw background
	@Override
	public void render(GL2 gl2) {
		Viewport viewport = ((RobotOverlord)this.getRoot()).getViewport();
		PoseEntity camera = viewport.getAttachedTo();
		
		//gl2.glDisable(GL2.GL_DEPTH_TEST);
		gl2.glDisable(GL2.GL_LIGHTING);
		gl2.glDisable(GL2.GL_COLOR_MATERIAL);
		gl2.glEnable(GL2.GL_TEXTURE_2D);
		gl2.glPushMatrix();
			Matrix4d m = camera.getPoseWorld();
			m.setTranslation(new Vector3d(0,0,0));
			gl2.glLoadIdentity();
			m.invert();
			MatrixHelper.applyMatrix(gl2, m);
		
			gl2.glColor3f(1, 1, 1);
			Vector3d p = camera.getPosition();
			//gl2.glTranslated(-p.x,-p.y,-p.z);

			skyboxtextureXPos.render(gl2);
			gl2.glBegin(GL2.GL_TRIANGLE_FAN);
				gl2.glTexCoord2d(0,1);  gl2.glVertex3d(10, 10, 10);
				gl2.glTexCoord2d(1,1);  gl2.glVertex3d(10, -10, 10);
				gl2.glTexCoord2d(1,0);  gl2.glVertex3d(10, -10, -10);
				gl2.glTexCoord2d(0,0);  gl2.glVertex3d(10, 10, -10);
			gl2.glEnd();

			skyboxtextureXNeg.render(gl2);
			gl2.glBegin(GL2.GL_TRIANGLE_FAN);
				gl2.glTexCoord2d(0,1);  gl2.glVertex3d(-10, -10, 10);
				gl2.glTexCoord2d(1,1);  gl2.glVertex3d(-10, 10, 10);
				gl2.glTexCoord2d(1,0);  gl2.glVertex3d(-10, 10, -10);
				gl2.glTexCoord2d(0,0);  gl2.glVertex3d(-10, -10, -10);
			gl2.glEnd();

			skyboxtextureYPos.render(gl2);
			gl2.glBegin(GL2.GL_TRIANGLE_FAN);
				gl2.glTexCoord2d(0,1);  gl2.glVertex3d(-10, 10, 10);
				gl2.glTexCoord2d(1,1);  gl2.glVertex3d(10, 10, 10);
				gl2.glTexCoord2d(1,0);  gl2.glVertex3d(10, 10, -10);
				gl2.glTexCoord2d(0,0);  gl2.glVertex3d(-10, 10, -10);
			gl2.glEnd();

			skyboxtextureYNeg.render(gl2);
			gl2.glBegin(GL2.GL_TRIANGLE_FAN);
				gl2.glTexCoord2d(0,1);  gl2.glVertex3d(10, -10, 10);
				gl2.glTexCoord2d(1,1);  gl2.glVertex3d(-10, -10, 10);
				gl2.glTexCoord2d(1,0);  gl2.glVertex3d(-10, -10, -10);
				gl2.glTexCoord2d(0,0);  gl2.glVertex3d(10, -10, -10);
			gl2.glEnd();

			skyboxtextureZPos.render(gl2);
			gl2.glBegin(GL2.GL_TRIANGLE_FAN);
				gl2.glTexCoord2d(0,0);  gl2.glVertex3d(-10, 10, 10);
				gl2.glTexCoord2d(1,0);  gl2.glVertex3d( 10, 10, 10);
				gl2.glTexCoord2d(1,1);  gl2.glVertex3d( 10,-10, 10);
				gl2.glTexCoord2d(0,1);  gl2.glVertex3d(-10,-10, 10);
			gl2.glEnd();

			skyboxtextureZNeg.render(gl2);
			gl2.glBegin(GL2.GL_TRIANGLE_FAN);
				gl2.glTexCoord2d(0,0);  gl2.glVertex3d(-10,-10, -10);
				gl2.glTexCoord2d(1,0);  gl2.glVertex3d( 10,-10, -10);
				gl2.glTexCoord2d(1,1);  gl2.glVertex3d( 10, 10, -10);
				gl2.glTexCoord2d(0,1);  gl2.glVertex3d(-10, 10, -10);
			gl2.glEnd();
			
		gl2.glPopMatrix();
		gl2.glEnable(GL2.GL_DEPTH_TEST);
	}
}