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
 * 4. The names "The Jakarta Project", "Tomcat", and "Apache Software
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

import java.io.*;
import java.util.*;

/**
 *
 *
 * @author duncan@x180.com
 */

public class Copydir extends Task {

    private File srcDir;
    private File destDir;
    private String[] includes;
    private String[] excludes;
    private boolean useDefaultExcludes = true;
    private Hashtable filecopyList = new Hashtable();

    public void setSrc(String src) {
	srcDir = project.resolveFile(src);
    }

    public void setDest(String dest) {
	destDir = project.resolveFile(dest);
    }
    
    /**
     * Sets the set of include patterns. Patterns may be separated by a comma
     * or a space.
     *
     * @param includes the string containing the include patterns
     */
    public void setIncludes(String includes) {
        if (includes != null && includes.length() > 0) {
            Vector tmpIncludes = new Vector();
            StringTokenizer tok = new StringTokenizer(includes, ", ", false);
            while (tok.hasMoreTokens()) {
                String pattern = tok.nextToken().trim();
                if (pattern.length() > 0) {
                    tmpIncludes.addElement(pattern);
                }
            }
            this.includes = new String[tmpIncludes.size()];
            for (int i = 0; i < tmpIncludes.size(); i++) {
                this.includes[i] = (String)tmpIncludes.elementAt(i);
            }
        } else {
            this.includes = null;
        }
    }

    /**
     * Sets the set of exclude patterns. Patterns may be separated by a comma
     * or a space.
     *
     * @param excludes the string containing the exclude patterns
     */
    public void setExcludes(String excludes) {
        if (excludes != null && excludes.length() > 0) {
            Vector tmpExcludes = new Vector();
            StringTokenizer tok = new StringTokenizer(excludes, ", ", false);
            while (tok.hasMoreTokens()) {
                String pattern = tok.nextToken().trim();
                if (pattern.length() > 0) {
                    tmpExcludes.addElement(pattern);
                }
            }
            this.excludes = new String[tmpExcludes.size()];
            for (int i = 0; i < tmpExcludes.size(); i++) {
                this.excludes[i] = (String)tmpExcludes.elementAt(i);
            }
        } else {
            this.excludes = null;
        }
    }

    /**
     * Sets whether default exclusions should be used or not.
     *
     * @param useDefaultExcludes "true" or "on" when default exclusions should
     *                           be used, "false" or "off" when they
     *                           shouldn't be used.
     */
    public void setDefaultexcludes(String useDefaultExcludes) {
        this.useDefaultExcludes = Project.toBoolean(useDefaultExcludes);
    }

    public void execute() throws BuildException {
        if (srcDir == null) {
            throw new BuildException("srcdir attribute must be set!");
        }
        if (!srcDir.exists()) {
            throw new BuildException("srcdir does not exist!");
        }
        DirectoryScanner ds = new DirectoryScanner();
        ds.setBasedir(srcDir);
        ds.setIncludes(includes);
        ds.setExcludes(excludes);
        if (useDefaultExcludes) {
            ds.addDefaultExcludes();
        }
        ds.scan();
        
        String[] files = ds.getIncludedFiles();
        scanDir(srcDir, destDir, files);
	if (filecopyList.size() > 0) {
	    project.log("Copying " + filecopyList.size() + " files to "
			+ destDir.getAbsolutePath());
	    Enumeration enum = filecopyList.keys();
	    while (enum.hasMoreElements()) {
		String fromFile = (String)enum.nextElement();
		String toFile = (String)filecopyList.get(fromFile);
		try {
		    copyFile(fromFile, toFile);
		} catch (IOException ioe) {
		    String msg = "Failed to copy " + fromFile + " to " + toFile
			+ " due to " + ioe.getMessage();
		    throw new BuildException(msg);
		}
	    }
	}
    }

    /**
        List of filenames and directory names to not 
        include in the final .jar file. They should be either 
        , or " " (space) separated.
        <p>
        For example:
        <p>
        ignore="package.html, foo.class"
        <p>
        The ignored files will be logged.
        
        @author Jon S. Stevens <a href="mailto:jon@clearink.com">jon@clearink.com</a>
    */
    public void setIgnore(String ignoreString) {
        project.log("The ignore attribute is deprecated. "+
                    "Please use the excludes attribute.",
                    Project.MSG_WARN);
        if (ignoreString != null && ignoreString.length() > 0) {
            Vector tmpExcludes = new Vector();
            StringTokenizer tok = new StringTokenizer(ignoreString, ", ", false);
            while (tok.hasMoreTokens()) {
                tmpExcludes.addElement("**/"+tok.nextToken().trim()+"/**");
            }
            this.excludes = new String[tmpExcludes.size()];
            for (int i = 0; i < tmpExcludes.size(); i++) {
                this.excludes[i] = (String)tmpExcludes.elementAt(i);
            }
        } else {
            this.excludes = null;
        }
    }

    private void scanDir(File from, File to, String[] files) {
        for (int i = 0; i < files.length; i++) {
            String filename = files[i];
            File srcFile = new File(from, filename);
            File destFile = new File(to, filename);
            if (srcFile.lastModified() > destFile.lastModified()) {
                filecopyList.put(srcFile.getAbsolutePath(),
                                 destFile.getAbsolutePath());
            }
        }
    }
}
