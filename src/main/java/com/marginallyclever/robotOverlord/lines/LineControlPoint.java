package com.marginallyclever.robotOverlord.lines;

import javax.vecmath.Vector3f;

import com.jogamp.opengl.GL2;
import com.jogamp.opengl.math.Quaternion;
import com.marginallyclever.convenience.MatrixHelper;

/**
 * 3D Bezier curve implementation
 * @author Dan Royer
 * @see https://en.wikipedia.org/wiki/B%C3%A9zier_curve
 *
 */
public class LineControlPoint {
	public Bezier3 position = new Bezier3();
	//public LineBezier4 angle;
	
	//Quaternion q = new Quaternion;
	
	/**
	 * visualize the line in opengl
	 * @param gl2
	 */
	public void render(GL2 gl2) {
		Vector3f u,v,w;
		
		//MatrixHelper.drawMatrix(gl2, position.interpolate(0), u, v, w);
		//MatrixHelper.drawMatrix(gl2, position.interpolate(1), u, v, w);

		gl2.glDisable(gl2.GL_LIGHTING);
		gl2.glDisable(GL2.GL_COLOR_MATERIAL);
		
		//*
		gl2.glColor4f(0, 0, 1, 1);
		gl2.glBegin(GL2.GL_LINES);
		gl2.glVertex3f(position.p0.x,position.p0.y,position.p0.z);
		gl2.glVertex3f(position.p1.x,position.p1.y,position.p1.z);
		
		gl2.glVertex3f(position.p2.x,position.p2.y,position.p2.z);
		gl2.glVertex3f(position.p3.x,position.p3.y,position.p3.z);
		gl2.glEnd();
		//*/
		
		gl2.glColor4f(0, 1, 0, 1);
		gl2.glBegin(GL2.GL_LINE_STRIP);
		final float NUM_STEPS=20;
		for(float i=0;i<=NUM_STEPS;++i) {
			Vector3f ipos = position.interpolate(i/NUM_STEPS);
			gl2.glVertex3f(ipos.x,ipos.y,ipos.z);
		}
		gl2.glEnd();
		
		gl2.glEnable(gl2.GL_LIGHTING);
		gl2.glEnable(GL2.GL_COLOR_MATERIAL);
	}
}