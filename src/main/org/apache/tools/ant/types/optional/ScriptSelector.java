/*
 * Copyright  2005 The Apache Software Foundation
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
package org.apache.tools.ant.types.optional;

import org.apache.tools.ant.types.selectors.BaseSelector;
import org.apache.tools.ant.util.ScriptRunner;
import org.apache.tools.ant.BuildException;

import java.io.File;

/**
 * Selector that lets you run a script with selection logic inline
 * @since Ant1.7
 */
public class ScriptSelector extends BaseSelector {

    /**
     * Has this object been initialized ?
     */
    private boolean initialized = false;

    /**
     * script runner
     */
    private ScriptRunner runner = new ScriptRunner();

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
     * Defines the language (required).
     *
     * @param language the scripting language name for the script.
     */
    public void setLanguage(String language) {
        runner.setLanguage(language);
    }

    /**
     * Initialize on demand.
     *
     * @throws org.apache.tools.ant.BuildException
     *          if someting goes wrong
     */
    private void init() throws BuildException {
        if (initialized) {
            return;
        }
        initialized = true;
        runner.bindToComponent(this);
    }


    /**
     * Load the script from an external file ; optional.
     *
     * @param file the file containing the script source.
     */
    public void setSrc(File file) {
        runner.setSrc(file);
    }

    /**
     * The script text.
     *
     * @param text a component of the script text to be added.
     */
    public void addText(String text) {
        runner.addText(text);
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

}
