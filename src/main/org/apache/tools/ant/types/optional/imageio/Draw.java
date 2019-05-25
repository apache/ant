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

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

/**
 *
 * @see org.apache.tools.ant.taskdefs.optional.image.ImageIOTask
 */
public class Draw extends TransformOperation {
    private int xloc = 0;
    private int yloc = 0;

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

    /**
     * Add text to the operation.
     * @param text the text to add.
     */
    public void addText(Text text) {
        instructions.add(text);
    }

    /**
     * Add a rectangle to the operation.
     * @param rect the rectangle to add.
     */
    public void addRectangle(Rectangle rect) {
        instructions.add(rect);
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
    public BufferedImage executeTransformOperation(BufferedImage bi) {
        Graphics2D graphics = bi.createGraphics();

        for (ImageOperation instr : instructions) {
            if (instr instanceof DrawOperation) {
                BufferedImage op = ((DrawOperation) instr).executeDrawOperation();
                log("\tDrawing to x=" + xloc + " y=" + yloc);
                graphics.drawImage(op, null, xloc, yloc);
            } else if (instr instanceof TransformOperation) {
                BufferedImage child
                    = ((TransformOperation) instr).executeTransformOperation(null);
                log("\tDrawing to x=" + xloc + " y=" + yloc);
                graphics.drawImage(child, null, xloc, yloc);
            }
        }
        return bi;
    }
}
