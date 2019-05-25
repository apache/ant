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

import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;

/**
 *
 * @see org.apache.tools.ant.taskdefs.optional.image.ImageIOTask
 */
public class Text extends ImageOperation implements DrawOperation {
    private static final int DEFAULT_POINT = 10;

    private String string = "";
    private String font = "Arial";
    private int point = DEFAULT_POINT;
    private boolean bold = false;
    private boolean italic = false;
    private String color = "black";

    /**
     * Set the string to be used as text.
     * @param str the string to be used.
     */
    public void setString(String str) {
        string = str;
    }

    /**
     * Set the font to be used to draw the text.
     * @param f the font to be used.
     */
    public void setFont(String f) {
        font = f;
    }

    /**
     * Set the number of points to be used.
     * @param p an integer value as a string.
     */
    public void setPoint(String p) {
        point = Integer.parseInt(p);
    }

    /**
     * Set the color of the text.
     * @param c the color name.
     */
    public void setColor(String c) {
        color = c;
    }

    /**
     * @todo is this used?
     * @param state not used at the moment.
     */
    public void setBold(boolean state) {
        bold = state;
    }

    /**
     * @todo is this used?
     * @param state not used at the moment.
     */
    public void setItalic(boolean state) {
        italic = state;
    }

    /**
     * Draw the text.
     * @return the resultant image.
     */
    @Override
    public BufferedImage executeDrawOperation() {
        log("\tCreating Text \"" + string + "\"");

        int width = 1;
        int height = 1;

        BufferedImage bi = new BufferedImage(width, height, BufferedImage.TYPE_4BYTE_ABGR_PRE);
        Graphics2D graphics = bi.createGraphics();
        graphics.setRenderingHint(
            RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        graphics.setRenderingHint(
            RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
        Font f = createFont();
        FontMetrics fmetrics = graphics.getFontMetrics(f);
        height = fmetrics.getMaxAscent() + fmetrics.getMaxDescent();
        width = fmetrics.stringWidth(string);

        bi = new BufferedImage(width, height, BufferedImage.TYPE_4BYTE_ABGR_PRE);
        graphics = bi.createGraphics();

        graphics.setRenderingHint(
            RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        graphics.setRenderingHint(
            RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);

        graphics.setFont(f);
        graphics.setColor(ColorMapper.getColorByName(color));
        graphics.drawString(string, 0, height - fmetrics.getMaxDescent());
        return bi;
    }

    private Font createFont() {
        int style = Font.PLAIN;
        if (bold) {
            style |= Font.BOLD;
        }
        if (italic) {
            style |= Font.ITALIC;
        }
        return new Font(font, style, point);
    }
}
