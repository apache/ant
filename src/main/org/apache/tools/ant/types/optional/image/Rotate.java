/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2002-2003 The Apache Software Foundation.  All rights
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
 * 4. The names "Ant" and "Apache Software
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
import javax.media.jai.InterpolationNearest;
import javax.media.jai.JAI;
import java.awt.image.renderable.ParameterBlock;
import java.awt.image.BufferedImage;
import java.awt.*;

/**
 * ImageOperation to rotate an image by a certain degree
 *
 * @author <a href="mailto:kzgrey@ntplx.net">Kevin Z Grey</a>
 * @see org.apache.tools.ant.tasks.optional.image.Image
 */
public class Rotate extends TransformOperation implements DrawOperation {
    protected float angle = 0.0F;

    /**
     * Sets the angle of rotation in degrees.
     * @param ang The angle at which to rotate the image
     */
    public void setAngle(String ang) {
        angle = Float.parseFloat(ang);
    }


    public PlanarImage performRotate(PlanarImage image) {
        float t_angle = (float) (angle * (Math.PI / 180.0F));
        ParameterBlock pb = new ParameterBlock();
        pb.addSource(image);
        pb.add(0.0F);
        pb.add(0.0F);
        pb.add(t_angle);
        pb.add(new InterpolationNearest());
        return JAI.create("Rotate", pb, null);
    }


    /**
     *  Performs the image rotation when being handled as a TransformOperation.
     * @param image The image to perform the transformation on.
     */
    public PlanarImage executeTransformOperation(PlanarImage image) {
        BufferedImage bi = null;
        Graphics2D graphics = null;
        for (int i = 0; i < instructions.size(); i++) {
            ImageOperation instr = ((ImageOperation) instructions.elementAt(i));
            if (instr instanceof DrawOperation) {
                // If this TransformOperation has DrawOperation children
                // then Rotate the first child and return.
                System.out.println("Execing Draws");
                PlanarImage op = ((DrawOperation) instr).executeDrawOperation();
                image = performRotate(op);
                return image;
            } else if (instr instanceof TransformOperation) {
                bi = image.getAsBufferedImage();
                graphics = (Graphics2D) bi.getGraphics();
                System.out.println("Execing Transforms");
                image = ((TransformOperation) instr).executeTransformOperation(PlanarImage.wrapRenderedImage(bi));
                bi = image.getAsBufferedImage();
            }
        }
        System.out.println("Execing as TransformOperation");
        image = performRotate(image);
        System.out.println(image);
        return image;
    }

    /**
     *  Performs the image rotation when being handled as a DrawOperation.
     *  It absolutely requires that there be a DrawOperation nested beneath it,
     *  but only the FIRST DrawOperation will be handled since it can only return
     *  ONE image.
     * @param image The image to perform the transformation on.
     */
    public PlanarImage executeDrawOperation() {
        for (int i = 0; i < instructions.size(); i++) {
            ImageOperation instr = ((ImageOperation) instructions.elementAt(i));
            if (instr instanceof DrawOperation) {
                // If this TransformOperation has DrawOperation children
                // then Rotate the first child and return.
                PlanarImage op = ((DrawOperation) instr).executeDrawOperation();
                op = performRotate(op);
                return op;
            }
        }
        return null;
    }

}
