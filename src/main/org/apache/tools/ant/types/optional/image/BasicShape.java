/*
 * Copyright  2002,2004 The Apache Software Foundation
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

public abstract class BasicShape extends ImageOperation implements DrawOperation {
    protected int stroke_width = 0;
    protected String fill = "transparent";
    protected String stroke = "black";


    public void setFill(String col) {
        fill = col;
    }

    public void setStroke(String col) {
        stroke = col;
    }

    public void setStrokewidth(int width) {
        stroke_width = width;
    }
}
