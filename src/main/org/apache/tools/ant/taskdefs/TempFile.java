/*
 *  The Apache Software License, Version 1.1
 *
 *  Copyright (c) 2002 The Apache Software Foundation.  All rights
 *  reserved.
 *
 *  Redistribution and use in source and binary forms, with or without
 *  modification, are permitted provided that the following conditions
 *  are met:
 *
 *  1. Redistributions of source code must retain the above copyright
 *  notice, this list of conditions and the following disclaimer.
 *
 *  2. Redistributions in binary form must reproduce the above copyright
 *  notice, this list of conditions and the following disclaimer in
 *  the documentation and/or other materials provided with the
 *  distribution.
 *
 *  3. The end-user documentation included with the redistribution, if
 *  any, must include the following acknowlegement:
 *  "This product includes software developed by the
 *  Apache Software Foundation (http://www.apache.org/)."
 *  Alternately, this acknowlegement may appear in the software itself,
 *  if and wherever such third-party acknowlegements normally appear.
 *
 *  4. The names "Ant" and "Apache Software
 *  Foundation" must not be used to endorse or promote products derived
 *  from this software without prior written permission. For written
 *  permission, please contact apache@apache.org.
 *
 *  5. Products derived from this software may not be called "Apache"
 *  nor may "Apache" appear in their names without prior written
 *  permission of the Apache Group.
 *
 *  THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 *  WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 *  OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 *  DISCLAIMED.  IN NO EVENT SHALL THE APACHE SOFTWARE FOUNDATION OR
 *  ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 *  SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 *  LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 *  USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 *  ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 *  OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 *  OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 *  SUCH DAMAGE.
 *  ====================================================================
 *
 *  This software consists of voluntary contributions made by many
 *  individuals on behalf of the Apache Software Foundation.  For more
 *  information on the Apache Software Foundation, please see
 *  <http://www.apache.org/>.
 */
package org.apache.tools.ant.taskdefs;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.util.FileUtils;

/**
 *  This task sets a property to  the name of a temporary file. 
 *  Unlike the Java1.2 method to create a temporary file, this task
 *  does work on Java1.1. Also, it does not actually create the
 *  temporary file, but it does guarantee that the file did not
 *  exist when the task was executed. 
 * <p>  
 * Examples
 * <pre>&lt;tempfile property="temp.file" /&gt;</pre>
 * create a temporary file
 * <pre>&lt;tempfile property="temp.file" suffix=".xml" /&gt;</pre>
 * create a temporary file with the .xml suffix.
 * <pre>&lt;tempfile property="temp.file" destDir="build"/&gt;</pre>
 * create a temp file in the build subdir
 *@author      steve loughran
 *@since       Ant 1.5
 *@ant.task
 */

public class TempFile extends Task {

    /**
     *  name of property to set
     */
    private String property;

    /**
     *  directory to create the file in. can be null
     */
    private File destDir = null;

    /**
     *  prefix for the file
     */
    private String prefix;

    /**
     *  suffix for the file
     */
    private String suffix = "";


    /**
     *  The property you wish to assign the temporary file to
     *
     *@param  property  The property to set
     */
    public void setProperty(String property) {
        this.property = property;
    }


    /**
     *  destination directory. If null, 
     the parent directory is used instead
     *
     *@param  destDir  The new destDir value
     */
    public void setDestDir(File destDir) {
        this.destDir = destDir;
    }


    /**
     *  optional prefix string
     *
     *@param  prefix  string to prepend to generated string
     */
    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }


    /**
     *  Suffix string for the temp file (optional)
     *
     *@param  suffix  suffix including any "." , e.g ".xml"
     */
    public void setSuffix(String suffix) {
        this.suffix = suffix;
    }


    /**
     *  create the temp file
     *
     *@exception  BuildException  if something goes wrong with the build
     */
    public void execute() throws BuildException {
        if (property == null || property.length() == 0) {
            throw new BuildException("no property specified");
        }
        if (destDir == null) {
            destDir = project.resolveFile(".");
        }
        FileUtils utils = FileUtils.newFileUtils();
        File tfile = utils.createTempFile(prefix, suffix, destDir);
        project.setNewProperty(property, tfile.toString());
    }
}
