/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
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

package org.apache.tools.ant.taskdefs.compilers;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.Javac;
import org.apache.tools.ant.types.Commandline;
import org.junit.Test;

import static org.apache.tools.ant.AntAssert.assertContains;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.types.Path;
import org.apache.tools.ant.util.FileUtils;
import org.junit.Assert;
import static org.junit.Assert.assertEquals;

public class DefaultCompilerAdapterTest {

    private static class LogCapturingJavac extends Javac {
        private StringBuffer sb = new StringBuffer();
        public void log(String msg, int msgLevel) {
            sb.append(msg);
        }
        String getLog() {
            return sb.toString();
        }
    }

    private static class SourceTargetHelper extends DefaultCompilerAdapter {
        /**
         * Overridden to have no effect.
         */
        protected Commandline setupJavacCommandlineSwitches(Commandline cmd,
                                                            boolean debug) {
            return cmd;
        }

        public boolean execute() { return false; }

        /**
         * public to avoid classloader issues.
         */
        public Commandline setupModernJavacCommandlineSwitches(Commandline cmd) {
            return super.setupModernJavacCommandlineSwitches(cmd);
        }
    }

    private static class SourceTargetHelperNoOverride extends DefaultCompilerAdapter {

        public boolean execute() { return false; }

        /**
         * public to avoid classloader issues.
         */
        public Commandline setupModernJavacCommandlineSwitches(Commandline cmd) {
            return super.setupModernJavacCommandlineSwitches(cmd);
        }
    }

    @Test
    public void testSourceIsIgnoredForJavac13() {
        testSource(null, "javac1.3", "", null, "1.1");
        testSource(null, "javac1.3", "", null, "1.2");
        testSource(null, "javac1.3", "", null, "1.3");
        testSource(null, "javac1.3", "", null, "1.4");
    }

    @Test
    public void testSource11IsUpgradedTo13() {
        testSource("1.3", "javac1.4", "", null, "1.1");
        testSource("1.3", "javac1.5", "", null, "1.1");
        testSource("1.3", "javac1.6", "", null, "1.1");
        testSource("1.3", "javac1.7", "", null, "1.1");
        testSource("1.3", "javac1.8", "", null, "1.1");
    }

    @Test
    public void testSource12IsUpgradedTo13() {
        testSource("1.3", "javac1.4", "", null, "1.2");
        testSource("1.3", "javac1.5", "", null, "1.2");
        testSource("1.3", "javac1.6", "", null, "1.2");
        testSource("1.3", "javac1.7", "", null, "1.2");
        testSource("1.3", "javac1.8", "", null, "1.2");
    }

    @Test
    public void testImplicitSourceForJava15() {
        commonSourceDowngrades("javac1.5");
        testSource(null, "javac1.5", "", "1.5");
        testSource(null, "javac1.5", "", "5");
    }

    @Test
    public void testImplicitSourceForJava16() {
        commonSourceDowngrades("javac1.6");
        testSource(null, "javac1.6", "", "1.5");
        testSource(null, "javac1.6", "", "5");
        testSource(null, "javac1.6", "", "1.6");
        testSource(null, "javac1.6", "", "6");
    }

    @Test
    public void testImplicitSourceForJava17() {
        commonSourceDowngrades("javac1.7");
        testSource("1.5", "javac1.7",
                   "If you specify -target 1.5 you now must also specify"
                   + " -source 1.5", "1.5");
        testSource("1.6", "javac1.7",
                   "If you specify -target 1.6 you now must also specify"
                   + " -source 1.6", "1.6");
        testSource("5", "javac1.7",
                   "If you specify -target 5 you now must also specify"
                   + " -source 5", "5");
        testSource("6", "javac1.7",
                   "If you specify -target 6 you now must also specify"
                   + " -source 6", "6");
        testSource(null, "javac1.7", "", "1.7");
        testSource(null, "javac1.7", "", "7");
    }

    @Test
    public void testImplicitSourceForJava18() {
        commonSourceDowngrades("javac1.8");
        testSource("1.5", "javac1.8",
                   "If you specify -target 1.5 you now must also specify"
                   + " -source 1.5", "1.5");
        testSource("1.6", "javac1.8",
                   "If you specify -target 1.6 you now must also specify"
                   + " -source 1.6", "1.6");
        testSource("1.7", "javac1.8",
                   "If you specify -target 1.7 you now must also specify"
                   + " -source 1.7", "1.7");
        testSource("5", "javac1.8",
                   "If you specify -target 5 you now must also specify"
                   + " -source 5", "5");
        testSource("6", "javac1.8",
                   "If you specify -target 6 you now must also specify"
                   + " -source 6", "6");
        testSource("7", "javac1.8",
                   "If you specify -target 7 you now must also specify"
                   + " -source 7", "7");
        testSource(null, "javac1.8", "", "1.8");
        testSource(null, "javac1.8", "", "8");
    }

    @Test
    public void testImplicitSourceForJava19() {
        commonSourceDowngrades("javac1.9");
        testSource("1.5", "javac1.9",
                   "If you specify -target 1.5 you now must also specify"
                   + " -source 1.5", "1.5");
        testSource("1.6", "javac1.9",
                   "If you specify -target 1.6 you now must also specify"
                   + " -source 1.6", "1.6");
        testSource("1.7", "javac1.9",
                   "If you specify -target 1.7 you now must also specify"
                   + " -source 1.7", "1.7");
        testSource("1.8", "javac1.9",
                   "If you specify -target 1.8 you now must also specify"
                   + " -source 1.8", "1.8");
        testSource("5", "javac1.9",
                   "If you specify -target 5 you now must also specify"
                   + " -source 5", "5");
        testSource("6", "javac1.9",
                   "If you specify -target 6 you now must also specify"
                   + " -source 6", "6");
        testSource("7", "javac1.9",
                   "If you specify -target 7 you now must also specify"
                   + " -source 7", "7");
        testSource("8", "javac1.9",
                   "If you specify -target 8 you now must also specify"
                   + " -source 8", "8");
        testSource(null, "javac1.9", "", "1.9");
        testSource(null, "javac1.9", "", "9");
    }

    @Test
    public void testImplicitSourceForJava9() {
        commonSourceDowngrades("javac9");
        testSource("1.5", "javac9",
                   "If you specify -target 1.5 you now must also specify"
                   + " -source 1.5", "1.5");
        testSource("1.6", "javac1.9",
                   "If you specify -target 1.6 you now must also specify"
                   + " -source 1.6", "1.6");
        testSource("1.7", "javac9",
                   "If you specify -target 1.7 you now must also specify"
                   + " -source 1.7", "1.7");
        testSource("1.8", "javac9",
                   "If you specify -target 1.8 you now must also specify"
                   + " -source 1.8", "1.8");
        testSource("5", "javac9",
                   "If you specify -target 5 you now must also specify"
                   + " -source 5", "5");
        testSource("6", "javac9",
                   "If you specify -target 6 you now must also specify"
                   + " -source 6", "6");
        testSource("7", "javac9",
                   "If you specify -target 7 you now must also specify"
                   + " -source 7", "7");
        testSource("8", "javac9",
                   "If you specify -target 8 you now must also specify"
                   + " -source 8", "8");
        testSource(null, "javac9", "", "1.9");
        testSource(null, "javac9", "", "9");
    }

    @Test
    public void testSingleModuleCompilation() throws IOException {
        final File workDir = createWorkDir("testSMC");
        try {
            final File src = new File(workDir, "src");
            src.mkdir();
            final File java1 = createFile(src,"org/apache/ant/tests/J1.java");
            final File java2 = createFile(src,"org/apache/ant/tests/J2.java");
            final File modules = new File(workDir, "modules");
            modules.mkdir();
            final Project prj = new Project();
            prj.setBaseDir(workDir);
            final LogCapturingJavac javac = new LogCapturingJavac();
            javac.setProject(prj);
            final Commandline[] cmd = new Commandline[1];
            final DefaultCompilerAdapter impl = new DefaultCompilerAdapter() {
                @Override
                public boolean execute() throws BuildException {
                    cmd[0] = setupModernJavacCommand();
                    return true;
                }
            };
            final Path srcPath = new Path(prj);
            srcPath.setLocation(src);
            javac.setSrcdir(srcPath);
            javac.createModulepath().setLocation(modules);
            javac.setSource("9");
            javac.setTarget("9");
            javac.setIncludeantruntime(false);
            javac.add(impl);
            javac.execute();
            Assert.assertNotNull(cmd[0]);
            final List<String> cmdLine = Arrays.asList(cmd[0].getCommandline());
            //No modulesourcepath
            assertEquals(-1, cmdLine.indexOf("--module-source-path"));
            //The -sourcepath has to be followed by src
            int index = cmdLine.indexOf("-sourcepath");
            Assert.assertTrue(index != -1 && index < cmdLine.size() - 1);
            assertEquals(src.getAbsolutePath(), cmdLine.get(index + 1));
            //The --module-path has to be followed by modules
            index = cmdLine.indexOf("--module-path");
            Assert.assertTrue(index != -1 && index < cmdLine.size() - 1);
            assertEquals(modules.getAbsolutePath(), cmdLine.get(index + 1));
            //J1.java & J2.java has to be in files list
            final Set<String> expected = new TreeSet<String>();
            Collections.addAll(expected, java1.getAbsolutePath(), java2.getAbsolutePath());
            final Set<String> actual = new TreeSet<String>();
            actual.addAll(cmdLine.subList(cmdLine.size() - 2, cmdLine.size()));
            assertEquals(expected, actual);
        } finally {
            delete(workDir);
        }
    }

    @Test
    public void testMultiModuleCompilation() throws IOException {
        final File workDir = createWorkDir("testMMC");
        try {
            final File src = new File(workDir, "src");
            src.mkdir();
            final File java1 = createFile(src,"main/m1/lin64/classes/org/apache/ant/tests/J1.java");
            final File java2 = createFile(src,"main/m2/lin32/classes/org/apache/ant/tests/J2.java");
            final File java3 = createFile(src,"main/m3/sol/classes/org/apache/ant/tests/J3.java");
            final File modules = new File(workDir, "modules");
            modules.mkdir();
            final File build = new File(workDir, "build");
            build.mkdirs();
            final Project prj = new Project();
            prj.setBaseDir(workDir);
            final LogCapturingJavac javac = new LogCapturingJavac();
            javac.setProject(prj);
            final Commandline[] cmd = new Commandline[1];
            final DefaultCompilerAdapter impl = new DefaultCompilerAdapter() {
                @Override
                public boolean execute() throws BuildException {
                    cmd[0] = setupModernJavacCommand();
                    return true;
                }
            };
            final String moduleSrcPathStr = "src/main/*/{lin{32,64},sol}/classes";
            final Path moduleSourcePath = new Path(prj);
            moduleSourcePath.setPath(moduleSrcPathStr);
            javac.setModulesourcepath(moduleSourcePath);
            javac.createModulepath().setLocation(modules);
            javac.setSource("9");
            javac.setTarget("9");
            javac.setDestdir(build);
            javac.setIncludeantruntime(false);
            javac.add(impl);
            javac.execute();
            Assert.assertNotNull(cmd[0]);
            final List<String> cmdLine = Arrays.asList(cmd[0].getCommandline());
            //No sourcepath
            assertEquals(-1, cmdLine.indexOf("-sourcepath"));
            //The --module-source-path has to be followed by the pattern
            int index = cmdLine.indexOf("--module-source-path");
            Assert.assertTrue(index != -1 && index < cmdLine.size() - 1);
            String expectedModSrcPath = String.format("%s/%s",
                    workDir.getAbsolutePath(),
                    moduleSrcPathStr)
                    .replace('/', File.separatorChar)
                    .replace('\\', File.separatorChar);
            assertEquals(expectedModSrcPath, cmdLine.get(index + 1));
            //The --module-path has to be followed by modules
            index = cmdLine.indexOf("--module-path");
            Assert.assertTrue(index != -1 && index < cmdLine.size() - 1);
            assertEquals(modules.getAbsolutePath(), cmdLine.get(index + 1));
            //J1.java, J2.java & J3.java has to be in files list
            final Set<String> expectedFiles = new TreeSet<String>();
            Collections.addAll(expectedFiles,
                    java1.getAbsolutePath(),
                    java2.getAbsolutePath(),
                    java3.getAbsolutePath());
            final Set<String> actualFiles = new TreeSet<String>();
            actualFiles.addAll(cmdLine.subList(cmdLine.size() - 3, cmdLine.size()));
            assertEquals(expectedFiles, actualFiles);
        } finally {
            delete(workDir);
        }
    }

    @Test
    public void testMultiModuleCompilationWithExcludes() throws IOException {
        final File workDir = createWorkDir("testMMCWE");
        try {
            final File src = new File(workDir, "src");
            src.mkdir();
            final File java1 = createFile(src,"main/m1/lin/classes/org/apache/ant/tests/J1.java");
            final File java2 = createFile(src,"main/m3/sol/classes/org/apache/ant/tests/J2.java");
            final File java3 = createFile(src,"main/m3/sol/classes/org/apache/ant/invisible/J3.java");
            final File build = new File(workDir, "build");
            build.mkdirs();
            final Project prj = new Project();
            prj.setBaseDir(workDir);
            final LogCapturingJavac javac = new LogCapturingJavac();
            javac.setProject(prj);
            final DefaultCompilerAdapter impl = new DefaultCompilerAdapter() {
                @Override
                public boolean execute() throws BuildException {
                    setupModernJavacCommand();
                    return true;
                }
            };
            final String moduleSrcPathStr = "src/main/*/{lin,sol}/classes";
            final Path moduleSourcePath = new Path(prj);
            moduleSourcePath.setPath(moduleSrcPathStr);
            javac.setModulesourcepath(moduleSourcePath);
            javac.setSource("9");
            javac.setTarget("9");
            javac.setDestdir(build);
            javac.setIncludeantruntime(false);
            javac.createExclude().setName("org/**/invisible/**");
            javac.add(impl);
            javac.execute();
            final File[] compileList = impl.compileList;
            Assert.assertNotNull(compileList);
            //J1.java, J2.java has to be in files list but not J3.java
            final Set<String> expectedFiles = new TreeSet<String>();
            Collections.addAll(expectedFiles,
                    java1.getAbsolutePath(),
                    java2.getAbsolutePath());
            final Set<String> actualFiles = new TreeSet<String>();
            for (File compileFile : compileList) {
                actualFiles.add(compileFile.getAbsolutePath());
            }
            assertEquals(expectedFiles, actualFiles);
        } finally {
            delete(workDir);
        }
    }

    @Test
    public void releaseIsIgnoredForJava8() {
        LogCapturingJavac javac = new LogCapturingJavac();
        Project p = new Project();
        javac.setProject(p);
        javac.setCompiler("javac8");
        javac.setSource("6");
        javac.setTarget("6");
        javac.setRelease("6");
        javac.setSourcepath(new Path(p));
        SourceTargetHelperNoOverride sth = new SourceTargetHelperNoOverride();
        sth.setJavac(javac);
        Commandline cmd = new Commandline();
        sth.setupModernJavacCommandlineSwitches(cmd);
        assertContains("Support for javac --release has been added in "
                       + "Java9 ignoring it", javac.getLog());
        String[] args = cmd.getCommandline();
        assertEquals(7, args.length);
        assertEquals("-classpath", args[0]);
        assertEquals("-target", args[2]);
        assertEquals("6", args[3]);
        assertEquals("-g:none", args[4]);
        assertEquals("-source", args[5]);
        assertEquals("6", args[6]);
    }

    @Test
    public void releaseIsUsedForJava9() {
        LogCapturingJavac javac = new LogCapturingJavac();
        Project p = new Project();
        javac.setProject(p);
        javac.setCompiler("javac9");
        javac.setSource("6");
        javac.setTarget("6");
        javac.setRelease("6");
        javac.setSourcepath(new Path(p));
        SourceTargetHelperNoOverride sth = new SourceTargetHelperNoOverride();
        sth.setJavac(javac);
        Commandline cmd = new Commandline();
        sth.setupModernJavacCommandlineSwitches(cmd);
        assertContains("Ignoring source, target and bootclasspath as "
                       + "release has been set", javac.getLog());
        String[] args = cmd.getCommandline();
        assertEquals(5, args.length);
        assertEquals("-classpath", args[0]);
        assertEquals("-g:none", args[2]);
        assertEquals("--release", args[3]);
        assertEquals("6", args[4]);
    }

    private void commonSourceDowngrades(String javaVersion) {
        testSource("1.3", javaVersion,
                   "If you specify -target 1.1 you now must also specify"
                   + " -source 1.3", "1.1");
        testSource("1.3", javaVersion,
                   "If you specify -target 1.2 you now must also specify"
                   + " -source 1.3", "1.2");
        testSource("1.3", javaVersion,
                   "If you specify -target 1.3 you now must also specify"
                   + " -source 1.3", "1.3");
        testSource("1.4", javaVersion,
                   "If you specify -target 1.4 you now must also specify"
                   + " -source 1.4", "1.4");
    }

    private void testSource(String expectedSource, String javaVersion,
                            String expectedLog, String configuredTarget) {
        testSource(expectedSource, javaVersion, expectedLog, configuredTarget,
                   null);
    }

    private void testSource(String expectedSource, String javaVersion,
                            String expectedLog, String configuredTarget,
                            String configuredSource) {
        LogCapturingJavac javac = new LogCapturingJavac();
        javac.setProject(new Project());
        javac.setCompiler(javaVersion);
        javac.setSource(configuredSource);
        javac.setTarget(configuredTarget);
        SourceTargetHelper sth = new SourceTargetHelper();
        sth.setJavac(javac);
        Commandline cmd = new Commandline();
        sth.setupModernJavacCommandlineSwitches(cmd);
        if ("".equals(expectedLog)) {
            assertEquals("", javac.getLog());
        } else {
            String l = javac.getLog();
            assertContains(expectedLog, l);
        }
        String[] args = cmd.getCommandline();
        assertEquals(expectedSource == null ? 0 : 2, args.length);
        if (expectedSource != null) {
            assertEquals("-source", args[0]);
            assertEquals(expectedSource, args[1]);
        }
    }

    private File createWorkDir(String testName) {
        final File tmp = new File(System.getProperty("java.io.tmpdir"));   //NOI18N
        final File destDir = new File(tmp, String.format("%s%s%d",
                getClass().getName(),
                testName,
                System.currentTimeMillis()/1000));
        destDir.mkdirs();
        return destDir;
    }

    private File createFile(File folder, String relativePath) throws IOException {
        final File file = new File(
                folder,
                relativePath.replace('/', File.separatorChar).replace('\\', File.separatorChar));
        FileUtils.getFileUtils().createNewFile(file, true);
        return file;
    }

    private void delete(File f) {
        if (f.isDirectory()) {
            final File[] clds = f.listFiles();
            if (clds != null) {
                for (File cld : clds) {
                    delete(cld);
                }
            }
        }
        f.delete();
    }

}
