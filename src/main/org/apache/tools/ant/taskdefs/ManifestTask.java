/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
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

package org.apache.tools.ant.taskdefs;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.Enumeration;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.taskdefs.Manifest.Attribute;
import org.apache.tools.ant.types.EnumeratedAttribute;
import org.apache.tools.ant.util.FileUtils;

/**
 * Creates a manifest file for inclusion in a JAR, Ant task wrapper
 * around {@link Manifest Manifest}.  This task can be used to write a
 * Manifest file, optionally replacing or updating an existing file.
 *
 * @since Ant 1.5
 *
 * @ant.task category="java"
 */
public class ManifestTask extends Task {

    /**
     * Holds the real data.
     */
    private Manifest nestedManifest = new Manifest();

    /**
     * The file to which the manifest should be written when used as a task
     */
    private File manifestFile;

    /**
     * The mode with which the manifest file is written
     */
    private Mode mode;

    /**
     * The encoding of the manifest file
     */
    private String encoding;

    /**
     * Helper class for Manifest's mode attribute.
     */
    public static class Mode extends EnumeratedAttribute {
        /**
         * Get Allowed values for the mode attribute.
         *
         * @return a String array of the allowed values.
         */
        public String[] getValues() {
            return new String[] {"update", "replace"};
        }
    }

    /**
     * Default constructor
     */
    public ManifestTask() {
        mode = new Mode();
        mode.setValue("replace");
    }

    /**
     * Add a section to the manifest
     *
     * @param section the manifest section to be added
     *
     * @exception ManifestException if the section is not valid.
     */
    public void addConfiguredSection(Manifest.Section section)
         throws ManifestException {
        Enumeration attributeKeys = section.getAttributeKeys();
        while (attributeKeys.hasMoreElements()) {
            Attribute attribute = section.getAttribute((String)attributeKeys.nextElement());
            checkAttribute(attribute);
        }
        nestedManifest.addConfiguredSection(section);
    }

    /**
     * Add an attribute to the manifest - it is added to the main section.
     *
     * @param attribute the attribute to be added.
     *
     * @exception ManifestException if the attribute is not valid.
     */
    public void addConfiguredAttribute(Manifest.Attribute attribute)
         throws ManifestException {
        checkAttribute(attribute);
        nestedManifest.addConfiguredAttribute(attribute);
    }

    private void checkAttribute(Manifest.Attribute attribute) throws ManifestException {
        /*
         * Jar-Specification "Name-Value pairs and Sections":
         *   name:       alphanum *headerchar 
         *   alphanum:   {A-Z} | {a-z} | {0-9} 
         *   headerchar: alphanum | - | _
         * 
         * So the resulting regexp would be [A-Za-z0-9][A-Za-z0-9-_]*
         */
        String namePattern = "[A-Za-z0-9][A-Za-z0-9-_]*";

        String name = attribute.getName();

        /* FIXME Does not work for me :-( 
        RegexpMatcherFactory factory = new RegexpMatcherFactory();
        RegexpMatcher regexpMatcher = factory.newRegexpMatcher(getProject());
        regexpMatcher.setPattern(namePattern);
        if (!regexpMatcher.matches(name)) {
            throw new ManifestException(
                  "Attribute name is not valid according to the specification. "
                + "(which means regexp: " + namePattern + ")" 
            );
        }
        */
        
        /* Works, but not JDK 1.2 compliant 
        if (!name.matches(namePattern)) {
            throw new ManifestException("Attribute name is not valid according to the specification.");
        }
        */
        /* */
        if (attribute.getName().indexOf(' ') >- 1) {
            throw new ManifestException("Manifest attribute name must not contain spaces.");
        }
        /* */
    }

    /**
     * The name of the manifest file to create/update.
     * Required if used as a task.
     * @param f the Manifest file to be written
     */
    public void setFile(File f) {
        manifestFile = f;
    }

    /**
     * The encoding to use for reading in an existing manifest file
     * @param encoding the manifest file encoding.
     */
    public void setEncoding(String encoding) {
        this.encoding = encoding;
    }

    /**
     * Update policy: either "update" or "replace"; default is "replace".
     * @param m the mode value - update or replace.
     */
    public void setMode(Mode m) {
        mode = m;
    }

    /**
     * Create or update the Manifest when used as a task.
     *
     * @throws BuildException if the manifest cannot be written.
     */
    public void execute() throws BuildException {
        if (manifestFile == null) {
            throw new BuildException("the file attribute is required");
        }

        Manifest toWrite = Manifest.getDefaultManifest();
        Manifest current = null;
        BuildException error = null;

        if (manifestFile.exists()) {
            FileInputStream fis = null;
            InputStreamReader isr = null;
            try {
                fis = new FileInputStream(manifestFile);
                if (encoding == null) {
                    isr = new InputStreamReader(fis, "UTF-8");
                } else {
                    isr = new InputStreamReader(fis, encoding);
                }
                current = new Manifest(isr);
            } catch (ManifestException m) {
                error = new BuildException("Existing manifest " + manifestFile
                                           + " is invalid", m, getLocation());
            } catch (IOException e) {
                error = new BuildException("Failed to read " + manifestFile,
                                           e, getLocation());
            } finally {
                FileUtils.close(isr);
            }
        }

        //look for and print warnings
        for (Enumeration e = nestedManifest.getWarnings();
                e.hasMoreElements();) {
            log("Manifest warning: " + (String) e.nextElement(),
                    Project.MSG_WARN);
        }
        try {
            if (mode.getValue().equals("update") && manifestFile.exists()) {
                if (current != null) {
                    toWrite.merge(current);
                } else if (error != null) {
                    throw error;
                }
            }

            toWrite.merge(nestedManifest);
        } catch (ManifestException m) {
            throw new BuildException("Manifest is invalid", m, getLocation());
        }

        if (toWrite.equals(current)) {
            log("Manifest has not changed, do not recreate",
                Project.MSG_VERBOSE);
            return;
        }

        PrintWriter w = null;
        try {
            FileOutputStream fos = new FileOutputStream(manifestFile);
            OutputStreamWriter osw = new OutputStreamWriter(fos, Manifest.JAR_ENCODING);
            w = new PrintWriter(osw);
            toWrite.write(w);
        } catch (IOException e) {
            throw new BuildException("Failed to write " + manifestFile,
                                     e, getLocation());
        } finally {
            if (w != null) {
                w.close();
            }
        }
    }

}
