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

package org.apache.tools.ant.taskdefs.optional.windows;

import java.io.File;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.taskdefs.ExecuteOn;
import org.apache.tools.ant.taskdefs.condition.Os;
import org.apache.tools.ant.types.FileSet;

/**
 * Attrib equivalent for Win32 environments.
 * Note: Attrib parameters /S and /D are not handled.
 *
 * @since Ant 1.6
 */
public class Attrib extends ExecuteOn {

    private static final String ATTR_READONLY = "R";
    private static final String ATTR_ARCHIVE  = "A";
    private static final String ATTR_SYSTEM   = "S";
    private static final String ATTR_HIDDEN   = "H";
    private static final String SET    = "+";
    private static final String UNSET  = "-";

    private boolean haveAttr = false;

    /** Constructor for Attrib. */
    public Attrib() {
        super.setExecutable("attrib");
        super.setParallel(false);
    }

    /**
     * A file to be attribed.
     * @param src a file
     */
    public void setFile(File src) {
        FileSet fs = new FileSet();
        fs.setFile(src);
        addFileset(fs);
    }

    /**
     * Set the ReadOnly file attribute.
     * @param value a <code>boolean</code> value
     */
    public void setReadonly(boolean value) {
        addArg(value, ATTR_READONLY);
    }

    /**
     * Set the Archive file attribute.
     * @param value a <code>boolean</code> value
     */
    public void setArchive(boolean value) {
        addArg(value, ATTR_ARCHIVE);
    }

    /**
     * Set the System file attribute.
     * @param value a <code>boolean</code> value
     */
    public void setSystem(boolean value) {
        addArg(value, ATTR_SYSTEM);
    }

    /**
     * Set the Hidden file attribute.
     * @param value a <code>boolean</code> value
     */
    public void setHidden(boolean value) {
        addArg(value, ATTR_HIDDEN);
    }

    /**
     * Check the attributes.
     */
    @Override
    protected void checkConfiguration() {
        if (!haveAttr()) {
            throw new BuildException("Missing attribute parameter",
                                     getLocation());
        }
        super.checkConfiguration();
    }

    /**
     * Set the executable.
     * This is not allowed, and it always throws a BuildException.
     * @param e ignored
     * @ant.attribute ignore="true"
     */
    @Override
    public void setExecutable(String e) {
        throw new BuildException(getTaskType()
            + " doesn't support the executable attribute", getLocation());
    }

    /**
     * Set the executable.
     * This is not allowed, and it always throws a BuildException.
     * @param e ignored
     * @ant.attribute ignore="true"
     */
    public void setCommand(String e) {
        throw new BuildException(getTaskType()
            + " doesn't support the command attribute", getLocation());
    }

    /**
     * Add source file.
     * This is not allowed, and it always throws a BuildException.
     * @param b ignored
     * @ant.attribute ignore="true"
     */
    @Override
    public void setAddsourcefile(boolean b) {
        throw new BuildException(getTaskType()
            + " doesn't support the addsourcefile attribute", getLocation());
    }

    /**
     * Set skip empty file sets.
     * This is not allowed, and it always throws a BuildException.
     * @param skip ignored
     * @ant.attribute ignore="true"
     */
    @Override
    public void setSkipEmptyFilesets(boolean skip) {
        throw new BuildException(getTaskType() + " doesn't support the "
                                 + "skipemptyfileset attribute",
                                 getLocation());
    }

    /**
     * Set parallel.
     * This is not allowed, and it always throws a BuildException.
     * @param parallel ignored
     * @ant.attribute ignore="true"
     */
    @Override
    public void setParallel(boolean parallel) {
        throw new BuildException(getTaskType()
                                 + " doesn't support the parallel attribute",
                                 getLocation());
    }

    /**
     * Set max parallel.
     * This is not allowed, and it always throws a BuildException.
     * @param max ignored
     * @ant.attribute ignore="true"
     */
    @Override
    public void setMaxParallel(int max) {
        throw new BuildException(getTaskType()
                                 + " doesn't support the maxparallel attribute",
                                 getLocation());
    }

    /**
     * Check if the os is valid.
     * Default is to allow windows
     * @return true if the os is valid.
     */
    @Override
    protected boolean isValidOs() {
        return getOs() == null && getOsFamily() == null
            ? Os.isFamily(Os.FAMILY_WINDOWS) : super.isValidOs();
    }

    private static String getSignString(boolean attr) {
        return attr ? SET : UNSET;
    }

    private void addArg(boolean sign, String attribute) {
        createArg().setValue(getSignString(sign) + attribute);
        haveAttr = true;
    }

    private boolean haveAttr() {
        return haveAttr;
    }

}
