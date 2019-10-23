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

package org.apache.tools.ant.taskdefs.compilers;

import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.Javac;
import org.apache.tools.ant.types.Commandline;
import org.apache.tools.ant.types.Path;
import org.apache.tools.ant.util.FileUtils;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.stream.Stream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class JavacExternalTest {
    private static class TestJavacExternal extends JavacExternal {
        private String[] args;
        private int firstFileName;

        @Override
        protected int executeExternalCompile(String[] args, int firstFileName, final boolean quoteFiles) {
            this.args = args;
            this.firstFileName = firstFileName;
            return 0;
        }

        public String[] getArgs() {
            return args;
        }

        public int getFirstFileName() {
            return firstFileName;
        }
    }

    @Test
    public void allJavacOptionsButJAreWrittenToFile() throws Exception {
        final File workDir = createWorkDir("testSMC");
        try {
            final File src = new File(workDir, "src");
            src.mkdir();
            createFile(src, "org/apache/ant/tests/J1.java");
            createFile(src, "org/apache/ant/tests/J2.java");
            final File modules = new File(workDir, "modules");
            modules.mkdir();
            final Project prj = new Project();
            prj.setBaseDir(workDir);
            final Javac javac = new Javac();
            javac.setProject(prj);
            final Commandline[] cmd = new Commandline[1];
            final TestJavacExternal impl = new TestJavacExternal();
            final Path srcPath = new Path(prj);
            srcPath.setLocation(src);
            javac.setSrcdir(srcPath);
            javac.createModulepath().setLocation(modules);
            javac.setSource("9");
            javac.setTarget("9");
            javac.setFork(true);
            javac.setMemoryInitialSize("80m");
            javac.setExecutable("javacExecutable");
            javac.add(impl);
            javac.execute();
            assertEquals("javacExecutable", impl.getArgs()[0]);
            assertEquals("-J-Xms80m", impl.getArgs()[1]);
            assertTrue(impl.getArgs()[impl.getArgs().length - 1].endsWith("J2.java"));
            assertEquals(2, impl.getFirstFileName());
        } finally {
            delete(workDir);
        }
    }

    @Test
    public void allJOptionsAreMovedToBeginning() throws Exception {
        final File workDir = createWorkDir("testSMC");
        try {
            final File src = new File(workDir, "src");
            src.mkdir();
            createFile(src, "org/apache/ant/tests/J1.java");
            createFile(src, "org/apache/ant/tests/J2.java");
            final File modules = new File(workDir, "modules");
            modules.mkdir();
            final Project prj = new Project();
            prj.setBaseDir(workDir);
            final Javac javac = new Javac();
            javac.setProject(prj);
            final Commandline[] cmd = new Commandline[1];
            final TestJavacExternal impl = new TestJavacExternal();
            final Path srcPath = new Path(prj);
            srcPath.setLocation(src);
            javac.setSrcdir(srcPath);
            javac.createModulepath().setLocation(modules);
            javac.setSource("9");
            javac.setTarget("9");
            javac.setFork(true);
            javac.setMemoryInitialSize("80m");
            javac.setExecutable("javacExecutable");
            javac.add(impl);
            javac.createCompilerArg().setValue("-JDfoo=bar");
            javac.createCompilerArg().setValue("-JDred=color");
            javac.createCompilerArg().setLine("-JDspace line");
            javac.execute();
            assertEquals("javacExecutable", impl.getArgs()[0]);
            assertEquals("-J-Xms80m", impl.getArgs()[1]);
            assertEquals("-JDfoo=bar", impl.getArgs()[2]);
            assertEquals("-JDred=color", impl.getArgs()[3]);
            assertEquals("-JDspace", impl.getArgs()[4]);
            assertTrue(impl.getArgs()[impl.getArgs().length - 1].endsWith("J2.java"));
            assertEquals(5, impl.getFirstFileName());
        } finally {
            delete(workDir);
        }
    }

    @Test
    public void argFileOptionIsMovedToBeginning() throws Exception {
        final File workDir = createWorkDir("testSMC");
        try {
            final File src = new File(workDir, "src");
            src.mkdir();
            createFile(src, "org/apache/ant/tests/J1.java");
            createFile(src, "org/apache/ant/tests/J2.java");
            final File modules = new File(workDir, "modules");
            modules.mkdir();
            final Project prj = new Project();
            prj.setBaseDir(workDir);
            final Javac javac = new Javac();
            javac.setProject(prj);
            final Commandline[] cmd = new Commandline[1];
            final TestJavacExternal impl = new TestJavacExternal();
            final Path srcPath = new Path(prj);
            srcPath.setLocation(src);
            javac.setSrcdir(srcPath);
            javac.createModulepath().setLocation(modules);
            javac.setSource("9");
            javac.setTarget("9");
            javac.setFork(true);
            javac.setMemoryInitialSize("80m");
            javac.setExecutable("javacExecutable");
            javac.add(impl);
            javac.createCompilerArg().setValue("-g");
            javac.createCompilerArg().setValue("@/home/my-compiler.args");
            javac.execute();
            assertEquals("javacExecutable", impl.getArgs()[0]);
            assertEquals("-J-Xms80m", impl.getArgs()[1]);
            assertEquals("@/home/my-compiler.args", impl.getArgs()[2]);
            assertTrue(Stream.of(impl.getArgs()).anyMatch(x -> x.equals("-g")));
            assertTrue(impl.getArgs()[impl.getArgs().length - 2].endsWith("J1.java"));
            assertTrue(impl.getArgs()[impl.getArgs().length - 1].endsWith("J2.java"));
            assertEquals(3, impl.getFirstFileName());
        } finally {
            delete(workDir);
        }
    }

    private File createWorkDir(String testName) {
        final File tmp = new File(System.getProperty("java.io.tmpdir"));   //NOI18N
        final File destDir = new File(tmp, String.format("%s%s%d",
                getClass().getName(),
                testName,
                System.currentTimeMillis() / 1000));
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
                Arrays.stream(clds).forEach(this::delete);
            }
        }
        f.delete();
    }
}
