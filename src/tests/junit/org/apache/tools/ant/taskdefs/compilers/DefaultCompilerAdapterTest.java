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

import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.Javac;
import org.apache.tools.ant.types.Commandline;
import org.junit.Test;

import static org.apache.tools.ant.AntAssert.assertContains;
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
}
