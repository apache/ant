/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 1999 The Apache Software Foundation.  All rights 
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
 * 4. The names "The Jakarta Project", "Ant", and "Apache Software
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

import org.apache.tools.ant.*;
import org.apache.tools.ant.types.*;

import java.io.*;
import java.util.*;

/**
 * Chmod equivalent for unix-like environments.
 *
 * @author costin@eng.sun.com
 * @author Mariusz Nowostawski (Marni) <a href="mailto:mnowostawski@infoscience.otago.ac.nz">mnowostawski@infoscience.otago.ac.nz</a>
 * @author <a href="mailto:stefan.bodewig@epost.de">Stefan Bodewig</a>
 */

public class Chmod extends ExecuteOn {

    private FileSet defaultSet = new FileSet();
    private boolean defaultSetDefined = false;
    private boolean havePerm = false;
    
    public Chmod() {
        super.setExecutable("chmod");
        super.setParallel(true);
        super.setSkipEmptyFilesets(true);
    }

    public void setFile(File src) {
        FileSet fs = new FileSet();
        fs.setDir(new File(src.getParent()));
        fs.createInclude().setName(src.getName());
        addFileset(fs);
    }

    public void setDir(File src) {
        defaultSet.setDir(src);
    }

    public void setPerm(String perm) {
        createArg().setValue(perm);
        havePerm = true;
    }

    /**
     * add a name entry on the include list
     */
    public PatternSet.NameEntry createInclude() {
        defaultSetDefined = true;
        return defaultSet.createInclude();
    }
    
    /**
     * add a name entry on the exclude list
     */
    public PatternSet.NameEntry createExclude() {
        defaultSetDefined = true;
        return defaultSet.createExclude();
    }

    /**
     * add a set of patterns
     */
    public PatternSet createPatternSet() {
        defaultSetDefined = true;
        return defaultSet.createPatternSet();
    }

    /**
     * Sets the set of include patterns. Patterns may be separated by a comma
     * or a space.
     *
     * @param includes the string containing the include patterns
     */
    public void setIncludes(String includes) {
        defaultSetDefined = true;
        defaultSet.setIncludes(includes);
    }

    /**
     * Sets the set of exclude patterns. Patterns may be separated by a comma
     * or a space.
     *
     * @param excludes the string containing the exclude patterns
     */
    public void setExcludes(String excludes) {
        defaultSetDefined = true;
        defaultSet.setExcludes(excludes);
    }

    /**
     * Sets whether default exclusions should be used or not.
     *
     * @param useDefaultExcludes "true"|"on"|"yes" when default exclusions 
     *                           should be used, "false"|"off"|"no" when they
     *                           shouldn't be used.
     */
    public void setDefaultexcludes(boolean useDefaultExcludes) {
        defaultSetDefined = true;
        defaultSet.setDefaultexcludes(useDefaultExcludes);
    }
    
    protected void checkConfiguration() {
        if (!havePerm) {
            throw new BuildException("Required attribute perm not set in chmod", 
                                     location);
        }
        
        if (defaultSetDefined && defaultSet.getDir(project) != null) {
            addFileset(defaultSet);
        }
        super.checkConfiguration();
    }

    public void execute() throws BuildException {
        if (defaultSetDefined || defaultSet.getDir(project) == null) {
            super.execute();
        }
        else if (isValidOs()) {
            // we are chmodding the given directory
            createArg().setValue(defaultSet.getDir(project).getPath());
            Execute execute = prepareExec();
            try {
                execute.setCommandline(cmdl.getCommandline());
                runExecute(execute);
            } catch (IOException e) {
                throw new BuildException("Execute failed: " + e, e, location);
            } finally {
                // close the output file if required
                logFlush();
            }
        }
    }
    

    public void setExecutable(String e) {
        throw new BuildException(taskType+" doesn\'t support the executable attribute", location);
    }

    public void setCommand(String e) {
        throw new BuildException(taskType+" doesn\'t support the command attribute", location);
    }

    public void setSkipEmptyFilesets(boolean skip) {
        throw new BuildException(taskType+" doesn\'t support the skipemptyfileset attribute", location);
    }

    protected boolean isValidOs() {
        // XXX if OS=unix
        return System.getProperty("path.separator").equals(":") 
            && (!System.getProperty("os.name").startsWith("Mac") 
                 || System.getProperty("os.name").endsWith("X"))
            && super.isValidOs();
    }
}
