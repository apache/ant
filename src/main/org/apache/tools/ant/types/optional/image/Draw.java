/* 
 * Copyright  2002-2004 Apache Software Foundation
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 * 
 */
package org.apache.tools.ant.types.optional.image;

import javax.media.jai.PlanarImage;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

/**
 *
 * @author <a href="mailto:kzgrey@ntplx.net">Kevin Z Grey</a>
 * @see org.apache.tools.ant.taskdefs.optional.image.Image
 */
public class Draw extends TransformOperation {
    protected int xloc = 0;
    protected int yloc = 0;

    public void setXloc(int x) {
        xloc = x;
    }

    public void setYloc(int y) {
        yloc = y;
    }

    public void addRectangle(Rectangle rect) {
        instructions.add(rect);
    }

    public void addText(Text text) {
        instructions.add(text);
    }

    public void addEllipse(Ellipse elip) {
        instructions.add(elip);
    }

    public void addArc(Arc arc) {
        instructions.add(arc);
    }


    public PlanarImage executeTransformOperation(PlanarImage image) {
        BufferedImage bi = image.getAsBufferedImage();
        Graphics2D graphics = (Graphics2D) bi.getGraphics();

        for (int i = 0; i < instructions.size(); i++) {
            ImageOperation instr = ((ImageOperation) instructions.elementAt(i));
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
                PlanarImage.wrapRenderedImage(bi);
            }
        }
        image = PlanarImage.wrapRenderedImage(bi);

        return image;
    }
}
