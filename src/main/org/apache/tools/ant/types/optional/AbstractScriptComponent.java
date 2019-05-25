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
package org.apache.tools.ant.types.optional;

import java.io.File;

import org.apache.tools.ant.Project;
import org.apache.tools.ant.ProjectComponent;
import org.apache.tools.ant.types.Path;
import org.apache.tools.ant.types.Reference;
import org.apache.tools.ant.util.ScriptRunnerBase;
import org.apache.tools.ant.util.ScriptRunnerHelper;

/**
 * This is a {@link ProjectComponent} that has script support built in
 * Use it as a foundation for scriptable things.
 */
public abstract class AbstractScriptComponent extends ProjectComponent {
    /**
     * script runner helper
     */
    private ScriptRunnerHelper helper = new ScriptRunnerHelper();

    /**
     * script runner.
     */
    private ScriptRunnerBase   runner = null;

    /**
     * Set the project.
     * @param project the owner of this component.
     */
    @Override
    public void setProject(Project project) {
        super.setProject(project);
        helper.setProjectComponent(this);
    }

    /**
     * Get our script runner
     * @return the runner
     */
    public ScriptRunnerBase getRunner() {
        initScriptRunner();
        return runner;
    }

    /**
     * Load the script from an external file; optional.
     *
     * @param file the file containing the script source.
     */
    public void setSrc(File file) {
        helper.setSrc(file);
    }

    /**
     * The script text.
     *
     * @param text a component of the script text to be added.
     */
    public void addText(String text) {
        helper.addText(text);
    }

    /**
     * Defines the manager.
     *
     * @param manager the scripting manager.
     */
    public void setManager(String manager) {
        helper.setManager(manager);
    }

    /**
     * Defines the language (required).
     *
     * @param language the scripting language name for the script.
     */
    public void setLanguage(String language) {
        helper.setLanguage(language);
    }

    /**
     * Initialize the script runner. Calls this before running the system
     */
    protected void initScriptRunner() {
        if (runner != null) {
            return;
        }
        helper.setProjectComponent(this);
        runner = helper.getScriptRunner();
    }
    /**
     * Set the classpath to be used when searching for classes and resources.
     *
     * @param classpath an Ant Path object containing the search path.
     */
    public void setClasspath(Path classpath) {
        helper.setClasspath(classpath);
    }

    /**
     * Classpath to be used when searching for classes and resources.
     *
     * @return an empty Path instance to be configured by Ant.
     */
    public Path createClasspath() {
        return helper.createClasspath();
    }

    /**
     * Set the classpath by reference.
     *
     * @param r a Reference to a Path instance to be used as the classpath
     *          value.
     */
    public void setClasspathRef(Reference r) {
        helper.setClasspathRef(r);
    }

    /**
     * Run a script
     * @param execName name of the script
     */
    protected void executeScript(String execName) {
        getRunner().executeScript(execName);
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
     * @since Ant 1.8.0
     */
    public void setSetBeans(boolean setBeans) {
        helper.setSetBeans(setBeans);
    }

    /**
     * Set the encoding of the script from an external file; optional.
     *
     * @param encoding the encoding of the file containing the script source.
     * @since Ant 1.10.2
     */
    public void setEncoding(String encoding) {
        helper.setEncoding(encoding);
    }
}
