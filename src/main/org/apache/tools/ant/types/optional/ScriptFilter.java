/*
 * Copyright  2003-2004 The Apache Software Foundation
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

import org.apache.tools.ant.filters.TokenFilter;
import java.io.File;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.util.ScriptRunner;


/**
 * Most of this is CAP (Cut And Paste) from the Script task
 * ScriptFilter class, implements TokenFilter.Filter
 * for scripts to use.
 * This provides the same beans as the Script Task
 * to a script.
 * The script is meant to use get self.token and
 * set self.token in the reply.
 *
 *
 * @since Ant 1.6
 */
public class ScriptFilter extends TokenFilter.ChainableReaderFilter {
    /** Has this object been initialized ? */
    private boolean initialized = false;
    /** the token used by the script */
    private String token;

    private ScriptRunner runner = new ScriptRunner();

    /**
     * Defines the language (required).
     *
     * @param language the scripting language name for the script.
     */
    public void setLanguage(String language) {
        runner.setLanguage(language);
    }

    /**
     * Initialize.
     *
     * @exception BuildException if someting goes wrong
     */
    private void init() throws BuildException {
        if (initialized) {
            return;
        }
        initialized = true;

        runner.addBeans(getProject().getProperties());
        runner.addBeans(getProject().getUserProperties());
        runner.addBeans(getProject().getTargets());
        runner.addBeans(getProject().getReferences());

        runner.addBean("project", getProject());
        runner.addBean("self", this);
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
    public String filter(String token) {
        init();
        setToken(token);
        runner.executeScript("<ANT-Filter>");
        return getToken();
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
}
