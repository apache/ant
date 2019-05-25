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
package org.apache.tools.ant.util;

import java.io.File;

import org.apache.tools.ant.ProjectComponent;
import org.apache.tools.ant.types.Path;
import org.apache.tools.ant.types.Reference;
import org.apache.tools.ant.types.ResourceCollection;
import org.apache.tools.ant.types.resources.Union;

/**
 * A class to help in creating, setting and getting script runners.
 */
public class ScriptRunnerHelper {
    private ClasspathUtils.Delegate cpDelegate = null;
    private File    srcFile;
    private String  encoding;
    private String  manager = "auto";
    private String  language;
    private String  text;
    private boolean compiled = false;
    private boolean setBeans = true;
    private ProjectComponent projectComponent;
    private ClassLoader scriptLoader = null;
    private Union resources = new Union();

    /**
     * Set the project component associated with this helper.
     * @param component the project component that owns this helper.
     */
    public void setProjectComponent(ProjectComponent component) {
        this.projectComponent = component;
    }

    /**
     * Create and set text on a script.
     * @return the created or reused script runner.
     */
    public ScriptRunnerBase getScriptRunner() {
        ScriptRunnerBase runner = getRunner();
        runner.setCompiled(compiled);

        if (encoding != null) {
            // set it first, because runner.setSrc() loads immediately the file
            runner.setEncoding(encoding);
        }
        if (srcFile != null) {
            runner.setSrc(srcFile);
        }
        if (text != null) {
            runner.addText(text);
        }
        if (resources != null) {
            runner.loadResources(resources);
        }
        if (setBeans) {
            runner.bindToComponent(projectComponent);
        } else {
            runner.bindToComponentMinimum(projectComponent);
        }
        return runner;
    }

    /**
     * Classpath to be used when searching for classes and resources.
     *
     * @return an empty Path instance to be configured by Ant.
     */
    public Path createClasspath() {
        return getClassPathDelegate().createClasspath();
    }

    /**
     * Set the classpath to be used when searching for classes and resources.
     *
     * @param classpath an Ant Path object containing the search path.
     */
    public void setClasspath(Path classpath) {
        getClassPathDelegate().setClasspath(classpath);
    }

    /**
     * Set the classpath by reference.
     *
     * @param r a Reference to a Path instance to be used as the classpath
     *          value.
     */
    public void setClasspathRef(Reference r) {
        getClassPathDelegate().setClasspathref(r);
    }

    /**
     * Load the script from an external file; optional.
     *
     * @param file the file containing the script source.
     */
    public void setSrc(File file) {
        this.srcFile = file;
    }

    /**
     * Get the external script file; optional.
     * @return the file containing the script source.
     * @since Ant 1.10.2
     */
    public File getSrc() {
        return srcFile;
    }

    /**
     * Set the encoding of the script from an external file; optional.
     *
     * @param encoding the encoding of the file containing the script source.
     * @since Ant 1.10.2
     */
    public void setEncoding(String encoding) {
        this.encoding = encoding;
    }

    /**
     * Get the external file encoding.
     * @return the encoding of the file containing the script source.
     * @since Ant 1.10.2
     */
    public String getEncoding() {
        return encoding;
    }

    /**
     * Add script text.
     *
     * @param text a component of the script text to be added.
     */
    public void addText(String text) {
        this.text = text;
    }

    /**
     * Defines the script manager - defaults to "auto".
     *
     * @param manager the scripting manager - "bsf" or "javax" or "auto"
     */
    public void setManager(String manager) {
        this.manager = manager;
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
     * Get the language.
     * @return the scripting language.
     */
    public String getLanguage() {
        return language;
    }

    /**
     * Enable the compilation of the script if possible.
     * If this is true and the compilation feature is available in
     * the script engine, the script is compiled before the first
     * evaluation, and should be cached for future evaluations.
     * Otherwise, the script is evaluated each time.
     * The default is false.
     *
     * @param compiled the value to set.
     */
    public void setCompiled(boolean compiled) {
        this.compiled = compiled;
    }

    /**
     * Get the compilation feature.
     * @return the compilation feature.
     */
    public boolean getCompiled() {
        return this.compiled;
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

    /**
     * Used when called by scriptdef.
     * @param loader the loader used by scriptdef.
     */
    public void setClassLoader(ClassLoader loader) {
        scriptLoader = loader;
    }

    private synchronized ClassLoader generateClassLoader() {
        if (scriptLoader != null) {
            return scriptLoader;
        }
        if (cpDelegate == null) {
            scriptLoader = getClass().getClassLoader();
            return scriptLoader;
        }
        scriptLoader = cpDelegate.getClassLoader();
        return scriptLoader;
    }

    private ClasspathUtils.Delegate getClassPathDelegate() {
        if (cpDelegate == null) {
            if (projectComponent == null) {
                throw new IllegalStateException("Can't access classpath without a project component");
            }
            cpDelegate = ClasspathUtils.getDelegate(projectComponent);
        }
        return cpDelegate;
    }

    /**
     * Get a script runner.
     */
    private ScriptRunnerBase getRunner() {
        return new ScriptRunnerCreator(projectComponent.getProject()).createRunner(
                manager, language, generateClassLoader());
    }

    /**
     * Add any source resource.
     *
     * @param resource source of script
     * @since Ant 1.7.1
     */
    public void add(ResourceCollection resource) {
        resources.add(resource);
    }
}
