/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2001 The Apache Software Foundation.  All rights
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
package org.apache.tools.ant.types.optional.depend;

import java.io.*;
import java.util.*;

import org.apache.tools.ant.util.depend.Dependencies;
import org.apache.tools.ant.util.depend.Filter;
import org.apache.tools.ant.DirectoryScanner;

import org.apache.bcel.classfile.*;
import org.apache.bcel.*;

/**
 * An interface used to describe the actions required by any type of 
 * directory scanner.
 */
public class DependScanner extends DirectoryScanner {
    File basedir;
    File baseClass;
    List included = new LinkedList();

    /**
     * Sets the basedir for scanning. This is the directory that is scanned
     * recursively. 
     *
     * @param basedir the (non-null) basedir for scanning
     */
    public void setBasedir(String basedir) {
        setBasedir(new File(basedir.replace('/',File.separatorChar).replace('\\',File.separatorChar)));
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
     * Sets the domain, where dependant classes are searched
     *
     * @param domain the domain
     */
    public void setBaseClass(File baseClass) {
        this.baseClass = baseClass;
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
            files[i] = included.get(i) + ".class";
            //System.err.println("  " + files[i]);
        }
        return files;
    }

    /**
     * Scans the base directory for files that baseClass depends on
     *
     * @exception IllegalStateException when basedir was set incorrecly
     */
    public void scan() {
        Dependencies visitor = new Dependencies();
        Set set = new TreeSet();
        Set newSet = new HashSet();
        final String base;
        String start;
        try {
            base = basedir.getCanonicalPath() + File.separator;
            start = baseClass.getCanonicalPath();
        }
        catch (Exception e) {
            throw new IllegalArgumentException(e.getMessage());
        }

        start = start.substring(base.length(), start.length() - ".class".length()).replace(File.separatorChar, '/');
        System.err.println("start: " + start);

        newSet.add(start);
        set.add(start);

        do {
            Iterator i = newSet.iterator();
            while (i.hasNext()) {
                String fileName = base + ((String)i.next()).replace('/', File.separatorChar) + ".class";

                try {
                    JavaClass javaClass = new ClassParser(fileName).parse();
                    javaClass.accept(visitor);
                }
                catch (IOException e) {
                    System.err.println("exception: " +  e.getMessage());
                }
            }
            newSet.clear();
            newSet.addAll(visitor.getDependencies());
            visitor.clearDependencies();

            Dependencies.applyFilter(newSet, new Filter() {
                    public boolean accept(Object object) {
                        String fileName = base + ((String)object).replace('/', File.separatorChar) + ".class";
                        return new File(fileName).exists();
                    }
                });
            newSet.removeAll(set);
            set.addAll(newSet);
        }
        while (newSet.size() > 0);

        included.clear();
        included.addAll(set);
    }

    public void addDefaultExcludes() {}
    public String[] getExcludedDirectories() { return null; };
    public String[] getExcludedFiles() { return null; }
    public String[] getIncludedDirectories() { return new String[0]; }
    public String[] getNotIncludedDirectories() { return null; }
    public String[] getNotIncludedFiles() { return null; }

    public void setExcludes(String[] excludes) {}
    public void setIncludes(String[] includes) {}
    public void setCaseSensitive(boolean isCaseSensitive) {}
}
