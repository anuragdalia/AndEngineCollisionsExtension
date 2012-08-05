package com.makersf.andengine.extension.collisions.pixelperfect;

import org.andengine.entity.shape.RectangularShape;
import org.andengine.util.adt.transformation.Transformation;
import org.andengine.util.adt.transformation.TransformationUtils;

import com.makersf.andengine.extension.collisions.pixelperfect.masks.IPixelPerfectMask;

public class PixelPerfectCollisionChecker {
	
	public static boolean checkCollision(final RectangularShape pA, final IPixelPerfectMask pMaskA, final RectangularShape pB, final IPixelPerfectMask pMaskB) {
		
		//Transformation from B's local space to A's local space
		final Transformation bLocToALoc = pB.getLocalToSceneTransformation();
		bLocToALoc.postConcat(pA.getSceneToLocalTransformation());
		
		//Transformation from A's local space to B's local space
		final Transformation aLocToBLoc = new Transformation();
		TransformationUtils.invert(bLocToALoc, aLocToBLoc);
		
		final float[] vertsB = new float[8];
		//[0,0] [width, 0] [0, height] [width, height]		
		vertsB[2] = pB.getWidth();
		vertsB[5] = pB.getHeight();
		vertsB[6] = pB.getWidth();
		vertsB[7] = pB.getHeight();
		
		//bring the coordinate of B in A's local spcae
		bLocToALoc.transform(vertsB);
		
		//find the bounding box of B
		float minX = Math.min(Math.min(vertsB[0], vertsB[2]), Math.min(vertsB[4], vertsB[6]));
		float maxX = Math.max(Math.max(vertsB[0], vertsB[2]), Math.max(vertsB[4], vertsB[6]));
		float minY = Math.min(Math.min(vertsB[1], vertsB[3]), Math.min(vertsB[5], vertsB[6]));
		float maxY = Math.max(Math.max(vertsB[1], vertsB[3]), Math.max(vertsB[5], vertsB[6]));
		
		
		//only keep it if it is inside of A
		minX = Math.min(Math.max(minX, 0), pA.getWidth());
		maxX = Math.min(Math.max(maxX, 0), pA.getWidth());
		minY = Math.min(Math.max(minY, 0), pA.getHeight());
		maxY = Math.min(Math.max(maxY, 0), pA.getHeight());
		
		final float[] X_StepVersor = new float[]{1,0};
		final float[] Y_StepVersor = new float[]{0,1};
		
		//calculate the variation in coordinates that appens between the pixel B1 and B2 in B when a "base step" is added from A1 to A2 in A
		TransformationUtils.transformNormal(aLocToBLoc, X_StepVersor);
		TransformationUtils.transformNormal(aLocToBLoc, Y_StepVersor);
		
		//find the pixel in B corresponding to the top-left vertex of the collision bounding box
		float[] initialPixelLocB = TransformationUtils.transform(aLocToBLoc, minX, minY, new float[2]);
		final float[] pixelLocB = new float[2];
		
		for(int y = (int) minY; y <  (int) maxY; y++) {
			pixelLocB[0] = initialPixelLocB[0];
			pixelLocB[1] = initialPixelLocB[1];

			for(int x =  (int) minX; x <  (int) maxX; x++) {
				
				if(pMaskA.isSolid(x, y) &&
						pMaskB.isSolid((int) pixelLocB[0], (int) pixelLocB[1]))
					return true;
				
				//next iteration will move to the next pixel on the right in A's local space. We need to update the "pointer" to the pixel in B by adding the displacement
				//generated by a step of one pixel to the right.
				pixelLocB[0] += X_StepVersor[0];
				pixelLocB[1] += X_StepVersor[1];
			}
			
			//same as above, but for the Y coordinate
			initialPixelLocB[0] += Y_StepVersor[0];
			initialPixelLocB[1] += Y_StepVersor[1];
		}
		
		return false;
	}
}
