/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2002 The Apache Software Foundation.  All rights
 * reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution, if
 *    any, must include the following acknowlegement:
 *       "This product includes software developed by the
 *        Apache Software Foundation (http://www.apache.org/)."
 *    Alternately, this acknowlegement may appear in the software itself,
 *    if and wherever such third-party acknowlegements normally appear.
 *
 * 4. The names "Ant" and "Apache Software
 *    Foundation" must not be used to endorse or promote products derived
 *    from this software without prior written permission. For written
 *    permission, please contact apache@apache.org.
 *
 * 5. Products derived from this software may not be called "Apache"
 *    nor may "Apache" appear in their names without prior written
 *    permission of the Apache Group.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE APACHE SOFTWARE FOUNDATION OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Apache Software Foundation.  For more
 * information on the Apache Software Foundation, please see
 * <http://www.apache.org/>.
 */
package org.apache.tools.ant.taskdefs;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
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
 * @author <a href="mailto:peter@apache.org">Peter Donald</a>
 * @version $Revision$ $Date$
 * @since Ant 1.5
 * @ant.task name="buildnumber"
 */
public class BuildNumber
     extends Task {
    /**
     * The name of the property in which the build number is stored.
     */
    private static final String DEFAULT_PROPERTY_NAME = "build.number";

    /** The default filename to use if no file specified.  */
    private static final String DEFAULT_FILENAME = DEFAULT_PROPERTY_NAME;

    /** The File in which the build number is stored.  */
    private File m_file;


    /**
     * The file in which the build number is stored. Defaults to
     * "build.number" if not specified.
     *
     * @param file the file in which build number is stored.
     */
    public void setFile(final File file) {
        m_file = file;
    }


    /**
     * Run task.
     *
     * @exception BuildException if an error occurs
     */
    public void execute()
         throws BuildException {
        File savedFile = m_file; // may be altered in validate

        validate();

        final Properties properties = loadProperties();
        final int buildNumber = getBuildNumber(properties);

        properties.put(DEFAULT_PROPERTY_NAME,
            String.valueOf(buildNumber + 1));

        // Write the properties file back out
        FileOutputStream output = null;

        try {
            output = new FileOutputStream(m_file);

            final String header = "Build Number for ANT. Do not edit!";

            properties.save(output, header);
        } catch (final IOException ioe) {
            final String message = "Error while writing " + m_file;

            throw new BuildException(message, ioe);
        } finally {
            if (null != output) {
                try {
                    output.close();
                } catch (final IOException ioe) {
                }
            }
            m_file = savedFile;
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
            final String message =
                m_file + " contains a non integer build number: " + buildNumber;

            throw new BuildException(message, nfe);
        }
    }


    /**
     * Utility method to load properties from file.
     *
     * @return the loaded properties
     * @throws BuildException
     */
    private Properties loadProperties()
         throws BuildException {
        FileInputStream input = null;

        try {
            final Properties properties = new Properties();

            input = new FileInputStream(m_file);
            properties.load(input);
            return properties;
        } catch (final IOException ioe) {
            throw new BuildException(ioe);
        } finally {
            if (null != input) {
                try {
                    input.close();
                } catch (final IOException ioe) {
                }
            }
        }
    }


    /**
     * Validate that the task parameters are valid.
     *
     * @throws BuildException if parameters are invalid
     */
    private void validate()
         throws BuildException {
        if (null == m_file) {
            m_file = getProject().resolveFile(DEFAULT_FILENAME);
        }

        if (!m_file.exists()) {
            try {
                FileUtils.newFileUtils().createNewFile(m_file);
            } catch (final IOException ioe) {
                final String message =
                    m_file + " doesn't exist and new file can't be created.";

                throw new BuildException(message, ioe);
            }
        }

        if (!m_file.canRead()) {
            final String message = "Unable to read from " + m_file + ".";

            throw new BuildException(message);
        }

        if (!m_file.canWrite()) {
            final String message = "Unable to write to " + m_file + ".";

            throw new BuildException(message);
        }
    }
}

