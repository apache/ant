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

import java.awt.BasicStroke;
import java.awt.Graphics2D;
import java.awt.geom.Arc2D;
import java.awt.image.BufferedImage;

import javax.media.jai.PlanarImage;

/**
 * Draw an arc.
 */
public class Arc extends BasicShape implements DrawOperation {
    // CheckStyle:VisibilityModifier OFF - bc
    protected int width = 0;
    protected int height = 0;
    protected int start = 0;
    protected int stop = 0;
    protected int type = Arc2D.OPEN;
    // CheckStyle:VisibilityModifier ON

    /**
     * Set the width.
     * @param width the width of the arc.
     */
    public void setWidth(int width) {
        this.width = width;
    }

    /**
     * Set the height.
     * @param height the height of the arc.
     */
    public void setHeight(int height) {
        this.height = height;
    }

    /**
     * Set the start of the arc.
     * @param start the start of the arc.
     */
    public void setStart(int start) {
        this.start = start;
    }

    /**
     * Set the stop of the arc.
     * @param stop the stop of the arc.
     */
    public void setStop(int stop) {
        this.stop = stop;
    }

    /**
     * Set the type of arc.
     * @param strType the type to use - open, pie or chord.
     * @todo refactor using an EnumeratedAttribute
     */
    public void setType(String strType) {
        if ("open".equalsIgnoreCase(strType)) {
            type = Arc2D.OPEN;
        } else if ("pie".equalsIgnoreCase(strType)) {
            type = Arc2D.PIE;
        } else if ("chord".equalsIgnoreCase(strType)) {
            type = Arc2D.CHORD;
        }
    }

    /** {@inheritDoc}. */
    @Override
    public PlanarImage executeDrawOperation() {
        BufferedImage bi = new BufferedImage(width + (stroke_width * 2),
            height + (stroke_width * 2), BufferedImage.TYPE_4BYTE_ABGR_PRE);

        Graphics2D graphics = bi.createGraphics();

        if (!"transparent".equalsIgnoreCase(stroke)) {
            BasicStroke bStroke = new BasicStroke(stroke_width);
            graphics.setColor(ColorMapper.getColorByName(stroke));
            graphics.setStroke(bStroke);
            graphics.draw(new Arc2D.Double(stroke_width, stroke_width, width,
                height, start, stop, type));
        }

        if (!"transparent".equalsIgnoreCase(fill)) {
            graphics.setColor(ColorMapper.getColorByName(fill));
            graphics.fill(new Arc2D.Double(stroke_width, stroke_width,
                width, height, start, stop, type));
        }

        for (ImageOperation instr : instructions) {
            if (instr instanceof DrawOperation) {
                PlanarImage img = ((DrawOperation) instr).executeDrawOperation();
                graphics.drawImage(img.getAsBufferedImage(), null, 0, 0);
            } else if (instr instanceof TransformOperation) {
                PlanarImage image = ((TransformOperation) instr)
                    .executeTransformOperation(PlanarImage.wrapRenderedImage(bi));
                bi = image.getAsBufferedImage();
                graphics = bi.createGraphics();
            }
        }
        return PlanarImage.wrapRenderedImage(bi);
    }
}
