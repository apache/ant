/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2003 The Apache Software Foundation.  All rights
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
 * 4. The names "Ant" and "Apache Software
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

        DotNetExecTask exec = new DotNetExecTask();
        exec.setProject(getProject());
        exec.setExecutable(getExecutable());
        exec.setTaskName(getTaskName());
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
