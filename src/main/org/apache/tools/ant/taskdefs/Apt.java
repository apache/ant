/*
 * Copyright  2002-2004 The Apache Software Foundation
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
package org.apache.tools.ant.taskdefs;

import org.apache.tools.ant.Project;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.taskdefs.compilers.AptExternalCompilerAdapter;
import org.apache.tools.ant.taskdefs.compilers.AptCompilerAdapter;
import org.apache.tools.ant.types.Path;
import org.apache.tools.ant.types.Reference;
import org.apache.tools.ant.util.JavaEnvUtils;

import java.util.Vector;
import java.io.File;

/**
 * Apt Task for running the Annotation processing tool for JDK 1.5.  It derives
 * from the existing Javac task, and forces the compiler based on whether we're
 * executing internally, or externally.
 *
 * @since Ant 1.7
 */


public class Apt
        extends Javac {
    private boolean compile = true;
    private String factory;
    private Path factoryPath;
    private Vector options;
    private File preprocessDir;
    public static final String EXECUTABLE_NAME = "apt";
    public static final String ERROR_IGNORING_COMPILER_OPTION = "Ignoring compiler attribute for the APT task, as it is fixed";
    public static final String ERROR_WRONG_JAVA_VERSION = "Apt task requires Java 1.5+";

    /**
     * option element
     */
    public static final class Option {
        private String name;
        private String value;

        public Option() {
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }
    }

    public Apt() {
        super();
        super.setCompiler(AptCompilerAdapter.class.getName());
        this.options = new Vector();
    }

    public String getAptExecutable() {
        return JavaEnvUtils.getJdkExecutable(EXECUTABLE_NAME);
    }

    public void setCompiler(String compiler) {
        log(ERROR_IGNORING_COMPILER_OPTION,
                Project.MSG_WARN);
    }

    public void setFork(boolean fork) {
        if (fork) {
            super.setCompiler(AptExternalCompilerAdapter.class.getName());
        } else {
            super.setCompiler(AptCompilerAdapter.class.getName());
        }
    }

    public String getCompiler() {
        return super.getCompiler();
    }

    public boolean isCompile() {
        return compile;
    }

    public void setCompile(boolean compile) {
        this.compile = compile;
    }

    public String getFactory() {
        return factory;
    }

    public void setFactory(String factory) {
        this.factory = factory;
    }

    public void setFactoryPathRef(Reference ref) {
        createFactoryPath().setRefid(ref);
    }

    public Path createFactoryPath() {
        if (factoryPath == null) {
            factoryPath = new Path(getProject());
        }
        return factoryPath.createPath();
    }

    public Path getFactoryPath() {
        return factoryPath;
    }

    public Option createOption() {
        Option opt = new Option();
        options.add(opt);
        return opt;
    }

    public Vector getOptions() {
        return options;
    }

    public File getPreprocessDir() {
        return preprocessDir;
    }

    public void setPreprocessDir(File preprocessDir) {
        this.preprocessDir = preprocessDir;
    }

    public void execute()
            throws BuildException {
        if (!JavaEnvUtils.isJavaVersion(JavaEnvUtils.JAVA_1_5)) {
            throw new BuildException(ERROR_WRONG_JAVA_VERSION);
        }

        super.execute();
    }
}
