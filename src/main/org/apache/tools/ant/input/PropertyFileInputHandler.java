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

package org.apache.tools.ant.input;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Properties;

import org.apache.tools.ant.BuildException;

/**
 * Reads input from a property file, the file name is read from the
 * system property ant.input.properties, the prompt is the key for input.
 *
 * @since Ant 1.5
 */
public class PropertyFileInputHandler implements InputHandler {
    private Properties props = null;

    /**
     * Name of the system property we expect to hold the file name.
     */
    public static final String FILE_NAME_KEY = "ant.input.properties";

    /**
     * Empty no-arg constructor.
     */
    public PropertyFileInputHandler() {
    }

    /**
     * Picks up the input from a property, using the prompt as the
     * name of the property.
     * @param request an input request.
     *
     * @exception BuildException if no property of that name can be found.
     */
    public void handleInput(InputRequest request) throws BuildException {
        readProps();

        Object o = props.get(request.getPrompt());
        if (o == null) {
            throw new BuildException("Unable to find input for '"
                                     + request.getPrompt() + "'");
        }
        request.setInput(o.toString());
        if (!request.isInputValid()) {
            throw new BuildException("Found invalid input " + o
                                     + " for '" + request.getPrompt() + "'");
        }
    }

    /**
     * Reads the properties file if it hasn't already been read.
     */
    private synchronized void readProps() throws BuildException {
        if (props == null) {
            String propsFile = System.getProperty(FILE_NAME_KEY);
            if (propsFile == null) {
                throw new BuildException("System property "
                                         + FILE_NAME_KEY
                                         + " for PropertyFileInputHandler not"
                                         + " set");
            }

            props = new Properties();

            try {
                props.load(Files.newInputStream(Paths.get(propsFile)));
            } catch (IOException e) {
                throw new BuildException("Couldn't load " + propsFile, e);
            }
        }
    }

}
