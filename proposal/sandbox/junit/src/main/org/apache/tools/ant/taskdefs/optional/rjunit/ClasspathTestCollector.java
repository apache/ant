/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2002 The Apache Software Foundation.  All rights
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
package org.apache.tools.ant.taskdefs.optional.rjunit;

import java.io.File;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;

import junit.runner.TestCollector;

import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.ProjectComponent;
import org.apache.tools.ant.types.Path;
import org.apache.tools.ant.types.PatternSet;

/**
 * A rough implementation of a test collector that will collect tests
 * using include/exclude patterns in a set of paths. A path can either
 * be a directory or an archive. (zip or jar file)
 *
 */
public final class ClasspathTestCollector extends ProjectComponent
        implements TestCollector {

    private final static int SUFFIX_LENGTH = ".class".length();

    private final PatternSet patterns = new PatternSet();

    private Path path = null;

    public Enumeration collectTests() {
        Hashtable collected = new Hashtable();
        // start from last, so that first elements
        // override last one in case there are duplicates.
        // ie mimic classpath behavior.
        String[] paths = path.list();
        for (int i = paths.length - 1; i >= 0; i--) {
            File f = new File(paths[i]);
            ArrayList included = null;
            if (f.isDirectory()) {
                included = gatherFromDirectory(f);
            } else if (f.getName().endsWith(".zip")
                    || f.getName().endsWith(".jar")) {
                included = gatherFromArchive(f);
            } else {
                continue;
            }
            // add tests to the already collected one
            final int includedCount = included.size();
            log("Adding " + includedCount + " testcases from " + f, Project.MSG_VERBOSE);
            for (int j = 0; j < includedCount; j++) {
                String testname = (String) included.get(j);
                collected.put(testname, "");
            }
        }
        log("Collected " + collected.size() + " testcases from " + paths.length + " path(s).", Project.MSG_VERBOSE);
        return collected.keys();
    }


    /**
     * Return the list of classnames from a directory that match
     * the specified patterns.
     * @param dir the base directory (must also be the base package)
     * @return the list of classnames matching the pattern.
     */
    protected ArrayList gatherFromDirectory(File dir) {
        Project project = getProject();
        DirectoryScanner ds = new DirectoryScanner();
        ds.setBasedir(dir);
        ds.setIncludes(patterns.getIncludePatterns(project));
        ds.setExcludes(patterns.getExcludePatterns(project));
        ds.scan();
        String[] included = ds.getIncludedFiles();
        return testClassNameFromFile(included);
    }

    /**
     * Return the list of classnames from a zipfile that match
     * the specified patterns.
     * @param zip the zipfile (must also be the base package)
     * @return the list of classnames matching the pattern.
     */
    protected ArrayList gatherFromArchive(File zip) {
        ZipScanner zs = new ZipScanner();
        zs.setBasedir(zip);
        zs.setIncludes(patterns.getIncludePatterns(project));
        zs.setExcludes(patterns.getExcludePatterns(project));
        zs.scan();
        String[] included = zs.getIncludedFiles();
        return testClassNameFromFile(included);
    }

    /**
     * transform a set of file into their matching classname
     * @todo what about using a mapper for this ?
     */
    protected ArrayList testClassNameFromFile(String[] classFileNames) {
        ArrayList tests = new ArrayList(classFileNames.length);
        for (int i = 0; i < classFileNames.length; i++) {
            String file = classFileNames[i];
            if (isTestClass(file)) {
                String classname = classNameFromFile(file);
                tests.add(classname);
            }
        }
        return tests;
    }

    protected boolean isTestClass(String classFileName) {
        return classFileName.endsWith(".class");
    }

    protected String classNameFromFile(String classFileName) {
        // convert /a/b.class to a.b
        String s = classFileName.substring(0, classFileName.length() - SUFFIX_LENGTH);
        String s2 = s.replace(File.separatorChar, '.');
        if (s2.startsWith(".")) {
            s2 = s2.substring(1);
        }
        return s2;
    }

// Ant bean accessors

    public void setPath(Path path) {
        this.path = path;
    }

    public Path getPath() {
        return this.path;
    }

    public PatternSet.NameEntry createInclude() {
        return patterns.createInclude();
    }

    public PatternSet.NameEntry createExclude() {
        return patterns.createExclude();
    }

}
