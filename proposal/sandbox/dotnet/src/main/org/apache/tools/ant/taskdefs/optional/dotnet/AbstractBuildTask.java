/*
 * Copyright  2003-2004 The Apache Software Foundation
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

package org.apache.tools.ant.taskdefs.optional.dotnet;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.util.DOMElementWriter;
import org.apache.tools.ant.util.FileUtils;
import org.apache.tools.ant.util.XMLFragment;

import org.w3c.dom.DocumentFragment;
import org.w3c.dom.Element;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Base class for NAntTask and MSBuildTask.
 */
public abstract class AbstractBuildTask extends Task {

    /**
     * The buildfile to invoke the build tool for.
     */
    private File buildFile;

    /**
     * The targets to execute.
     */
    private List targets = new ArrayList();

    /**
     * Properties to set.
     */
    private List properties = new ArrayList(1);

    /**
     * Nested build file fragment.
     */
    private XMLFragment buildSnippet;

    /**
     * The vm attribute - if given.
     */
    private String vm;

    /**
     * Empty constructor.
     */
    protected AbstractBuildTask() {
    }

    /**
     * Sets the name of the build file.
     */
    public final void setBuildfile(File f) {
        buildFile = f;
    }

    /**
     * Adds a build file fragment.
     */
    public void addBuild(XMLFragment f) {
        if (buildSnippet == null) {
            buildSnippet = f;
        } else {
            throw new BuildException("You must not specify more than one "
                                     + "build element");
        }
    }

    /**
     * Set the name of the executable for the virtual machine.
     *
     * @param value the name of the executable for the virtual machine
     */
    public void setVm(String value) {
        this.vm = value;
    }

    /**
     * A target.
     */
    public static class Target {
        private String name;

        public void setName(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }

    }

    /**
     * A target to execute.
     */
    public final void addTarget(Target t) {
        targets.add(t);
    }

    /**
     * A property.
     */
    // XXX, could have reused Property or Environment.Variable 
    //      - not decided so far
    public static class Property {
        private String name;
        private String value;

        public void setName(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }

        public void setValue(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }

    /**
     * A target to execute.
     */
    public final void addProperty(Property t) {
        properties.add(t);
    }

    /**
     * Must return the executable.
     *
     * @return must not return null
     */
    protected abstract String getExecutable();

    /**
     * Must return buildfile argument(s).
     *
     * @param buildFile the absolute File for the buildfile or null if
     * the user didn't specify a buildfile.
     *
     * @return must not return null
     */
    protected abstract String[] getBuildfileArguments(File buildFile);

    /**
     * Must return target argument(s).
     *
     * @return must not return null
     */
    protected abstract String[] getTargetArguments(List targets);

    /**
     * Must return property argument(s).
     *
     * @return must not return null
     */
    protected abstract String[] getPropertyArguments(List properties);

    /**
     * Turn the DoucmentFragment into a DOM tree suitable as a build
     * file when serialized.
     *
     * <p>Must throw a BuildException if the snippet can not be turned
     * into a build file.</p>
     */
    protected abstract Element makeTree(DocumentFragment f);

    /**
     * Perform the build.
     */
    public void execute() {
        if (buildFile != null && buildSnippet != null) {
            throw new BuildException("You must not specify the build file"
                                     + " attribute and a nested build at the"
                                     + " same time");
        }

        DotNetExecTask exec = DotNetExecTask.getTask(this, vm, 
                                                     getExecutable(), null);
        String[] args = getPropertyArguments(properties);
        for (int i = 0; i < args.length; i++) {
            exec.createArg().setValue(args[i]);
        }
        args = getTargetArguments(targets);
        for (int i = 0; i < args.length; i++) {
            exec.createArg().setValue(args[i]);
        }

        File generatedFile = null;
        if (buildSnippet != null) {
            try {
                generatedFile = getBuildFile();
            } catch (IOException e) {
                throw new BuildException(e);
            }
            args = getBuildfileArguments(generatedFile);
        } else {
            args = getBuildfileArguments(buildFile);
        }        

        for (int i = 0; i < args.length; i++) {
            exec.createArg().setValue(args[i]);
        }

        try {
            exec.execute();
        } finally {
            if (generatedFile != null) {
                generatedFile.delete();
            }
        }
    }

    private File getBuildFile() throws IOException {
        File f = null;
        if (buildSnippet != null) {
            Element e = makeTree(buildSnippet.getFragment());
            f = FileUtils.newFileUtils().createTempFile("build", ".xml", null);
            f.deleteOnExit();
            FileOutputStream out = null;
            try {
                out = new FileOutputStream(f);
                (new DOMElementWriter()).write(e, out);
            } finally {
                if (out != null) {
                    out.close();
                }
            }
        }
        return f;
    }
}
