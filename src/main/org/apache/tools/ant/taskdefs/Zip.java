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
import java.util.Enumeration;
import java.util.StringTokenizer;
import java.util.Vector;
import java.util.zip.*;

/**
 * Same as the Jar task, but creates .zip files without the MANIFEST 
 * stuff that .jar files have.
 *
 * @author James Davidson <a href="mailto:duncan@x180.com">duncan@x180.com</a>
 * @author Jon S. Stevens <a href="mailto:jon@clearink.com">jon@clearink.com</a>
 */

public class Zip extends Task {

    private File zipFile;
    private File baseDir;
    private String[] includes;
    private String[] excludes;
    private boolean useDefaultExcludes = true;
    private File manifest;    
    protected String archiveType = "zip";
    
    /**
        This is the name/location of where to 
        create the .zip file.
    */
    public void setZipfile(String zipFilename) {
        zipFile = project.resolveFile(zipFilename);
    }
    /**
        This is the base directory to look in for 
        things to zip.
    */
    public void setBasedir(String baseDirname) {
        baseDir = project.resolveFile(baseDirname);
    }

    /**
        Set this to be the items in the base directory 
        that you want to include in the zip archive. 
        (ie: items="foo, bar, ack.html, f.java").
        You can also specify "*" for the items (ie: items="*") 
        and it will include all the items in the base directory.
        Do not try to have items="*, foo". Also note that 
        you can specify items to ignore with setIgnore and they 
        will still be ignored if you choose "*". Sometimes 
        ignore lists are easier than include lists. ;-)
    */
    public void setItems(String itemString) {
        project.log("The items attribute is deprecated. "+
                    "Please use the includes attribute.",
                    Project.MSG_WARN);
        if (itemString == null || itemString.equals("*")) {
            includes = new String[1];
            includes[0] = "**";
        } else {
            Vector tmpIncludes = new Vector();
            StringTokenizer tok = new StringTokenizer(itemString, ", ");
            while (tok.hasMoreTokens()) {
                String pattern = tok.nextToken().trim();
                if (pattern.length() > 0) {
                    tmpIncludes.addElement(pattern+"/**");
                }
            }
            this.includes = new String[tmpIncludes.size()];
            for (int i = 0; i < tmpIncludes.size(); i++) {
                this.includes[i] = (String)tmpIncludes.elementAt(i);
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
        if (ignoreString == null) {
            this.excludes = null;
        } else {
            Vector tmpExcludes = new Vector();
            StringTokenizer tok = new StringTokenizer(ignoreString, ", ");
            while (tok.hasMoreTokens()) {
                String pattern = tok.nextToken().trim();
                if (pattern.length() > 0) {
                    tmpExcludes.addElement("**/"+pattern+"/**");
                }
            }
            this.excludes = new String[tmpExcludes.size()];
            for (int i = 0; i < tmpExcludes.size(); i++) {
                this.excludes[i] = (String)tmpExcludes.elementAt(i);
            }
        }
    }
    
    /**
     * Sets the set of include patterns. Patterns may be separated by a comma
     * or a space.
     *
     * @param includes the string containing the include patterns
     */
    public void setIncludes(String includes) {
        if (includes == null) {
            this.includes = null;
        } else {
            Vector tmpIncludes = new Vector();
            StringTokenizer tok = new StringTokenizer(includes, ", ");
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
        }
    }

    /**
     * Sets the set of exclude patterns. Patterns may be separated by a comma
     * or a space.
     *
     * @param excludes the string containing the exclude patterns
     */
    public void setExcludes(String excludes) {
        if (excludes == null) {
            this.excludes = null;
        } else {
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
        project.log("Building "+ archiveType +": "+ zipFile.getAbsolutePath());

        if (baseDir == null) {
            throw new BuildException("basedir attribute must be set!");
        }
        if (!baseDir.exists()) {
            throw new BuildException("basedir does not exist!");
        }

        DirectoryScanner ds = new DirectoryScanner();
        ds.setBasedir(baseDir);
        ds.setIncludes(includes);
        ds.setExcludes(excludes);
        if (useDefaultExcludes) {
            ds.addDefaultExcludes();
        }
        ds.scan();

        String[] files = ds.getIncludedFiles();
        String[] dirs  = ds.getIncludedDirectories();

        try {
            ZipOutputStream zOut = new ZipOutputStream(new FileOutputStream(zipFile));
            initZipOutputStream(zOut);

            for (int i = 0; i < dirs.length; i++) {
                File f = new File(baseDir,dirs[i]);
                String name = dirs[i].replace(File.separatorChar,'/')+"/";
                zipDir(f, zOut, name);
            }

            for (int i = 0; i < files.length; i++) {
                File f = new File(baseDir,files[i]);
                String name = files[i].replace(File.separatorChar,'/');
                zipFile(f, zOut, name);
            }

            // close up
            zOut.close();
        } catch (IOException ioe) {
            String msg = "Problem creating " + archiveType + " " + ioe.getMessage();
            throw new BuildException(msg);
        }
    }

    protected void initZipOutputStream(ZipOutputStream zOut)
        throws IOException, BuildException
    {
        zOut.setMethod(ZipOutputStream.DEFLATED);
    }

    protected void zipDir(File dir, ZipOutputStream zOut, String vPath)
        throws IOException
    {
    }

    protected void zipFile(InputStream in, ZipOutputStream zOut, String vPath)
        throws IOException
    {
        ZipEntry ze = new ZipEntry(vPath);
        zOut.putNextEntry(ze);

        byte[] buffer = new byte[8 * 1024];
        int count = 0;
        do {
            zOut.write(buffer, 0, count);
            count = in.read(buffer, 0, buffer.length);
        } while (count != -1);
    }

    protected void zipFile(File file, ZipOutputStream zOut, String vPath)
        throws IOException
    {
        FileInputStream fIn = new FileInputStream(file);
        zipFile(fIn, zOut, vPath);
        fIn.close();
    }
}
