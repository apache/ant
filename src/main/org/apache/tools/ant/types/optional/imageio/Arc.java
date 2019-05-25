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

import java.awt.BasicStroke;
import java.awt.Graphics2D;
import java.awt.geom.Arc2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

/**
 * Draw an arc.
 */
public class Arc extends BasicShape implements DrawOperation {
    private int start = 0;
    private int stop = 0;
    private ArcType type = ArcType.getDefault();

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
     * @param arcType the type to use - open, pie or chord.
     */
    public void setType(ArcType arcType) {
        type = arcType;
    }

    /** {@inheritDoc}. */
    @Override
    public BufferedImage executeDrawOperation() {
        BufferedImage bi = new BufferedImage(width + (strokeWidth * 2),
            height + (strokeWidth * 2), BufferedImage.TYPE_4BYTE_ABGR_PRE);

        Graphics2D graphics = bi.createGraphics();

        if (!"transparent".equalsIgnoreCase(stroke)) {
            BasicStroke bStroke = new BasicStroke(strokeWidth);
            graphics.setColor(ColorMapper.getColorByName(stroke));
            graphics.setStroke(bStroke);
            graphics.draw(new Arc2D.Double(strokeWidth, strokeWidth, width,
                height, start, stop, type.getIndex()));
        }

        if (!"transparent".equalsIgnoreCase(fill)) {
            graphics.setColor(ColorMapper.getColorByName(fill));
            graphics.fill(new Arc2D.Double(strokeWidth, strokeWidth,
                width, height, start, stop, type.getIndex()));
        }

        for (ImageOperation instr : instructions) {
            if (instr instanceof DrawOperation) {
                BufferedImage img = ((DrawOperation) instr).executeDrawOperation();
                graphics.drawImage(img, null, 0, 0);
            } else if (instr instanceof TransformOperation) {
                bi = ((TransformOperation) instr).executeTransformOperation(bi);
                graphics = bi.createGraphics();
            }
        }
        return bi;
    }

    public static class ArcType extends EnumeratedAttribute {

        private static final List<String> VALUES = new ArrayList<>();

        static {
            VALUES.add(Arc2D.OPEN, "open");
            VALUES.add(Arc2D.CHORD, "chord");
            VALUES.add(Arc2D.PIE, "pie");
        }

        /**
         * Constructor
         */
        public ArcType() {
        }

        /**
         * Constructor using a string.
         * @param value the value of the attribute
         */
        public ArcType(String value) {
            setValue(value);
        }

        /**
         * Get the default ArcType value.
         * @return the default value.
         */
        public static ArcType getDefault() {
            return new ArcType(VALUES.get(0));
        }

        /**
         * @see EnumeratedAttribute#getValues()
         * {@inheritDoc}.
         */
        @Override
        public String[] getValues() {
            return VALUES.toArray(new String[0]);
        }
    }
}
