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
import org.apache.tools.ant.types.Path;
import org.apache.tools.ant.types.Reference;
import org.apache.tools.ant.types.selectors.BaseSelector;
import org.apache.tools.ant.util.ScriptRunnerBase;
import org.apache.tools.ant.util.ScriptRunnerHelper;

/**
 * Selector that lets you run a script with selection logic inline
 * @since Ant1.7
 */
public class ScriptSelector extends BaseSelector {

    /**
     * script runner helper
     */
    private ScriptRunnerHelper helper = new ScriptRunnerHelper();

    /**
     * script runner
     */
    private ScriptRunnerBase runner;

    /**
     * fields updated for every selection
     */
    private File basedir;
    private String filename;
    private File file;

    /**
     * selected flag
     */
    private boolean selected;

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
     * Initialize on demand.
     *
     * @throws BuildException
     *          if something goes wrong
     */
    private void init() throws BuildException {
        if (runner != null) {
            return;
        }
        runner = helper.getScriptRunner();
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
     */
    public void setSetBeans(boolean setBeans) {
        helper.setSetBeans(setBeans);
    }

    /**
     * Method that each selector will implement to create their selection
     * behaviour. If there is a problem with the setup of a selector, it can
     * throw a BuildException to indicate the problem.
     *
     * @param basedir  A java.io.File object for the base directory
     * @param filename The name of the file to check
     * @param file     A File object for this filename
     *
     * @return whether the file should be selected or not
     */
    @Override
    public boolean isSelected(File basedir, String filename, File file) {
        init();
        setSelected(true);
        this.file = file;
        this.basedir = basedir;
        this.filename = filename;
        runner.addBean("basedir", basedir);
        runner.addBean("filename", filename);
        runner.addBean("file", file);
        runner.executeScript("ant_selector");
        return isSelected();
    }

    /**
     * get the base directory
     * @return the base directory
     */
    public File getBasedir() {
        return basedir;
    }

    /**
     * get the filename of the file
     * @return the filename of the file that is currently been tested
     */
    public String getFilename() {
        return filename;
    }

    /**
     * get the file that is currently to be tested
     * @return the file that is currently been tested
     */
    public File getFile() {
        return file;
    }

    /**
     * get state of selected flag
     * @return the selected flag
     */
    public boolean isSelected() {
        return selected;
    }

    /**
     * set the selected state
     * Intended for script use, not as an Ant attribute
     * @param selected the selected state
     */
    public void setSelected(boolean selected) {
        this.selected = selected;
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
