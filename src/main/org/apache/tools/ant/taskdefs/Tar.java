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

import java.io.*;
import org.apache.tools.ant.*;
import org.apache.tools.tar.*;

/**
 * Creates a TAR archive.
 *
 * @author Stefano Mazzocchi <a href="mailto:stefano@apache.org">stefano@apache.org</a>
 */

public class Tar extends MatchingTask {

    File tarFile;
    File baseDir;
    
    /**
     * This is the name/location of where to create the tar file.
     */
    public void setTarfile(String tarFilename) {
        tarFile = project.resolveFile(tarFilename);
    }
    
    /**
     * This is the base directory to look in for things to tar.
     */
    public void setBasedir(String baseDirname) {
        baseDir = project.resolveFile(baseDirname);
    }

    public void execute() throws BuildException {
        log("Building tar: "+ tarFile.getAbsolutePath());

        if (baseDir == null) {
            throw new BuildException("basedir attribute must be set!", 
                                     location);
        }
        if (!baseDir.exists()) {
            throw new BuildException("basedir does not exist!", location);
        }

        DirectoryScanner ds = super.getDirectoryScanner(baseDir);

        String[] files = ds.getIncludedFiles();

        TarOutputStream tOut = null;
        try {
            tOut = new TarOutputStream(new FileOutputStream(tarFile));
            tOut.setDebug(true);

            for (int i = 0; i < files.length; i++) {
                File f = new File(baseDir,files[i]);
                String name = files[i].replace(File.separatorChar,'/');
                tarFile(f, tOut, name);
            }
        } catch (IOException ioe) {
            String msg = "Problem creating TAR: " + ioe.getMessage();
            throw new BuildException(msg, ioe, location);
	} finally {
	    if (tOut != null) {
	        try {
                    // close up
	            tOut.close();
	        }
	        catch (IOException e) {}
	    }
        }
    }

    protected void tarFile(File file, TarOutputStream tOut, String vPath)
        throws IOException
    {
        FileInputStream fIn = new FileInputStream(file);

        try {
            TarEntry te = new TarEntry(vPath);
            te.setSize(file.length());
            te.setModTime(file.lastModified());
            tOut.putNextEntry(te);
            
            byte[] buffer = new byte[8 * 1024];
            int count = 0;
            do {
                tOut.write(buffer, 0, count);
                count = fIn.read(buffer, 0, buffer.length);
            } while (count != -1);
            
            tOut.closeEntry();        
        } finally {
            fIn.close();
        }
    }
}
