/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2001-2002 The Apache Software Foundation.  All rights
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
package org.apache.tools.ant.types.optional.depend;

import java.io.File;
import java.util.Vector;
import java.util.Enumeration;
import java.util.Hashtable;
import org.apache.tools.ant.util.depend.DependencyAnalyzer;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.types.Path;


/**
 * An interface used to describe the actions required by any type of 
 * directory scanner.
 *
 * @author Conor MacNeill
 * @author <a href="mailto:hengels@innovidata.com">Holger Engels</a>
 */
public class DependScanner extends DirectoryScanner {
    /**
     * The name of the analyzer to use by default.
     */
    public static final String DEFAULT_ANALYZER_CLASS
        = "org.apache.tools.ant.util.depend.bcel.FullAnalyzer";

    /**
     * The base directory for the scan
     */
    private File basedir;
    
    /**
     * The root classes to drive the search for dependent classes
     */
    private Vector rootClasses;
    
    /**
     * The names of the classes to include in the fileset
     */
    private Vector included;

    /**
     * The parent scanner which gives the basic set of files. Only files which 
     * are in this set and which can be reached from a root class will end
     * up being included in the result set
     */
    private DirectoryScanner parentScanner;
    
    /**
     * Create a DependScanner, using the given scanner to provide the basic 
     * set of files from which class files come.
     *
     * @param parentScanner the DirectoryScanner which returns the files from 
     *        which class files must come.
     */
    public DependScanner(DirectoryScanner parentScanner) {
        this.parentScanner = parentScanner;
    }
    
    /**
     * Sets the basedir for scanning. This is the directory that is scanned
     * recursively.
     *
     * @param basedir the basedir for scanning
     */
    public void setBasedir(File basedir) {
        this.basedir = basedir;
    }

    /**
     * Gets the basedir that is used for scanning.
     *
     * @return the basedir that is used for scanning
     */
    public File getBasedir() { return basedir; }

    /**
     * Sets the root classes to be used to drive the scan.
     *
     * @param rootClasses the rootClasses to be used for this scan
     */
    public void setRootClasses(Vector rootClasses) {
        this.rootClasses = rootClasses;
    }

    /**
     * Get the names of the class files, baseClass depends on
     *
     * @return the names of the files
     */
    public String[] getIncludedFiles() {
        int count = included.size();
        String[] files = new String[count];
        for (int i = 0; i < count; i++) {
            files[i] = (String) included.elementAt(i); 
        }
        return files;
    }

    /**
     * Scans the base directory for files that baseClass depends on
     *
     * @exception IllegalStateException when basedir was set incorrecly
     */
    public void scan() throws IllegalStateException {
        included = new Vector();
        String analyzerClassName = DEFAULT_ANALYZER_CLASS;
        DependencyAnalyzer analyzer = null;
        try {
            Class analyzerClass = Class.forName(analyzerClassName);
            analyzer = (DependencyAnalyzer) analyzerClass.newInstance();
        } catch (Exception e) {
            throw new BuildException("Unable to load dependency analyzer: " 
                + analyzerClassName, e);
        }
        analyzer.addClassPath(new Path(null, basedir.getPath()));
        
        for (Enumeration e = rootClasses.elements(); e.hasMoreElements();) {
            String rootClass = (String) e.nextElement();
            analyzer.addRootClass(rootClass);
        }

        Enumeration e = analyzer.getClassDependencies();

        String[] parentFiles = parentScanner.getIncludedFiles();
        Hashtable parentSet = new Hashtable();
        for (int i = 0; i < parentFiles.length; ++i) {
            parentSet.put(parentFiles[i], parentFiles[i]);
        }

        while (e.hasMoreElements()) {
            String classname = (String) e.nextElement();
            String filename = classname.replace('.', File.separatorChar);
            filename = filename + ".class";
            File depFile = new File(basedir, filename);
            if (depFile.exists() && parentSet.containsKey(filename)) {
                // This is included
                included.addElement(filename);
            }
        }
    }

    /**
     * @see DirectoryScanner#addDefaultExcludes
     */
    public void addDefaultExcludes() {
    }
    
    /**
     * @see DirectoryScanner#getExcludedDirectories
     */
    public String[] getExcludedDirectories() { 
        return null; 
    }
    
    /**
     * @see DirectoryScanner#getExcludedFiles
     */
    public String[] getExcludedFiles() { 
        return null; 
    }
    
    /**
     * @see DirectoryScanner#getIncludedDirectories
     */
    public String[] getIncludedDirectories() { 
        return new String[0]; 
    }
    
    /**
     * @see DirectoryScanner#getNotIncludedDirectories
     */
    public String[] getNotIncludedDirectories() { 
        return null; 
    }
    
    /**
     * @see DirectoryScanner#getNotIncludedFiles
     */
    public String[] getNotIncludedFiles() { 
        return null; 
    }

    /**
     * @see DirectoryScanner#setExcludes
     */
    public void setExcludes(String[] excludes) {
    }
    
    /**
     * @see DirectoryScanner#setIncludes
     */
    public void setIncludes(String[] includes) {
    }
    
    /**
     * @see DirectoryScanner#setCaseSensitive
     */
    public void setCaseSensitive(boolean isCaseSensitive) {
    }
}
