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
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;

/**
 *
 * @author <a href="mailto:kzgrey@ntplx.net">Kevin Z Grey</a>
 * @see org.apache.tools.ant.taskdefs.optional.image.Image
 */
public class Text extends ImageOperation implements DrawOperation {
    private String str_text = "";
    private String font = "Arial";
    private int point = 10;
    private boolean bold = false;
    private boolean italic = false;
    private String color = "black";

    public void setString(String str) {
        str_text = str;
    }

    public void setFont(String f) {
        font = f;
    }

    public void setPoint(String p) {
        point = Integer.parseInt(p);
    }

    public void setColor(String c) {
        color = c;
    }

    /**
     * @todo is this used?
     */
    public void setBold(boolean state) {
        bold = state;
    }

    /**
     * @todo is this used?
     */
    public void setItalic(boolean state) {
        italic = state;
    }

    public PlanarImage executeDrawOperation() {
        log("\tCreating Text \"" + str_text + "\"");

        Color couloir = ColorMapper.getColorByName(color);
        int width = 1;
        int height = 1;

        BufferedImage bi = new BufferedImage(width, height, BufferedImage.TYPE_4BYTE_ABGR_PRE);
        Graphics2D graphics = (Graphics2D) bi.getGraphics();
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        graphics.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
        Font f = new Font(font, Font.PLAIN, point);
        FontMetrics fmetrics = graphics.getFontMetrics(f);
        height = fmetrics.getMaxAscent() + fmetrics.getMaxDescent();
        width = fmetrics.stringWidth(str_text);


        bi = new BufferedImage(width, height, BufferedImage.TYPE_4BYTE_ABGR_PRE);
        graphics = (Graphics2D) bi.getGraphics();

        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        graphics.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);

        graphics.setFont(f);
        graphics.setColor(couloir);
        graphics.drawString(str_text, 0, height - fmetrics.getMaxDescent());
        PlanarImage image = PlanarImage.wrapRenderedImage(bi);
        return image;
    }
}
