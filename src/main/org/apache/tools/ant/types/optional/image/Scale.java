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

import javax.media.jai.JAI;
import javax.media.jai.PlanarImage;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.awt.image.renderable.ParameterBlock;

/**
 *
 * @author <a href="mailto:kzgrey@ntplx.net">Kevin Z Grey</a>
 * @see org.apache.tools.ant.taskdefs.optional.image.Image
 */
public class Scale extends TransformOperation implements DrawOperation {
    private String width_str = null;
    private String height_str = null;
    private boolean x_percent = true;
    private boolean y_percent = true;
    private boolean keep_proportions = false;

    public void setKeepproportions(boolean props) {
        keep_proportions = props;
    }

    public void setWidth(String width) {
        width_str = width;
    }

    public void setHeight(String height) {
        height_str = height;
    }

    public float getWidth() {
        float width = 0.0F;
        int perc_index = width_str.indexOf('%');
        if (perc_index > 0) {
            width = Float.parseFloat(width_str.substring(0, perc_index));
            x_percent = true;
            return width / 100;
        } else {
            x_percent = false;
            return Float.parseFloat(width_str);
        }
    }

    public float getHeight() {
        int perc_index = height_str.indexOf('%');
        if (perc_index > 0) {
            float height = Float.parseFloat(height_str.substring(0, perc_index));
            y_percent = true;
            return height / 100;
        } else {
            y_percent = false;
            return Float.parseFloat(height_str);
        }
    }

    public PlanarImage performScale(PlanarImage image) {
        ParameterBlock pb = new ParameterBlock();
        pb.addSource(image);
        float x_fl = getWidth();
        float y_fl = getHeight();
        if (!x_percent) {
            x_fl = (x_fl / image.getWidth());
        }
        if (!y_percent) {
            y_fl = (y_fl / image.getHeight());
        }
        if (keep_proportions) {
            y_fl = x_fl;
        }
        pb.add(new Float(x_fl));
        pb.add(new Float(y_fl));

        log("\tScaling to " + x_fl + "% x " + y_fl + "%");
        return JAI.create("scale", pb);
    }


    public PlanarImage executeTransformOperation(PlanarImage image) {
        BufferedImage bi = null;
        for (int i = 0; i < instructions.size(); i++) {
            ImageOperation instr = ((ImageOperation) instructions.elementAt(i));
            if (instr instanceof DrawOperation) {
                return performScale(image);
            } else if (instr instanceof TransformOperation) {
                bi = image.getAsBufferedImage();
                image = ((TransformOperation) instr).executeTransformOperation(PlanarImage.wrapRenderedImage(bi));
                bi = image.getAsBufferedImage();
            }
        }
        return performScale(image);
    }


    public PlanarImage executeDrawOperation() {
        for (int i = 0; i < instructions.size(); i++) {
            ImageOperation instr = ((ImageOperation) instructions.elementAt(i));
            if (instr instanceof DrawOperation) {
                PlanarImage image = null;
                // If this TransformOperation has DrawOperation children
                // then Rotate the first child and return.
                performScale(image);
                return image;
            }
        }
        return null;
    }
}
