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

import org.apache.tools.ant.types.EnumeratedAttribute;

import javax.media.jai.JAI;
import javax.media.jai.PlanarImage;
import java.awt.image.BufferedImage;
import java.awt.image.renderable.ParameterBlock;

/**
 *
 * @author <a href="mailto:kzgrey@ntplx.net">Kevin Z Grey</a>
 * @author <a href="mailto:roxspring@imapmail.org">Rob Oxspring</a>
 * @see org.apache.tools.ant.taskdefs.optional.image.Image
 */
public class Scale extends TransformOperation implements DrawOperation {

    private String width_str = "100%";
    private String height_str = "100%";
    private boolean x_percent = true;
    private boolean y_percent = true;
    private String proportions = "ignore";

    public static class ProportionsAttribute extends EnumeratedAttribute {
        public String[] getValues() {
            return new String[] {"ignore", "width", "height", "cover", "fit"};
        }
    }

    /**
     *  Sets the behaviour regarding the image proportions.
     */
    public void setProportions(ProportionsAttribute pa) {
        proportions = pa.getValue();
    }

    /**
     *  Sets the width of the image, either as an integer or a %.  Defaults to 100%.
     */
    public void setWidth(String width) {
        width_str = width;
    }

    /**
     *  Sets the height of the image, either as an integer or a %.  Defaults to 100%.
     */
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

        if ("width".equals(proportions)) {
            y_fl = x_fl;
        } else if ("height".equals(proportions)) {
            x_fl = y_fl;
        } else if ("fit".equals(proportions)) {
            x_fl = y_fl = Math.min(x_fl, y_fl);
        } else if ("cover".equals(proportions)) {
            x_fl = y_fl = Math.max(x_fl, y_fl);
        }

        pb.add(new Float(x_fl));
        pb.add(new Float(y_fl));

        log("\tScaling to " + (x_fl * 100) + "% x " + (y_fl * 100) + "%");

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
