/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package org.apache.tools.ant.types.optional.image;

import java.awt.image.BufferedImage;
import java.awt.image.renderable.ParameterBlock;

import javax.media.jai.InterpolationNearest;
import javax.media.jai.JAI;
import javax.media.jai.PlanarImage;

/**
 * ImageOperation to rotate an image by a certain degree
 *
 * @see org.apache.tools.ant.taskdefs.optional.image.Image
 */
public class Rotate extends TransformOperation implements DrawOperation {
    private static final float HALF_CIRCLE = 180.0F;

    // CheckStyle:VisibilityModifier OFF - bc
    protected float angle = 0.0F;
    // CheckStyle:VisibilityModifier ON

    /**
     * Sets the angle of rotation in degrees.
     * @param ang The angle at which to rotate the image
     */
    public void setAngle(String ang) {
        angle = Float.parseFloat(ang);
    }


    /**
     * Rotate an image.
     * @param image the image to rotate.
     * @return the rotated image.
     */
    public PlanarImage performRotate(PlanarImage image) {
        float tAngle = (float) (angle * (Math.PI / HALF_CIRCLE));
        ParameterBlock pb = new ParameterBlock();
        pb.addSource(image);
        pb.add(0.0F);
        pb.add(0.0F);
        pb.add(tAngle);
        pb.add(new InterpolationNearest());
        return JAI.create("Rotate", pb, null);
    }


    /**
     * Performs the image rotation when being handled as a TransformOperation.
     * @param image The image to perform the transformation on.
     * @return the transformed image.
     */
    @Override
    public PlanarImage executeTransformOperation(PlanarImage image) {
        for (ImageOperation instr : instructions) {
            if (instr instanceof DrawOperation) {
                // If this TransformOperation has DrawOperation children
                // then Rotate the first child and return.
                System.out.println("Exec'ing Draws");
                PlanarImage op = ((DrawOperation) instr).executeDrawOperation();
                return performRotate(op);
            }
            if (instr instanceof TransformOperation) {
                BufferedImage bi = image.getAsBufferedImage();
                System.out.println("Exec'ing Transforms");
                image = ((TransformOperation) instr)
                    .executeTransformOperation(PlanarImage.wrapRenderedImage(bi));
            }
        }
        System.out.println("Exec'ing as TransformOperation");
        image = performRotate(image);
        System.out.println(image);
        return image;
    }

    /**
     *  Performs the image rotation when being handled as a DrawOperation.
     *  It absolutely requires that there be a DrawOperation nested beneath it,
     *  but only the FIRST DrawOperation will be handled since it can only return
     *  ONE image.
     * @return the image.
     */
    @Override
    public PlanarImage executeDrawOperation() {
        for (ImageOperation instr : instructions) {
            if (instr instanceof DrawOperation) {
                // If this TransformOperation has DrawOperation children
                // then Rotate the first child and return.
                PlanarImage op = ((DrawOperation) instr).executeDrawOperation();
                return performRotate(op);
            }
        }
        return null;
    }

}
