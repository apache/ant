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

import java.io.*;
import java.util.*;
import org.apache.tools.ant.*;
import org.apache.tools.ant.util.*;
import org.apache.tools.tar.*;
import org.apache.tools.ant.types.*;

/**
 * Creates a TAR archive.
 *
 * @author Stefano Mazzocchi <a href="mailto:stefano@apache.org">stefano@apache.org</a>
 * @author <a href="mailto:stefan.bodewig@epost.de">Stefan Bodewig</a>
 */

public class Tar extends MatchingTask {

    static public final String TRUNCATE = "truncate";
    static public final String GNU = "gnu";

    File tarFile;
    File baseDir;
    
    String longFileMode = null;
    
    Vector filesets = new Vector();
    Vector fileSetFiles = new Vector();

    public TarFileSet createTarFileSet() {
        TarFileSet fileset = new TarFileSet();
        filesets.addElement(fileset);
        return fileset;
    }
    
    
    /**
     * This is the name/location of where to create the tar file.
     */
    public void setTarfile(File tarFile) {
        this.tarFile = tarFile;
    }
    
    /**
     * This is the base directory to look in for things to tar.
     */
    public void setBasedir(File baseDir) {
        this.baseDir = baseDir;
    }
    
    /**
     * Set how to handle long files.
     *
     * Allowable values are
     *   truncate
     *   gnu
     */
    public void setLongfile(String method) {
        this.longFileMode = method;
    }

    public void execute() throws BuildException {
        if (tarFile == null) {
            throw new BuildException("tarfile attribute must be set!", 
                                     location);
        }

        if (tarFile.exists() && tarFile.isDirectory()) {
            throw new BuildException("tarfile is a directory!", 
                                     location);
        }

        if (tarFile.exists() && !tarFile.canWrite()) {
            throw new BuildException("Can not write to the specified tarfile!", 
                                     location);
        }

        if (baseDir != null) {
            if (!baseDir.exists()) {
                throw new BuildException("basedir does not exist!", location);
            }
            
            // add the main fileset to the list of filesets to process.
            TarFileSet mainFileSet = new TarFileSet(fileset);
            mainFileSet.setDir(baseDir);
            mainFileSet.setDefaultexcludes(useDefaultExcludes);
            filesets.addElement(mainFileSet);
        }

        // check if tr is out of date with respect to each
        // fileset
        boolean upToDate = true;
        for (Enumeration e = filesets.elements(); e.hasMoreElements();) {
            TarFileSet fs = (TarFileSet)e.nextElement();
            String[] files = fs.getFiles(project);
            
            if (!archiveIsUpToDate(files)) {
                upToDate = false;
                break;
            }
        }

        if (upToDate) {
            log("Nothing to do: "+tarFile.getAbsolutePath()+" is up to date.",
                Project.MSG_INFO);
            return;
        }

        log("Building tar: "+ tarFile.getAbsolutePath(), Project.MSG_INFO);

        TarOutputStream tOut = null;
        try {
            tOut = new TarOutputStream(new FileOutputStream(tarFile));
            tOut.setDebug(true);
            if (longFileMode == null) {
                tOut.setLongFileMode(TarOutputStream.LONGFILE_ERROR);
            }
            else if (longFileMode.equalsIgnoreCase(TRUNCATE)) {
                tOut.setLongFileMode(TarOutputStream.LONGFILE_TRUNCATE);
            }
            else if (longFileMode.equalsIgnoreCase(GNU)) {
                tOut.setLongFileMode(TarOutputStream.LONGFILE_GNU);
            }
        
            for (Enumeration e = filesets.elements(); e.hasMoreElements();) {
                TarFileSet fs = (TarFileSet)e.nextElement();
                String[] files = fs.getFiles(project);
                for (int i = 0; i < files.length; i++) {
                    File f = new File(baseDir,files[i]);
                    String name = files[i].replace(File.separatorChar,'/');
                    tarFile(f, tOut, name, fs);
                }
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

    protected void tarFile(File file, TarOutputStream tOut, String vPath,
                           TarFileSet tarFileSet)
        throws IOException
    {
        FileInputStream fIn = new FileInputStream(file);

        try {
            TarEntry te = new TarEntry(vPath);
            te.setSize(file.length());
            te.setModTime(file.lastModified());
            if (!file.isDirectory()) {
                te.setMode(tarFileSet.getMode());
            }
            te.setUserName(tarFileSet.getUserName());
            te.setGroupName(tarFileSet.getGroup());
            
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

    protected boolean archiveIsUpToDate(String[] files) {
        SourceFileScanner sfs = new SourceFileScanner(this);
        MergingMapper mm = new MergingMapper();
        mm.setTo(tarFile.getAbsolutePath());
        return sfs.restrict(files, baseDir, null, mm).length == 0;
    }

    static public class TarFileSet extends FileSet {
        private String[] files = null;
        
        private int mode = 0100644;
        
        private String userName = "";
        private String groupName = "";
        
           
        public TarFileSet(FileSet fileset) {
            super(fileset);
        }
        
        public TarFileSet() {
            super();
        }
        
        public String[] getFiles(Project p) {
            if (files == null) {
                DirectoryScanner ds = getDirectoryScanner(p);
                files = ds.getIncludedFiles();
            }
            
            return files;
        }
        
        public void setMode(String octalString) {
            this.mode = 0100000 | Integer.parseInt(octalString, 8);
        }
            
        public int getMode() {
            return mode;
        }
        
        public void setUserName(String userName) {
            this.userName = userName;
        }
        
        public String getUserName() {
            return userName;
        }
        
        public void setGroup(String groupName) {
            this.groupName = groupName;
        }
        
        public String getGroup() {
            return groupName;
        }
        
    }
}
