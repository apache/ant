/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2002 The Apache Software Foundation.  All rights
 * reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution, if
 *    any, must include the following acknowlegement:
 *       "This product includes software developed by the
 *        Apache Software Foundation (http://www.apache.org/)."
 *    Alternately, this acknowlegement may appear in the software itself,
 *    if and wherever such third-party acknowlegements normally appear.
 *
 * 4. The names "The Jakarta Project", "Ant", and "Apache Software
 *    Foundation" must not be used to endorse or promote products derived
 *    from this software without prior written permission. For written
 *    permission, please contact apache@apache.org.
 *
 * 5. Products derived from this software may not be called "Apache"
 *    nor may "Apache" appear in their names without prior written
 *    permission of the Apache Group.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE APACHE SOFTWARE FOUNDATION OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Apache Software Foundation.  For more
 * information on the Apache Software Foundation, please see
 * <http://www.apache.org/>.
 */
package org.apache.tools.ant.types.optional.image;

import javax.media.jai.PlanarImage;
import java.awt.BasicStroke;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

/**
 *
 * @author <a href="mailto:kzgrey@ntplx.net">Kevin Z Grey</a>
 * @see org.apache.tools.ant.taskdefs.optional.image.Image
 */
public class Rectangle extends BasicShape implements DrawOperation {
    protected int width = 0;
    protected int height = 0;
    protected int arcwidth = 0;
    protected int archeight = 0;

    public void setWidth(int w) {
        width = w;
    }

    public void setHeight(int h) {
        height = h;
    }

    public void setArcwidth(int w) {
        arcwidth = w;
    }

    public void setArcheight(int h) {
        archeight = h;
    }

    public PlanarImage executeDrawOperation() {
        log("\tCreating Rectangle w=" + width + " h=" + height + " arcw=" + arcwidth + " arch=" + archeight);
        BufferedImage bi = new BufferedImage(width, height, BufferedImage.TYPE_4BYTE_ABGR_PRE);

        Graphics2D graphics = (Graphics2D) bi.getGraphics();

        if (!stroke.equals("transparent")) {
            BasicStroke b_stroke = new BasicStroke(stroke_width);
            graphics.setColor(ColorMapper.getColorByName(stroke));
            graphics.setStroke(b_stroke);

            if ((arcwidth != 0) || (archeight != 0)) {
                graphics.drawRoundRect(0, 0, width, height, arcwidth, archeight);
            } else {
                graphics.drawRect(0, 0, width, height);
            }
        }

        if (!fill.equals("transparent")) {
            graphics.setColor(ColorMapper.getColorByName(fill));
            if ((arcwidth != 0) || (archeight != 0)) {
                graphics.fillRoundRect(stroke_width, stroke_width, width - (stroke_width * 2), height - (stroke_width * 2), arcwidth, archeight);
            } else {
                graphics.fillRect(stroke_width, stroke_width, width - (stroke_width * 2), height - (stroke_width * 2));
            }
        }


        for (int i = 0; i < instructions.size(); i++) {
            ImageOperation instr = ((ImageOperation) instructions.elementAt(i));
            if (instr instanceof DrawOperation) {
                PlanarImage img = ((DrawOperation) instr).executeDrawOperation();
                graphics.drawImage(img.getAsBufferedImage(), null, 0, 0);
            } else if (instr instanceof TransformOperation) {
                graphics = (Graphics2D) bi.getGraphics();
                PlanarImage image = ((TransformOperation) instr).executeTransformOperation(PlanarImage.wrapRenderedImage(bi));
                bi = image.getAsBufferedImage();
            }
        }
        return PlanarImage.wrapRenderedImage(bi);
    }
}
