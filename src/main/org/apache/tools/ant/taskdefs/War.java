/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2000 The Apache Software Foundation.  All rights 
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
import org.apache.tools.ant.types.FileSet;

import java.io.*;
import java.util.Vector;
import java.util.zip.*;

/**
 * Creates a WAR archive.
 * 
 * @author <a href="mailto:stefan.bodewig@megabit.net">Stefan Bodewig</a> 
 */
public class War extends Jar {

    private File deploymentDescriptor;

    private Vector libFileSets = new Vector();
    private Vector classesFileSets = new Vector();
    private Vector webInfFileSets = new Vector();

    public War() {
        super();
	archiveType = "war";
        emptyBehavior = "create";
    }

    public void setWarfile(File warFile) {
	super.setZipfile(warFile);
    }
    
    public void setWebxml(File descr) {
        deploymentDescriptor = descr;
    }

    public void addLib(FileSet fs) {
        libFileSets.addElement(fs);
    }

    public void addClasses(FileSet fs) {
        classesFileSets.addElement(fs);
    }

    public void addWebinf(FileSet fs) {
        webInfFileSets.addElement(fs);
    }

    /**
     * Add the deployment descriptor as well as all files added the
     * special way of nested lib, classes or webinf filesets.  
     */
    protected void initZipOutputStream(ZipOutputStream zOut)
	throws IOException, BuildException
    {
	// add deployment descriptor first
	if (deploymentDescriptor != null) {
            zipDir(new File(deploymentDescriptor.getParent()), zOut, 
                   "WEB-INF/");
	    super.zipFile(deploymentDescriptor, zOut, "WEB-INF/web.xml");
	} else {
            throw new BuildException("webxml attribute is required", location);
 	}

        addFiles(libFileSets, zOut, "WEB-INF/lib/");
        addFiles(classesFileSets, zOut, "WEB-INF/classes/");
        addFiles(webInfFileSets, zOut, "WEB-INF/");

        super.initZipOutputStream(zOut);
     }

    protected boolean isUpToDate(FileScanner[] scanners, File zipFile) throws BuildException
    {
        if (deploymentDescriptor == null) {
            throw new BuildException("webxml attribute is required", location);
        }
        
        // just add some Scanners for our filesets and web.xml and let
        // Jar/Zip do the rest of the work

        FileScanner[] myScanners = new FileScanner[scanners.length
                                                  + 1 // web.xml
                                                  + libFileSets.size()
                                                  + classesFileSets.size()
                                                  + webInfFileSets.size()];

        System.arraycopy(scanners, 0, myScanners, 0, scanners.length);

        DirectoryScanner ds = new DirectoryScanner();
        ds.setBasedir(new File(deploymentDescriptor.getParent()));
        ds.setIncludes(new String[] {deploymentDescriptor.getName()});
        ds.scan();
        myScanners[scanners.length] = ds;
        
        addScanners(myScanners, scanners.length+1, libFileSets);
        addScanners(myScanners, scanners.length+1+libFileSets.size(), 
                    classesFileSets);
        addScanners(myScanners, scanners.length+1+libFileSets.size()+classesFileSets.size(), 
                    webInfFileSets);

        return super.isUpToDate(myScanners, zipFile);
    }

    protected void zipFile(File file, ZipOutputStream zOut, String vPath)
        throws IOException
    {
        // We already added a WEB-INF/web.xml
        if (!vPath.equalsIgnoreCase("WEB-INF/web.xml")) {
            super.zipFile(file, zOut, vPath);
        } else {
            log("Warning: selected "+archiveType+" files include a WEB-INF/web.xml which will be ignored " +
                "(please use webxml attribute to "+archiveType+" task)", Project.MSG_WARN);
        }
    }

    /**
     * Add a DirectoryScanner for each FileSet included in fileSets to scanners
     * starting with index startIndex.
     */
    protected void addScanners(FileScanner[] scanners, int startIndex, 
                               Vector fileSets) {
        for (int i=0; i<fileSets.size(); i++) {
            FileSet fs = (FileSet) fileSets.elementAt(i);
            scanners[startIndex+i] = fs.getDirectoryScanner(project);
        }
    }

    /**
     * Iterate over the given Vector of filesets and add all files to the
     * ZipOutputStream using the given prefix.
     */
    protected void addFiles(Vector v, ZipOutputStream zOut, String prefix)
        throws IOException {
        for (int i=0; i<v.size(); i++) {
            FileSet fs = (FileSet) v.elementAt(i);
            DirectoryScanner ds = fs.getDirectoryScanner(project);
            addFiles(ds, zOut, prefix);
        }
    }
}
