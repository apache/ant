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

import java.util.Vector;

import org.apache.tools.ant.types.DataType;

/**
 *
 * @see org.apache.tools.ant.taskdefs.optional.image.Image
 */
public abstract class ImageOperation extends DataType {
     // CheckStyle:VisibilityModifier OFF - bc
    protected Vector<ImageOperation> instructions = new Vector<>();
     // CheckStyle:VisibilityModifier ON

    /**
     * Add a rotate to the operation.
     * @param instr the rotate to add.
     */
    public void addRotate(Rotate instr) {
        instructions.add(instr);
    }

    /**
     * Add a draw to the operation.
     * @param instr the draw to add.
     */
    public void addDraw(Draw instr) {
        instructions.add(instr);
    }

    /**
     * Add a rectangle to the operation.
     * @param instr the rectangle to add.
     */
    public void addRectangle(Rectangle instr) {
        instructions.add(instr);
    }

    /**
     * Add text to the operation.
     * @param instr the text to add.
     */
    public void addText(Text instr) {
        instructions.add(instr);
    }

    /**
     * Add a scale to the operation.
     * @param instr the scale to add.
     */
    public void addScale(Scale instr) {
        instructions.add(instr);
    }
}
