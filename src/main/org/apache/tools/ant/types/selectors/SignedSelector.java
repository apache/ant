/*
 * Copyright  2004 The Apache Software Foundation
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

package org.apache.tools.ant.types.selectors;
import java.io.File;
import org.apache.tools.ant.types.DataType;
import org.apache.tools.ant.taskdefs.IsSigned;

/**
 * Selector that chooses files based on whether they are signed or not.
 *
 * @since 1.7
 */
public class SignedSelector extends DataType implements FileSelector {
    IsSigned isSigned = new IsSigned();

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
     * @param basedir the base directory the scan is being done from
     * @param filename is the name of the file to check
     * @param file is a java.io.File object the selector can use
     * @return whether the file should be selected or not
     */
    public boolean isSelected(File basedir, String filename, File file) {
        isSigned.setProject(getProject());
        isSigned.setFile(file);
        return isSigned.eval();
    }
}
