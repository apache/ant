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

package org.apache.tools.ant.taskdefs.optional.windows;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.taskdefs.ExecuteOn;
import org.apache.tools.ant.taskdefs.condition.Os;
import org.apache.tools.ant.types.FileSet;

import java.io.File;

/**
 * Attrib equivalent for Win32 environments.
 * Note: Attrib parameters /S and /D are not handled.
 *
 * @author skanga@bigfoot.com
 * @author <a href="mailto:Jerome@jeromelacoste.com">Jerome Lacoste</a>
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

    public Attrib() {
        super.setExecutable("attrib");
        super.setParallel(false);
    }

    public void setFile(File src) {
        FileSet fs = new FileSet();
        fs.setFile(src);
        addFileset(fs);
    }

    /** set the ReadOnly file attribute */
    public void setReadonly(boolean value) {
        addArg(value, ATTR_READONLY);
    }

    /** set the Archive file attribute */
    public void setArchive(boolean value) {
        addArg(value, ATTR_ARCHIVE);
    }

    /** set the System file attribute */
    public void setSystem(boolean value) {
        addArg(value, ATTR_SYSTEM);
    }

    /** set the Hidden file attribute */
    public void setHidden(boolean value) {
        addArg(value, ATTR_HIDDEN);
    }

    protected void checkConfiguration() {
        if (!haveAttr()) {
            throw new BuildException("Missing attribute parameter",
                                     getLocation());
        }
        super.checkConfiguration();
    }

    /**
     * @ant.attribute ignore="true"
     */
    public void setExecutable(String e) {
        throw new BuildException(taskType
            + " doesn\'t support the executable attribute", getLocation());
    }

    /**
     * @ant.attribute ignore="true"
     */
    public void setCommand(String e) {
        throw new BuildException(taskType
            + " doesn\'t support the command attribute", getLocation());
    }

    /**
     * @ant.attribute ignore="true"
     */
    public void setAddsourcefile(boolean b) {
        throw new BuildException(getTaskType()
            + " doesn\'t support the addsourcefile attribute", getLocation());
    }

    /**
     * @ant.attribute ignore="true"
     */
    public void setSkipEmptyFilesets(boolean skip) {
        throw new BuildException(taskType + " doesn\'t support the "
                                 + "skipemptyfileset attribute",
                                 getLocation());
    }

    /**
     * @ant.attribute ignore="true"
     */
    public void setParallel(boolean parallel) {
        throw new BuildException(getTaskType()
                                 + " doesn\'t support the parallel attribute",
                                 getLocation());
    }

    /**
     * @ant.attribute ignore="true"
     */
    public void setMaxParallel(int max) {
        throw new BuildException(getTaskType()
                                 + " doesn\'t support the maxparallel attribute",
                                 getLocation());
    }

    protected boolean isValidOs() {
        return Os.isFamily("windows") && super.isValidOs();
    }

    private static String getSignString(boolean attr) {
        return (attr == true ? SET : UNSET);
    }

    private void addArg(boolean sign, String attribute) {
        createArg().setValue(getSignString(sign) + attribute);
        haveAttr = true;
    }

    private boolean haveAttr() {
        return haveAttr;
    }

}
