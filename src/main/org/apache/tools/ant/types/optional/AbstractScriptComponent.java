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

import org.apache.tools.ant.ProjectComponent;
import org.apache.tools.ant.util.ScriptRunner;

import java.io.File;

/**
 * This is a {@link ProjectComponent} that has script support built in
 * Use it as a foundation for scriptable things.
 */
public abstract class AbstractScriptComponent extends ProjectComponent {
    /**
     * script runner
     */
    private ScriptRunner runner = new ScriptRunner();

    /**
     * Get our script runner
     * @return
     */ 
    public ScriptRunner getRunner() {
        return runner;
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
     * Defines the language (required).
     *
     * @param language the scripting language name for the script.
     */
    public void setLanguage(String language) {
        runner.setLanguage(language);
    }

    /**
     * Initialize the script runner. Calls this before running the system
     */ 
    protected void initScriptRunner() {
        getRunner().bindToComponent(this);
    }

    /**
     * Run a script
     * @param execName name of the script
     */
    protected void executeScript(String execName) {
        getRunner().executeScript(execName);
    }
}
