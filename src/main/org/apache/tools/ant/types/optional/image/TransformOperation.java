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
import javax.media.jai.PlanarImage;

/**
 *
 * @see org.apache.tools.ant.tasks.optional.image.Image
 */
public abstract class TransformOperation extends ImageOperation {
    public abstract PlanarImage executeTransformOperation(PlanarImage img);

    public void addRectangle(Rectangle instr) {
        instructions.add(instr);
    }

}
