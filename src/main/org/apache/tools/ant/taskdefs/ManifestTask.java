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
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.nio.file.Files;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.types.EnumeratedAttribute;
import org.apache.tools.ant.util.StreamUtils;

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
     * Specifies the valid characters which can be used in attribute names.
     * {@value}
     */
    public static final String VALID_ATTRIBUTE_CHARS =
        "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789-_";

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
     * whether to merge Class-Path attributes.
     */
    private boolean mergeClassPaths = false;

    /**
     * whether to flatten Class-Path attributes into a single one.
     */
    private boolean flattenClassPaths = false;

    /**
     * Helper class for Manifest's mode attribute.
     */
    public static class Mode extends EnumeratedAttribute {
        /**
         * Get Allowed values for the mode attribute.
         *
         * @return a String array of the allowed values.
         */
        @Override
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
        StreamUtils.enumerationAsStream(section.getAttributeKeys())
                .map(section::getAttribute).forEach(this::checkAttribute);
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

    /**
     * Checks the attribute against the Jar-specification.
     *
     * Jar-Specification <i>"Name-Value pairs and Sections"</i>: <pre>
     *   name:       alphanum *headerchar
     *   alphanum:   {A-Z} | {a-z} | {0-9}
     *   headerchar: alphanum | - | _
     * </pre>
     * So the resulting regexp would be <code>[A-Za-z0-9][A-Za-z0-9-_]*</code>.
     *
     * Because of JDK 1.2 compliance and the possible absence of a
     * regexp matcher we can not use regexps here. Instead we have to
     * check each character.
     *
     * @param attribute The attribute to check
     * @throws BuildException if the check fails
     */
    private void checkAttribute(Manifest.Attribute attribute) throws BuildException {
        String name = attribute.getName();
        char ch = name.charAt(0);

        if (ch == '-' || ch == '_') {
            throw new BuildException(
                "Manifest attribute names must not start with '%c'.", ch);
        }

        for (int i = 0; i < name.length(); i++) {
            ch = name.charAt(i);
            if (VALID_ATTRIBUTE_CHARS.indexOf(ch) < 0) {
                throw new BuildException(
                    "Manifest attribute names must not contain '%c'", ch);
            }
        }
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
     * Whether to merge Class-Path attributes.
     *
     * @param b boolean
     * @since Ant 1.8.0
     */
    public void setMergeClassPathAttributes(boolean b) {
        mergeClassPaths = b;
    }

    /**
     * Whether to flatten multi-valued attributes (i.e. Class-Path)
     * into a single one.
     *
     * @param b boolean
     * @since Ant 1.8.0
     */
    public void setFlattenAttributes(boolean b) {
        flattenClassPaths = b;
    }

    /**
     * Create or update the Manifest when used as a task.
     *
     * @throws BuildException if the manifest cannot be written.
     */
    @Override
    public void execute() throws BuildException {
        if (manifestFile == null) {
            throw new BuildException("the file attribute is required");
        }

        Manifest toWrite = Manifest.getDefaultManifest();
        Manifest current = null;
        BuildException error = null;

        if (manifestFile.exists()) {
            Charset charset = Charset.forName(encoding == null ? "UTF-8" : encoding);
            try (InputStreamReader isr = new InputStreamReader(
                Files.newInputStream(manifestFile.toPath()), charset)) {
                current = new Manifest(isr);
            } catch (ManifestException m) {
                error = new BuildException("Existing manifest " + manifestFile
                                           + " is invalid", m, getLocation());
            } catch (IOException e) {
                error = new BuildException("Failed to read " + manifestFile,
                                           e, getLocation());
            }
        }

        // look for and print warnings
        StreamUtils.enumerationAsStream(nestedManifest.getWarnings())
                .forEach(e -> log("Manifest warning: " + e, Project.MSG_WARN));
        try {
            if ("update".equals(mode.getValue()) && manifestFile.exists()) {
                if (current != null) {
                    toWrite.merge(current, false, mergeClassPaths);
                } else if (error != null) {
                    throw error;
                }
            }

            toWrite.merge(nestedManifest, false, mergeClassPaths);
        } catch (ManifestException m) {
            throw new BuildException("Manifest is invalid", m, getLocation());
        }

        if (toWrite.equals(current)) {
            log("Manifest has not changed, do not recreate",
                Project.MSG_VERBOSE);
            return;
        }

        try (PrintWriter w = new PrintWriter(new OutputStreamWriter(
            Files.newOutputStream(manifestFile.toPath()), Manifest.JAR_CHARSET))) {
            toWrite.write(w, flattenClassPaths);
            if (w.checkError()) {
                throw new IOException("Encountered an error writing manifest");
            }
        } catch (IOException e) {
            throw new BuildException("Failed to write " + manifestFile,
                                     e, getLocation());
        }
    }
}
