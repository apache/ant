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
import java.util.zip.*;

/**
 * Creates a JAR archive.
 * 
 * @author James Davidson <a href="mailto:duncan@x180.com">duncan@x180.com</a>
 */

public class Jar extends Zip {

    private File manifest;    
    
    public Jar() {
        super();
        archiveType = "jar";
        emptyBehavior = "create";
    }

    public void execute() {
        if (manifest != null && !manifest.exists())
            throw new BuildException("Manifest file: " + manifest + " does not exists.");
        super.execute();
    }

    public void setJarfile(File jarFile) {
        super.setZipfile(jarFile);
    }
    
    public void setManifest(File manifestFile) {
        manifest = manifestFile;
    }

    protected void initZipOutputStream(ZipOutputStream zOut)
        throws IOException, BuildException
    {
        // add manifest first
        if (manifest != null) {
            zipDir(new File(manifest.getParent()), zOut, "META-INF/");
            super.zipFile(manifest, zOut, "META-INF/MANIFEST.MF");
        } else {
            String s = "/org/apache/tools/ant/defaultManifest.mf";
            InputStream in = this.getClass().getResourceAsStream(s);
            if ( in == null )
                throw new BuildException ( "Could not find: " + s );
            zipDir(null, zOut, "META-INF/");
            zipFile(in, zOut, "META-INF/MANIFEST.MF", System.currentTimeMillis());
        }
    }

    protected boolean isUpToDate(FileScanner[] scanners, File zipFile) throws BuildException
    {
        File[] files = grabFiles(scanners);
        
        if (manifest != null) {
            // just add the manifest file to the mix

            DirectoryScanner ds = new DirectoryScanner();
            ds.setBasedir(new File(manifest.getParent()));
            ds.setIncludes(new String[] {manifest.getName()});
            ds.scan();

            FileScanner[] myScanners = new FileScanner[scanners.length+1];
            System.arraycopy(scanners, 0, myScanners, 0, scanners.length);
            myScanners[scanners.length] = ds;

            boolean retval = super.isUpToDate(myScanners, zipFile);
            if (!retval && files.length == 0) {
                log("Note: creating empty "+archiveType+" archive " + zipFile, 
                    Project.MSG_INFO);
            }
            return retval;

        } else if (emptyBehavior.equals("create") && files.length == 0) {

            log("Note: creating empty "+archiveType+" archive " + zipFile, 
                Project.MSG_INFO);
            return false;

        } else {
            // all other cases are handled correctly by Zip's method
            return super.isUpToDate(scanners, zipFile);
        }
    }

    protected void zipFile(File file, ZipOutputStream zOut, String vPath)
        throws IOException
    {
        // We already added a META-INF/MANIFEST.MF
        if (!vPath.equalsIgnoreCase("META-INF/MANIFEST.MF")) {
            super.zipFile(file, zOut, vPath);
        } else {
            log("Warning: selected "+archiveType+" files include a META-INF/MANIFEST.MF which will be ignored " +
                "(please use manifest attribute to "+archiveType+" task)", Project.MSG_WARN);
        }
    }

}
