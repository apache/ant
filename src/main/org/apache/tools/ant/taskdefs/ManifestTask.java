/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2002-2003 The Apache Software Foundation.  All rights
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

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.types.EnumeratedAttribute;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * Creates a manifest file for inclusion in a JAR, Ant task wrapper
 * around {@link Manifest Manifest}.  This task can be used to write a
 * Manifest file, optionally replacing or updating an existing file.
 *
 * @author Conor MacNeill
 * @author <a href="mailto:stefan.bodewig@epost.de">Stefan Bodewig</a>
 * @author <a href="mailto:j_a_fernandez@yahoo.com">Jose Alberto Fernandez</a>
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

    public ManifestTask() {
        mode = new Mode();
        mode.setValue("replace");
    }

    /**
     * Add a section to the manifest
     *
     * @param section the manifest section to be added
     *
     * @exception ManifestException if the secti0on is not valid.
     */
    public void addConfiguredSection(Manifest.Section section)
         throws ManifestException {
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
        nestedManifest.addConfiguredAttribute(attribute);
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
     * @param encoding the maniofets file encoding.
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
                                           + " is invalid", m, location);
            } catch (IOException e) {
                error = new BuildException("Failed to read " + manifestFile,
                                           e, location);
            } finally {
                if (isr != null) {
                    try {
                        isr.close();
                    } catch (IOException e) {}
                }
            }
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
            throw new BuildException("Manifest is invalid", m, location);
        }

        if (toWrite.equals(current)) {
            log("Manifest has not changed, do not recreate",
                Project.MSG_VERBOSE);
            return;
        }

        PrintWriter w = null;
        try {
            FileOutputStream fos = new FileOutputStream(manifestFile);
            OutputStreamWriter osw = new OutputStreamWriter(fos, "UTF-8");
            w = new PrintWriter(osw);
            toWrite.write(w);
        } catch (IOException e) {
            throw new BuildException("Failed to write " + manifestFile,
                                     e, location);
        } finally {
            if (w != null) {
                w.close();
            }
        }
    }

}
