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
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;

/**
 * Sets a property to the base name of a specified file, optionally minus a
 * suffix.
 *
 * This task can accept the following attributes:
 * <ul>
 * <li>file
 * <li>property
 * <li>suffix
 * </ul>
 * The <b>file</b> and <b>property</b> attributes are required. The
 * <b>suffix</b> attribute can be specified either with or without
 * the &quot;.&quot;, and the result will be the same (ie., the
 * returned file name will be minus the .suffix).
 * <p>
 * When this task executes, it will set the specified property to the
 * value of the last element in the specified file. If file is a
 * directory, the basename will be the last directory element. If file
 * is a full-path filename, the basename will be the simple file name.
 * If a suffix is specified, and the specified file ends in that suffix,
 * the basename will be the simple file name without the suffix.
 *
 * @author Diane Holt <a href="mailto:holtdl@apache.org">holtdl@apache.org</a>
 *
 * @version $Revision$
 *
 * @since Ant 1.5
 *
 * @ant.task category="property"
 */

public class Basename extends Task {
    private File file;
    private String property;
    private String suffix;

    /**
    * File or directory to get base name from.
    */
    public void setFile(File file) {
        this.file = file;
    }

    /**
    * Property to set base name to.
    */
    public void setProperty(String property) {
        this.property  = property;
    }

    /**
    * Optional suffix to remove from base name.
    */
    public void setSuffix(String suffix) {
        this.suffix = suffix;
    }


    // The method executing the task
    public void execute() throws BuildException {
        if (property == null) {
            throw new BuildException("property attribute required", getLocation());
        }
        if (file == null) {
            throw new BuildException("file attribute required", getLocation());
        }
        String value = file.getName();
        if (suffix != null && value.endsWith(suffix)) {
            // if the suffix does not starts with a '.' and the
            // char preceding the suffix is a '.', we assume the user
            // wants to remove the '.' as well (see docs)
            int pos = value.length() - suffix.length();
            if (pos > 0 && suffix.charAt(0) != '.'
                && value.charAt(pos - 1) == '.') {
                pos--;
            }
            value = value.substring(0, pos);
        }
        getProject().setNewProperty(property, value);
    }
}

