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

import org.apache.tools.ant.types.EnumeratedAttribute;

import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;

/**
 *
 * @see org.apache.tools.ant.taskdefs.optional.image.ImageIOTask
 */
public class Scale extends TransformOperation implements DrawOperation {
    private static final int HUNDRED = 100;

    private String widthStr = "100%";
    private String heightStr = "100%";
    private boolean xPercent = true;
    private boolean yPercent = true;
    private String proportions = "ignore";

    /** Enumerated class for proportions attribute. */
    public static class ProportionsAttribute extends EnumeratedAttribute {
        /** {@inheritDoc}. */
        @Override
        public String[] getValues() {
            return new String[] { "ignore", "width", "height", "cover", "fit" };
        }
    }

    /**
     *  Sets the behaviour regarding the image proportions.
     * @param pa the enumerated value.
     */
    public void setProportions(ProportionsAttribute pa) {
        proportions = pa.getValue();
    }

    /**
     * Sets the width of the image, either as an integer or a %.
     * Defaults to 100%.
     * @param width the value to use.
     */
    public void setWidth(String width) {
        widthStr = width;
    }

    /**
     *  Sets the height of the image, either as an integer or a %.  Defaults to 100%.
     * @param height the value to use.
     */
    public void setHeight(String height) {
        heightStr = height;
    }

    /**
     * Get the width.
     * @return the value converted from the width string.
     */
    public float getWidth() {
        int percIndex = widthStr.indexOf('%');
        if (percIndex > 0) {
            xPercent = true;
            float width = Float.parseFloat(widthStr.substring(0, percIndex));
            return width / HUNDRED;
        }
        xPercent = false;
        return Float.parseFloat(widthStr);
    }

    /**
     * Get the height.
     * @return the value converted from the height string.
     */
    public float getHeight() {
        int percIndex = heightStr.indexOf('%');
        if (percIndex > 0) {
            yPercent = true;
            return Float.parseFloat(heightStr.substring(0, percIndex)) / HUNDRED;
        }
        yPercent = false;
        return Float.parseFloat(heightStr);
    }

    /**
     * Scale an image.
     * @param image the image to scale.
     * @return the scaled image.
     */
    public BufferedImage performScale(BufferedImage image) {
        float xFl = getWidth();
        float yFl = getHeight();

        if (!xPercent) {
            xFl /= image.getWidth();
        }
        if (!yPercent) {
            yFl /= image.getHeight();
        }

        if ("width".equals(proportions)) {
            yFl = xFl;
        } else if ("height".equals(proportions)) {
            xFl = yFl;
        } else if ("fit".equals(proportions)) {
            yFl = Math.min(xFl, yFl);
            xFl = yFl;
        } else if ("cover".equals(proportions)) {
            yFl = Math.max(xFl, yFl);
            xFl = yFl;
        }

        log("\tScaling to " + (xFl * HUNDRED) + "% x "
            + (yFl * HUNDRED) + "%");

        AffineTransform tx = AffineTransform.getScaleInstance(xFl, yFl);
        BufferedImage scaleImage = new BufferedImage((int) (xFl * image.getWidth()),
                (int) (yFl * image.getHeight()), image.getType());
        AffineTransformOp scaleOp = new AffineTransformOp(tx, AffineTransformOp.TYPE_BILINEAR);
        scaleOp.filter(image, scaleImage);
        return scaleImage;
    }

    /** {@inheritDoc}. */
    @Override
    public BufferedImage executeTransformOperation(BufferedImage image) {
        for (ImageOperation instr : instructions) {
            if (instr instanceof DrawOperation) {
                return performScale(image);
            }
            if (instr instanceof TransformOperation) {
                image = ((TransformOperation) instr).executeTransformOperation(image);
            }
        }
        return performScale(image);
    }


    /** {@inheritDoc}. */
    @Override
    public BufferedImage executeDrawOperation() {
        for (ImageOperation instr : instructions) {
            if (instr instanceof DrawOperation) {
                BufferedImage image = null;
                // If this TransformOperation has DrawOperation children
                // then Rotate the first child and return.
                performScale(image);
                return image;
            }
        }
        return null;
    }
}
