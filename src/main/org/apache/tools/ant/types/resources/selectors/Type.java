/*
 * Copyright 2005 The Apache Software Foundation
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
package org.apache.tools.ant.types.resources.selectors;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.types.Resource;
import org.apache.tools.ant.types.EnumeratedAttribute;

/**
 * Type file/dir ResourceSelector.
 * @since Ant 1.7
 */
public class Type implements ResourceSelector {

    /**
     * Implements the type attribute.
     */
    public static class FileDir extends EnumeratedAttribute {
        private static final String[] VALUES = new String[] {"file", "dir"};

        /**
         * Return the possible values.
         * @return a String array.
         */
        public String[] getValues() {
            return VALUES;
        }
    }

    private FileDir type = null;

    /**
     * Set type; file|dir.
     * @param fd a FileDir object.
     */
    public void setType(FileDir fd) {
        type = fd;
    }

    /**
     * Return true if this Resource is selected.
     * @param r the Resource to check.
     * @return whether the Resource was selected.
     */
    public boolean isSelected(Resource r) {
        if (type == null) {
            throw new BuildException("The type attribute is required.");
        }
        int i = type.getIndex();
        return r.isDirectory() ? i == 1 : i == 0;
    }

}
