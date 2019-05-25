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

package org.apache.tools.ant.types;

import java.util.Vector;

import org.apache.tools.ant.BuildException;

/**
 * Wrapper for environment variables.
 *
 */
public class Environment {
    // CheckStyle:VisibilityModifier OFF - bc

    /**
     * a vector of type Environment.Variable
     * @see Variable
     */
    protected Vector<Variable> variables;

    // CheckStyle:VisibilityModifier ON

    /**
     * representation of a single env value
     */
    public static class Variable {

        /**
         * env key and value pair; everything gets expanded to a string
         * during assignment
         */
        private String key, value;

        /**
         * Constructor for variable
         *
         */
        public Variable() {
            super();
        }

        /**
         * set the key
         * @param key string
         */
        public void setKey(String key) {
            this.key = key;
        }

        /**
         * set the value
         * @param value string value
         */
        public void setValue(String value) {
            this.value = value;
        }

        /**
         * key accessor
         * @return key
         */
        public String getKey() {
            return this.key;
        }

        /**
         * value accessor
         * @return value
         */
        public String getValue() {
            return this.value;
        }

        /**
         * stringify path and assign to the value.
         * The value will contain all path elements separated by the appropriate
         * separator
         * @param path path
         */
        public void setPath(Path path) {
            this.value = path.toString();
        }

        /**
         * get the absolute path of a file and assign it to the value
         * @param file file to use as the value
         */
        public void setFile(java.io.File file) {
            this.value = file.getAbsolutePath();
        }

        /**
         * get the assignment string
         * This is not ready for insertion into a property file without following
         * the escaping rules of the properties class.
         * @return a string of the form key=value.
         * @throws BuildException if key or value are unassigned
         */
        public String getContent() throws BuildException {
            validate();
            return key.trim() + "=" + value.trim();
        }

        /**
         * checks whether all required attributes have been specified.
         * @throws BuildException if key or value are unassigned
         */
        public void validate() {
            if (key == null || value == null) {
                throw new BuildException(
                    "key and value must be specified for environment variables.");
            }
        }
    }

    /**
     * constructor
     */
    public Environment() {
        variables = new Vector<>();
    }

    /**
     * add a variable.
     * Validity checking is <i>not</i> performed at this point. Duplicates
     * are not caught either.
     * @param var new variable.
     */
    public void addVariable(Variable var) {
        variables.addElement(var);
    }

    /**
     * get the variable list as an array
     * @return array of key=value assignment strings
     * @throws BuildException if any variable is misconfigured
     */
    public String[] getVariables() throws BuildException {
        if (variables.isEmpty()) {
            return null;
        }
        return variables.stream().map(Variable::getContent).toArray(String[]::new);
    }

    /**
     * Get the raw vector of variables. This is not a clone.
     * @return a potentially empty (but never null) vector of elements of type
     * Variable
     * @since Ant 1.7
     */
    public Vector<Variable> getVariablesVector() {
        return variables;
    }
}
