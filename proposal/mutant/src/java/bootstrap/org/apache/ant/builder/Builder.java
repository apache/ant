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
import java.util.ArrayList;

import java.util.List;

/**
 * The Builder object builds the code for bootstrap purposes. It invokes the
 * mathods of the required targets in the converted build files.
 *
 * @author <a href="mailto:conor@apache.org">Conor MacNeill</a>
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
     * Get the Ant1 files currently required to build a bootstrap build.
     *
     * @return an array of files which need to be copied into the bootstrap
     *      build.
     */
    private File[] getAnt1Files() {
        List files = new ArrayList();
        files.add(new File(TYPES_ROOT, "EnumeratedAttribute.java"));
        files.add(new File(TYPES_ROOT, "Path.java"));
        files.add(new File(TYPES_ROOT, "FileSet.java"));
        files.add(new File(TYPES_ROOT, "PatternSet.java"));
        files.add(new File(TYPES_ROOT, "Reference.java"));
        files.add(new File(TYPES_ROOT, "FilterSet.java"));
        files.add(new File(TYPES_ROOT, "FilterSetCollection.java"));
        files.add(new File(TYPES_ROOT, "Mapper.java"));
        files.add(new File(TYPES_ROOT, "ZipFileSet.java"));
        files.add(new File(TYPES_ROOT, "ZipScanner.java"));
        files.add(new File(TYPES_ROOT, "FilterChain.java"));
        files.add(new File(TYPES_ROOT, "Parameter.java"));
        files.add(new File(TYPES_ROOT, "Parameterizable.java"));
        files.add(new File(TYPES_ROOT, "RegularExpression.java"));
        files.add(new File(UTIL_ROOT, "FileNameMapper.java"));
        files.add(new File(UTIL_ROOT, "FlatFileNameMapper.java"));
        files.add(new File(UTIL_ROOT, "SourceFileScanner.java"));
        files.add(new File(UTIL_ROOT, "IdentityMapper.java"));
        files.add(new File(UTIL_ROOT, "MergingMapper.java"));
        files.add(new File(UTIL_ROOT, "GlobPatternMapper.java"));
        files.add(new File(UTIL_ROOT, "regexp/Regexp.java"));
        files.add(new File(UTIL_ROOT, "regexp/RegexpMatcher.java"));
        files.add(new File(UTIL_ROOT, "regexp/RegexpFactory.java"));
        files.add(new File(UTIL_ROOT, "regexp/RegexpMatcherFactory.java"));
        files.add(new File(TYPES_ROOT, "Commandline.java"));
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
        files.add(new File(UTIL_ROOT, "FileUtils.java"));
        files.add(new File(PACKAGE_ROOT, "defaultManifest.mf"));
        files.add(new File(TASKDEFS_ROOT, "defaults.properties"));
        files.add(new File(TYPES_ROOT, "defaults.properties"));
        files.add(new File(TASKDEFS_ROOT, "Property.java"));
        files.add(new File(TASKDEFS_ROOT, "Execute.java"));
        files.add(new File(TASKDEFS_ROOT, "ExecuteStreamHandler.java"));
        files.add(new File(TASKDEFS_ROOT, "ExecuteWatchdog.java"));
        files.add(new File(TASKDEFS_ROOT, "ProcessDestroyer.java"));
        files.add(new File(TASKDEFS_ROOT, "PumpStreamHandler.java"));
        files.add(new File(TASKDEFS_ROOT, "StreamPumper.java"));
        files.add(new File(TASKDEFS_ROOT, "LogStreamHandler.java"));
        files.add(new File(TASKDEFS_ROOT, "LogOutputStream.java"));
        files.add(new File(TASKDEFS_ROOT, "condition/Os.java"));
        files.add(new File(TASKDEFS_ROOT, "condition/Contains.java"));
        files.add(new File(TASKDEFS_ROOT, "condition/Condition.java"));
        files.add(new File(TASKDEFS_ROOT, "Available.java"));
        files.add(new File(TASKDEFS_ROOT, "Mkdir.java"));
        files.add(new File(TASKDEFS_ROOT, "Copy.java"));
        files.add(new File(TASKDEFS_ROOT, "Echo.java"));
        files.add(new File(TASKDEFS_ROOT, "MatchingTask.java"));
        files.add(new File(DEPEND_ROOT, "Depend.java"));
        files.add(new File(DEPEND_ROOT, "ClassFile.java"));
        files.add(new File(DEPEND_ROOT, "ClassFileUtils.java"));
        files.add(new File(DEPEND_ROOT, "constantpool/ClassCPInfo.java"));
        files.add(new File(DEPEND_ROOT, "constantpool/ConstantPool.java"));
        files.add(new File(DEPEND_ROOT, "constantpool/ConstantPoolEntry.java"));
        files.add(new File(DEPEND_ROOT, "constantpool/Utf8CPInfo.java"));
        files.add(new File(DEPEND_ROOT, "constantpool/ConstantCPInfo.java"));
        files.add(new File(DEPEND_ROOT, "constantpool/MethodRefCPInfo.java"));
        files.add(new File(DEPEND_ROOT,
            "constantpool/InterfaceMethodRefCPInfo.java"));
        files.add(new File(DEPEND_ROOT, "constantpool/FieldRefCPInfo.java"));
        files.add(new File(DEPEND_ROOT, "constantpool/NameAndTypeCPInfo.java"));
        files.add(new File(DEPEND_ROOT, "constantpool/IntegerCPInfo.java"));
        files.add(new File(DEPEND_ROOT, "constantpool/FloatCPInfo.java"));
        files.add(new File(DEPEND_ROOT, "constantpool/LongCPInfo.java"));
        files.add(new File(DEPEND_ROOT, "constantpool/StringCPInfo.java"));
        files.add(new File(DEPEND_ROOT, "constantpool/DoubleCPInfo.java"));
        files.add(new File(TASKDEFS_ROOT, "Javac.java"));
        files.add(new File(TASKDEFS_ROOT, "compilers/CompilerAdapter.java"));
        files.add(new File(TASKDEFS_ROOT,
            "compilers/DefaultCompilerAdapter.java"));
        files.add(new File(TASKDEFS_ROOT,
            "compilers/CompilerAdapterFactory.java"));
        files.add(new File(TASKDEFS_ROOT, "compilers/Jikes.java"));
        files.add(new File(TASKDEFS_ROOT, "compilers/JavacExternal.java"));
        files.add(new File(TASKDEFS_ROOT, "compilers/Javac12.java"));
        files.add(new File(TASKDEFS_ROOT, "compilers/Javac13.java"));
        files.add(new File(TASKDEFS_ROOT, "compilers/Kjc.java"));
        files.add(new File(TASKDEFS_ROOT, "compilers/Gcj.java"));
        files.add(new File(TASKDEFS_ROOT, "compilers/Jvc.java"));
        files.add(new File(TASKDEFS_ROOT, "compilers/Sj.java"));
        files.add(new File(TASKDEFS_ROOT, "Jar.java"));
        files.add(new File(TASKDEFS_ROOT, "Zip.java"));
        files.add(new File(TASKDEFS_ROOT, "Manifest.java"));
        files.add(new File(TASKDEFS_ROOT, "ManifestException.java"));
        files.add(new File(ZIP_ROOT, "ZipOutputStream.java"));
        files.add(new File(ZIP_ROOT, "ZipOutputStream.java"));
        files.add(new File(ZIP_ROOT, "ZipEntry.java"));
        files.add(new File(ZIP_ROOT, "ZipLong.java"));
        files.add(new File(ZIP_ROOT, "ZipShort.java"));
        files.add(new File(ZIP_ROOT, "ZipExtraField.java"));
        files.add(new File(ZIP_ROOT, "ExtraFieldUtils.java"));
        files.add(new File(ZIP_ROOT, "AsiExtraField.java"));
        files.add(new File(ZIP_ROOT, "UnrecognizedExtraField.java"));
        files.add(new File(ZIP_ROOT, "UnixStat.java"));
        files.add(new File(TASKDEFS_ROOT, "ConditionTask.java"));
        files.add(new File(TASKDEFS_ROOT, "condition/ConditionBase.java"));
        files.add(new File(TASKDEFS_ROOT, "Checksum.java"));
        files.add(new File(TASKDEFS_ROOT, "UpToDate.java"));
        files.add(new File(TASKDEFS_ROOT, "condition/Not.java"));
        files.add(new File(TASKDEFS_ROOT, "condition/And.java"));
        files.add(new File(TASKDEFS_ROOT, "condition/Equals.java"));
        files.add(new File(TASKDEFS_ROOT, "condition/Or.java"));
        files.add(new File(TASKDEFS_ROOT, "condition/IsSet.java"));
        files.add(new File(TASKDEFS_ROOT, "condition/Http.java"));
        files.add(new File(TASKDEFS_ROOT, "condition/Socket.java"));
        files.add(new File(TASKDEFS_ROOT, "condition/FilesMatch.java"));
        files.add(new File(TASKDEFS_ROOT, "Taskdef.java"));
        files.add(new File(TASKDEFS_ROOT, "Definer.java"));
        
        files.add(new File(FILTERS_ROOT, "util/ChainReaderHelper.java"));
        files.add(new File(FILTERS_ROOT, "ClassConstants.java"));
        files.add(new File(FILTERS_ROOT, "ExpandProperties.java"));
        files.add(new File(FILTERS_ROOT, "HeadFilter.java"));
        files.add(new File(FILTERS_ROOT, "LineContains.java"));
        files.add(new File(FILTERS_ROOT, "LineContainsRegExp.java"));
        files.add(new File(FILTERS_ROOT, "PrefixLines.java"));
        files.add(new File(FILTERS_ROOT, "ReplaceTokens.java"));
        files.add(new File(FILTERS_ROOT, "StripJavaComments.java"));
        files.add(new File(FILTERS_ROOT, "StripLineBreaks.java"));
        files.add(new File(FILTERS_ROOT, "StripLineComments.java"));
        files.add(new File(FILTERS_ROOT, "TabsToSpaces.java"));
        files.add(new File(FILTERS_ROOT, "TailFilter.java"));
        files.add(new File(FILTERS_ROOT, "BaseFilterReader.java"));
        files.add(new File(FILTERS_ROOT, "ChainableReader.java"));
        files.add(new File(TYPES_ROOT, "AntFilterReader.java"));
        files.add(new File(FILTERS_ROOT, "BaseParamFilterReader.java"));
        files.add(new File(FILTERS_ROOT, ".java"));
        return (File[])files.toArray(new File[0]);
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
        mutantBuilder.cli(mainBuild);

        BuildHelper systemBuild = new BuildHelper();
        systemBuild.setProperty("libset", "system");
        systemBuild.setProperty("dist.dir", "bootstrap");
        mutantBuilder._init(systemBuild);
        mutantBuilder.build_lib(systemBuild);

        Ant1CompatBuilder ant1Builder = new Ant1CompatBuilder();
        BuildHelper ant1Build = new BuildHelper();
        ant1Build.setProperty("dist.dir", "bootstrap");
        ant1Build.addFileSet("ant1src", ANT1_SRC_ROOT, getAnt1Files());
        ant1Builder._init(ant1Build);
        ant1Builder.ant1compat(ant1Build);
    }
}

