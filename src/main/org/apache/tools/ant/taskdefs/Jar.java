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
import org.apache.tools.ant.types.ZipFileSet;
import org.apache.tools.zip.*;

import java.io.*;
import java.util.Enumeration;

/**
 * Creates a JAR archive.
 * 
 * @author James Davidson <a href="mailto:duncan@x180.com">duncan@x180.com</a>
 */
public class Jar extends Zip {

    private File manifestFile;
    private Manifest manifest;
    private Manifest execManifest;  
    
    /** true if a manifest has been specified in the task */
    private boolean buildFileManifest = false;
    
    public Jar() {
        super();
        archiveType = "jar";
        emptyBehavior = "create";
        setEncoding("UTF8");
    }

    public void setJarfile(File jarFile) {
        super.setZipfile(jarFile);
    }

    public void addConfiguredManifest(Manifest newManifest) throws ManifestException {
        if (manifest == null) {
            manifest = getDefaultManifest();
        }
        manifest.merge(newManifest);
        buildFileManifest = true;
    }
    
    public void setManifest(File manifestFile) {
        if (!manifestFile.exists()) {
            throw new BuildException("Manifest file: " + manifestFile + " does not exist.", 
                                     getLocation());
        }

        this.manifestFile = manifestFile;
        
        InputStream is = null;
        try {
            is = new FileInputStream(manifestFile);
            Manifest newManifest = new Manifest(is);
            if (manifest == null) {
                manifest = getDefaultManifest();
            }
            manifest.merge(newManifest);
        }
        catch (ManifestException e) {
            log("Manifest is invalid: " + e.getMessage(), Project.MSG_ERR);
            throw new BuildException("Invalid Manifest: " + manifestFile, e, getLocation());
        }
        catch (IOException e) {
            throw new BuildException("Unable to read manifest file: " + manifestFile, e);
        }
        finally {
            if (is != null) {
                try {
                    is.close();
                }
                catch (IOException e) {
                    // do nothing
                }
            }
        }
    }

    public void addMetainf(ZipFileSet fs) {
        // We just set the prefix for this fileset, and pass it up.
        fs.setPrefix("META-INF/");
        super.addFileset(fs);
    }

    protected void initZipOutputStream(ZipOutputStream zOut)
        throws IOException, BuildException
    {
        try {
            execManifest = getDefaultManifest();

            if (manifest != null) {
                execManifest.merge(manifest);
            }
            for (Enumeration e = execManifest.getWarnings(); e.hasMoreElements(); ) {
                log("Manifest warning: " + (String)e.nextElement(), Project.MSG_WARN);
            }
        
            zipDir(null, zOut, "META-INF/");
            // time to write the manifest
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            PrintWriter writer = new PrintWriter(baos);
            execManifest.write(writer);
            writer.flush();
        
            ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
            super.zipFile(bais, zOut, "META-INF/MANIFEST.MF", System.currentTimeMillis());
            super.initZipOutputStream(zOut);
        }
        catch (ManifestException e) {
            log("Manifest is invalid: " + e.getMessage(), Project.MSG_ERR);
            throw new BuildException("Invalid Manifest", e, getLocation());
        }
    }
        
    private Manifest getDefaultManifest() {
        try {
            String s = "/org/apache/tools/ant/defaultManifest.mf";
            InputStream in = this.getClass().getResourceAsStream(s);
            if (in == null) {
                throw new BuildException("Could not find default manifest: " + s);
            }
            return new Manifest(in);
        }
        catch (ManifestException e) {
            throw new BuildException("Default manifest is invalid !!");
        }
        catch (IOException e) {
            throw new BuildException("Unable to read default manifest", e);
        }
    }   
    
    /**
     * Handle situation when we encounter a manifest file
     *
     * If we haven't been given one, we use this one.
     *
     * If we have, we merge the manifest in, provided it is a new file
     * and not the old one from the JAR we are updating
     */
    private void zipManifestEntry(InputStream is) throws IOException {
        try {
            if (execManifest == null) {
                execManifest = new Manifest(is);
            }
            else if (isAddingNewFiles()) {
                execManifest.merge(new Manifest(is));
            }
        }
        catch (ManifestException e) {
            log("Manifest is invalid: " + e.getMessage(), Project.MSG_ERR);
            throw new BuildException("Invalid Manifest", e, getLocation());
        }
    }
    
    protected void zipFile(File file, ZipOutputStream zOut, String vPath)
        throws IOException
    {
        // If the file being added is META-INF/MANIFEST.MF, we warn if it's not the
        // one specified in the "manifest" attribute - or if it's being added twice, 
        // meaning the same file is specified by the "manifeset" attribute and in
        // a <fileset> element.
        if (vPath.equalsIgnoreCase("META-INF/MANIFEST.MF"))  {
            log("Warning: selected "+archiveType+" files include a META-INF/MANIFEST.MF which will be ignored " +
                "(please use manifest attribute to "+archiveType+" task)", Project.MSG_WARN);
        } else {
            super.zipFile(file, zOut, vPath);
        }

    }

    protected void zipFile(InputStream is, ZipOutputStream zOut, String vPath, long lastModified)
        throws IOException
    {
        // If the file being added is META-INF/MANIFEST.MF, we merge it with the
        // current manifest 
        if (vPath.equalsIgnoreCase("META-INF/MANIFEST.MF"))  {
            try {
                zipManifestEntry(is);
            }
            catch (IOException e) {
                throw new BuildException("Unable to read manifest file: ", e);
            }
        } else {
            super.zipFile(is, zOut, vPath, lastModified);
        }
    }

    /**
     * Check whether the archive is up-to-date; 
     * @param scanners list of prepared scanners containing files to archive
     * @param zipFile intended archive file (may or may not exist)
     * @return true if nothing need be done (may have done something already); false if
     *         archive creation should proceed
     * @exception BuildException if it likes
     */
    protected boolean isUpToDate(FileScanner[] scanners, File zipFile) throws BuildException {
        // need to handle manifest as a special check
        if (buildFileManifest || manifestFile == null) {
            java.util.zip.ZipFile theZipFile = null;
            try {
                theZipFile = new java.util.zip.ZipFile(zipFile);
                java.util.zip.ZipEntry entry = theZipFile.getEntry("META-INF/MANIFEST.MF");
                if (entry == null) {
                    log("Updating jar since the current jar has no manifest", Project.MSG_VERBOSE);
                    return false;
                }
                Manifest currentManifest = new Manifest(theZipFile.getInputStream(entry));
                if (manifest == null) {
                    manifest = getDefaultManifest();
                }
                if (!currentManifest.equals(manifest)) {
                    log("Updating jar since jar manifest has changed", Project.MSG_VERBOSE);
                    return false;
                }
            }
            catch (Exception e) {
                // any problems and we will rebuild
                log("Updating jar since cannot read current jar manifest: " + e.getClass().getName() + e.getMessage(), 
                    Project.MSG_VERBOSE);
                return false;
            }
            finally {
                if (theZipFile != null) {
                    try {
                        theZipFile.close();
                    }
                    catch (IOException e) {
                        //ignore
                    }
                }
            }
        }
        else if (manifestFile.lastModified() > zipFile.lastModified()) {
            return false;
        }
        return super.isUpToDate(scanners, zipFile);
    }
        
    protected boolean createEmptyZip(File zipFile) {
        // Jar files always contain a manifest and can never be empty        
        return false;
    }
    
    /**
     * Make sure we don't think we already have a MANIFEST next time this task
     * gets executed.
     */
    protected void cleanUp() {
        super.cleanUp();
    }
}
