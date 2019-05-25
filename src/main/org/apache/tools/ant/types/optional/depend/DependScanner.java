/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
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
import java.util.Set;
import java.util.Vector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.types.Path;
import org.apache.tools.ant.util.StreamUtils;
import org.apache.tools.ant.util.depend.DependencyAnalyzer;

/**
 * DirectoryScanner for finding class dependencies.
 */
public class DependScanner extends DirectoryScanner {
    /**
     * The name of the analyzer to use by default.
     */
    public static final String DEFAULT_ANALYZER_CLASS
        = "org.apache.tools.ant.util.depend.bcel.FullAnalyzer";

    /**
     * The root classes to drive the search for dependent classes.
     */
    private Vector<String> rootClasses;

    /**
     * The names of the classes to include in the fileset.
     */
    private Vector<String> included;

    private Vector<File> additionalBaseDirs = new Vector<>();

    /**
     * The parent scanner which gives the basic set of files. Only files which
     * are in this set and which can be reached from a root class will end
     * up being included in the result set.
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
     * Sets the root classes to be used to drive the scan.
     *
     * @param rootClasses the rootClasses to be used for this scan.
     */
    public synchronized void setRootClasses(Vector<String> rootClasses) {
        this.rootClasses = rootClasses;
    }

    /**
     * Get the names of the class files on which baseClass depends.
     *
     * @return the names of the files.
     */
    @Override
    public String[] getIncludedFiles() {
        return included.toArray(new String[getIncludedFilesCount()]);
    }

    /** {@inheritDoc}. */
    @Override
    public synchronized int getIncludedFilesCount() {
        if (included == null) {
            throw new IllegalStateException();
        }
        return included.size();
    }

    /**
     * Scans the base directory for files on which baseClass depends.
     *
     * @exception IllegalStateException when basedir was set incorrectly.
     */
    @Override
    public synchronized void scan() throws IllegalStateException {
        included = new Vector<>();
        String analyzerClassName = DEFAULT_ANALYZER_CLASS;
        DependencyAnalyzer analyzer;
        try {
            Class<? extends DependencyAnalyzer> analyzerClass =
                Class.forName(analyzerClassName)
                    .asSubclass(DependencyAnalyzer.class);
            analyzer = analyzerClass.newInstance();
        } catch (Exception e) {
            throw new BuildException("Unable to load dependency analyzer: "
                                     + analyzerClassName, e);
        }
        analyzer.addClassPath(new Path(null, basedir.getPath()));
        additionalBaseDirs.stream().map(File::getPath)
            .map(p -> new Path(null, p)).forEach(analyzer::addClassPath);

        rootClasses.forEach(analyzer::addRootClass);

        Set<String> parentSet = Stream.of(parentScanner.getIncludedFiles())
            .collect(Collectors.toSet());

        // This is included
        StreamUtils.enumerationAsStream(analyzer.getClassDependencies())
                .map(cName -> cName.replace('.', File.separatorChar) + ".class")
                .filter(fName -> new File(basedir, fName).exists() && parentSet.contains(fName))
                .forEach(fName -> included.addElement(fName));
    }

    /**
     * @see DirectoryScanner#addDefaultExcludes
     */
    @Override
    public void addDefaultExcludes() {
    }

    /**
     * @see DirectoryScanner#getExcludedDirectories
     * {@inheritDoc}.
     */
    @Override
    public String[] getExcludedDirectories() {
        return null;
    }

    /**
     * @see DirectoryScanner#getExcludedFiles
     * {@inheritDoc}.
     */
    @Override
    public String[] getExcludedFiles() {
        return null;
    }

    /**
     * @see DirectoryScanner#getIncludedDirectories
     * {@inheritDoc}.
     */
    @Override
    public String[] getIncludedDirectories() {
        return new String[0];
    }

    /**
     * @see DirectoryScanner#getIncludedDirsCount
     * {@inheritDoc}.
     */
    @Override
    public int getIncludedDirsCount() {
        return 0;
    }

    /**
     * @see DirectoryScanner#getNotIncludedDirectories
     * {@inheritDoc}.
     */
    @Override
    public String[] getNotIncludedDirectories() {
        return null;
    }

    /**
     * @see DirectoryScanner#getNotIncludedFiles
     * {@inheritDoc}.
     */
    @Override
    public String[] getNotIncludedFiles() {
        return null;
    }

    /**
     * @see DirectoryScanner#setExcludes
     * {@inheritDoc}.
     */
    @Override
    public void setExcludes(String[] excludes) {
    }

    /**
     * @see DirectoryScanner#setIncludes
     * {@inheritDoc}.
     */
    @Override
    public void setIncludes(String[] includes) {
    }

    /**
     * @see DirectoryScanner#setCaseSensitive
     * {@inheritDoc}.
     */
    @Override
    public void setCaseSensitive(boolean isCaseSensitive) {
    }

    public void addBasedir(File baseDir) {
        additionalBaseDirs.addElement(baseDir);
    }
}
