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
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;

import javax.media.jai.PlanarImage;

/**
 * Draw an ellipse.
 * @see org.apache.tools.ant.taskdefs.optional.image.Image
 */
public class Ellipse extends BasicShape implements DrawOperation {
    // CheckStyle:VisibilityModifier OFF - bc
    protected int width = 0;
    protected int height = 0;
    // CheckStyle:VisibilityModifier ON

    /**
     * Set the width.
     * @param width the width of the ellipse.
     */
    public void setWidth(int width) {
        this.width = width;
    }

    /**
     * Set the height.
     * @param height the height of the ellipse.
     */
    public void setHeight(int height) {
        this.height = height;
    }

    /** {@inheritDoc}. */
    @Override
    public PlanarImage executeDrawOperation() {
        BufferedImage bi = new BufferedImage(width, height, BufferedImage.TYPE_4BYTE_ABGR_PRE);

        Graphics2D graphics = bi.createGraphics();

        if (!"transparent".equalsIgnoreCase(stroke)) {
            BasicStroke bStroke = new BasicStroke(stroke_width);
            graphics.setColor(ColorMapper.getColorByName(stroke));
            graphics.setStroke(bStroke);
            graphics.draw(new Ellipse2D.Double(0, 0, width, height));
        }

        if (!"transparent".equalsIgnoreCase(fill)) {
            graphics.setColor(ColorMapper.getColorByName(fill));
            graphics.fill(new Ellipse2D.Double(0, 0, width, height));
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
