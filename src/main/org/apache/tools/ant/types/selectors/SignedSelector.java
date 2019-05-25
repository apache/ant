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

package org.apache.tools.ant.types.selectors;
import java.io.File;

import org.apache.tools.ant.taskdefs.condition.IsSigned;
import org.apache.tools.ant.types.DataType;

/**
 * Selector that chooses files based on whether they are signed or not.
 *
 * @since 1.7
 */
public class SignedSelector extends DataType implements FileSelector {
    private IsSigned isSigned = new IsSigned();

    /**
     * The signature name to check jarfile for.
     *
     * @param name signature to look for.
     */
    public void setName(String name) {
        isSigned.setName(name);
    }

    /**
     * The heart of the matter. This is where the selector gets to decide
     * on the inclusion of a file in a particular fileset.
     *
     * @param basedir not used by this selector
     * @param filename not used by this selector
     * @param file     path to file to be selected
     * @return whether the file should be selected or not
     */
    @Override
    public boolean isSelected(File basedir, String filename, File file) {
        if (file.isDirectory()) {
            return false; // Quick return: directories cannot be signed
        }
        isSigned.setProject(getProject());
        isSigned.setFile(file);
        return isSigned.eval();
    }
}
