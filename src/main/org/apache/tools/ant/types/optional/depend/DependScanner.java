/* 
 * Copyright  2001-2002,2004 Apache Software Foundation
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 * 
 */
package org.apache.tools.ant.types.optional.depend;

import java.io.File;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.types.Path;
import org.apache.tools.ant.util.depend.DependencyAnalyzer;


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
