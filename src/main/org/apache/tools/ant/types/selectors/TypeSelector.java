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

import org.apache.tools.ant.types.EnumeratedAttribute;
import org.apache.tools.ant.types.Parameter;

/**
 * Selector that selects a certain kind of file: directory or regular.
 *
 * @since 1.6
 */
public class TypeSelector extends BaseExtendSelector {

    /** Key to used for parameterized custom selector */
    public static final String TYPE_KEY = "type";

    private String type = null;

    /**
     * @return a string describing this object
     */
    public String toString() {
        return "{typeselector type: " + type + "}";
    }

    /**
     * Set the type of file to require.
     * @param fileTypes the type of file - file or dir
     */
    public void setType(FileType fileTypes) {
        this.type = fileTypes.getValue();
    }

    /**
     * When using this as a custom selector, this method will be called.
     * It translates each parameter into the appropriate setXXX() call.
     *
     * @param parameters the complete set of parameters for this selector
     */
    @Override
    public void setParameters(Parameter... parameters) {
        super.setParameters(parameters);
        if (parameters != null) {
            for (Parameter parameter : parameters) {
                String paramname = parameter.getName();
                if (TYPE_KEY.equalsIgnoreCase(paramname)) {
                    FileType t = new FileType();
                    t.setValue(parameter.getValue());
                    setType(t);
                } else {
                    setError("Invalid parameter " + paramname);
                }
            }
        }
    }

    /**
     * Checks to make sure all settings are kosher. In this case, it
     * means that the pattern attribute has been set.
     *
     */
    @Override
    public void verifySettings() {
        if (type == null) {
            setError("The type attribute is required");
        }
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
    @Override
    public boolean isSelected(File basedir, String filename, File file) {

        // throw BuildException on error
        validate();

        if (file.isDirectory()) {
            return type.equals(FileType.DIR);
        }
        return type.equals(FileType.FILE);
    }

    /**
     * Enumerated attribute with the values for types of file
     */
    public static class FileType extends EnumeratedAttribute {
        /** the string value for file */
        public static final String FILE = "file";
        /** the string value for dir */
        public static final String DIR = "dir";

        /**
         * @return the values as an array of strings
         */
        @Override
        public String[] getValues() {
            return new String[]{FILE, DIR};
        }
    }

}
