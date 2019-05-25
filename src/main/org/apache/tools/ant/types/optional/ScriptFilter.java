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

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.filters.TokenFilter;
import org.apache.tools.ant.types.Path;
import org.apache.tools.ant.types.Reference;
import org.apache.tools.ant.util.ScriptRunnerBase;
import org.apache.tools.ant.util.ScriptRunnerHelper;

/**
 * Most of this is CAP (Cut And Paste) from the Script task
 * ScriptFilter class, implements TokenFilter.Filter
 * for scripts to use.
 * This provides the same beans as the Script Task
 * to a script.
 * The script is meant to use get self.token and
 * set self.token in the reply.
 *
 * @since Ant 1.6
 */
public class ScriptFilter extends TokenFilter.ChainableReaderFilter {
    /** script runner helper */
    private ScriptRunnerHelper helper = new ScriptRunnerHelper();

    /** script runner. */
    private ScriptRunnerBase   runner = null;

    /** the token used by the script */
    private String token;

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
     * Defines the language (required).
     *
     * @param language the scripting language name for the script.
     */
    public void setLanguage(String language) {
        helper.setLanguage(language);
    }

    /**
     * Initialize.
     *
     * @exception BuildException if something goes wrong
     */
    private void init() throws BuildException {
        if (runner != null) {
            return;
        }
        runner = helper.getScriptRunner();
    }

    /**
     * The current token
     *
     * @param token the string filtered by the script
     */
    public void setToken(String token) {
        this.token = token;
    }

    /**
     * The current token
     *
     * @return the string filtered by the script
     */
    public String getToken() {
        return token;
    }

    /**
     * Called filter the token.
     * This sets the token in this object, calls
     * the script and returns the token.
     *
     * @param token the token to be filtered
     * @return the filtered token
     */
    @Override
    public String filter(String token) {
        init();
        setToken(token);
        runner.executeScript("ant_filter");
        return getToken();
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
