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
    
    public void setJarfile(String jarFilename) {
	super.setZipfile(jarFilename);
	super.archiveType = "jar";
    }
    
    public void setManifest(String manifestFilename) {
	manifest = project.resolveFile(manifestFilename);
    }

    protected void initZipOutputStream(ZipOutputStream zOut)
	throws IOException, BuildException
    {
	// add manifest first
	if (manifest != null) {
            super.zipDir(new File(manifest.getParent()), zOut, "META-INF/");
	    super.zipFile(manifest, zOut, "META-INF/MANIFEST.MF");
	} else {
            /*
             * We don't store directories at all and this one will cause a lot
             * of problems with STORED Zip-Mode.
             *
             * That's why i've removed it -- Stefan Bodewig
             */
            //            ZipEntry ze = new ZipEntry("META-INF/");
            //            zOut.putNextEntry(ze);
	    String s = "/org/apache/tools/ant/defaultManifest.mf";
	    InputStream in = this.getClass().getResourceAsStream(s);
            if ( in == null )
		throw new BuildException ( "Could not find: " + s );
	    zipFile(in, zOut, "META-INF/MANIFEST.MF", System.currentTimeMillis());
 	}
     }

    protected void zipDir(File dir, ZipOutputStream zOut, String vPath)
        throws IOException
    {
        // First add directory to zip entry
        if(!vPath.equals("META-INF/")) {
            // we already added a META-INF
            super.zipDir(dir, zOut, vPath);
        }
    }

    protected void zipFile(File file, ZipOutputStream zOut, String vPath)
        throws IOException
    {
        // We already added a META-INF/MANIFEST.MF
        if (!vPath.equals("META-INF/MANIFEST.MF")) {
            super.zipFile(file, zOut, vPath);
        }
    }
}
