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
package org.apache.ant.builder;
import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;

import java.util.List;

/**
 * The Builder object builds the code for bootstrap purposes. It invokes the
 * mathods of the required targets in the converted build files.
 *
 * @author Conor MacNeill
 * @created 18 February 2002
 */
public class Builder {
    /** The root of the Ant1 source tree */
    private static final File ANT1_SRC_ROOT = new File("../../src/main");
    /** the root of the Ant package in the Ant1 source tree */
    private static final File PACKAGE_ROOT
         = new File(ANT1_SRC_ROOT, "org/apache/tools/ant");
    /** The zip utilities root */
    private static final File ZIP_ROOT
         = new File(ANT1_SRC_ROOT, "org/apache/tools/zip");

    /** the taskdefs root */
    private static final File TASKDEFS_ROOT
         = new File(PACKAGE_ROOT, "taskdefs");
    /** the types root */
    private static final File TYPES_ROOT
         = new File(PACKAGE_ROOT, "types");
    /** the filters root */
    private static final File FILTERS_ROOT
         = new File(PACKAGE_ROOT, "filters");
    /** the util root */
    private static final File UTIL_ROOT
         = new File(PACKAGE_ROOT, "util");
    /** the input root */
    private static final File INPUT_ROOT
         = new File(PACKAGE_ROOT, "input");
         
         
    /** the root forthe depend task's support classes */
    private static final File DEPEND_ROOT
         = new File(TASKDEFS_ROOT, "optional/depend");

    /**
     * The main program - create a builder and run the build
     *
     * @param args the command line arguments - not currently used
     */
    public static void main(String[] args) {
        Builder builder = new Builder();
        builder.runBuild(args);
    }

    /**
     * Add all the java files fro, a given directory.
     *
     * @param files the list to which the files are to be added.
     * @param dir the directory from which the Java files are added.
     */
    private void addJavaFiles(List files, File dir) {
        File[] javaFiles = dir.listFiles(new FilenameFilter() {
            public boolean accept(File dir, String name) {
                return name.endsWith(".java");
            }
        });
        
        if (javaFiles != null) {
            for (int i = 0; i < javaFiles.length; ++i) {
                files.add(javaFiles[i]);
            }
        }
    }
    
    /**
     * Get the Ant1 files currently required to build a bootstrap build.
     *
     * @return an array of files which need to be copied into the bootstrap
     *      build.
     */
    private File[] getAnt1Files() {
        List files = new ArrayList();
        addJavaFiles(files, TASKDEFS_ROOT);
        addJavaFiles(files, new File(TASKDEFS_ROOT, "compilers"));
        addJavaFiles(files, new File(TASKDEFS_ROOT, "condition"));
        addJavaFiles(files, DEPEND_ROOT);
        addJavaFiles(files, new File(DEPEND_ROOT, "constantpool"));
        addJavaFiles(files, TYPES_ROOT);
        addJavaFiles(files, FILTERS_ROOT);
        addJavaFiles(files, UTIL_ROOT);
        addJavaFiles(files, new File(UTIL_ROOT, "depend"));
        addJavaFiles(files, ZIP_ROOT);
        addJavaFiles(files, new File(UTIL_ROOT, "facade"));
        addJavaFiles(files, INPUT_ROOT);

        files.add(new File(PACKAGE_ROOT, "BuildException.java"));
        files.add(new File(PACKAGE_ROOT, "Location.java"));
        files.add(new File(PACKAGE_ROOT, "AntClassLoader.java"));
        files.add(new File(PACKAGE_ROOT, "BuildListener.java"));
        files.add(new File(PACKAGE_ROOT, "BuildEvent.java"));
        files.add(new File(PACKAGE_ROOT, "DirectoryScanner.java"));
        files.add(new File(PACKAGE_ROOT, "FileScanner.java"));
        files.add(new File(PACKAGE_ROOT, "PathTokenizer.java"));
        files.add(new File(PACKAGE_ROOT, "TaskAdapter.java"));
        files.add(new File(PACKAGE_ROOT, "MatchingTask.java"));
        files.add(new File(PACKAGE_ROOT, "defaultManifest.mf"));
        
        files.add(new File(TASKDEFS_ROOT, "defaults.properties"));
        files.add(new File(TYPES_ROOT, "defaults.properties"));

        files.add(new File(UTIL_ROOT, "regexp/Regexp.java"));
        files.add(new File(UTIL_ROOT, "regexp/RegexpMatcher.java"));
        files.add(new File(UTIL_ROOT, "regexp/RegexpFactory.java"));
        files.add(new File(UTIL_ROOT, "regexp/RegexpMatcherFactory.java"));
        files.add(new File(FILTERS_ROOT, "util/ChainReaderHelper.java"));
        
        // these should not be included
        files.remove(new File(TYPES_ROOT, "DataType.java"));
        files.remove(new File(TASKDEFS_ROOT, "Ant.java"));
        files.remove(new File(TASKDEFS_ROOT, "CallTarget.java"));
        files.remove(new File(TASKDEFS_ROOT, "AntStructure.java"));
        files.remove(new File(TASKDEFS_ROOT, "Recorder.java"));
        files.remove(new File(TASKDEFS_ROOT, "RecorderEntry.java"));
        files.remove(new File(TASKDEFS_ROOT, "SendEmail.java"));
        files.remove(new File(TASKDEFS_ROOT, "Do.java"));
        files.remove(new File(INPUT_ROOT, "InputRequest.java"));
        
        // not needed for bootstrap
        files.remove(new File(TASKDEFS_ROOT, "Java.java"));
        files.remove(new File(TASKDEFS_ROOT, "Tar.java"));
        files.remove(new File(TASKDEFS_ROOT, "Untar.java"));
        files.remove(new File(TASKDEFS_ROOT, "BZip2.java"));
        files.remove(new File(TASKDEFS_ROOT, "BUnzip2.java"));
        files.remove(new File(TASKDEFS_ROOT, "Rmic.java"));
        files.remove(new File(TASKDEFS_ROOT, "SendEmail.java"));
        
        
        return (File[]) files.toArray(new File[0]);
    }

    /**
     * Run the build
     *
     * @param args the command line arguments for the build - currently not
     *      used.
     */
    private void runBuild(String[] args) {
        BuildHelper mainBuild = new BuildHelper();
        mainBuild.setProperty("dist.dir", "bootstrap");
        MutantBuilder mutantBuilder = new MutantBuilder();
        mutantBuilder._init(mainBuild);
        mutantBuilder.buildsetup(mainBuild);
        mutantBuilder.init(mainBuild);
        mutantBuilder.common(mainBuild);
        mutantBuilder.antcore(mainBuild);
        mutantBuilder.start(mainBuild);
        mutantBuilder.frontend(mainBuild);

        BuildHelper systemBuild = new BuildHelper();
        systemBuild.setProperty("libset", "system");
        systemBuild.setProperty("dist.dir", "bootstrap");
        mutantBuilder._init(systemBuild);
        mutantBuilder.build_lib(systemBuild);

        Ant1CompatBuilder ant1Builder = new Ant1CompatBuilder();
        BuildHelper ant1Build = new BuildHelper();
        ant1Build.setProperty("dist.dir", "bootstrap");
        ant1Build.addFileSet("ant1src_tocopy", ANT1_SRC_ROOT, getAnt1Files());
        ant1Builder._init(ant1Build);
        ant1Builder.ant1compat(ant1Build);
    }
}

