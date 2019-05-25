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
package org.apache.tools.ant.taskdefs;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.Properties;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.util.FileUtils;

/**
 * Read, increment, and write a build number in a file
 * It will first
 * attempt to read a build number from a file, then set the property
 * "build.number" to the value that was read in (or 0 if no such value). Then
 * it will increment the build number by one and write it back out into the
 * file.
 *
 * @since Ant 1.5
 * @ant.task name="buildnumber"
 */
public class BuildNumber extends Task {
    /**
     * The name of the property in which the build number is stored.
     */
    private static final String DEFAULT_PROPERTY_NAME = "build.number";

    /** The default filename to use if no file specified.  */
    private static final String DEFAULT_FILENAME = DEFAULT_PROPERTY_NAME;

    private static final FileUtils FILE_UTILS = FileUtils.getFileUtils();

    /** The File in which the build number is stored.  */
    private File myFile;

    /**
     * The file in which the build number is stored. Defaults to
     * "build.number" if not specified.
     *
     * @param file the file in which build number is stored.
     */
    public void setFile(final File file) {
        myFile = file;
    }

    /**
     * Run task.
     *
     * @exception BuildException if an error occurs
     */
    @Override
    public void execute() throws BuildException {
        File savedFile = myFile; // may be altered in validate

        validate();

        final Properties properties = loadProperties();
        final int buildNumber = getBuildNumber(properties);

        properties.put(DEFAULT_PROPERTY_NAME,
            String.valueOf(buildNumber + 1));

        // Write the properties file back out

        try (OutputStream output = Files.newOutputStream(myFile.toPath())) {
            properties.store(output, "Build Number for ANT. Do not edit!");
        } catch (final IOException ioe) {
            throw new BuildException("Error while writing " + myFile, ioe);
        } finally {
            myFile = savedFile;
        }

        //Finally set the property
        getProject().setNewProperty(DEFAULT_PROPERTY_NAME,
            String.valueOf(buildNumber));
    }

    /**
     * Utility method to retrieve build number from properties object.
     *
     * @param properties the properties to retrieve build number from
     * @return the build number or if no number in properties object
     * @throws BuildException if build.number property is not an integer
     */
    private int getBuildNumber(final Properties properties)
         throws BuildException {
        final String buildNumber =
            properties.getProperty(DEFAULT_PROPERTY_NAME, "0").trim();

        // Try parsing the line into an integer.
        try {
            return Integer.parseInt(buildNumber);
        } catch (final NumberFormatException nfe) {
            throw new BuildException(
                myFile + " contains a non integer build number: " + buildNumber,
                nfe);
        }
    }

    /**
     * Utility method to load properties from file.
     *
     * @return the loaded properties
     * @throws BuildException if something goes wrong
     */
    private Properties loadProperties() throws BuildException {
        try (InputStream input = Files.newInputStream(myFile.toPath())) {
            final Properties properties = new Properties();
            properties.load(input);
            return properties;
        } catch (final IOException ioe) {
            throw new BuildException(ioe);
        }
    }

    /**
     * Validate that the task parameters are valid.
     *
     * @throws BuildException if parameters are invalid
     */
    private void validate()
         throws BuildException {
        if (null == myFile) {
            myFile = FILE_UTILS.resolveFile(getProject().getBaseDir(), DEFAULT_FILENAME);
        }

        if (!myFile.exists()) {
            try {
                FILE_UTILS.createNewFile(myFile);
            } catch (final IOException ioe) {
                throw new BuildException(
                    myFile + " doesn't exist and new file can't be created.",
                    ioe);
            }
        }

        if (!myFile.canRead()) {
            throw new BuildException("Unable to read from " + myFile + ".");
        }

        if (!myFile.canWrite()) {
            throw new BuildException("Unable to write to " + myFile + ".");
        }
    }
}
