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

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.ProjectComponent;
import org.apache.tools.ant.types.Resource;
import org.apache.tools.ant.types.ResourceCollection;
import org.apache.tools.ant.types.resources.PropertyResource;
import org.apache.tools.ant.types.resources.StringResource;

/**
 * This is a common abstract base case for script runners.
 * These classes need to implement executeScript, evaluateScript
 * and supportsLanguage.
 * @since Ant 1.7.0
 */
public abstract class ScriptRunnerBase {
    /** Whether to keep the engine between calls to execute/eval */
    private boolean keepEngine = false;

    /** Script language */
    private String language;

    /** Script content */
    private String script = "";

    private String encoding;

    /** Enable script compilation. */
    private boolean compiled;

    /** Project this runner is used in */
    private Project project;

    /** Classloader to be used when running the script. */
    private ClassLoader scriptLoader;

    /** Beans to be provided to the script */
    private final Map<String, Object> beans = new HashMap<>();

    /**
     * Add a list of named objects to the list to be exported to the script
     *
     * @param dictionary a map of objects to be placed into the script context
     *        indexed by String names.
     */
    public void addBeans(Map<String, ?> dictionary) {
        dictionary.forEach((k, v) -> {
            try {
                addBean(k, v);
            } catch (BuildException ex) {
                // The key is in the dictionary but cannot be retrieved
                // This is usually due references that refer to tasks
                // that have not been taskdefed in the current run.
                // Ignore
            }
        });
    }

    /**
     * Add a single object into the script context.
     *
     * @param key the name in the context this object is to stored under.
     * @param bean the object to be stored in the script context.
     */
    public void addBean(String key, Object bean) {
        if (!key.isEmpty() && Character.isJavaIdentifierStart(key.charAt(0))
                && key.chars().skip(1).allMatch(Character::isJavaIdentifierPart)) {
            beans.put(key, bean);
        }
    }

    /**
     * Get the beans used for the script.
     * @return the map of beans.
     */
    protected Map<String, Object> getBeans() {
        return beans;
    }

    /**
     * Do the work.
     * @param execName the name that will be passed to BSF for this script
     *        execution.
     */
    public abstract void executeScript(String execName);

    /**
     * Evaluate the script.
     * @param execName the name that will be passed to the
     *                 scripting engine for this script execution.
     * @return the result of evaluating the script.
     */
    public abstract Object evaluateScript(String execName);

    /**
     * Check if a script engine can be created for
     * this language.
     * @return true if a script engine can be created, false
     *              otherwise.
     */
    public abstract boolean supportsLanguage();

    /**
     * Get the name of the manager prefix used for this
     * scriptrunner.
     * @return the prefix string.
     */
    public abstract String getManagerName();

    /**
     * Defines the language (required).
     * @param language the scripting language name for the script.
     */
    public void setLanguage(String language) {
        this.language = language;
    }

    /**
     * Get the script language
     * @return the script language
     */
    public String getLanguage() {
        return language;
    }

    /**
     * Set the script classloader.
     * @param classLoader the classloader to use.
     */
    public void setScriptClassLoader(ClassLoader classLoader) {
        this.scriptLoader = classLoader;
    }

    /**
     * Get the classloader used to load the script engine.
     * @return the classloader.
     */
    protected ClassLoader getScriptClassLoader() {
        return scriptLoader;
    }

    /**
     * Whether to keep the script engine between calls.
     * @param keepEngine if true, keep the engine.
     */
    public void setKeepEngine(boolean keepEngine) {
        this.keepEngine = keepEngine;
    }

    /**
     * Get the keep engine attribute.
     * @return the attribute.
     */
    public boolean getKeepEngine() {
        return keepEngine;
    }

    /**
     * Whether to use script compilation if available.
     * @since Ant 1.10.2
     * @param compiled if true, compile the script if possible.
     */
    public final void setCompiled(boolean compiled) {
        this.compiled = compiled;
    }

    /**
     * Get the compiled attribute.
     * @since Ant 1.10.2
     * @return the attribute.
     */
    public final boolean getCompiled() {
        return compiled;
    }

    /**
     * Set encoding of the script from an external file; optional.
     * @since Ant 1.10.2
     * @param encoding  encoding of the external file containing the script source.
     */
    public void setEncoding(String encoding) {
        this.encoding = encoding;
    }

    /**
     * Load the script from an external file; optional.
     * @param file the file containing the script source.
     */
    public void setSrc(File file) {
        String filename = file.getPath();
        if (!file.exists()) {
            throw new BuildException("file " + filename + " not found.");
        }

        try (InputStream in = Files.newInputStream(file.toPath())) {
            final Charset charset = null == encoding ? Charset.defaultCharset()
                : Charset.forName(encoding);

            readSource(in, filename, charset);
        } catch (IOException e) {
            //this can only happen if the file got deleted a short moment ago
            throw new BuildException("file " + filename + " not found.", e);
        }
    }

    /**
     * Read some source in from the given reader
     * @param in the input stream to pass into a buffered reader.
     * @param name the name to use in error messages
     * @param charset the encoding for the reader, may be null.
     */
    private void readSource(InputStream in, String name, Charset charset) {
        try (Reader reader =
            new BufferedReader(new InputStreamReader(in, charset))) {
            script += FileUtils.safeReadFully(reader);
        } catch (IOException ex) {
            throw new BuildException("Failed to read " + name, ex);
        }
    }

    /**
     * Add a resource to the source list.
     * @since Ant 1.7.1
     * @param sourceResource the resource to load
     * @throws BuildException if the resource cannot be read
     */
    public void loadResource(Resource sourceResource) {
        if (sourceResource instanceof StringResource) {
            script += ((StringResource) sourceResource).getValue();
            return;
        }
        if (sourceResource instanceof PropertyResource) {
            script += ((PropertyResource) sourceResource).getValue();
            return;
        }

        String name = sourceResource.toLongString();
        try (InputStream in = sourceResource.getInputStream()) {
            readSource(in, name, Charset.defaultCharset());
        } catch (IOException e) {
            throw new BuildException("Failed to open " + name, e);
        } catch (UnsupportedOperationException e) {
            throw new BuildException(
                "Failed to open " + name + " - it is not readable", e);
        }
    }

    /**
     * Add all resources in a resource collection to the source list.
     * @since Ant 1.7.1
     * @param collection the resource to load
     * @throws BuildException if a resource cannot be read
     */
    public void loadResources(ResourceCollection collection) {
        collection.forEach(this::loadResource);
    }

    /**
     * Set the script text. Properties in the text are not expanded!
     *
     * @param text a component of the script text to be added.
     */
    public void addText(String text) {
        script += text;
    }

    /**
     * Get the current script text content.
     * @return the script text.
     */
    public String getScript() {
        return script;
    }

    /**
     * Clear the current script text content.
     */
    public void clearScript() {
        this.script = "";
    }

    /**
     * Set the project for this runner.
     * @param project the project.
     */
    public void setProject(Project project) {
        this.project = project;
    }

    /**
     * Get the project for this runner.
     * @return the project.
     */
    public Project getProject() {
        return project;
    }

    /**
     * Bind the runner to a project component.
     * Properties, targets and references are all added as beans;
     * project is bound to project, and self to the component.
     * @param component to become <code>self</code>
     */
    public void bindToComponent(ProjectComponent component) {
        project = component.getProject();
        addBeans(project.getProperties());
        addBeans(project.getUserProperties());
        addBeans(project.getCopyOfTargets());
        addBeans(project.getCopyOfReferences());
        addBean("project", project);
        addBean("self", component);
    }

    /**
     * Bind the runner to a project component.
     * The project and self are the only beans set.
     * @param component to become <code>self</code>
     */
    public void bindToComponentMinimum(ProjectComponent component) {
        project = component.getProject();
        addBean("project", project);
        addBean("self", component);
    }

    /**
     * Check if the language attribute is set.
     * @throws BuildException if it is not.
     */
    protected void checkLanguage() {
        if (language == null) {
            throw new BuildException("script language must be specified");
        }
    }

    /**
     * Replace the current context classloader with the
     * script context classloader.
     * @return the current context classloader.
     */
    protected ClassLoader replaceContextLoader() {
        ClassLoader origContextClassLoader =
            Thread.currentThread().getContextClassLoader();
        if (getScriptClassLoader() == null) {
            setScriptClassLoader(getClass().getClassLoader());
        }
        Thread.currentThread().setContextClassLoader(getScriptClassLoader());
        return origContextClassLoader;
    }

    /**
     * Restore the context loader with the original context classloader.
     *
     * script context loader.
     * @param origLoader the original context classloader.
     */
    protected void restoreContextLoader(ClassLoader origLoader) {
        Thread.currentThread().setContextClassLoader(
                 origLoader);
    }
}
