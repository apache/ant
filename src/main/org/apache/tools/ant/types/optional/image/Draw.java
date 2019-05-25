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

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import javax.media.jai.PlanarImage;

/**
 *
 * @see org.apache.tools.ant.taskdefs.optional.image.Image
 */
public class Draw extends TransformOperation {
    // CheckStyle:VisibilityModifier OFF - bc
    protected int xloc = 0;
    protected int yloc = 0;
    // CheckStyle:VisibilityModifier ON

    /**
     * Set the X location.
     * @param x the value to use.
     */
    public void setXloc(int x) {
        xloc = x;
    }

    /**
     * Set the Y location.
     * @param y the value to use.
     */
    public void setYloc(int y) {
        yloc = y;
    }

    /** {@inheritDoc}. */
    @Override
    public void addRectangle(Rectangle rect) {
        instructions.add(rect);
    }

    /** {@inheritDoc}. */
    @Override
    public void addText(Text text) {
        instructions.add(text);
    }

    /**
     * Add an ellipse.
     * @param elip the ellipse to add.
     */
    public void addEllipse(Ellipse elip) {
        instructions.add(elip);
    }

    /**
     * Add an arc.
     * @param arc the arc to add.
     */
    public void addArc(Arc arc) {
        instructions.add(arc);
    }

    /** {@inheritDoc}. */
    @Override
    public PlanarImage executeTransformOperation(PlanarImage image) {
        BufferedImage bi = image.getAsBufferedImage();
        Graphics2D graphics = bi.createGraphics();

        for (ImageOperation instr : instructions) {
            if (instr instanceof DrawOperation) {
                PlanarImage op = ((DrawOperation) instr).executeDrawOperation();
                log("\tDrawing to x=" + xloc + " y=" + yloc);
                graphics.drawImage(op.getAsBufferedImage(), null, xloc, yloc);
            } else if (instr instanceof TransformOperation) {
                PlanarImage op
                    = ((TransformOperation) instr).executeTransformOperation(null);
                BufferedImage child = op.getAsBufferedImage();
                log("\tDrawing to x=" + xloc + " y=" + yloc);
                graphics.drawImage(child, null, xloc, yloc);
            }
        }
        return PlanarImage.wrapRenderedImage(bi);
    }
}
