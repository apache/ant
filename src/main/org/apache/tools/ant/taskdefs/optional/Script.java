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
package org.apache.tools.ant.taskdefs.optional;

import java.io.File;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.util.optional.ScriptRunner;
import org.apache.tools.ant.util.ScriptRunnerBase;
import org.apache.tools.ant.types.Path;
import org.apache.tools.ant.types.Reference;

/**
 * Executes a script.
 *
 * @ant.task name="script"
 */
public class Script extends Task {

    private Path    classpath;
    private String  language;
    private File    src;
    private String  text;
    private boolean setBeans = true;

    /**
     * Do the work.
     *
     * @exception BuildException if something goes wrong with the build
     */
    public void execute() throws BuildException {
        ScriptRunnerBase runner = new ScriptRunner();
        if (language != null) {
            runner.setLanguage(language);
        }
        if (src != null) {
            runner.setSrc(src);
        }
        if (text != null) {
            runner.addText(text);
        }
        if (classpath != null) {
            runner.setScriptClassLoader(
                getProject().createClassLoader(
                    getClass().getClassLoader(), classpath));
        }
        if (setBeans) {
            runner.bindToComponent(this);
        } else {
            runner.bindToComponentMinimum(this);
        }

        runner.executeScript("ANT");
    }

    /**
     * Defines the language (required).
     *
     * @param language the scripting language name for the script.
     */
    public void setLanguage(String language) {
        this.language = language;
    }

    /**
     * Load the script from an external file ; optional.
     *
     * @param fileName the name of the file containing the script source.
     */
    public void setSrc(String fileName) {
        this.src = new File(fileName);
    }

    /**
     * Set the script text.
     *
     * @param text a component of the script text to be added.
     */
    public void addText(String text) {
        this.text = text;
    }

    /**
     * Set the classpath to be used when searching for classes and resources.
     *
     * @param classpath an Ant Path object containing the search path.
     */
    public void setClasspath(Path classpath) {
        createClasspath().append(classpath);
    }

    /**
     * Classpath to be used when searching for classes and resources.
     *
     * @return an empty Path instance to be configured by Ant.
     */
    public Path createClasspath() {
        if (this.classpath == null) {
            this.classpath = new Path(getProject());
        }
        return this.classpath.createPath();
    }

    /**
     * Set the classpath by reference.
     *
     * @param r a Reference to a Path instance to be used as the classpath
     *          value.
     */
    public void setClasspathRef(Reference r) {
        createClasspath().setRefid(r);
    }

    /**
     * Set the setbeans attribute.
     * If this is true, &lt;script&gt; will create variables in the
     * script instance for all
     * properties, targets and references of the current project.
     * It this is false, only the project and self variables will
     * be set.
     * The default is true.
     * @param setBeans the value to set.
     */
    public void setSetBeans(boolean setBeans) {
        this.setBeans = setBeans;
    }
}
