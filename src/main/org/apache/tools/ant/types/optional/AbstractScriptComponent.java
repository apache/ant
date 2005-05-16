/** (C) Copyright 2005 Hewlett-Packard Development Company, LP

 This library is free software; you can redistribute it and/or
 modify it under the terms of the GNU Lesser General Public
 License as published by the Free Software Foundation; either
 version 2.1 of the License, or (at your option) any later version.

 This library is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 Lesser General Public License for more details.

 You should have received a copy of the GNU Lesser General Public
 License along with this library; if not, write to the Free Software
 Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA

 For more information: www.smartfrog.org

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
