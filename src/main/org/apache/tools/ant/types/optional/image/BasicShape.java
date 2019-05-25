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


/** Draw a basic shape */
public abstract class BasicShape extends ImageOperation implements DrawOperation {
    // CheckStyle:VisibilityModifier OFF - bc
    // CheckStyle:MemberNameCheck OFF - bc
    protected int stroke_width = 0;
    // CheckStyle:MemberNameCheck ON
    protected String fill = "transparent";
    protected String stroke = "black";
    // CheckStyle:VisibilityModifier ON


    /**
     * Set the fill attribute.
     * @param col the color value to use.
     */
    public void setFill(String col) {
        fill = col;
    }

    /**
     * Set the stroke attribute.
     * @param col the color value to use.
     */
    public void setStroke(String col) {
        stroke = col;
    }

    /**
     * Set the stroke width attribute.
     * @param width the value to use.
     */
    public void setStrokewidth(int width) {
        stroke_width = width;
    }
}
