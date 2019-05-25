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
package org.apache.tools.ant.types.optional.imageio;

import org.apache.tools.ant.Project;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.util.Arrays;

/**
 * ImageOperation to rotate an image by a certain degree
 *
 * @see org.apache.tools.ant.taskdefs.optional.image.ImageIOTask
 */
public class Rotate extends TransformOperation implements DrawOperation {
    private static final float HALF_CIRCLE = 180.0F;

    private float angle = 0.0F;

    /**
     * Sets the angle of rotation in degrees.
     * @param ang The angle at which to rotate the image
     */
    public void setAngle(String ang) {
        angle = Float.parseFloat(ang) % (2 * HALF_CIRCLE);
    }

    /**
     * Rotate an image.
     * @param image the image to rotate.
     * @return the rotated image.
     */
    public BufferedImage performRotate(BufferedImage image) {
        // Float zero can be negative
        if (Float.compare(Math.abs(angle), 0.0F) == 0) {
            return image;
        }

        if (angle < 0) {
            angle += 2 * HALF_CIRCLE;
        }

        // 180 degree rotation == flip the image vertically and horizontally
        if (Float.compare(angle, HALF_CIRCLE) == 0) {
            log("Flipping an image", Project.MSG_DEBUG);
            AffineTransform tx = AffineTransform.getScaleInstance(-1, -1);
            tx.translate(-image.getWidth(null), -image.getHeight(null));
            AffineTransformOp op = new AffineTransformOp(tx, AffineTransformOp.TYPE_NEAREST_NEIGHBOR);
            return op.filter(image, null);
        }

        AffineTransform tx = AffineTransform.getRotateInstance((float) (angle
                * (Math.PI / HALF_CIRCLE)));
        // Figure out the new bounding box
        Rectangle2D box = getBoundingBox(image, tx);
        AffineTransform translation = AffineTransform.getTranslateInstance(-box.getMinX(),
                -box.getMinY());
        tx.preConcatenate(translation);
        BufferedImage rotatedImage = new BufferedImage((int) Math.round(box.getWidth()),
                (int) Math.round(box.getHeight()), image.getType());
        // Avoid black space around the rotated image
        Graphics2D graphics = rotatedImage.createGraphics();
        graphics.setPaint(new Color(image.getRGB(0, 0)));
        graphics.fillRect(0, 0, rotatedImage.getWidth(), rotatedImage.getHeight());
        graphics.dispose();
        // Rotate
        AffineTransformOp rotateOp = new AffineTransformOp(tx, AffineTransformOp.TYPE_BILINEAR);
        rotateOp.filter(image, rotatedImage);
        return rotatedImage;
    }

    private Rectangle2D getBoundingBox(BufferedImage image, AffineTransform tx) {
        int xmax = image.getWidth() - 1;
        int ymax = image.getHeight() - 1;
        Point2D[] corners = new Point2D.Double[4];
        corners[0] = new Point2D.Double(0, 0);
        corners[1] = new Point2D.Double(xmax, 0);
        corners[2] = new Point2D.Double(xmax, ymax);
        corners[3] = new Point2D.Double(0, ymax);
        tx.transform(corners, 0, corners, 0, 4);

        // Create bounding box of transformed corner points
        Rectangle2D boundingBox = new Rectangle2D.Double();
        Arrays.stream(corners, 0, 4).forEach(boundingBox::add);
        return boundingBox;
    }

    /**
     * Performs the image rotation when being handled as a TransformOperation.
     * @param image The image to perform the transformation on.
     * @return the transformed image.
     */
    @Override
    public BufferedImage executeTransformOperation(BufferedImage image) {
        for (ImageOperation instr : instructions) {
            if (instr instanceof DrawOperation) {
                // If this TransformOperation has DrawOperation children
                // then Rotate the first child and return.
                BufferedImage op = ((DrawOperation) instr).executeDrawOperation();
                return performRotate(op);
            }
            if (instr instanceof TransformOperation) {
                image = ((TransformOperation) instr).executeTransformOperation(image);
            }
        }
        image = performRotate(image);
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
    public BufferedImage executeDrawOperation() {
        for (ImageOperation instr : instructions) {
            if (instr instanceof DrawOperation) {
                // If this TransformOperation has DrawOperation children
                // then Rotate the first child and return.
                BufferedImage op = ((DrawOperation) instr).executeDrawOperation();
                return performRotate(op);
            }
        }
        return null;
    }

}
