/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2002 The Apache Software Foundation.  All rights
 * reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution, if
 *    any, must include the following acknowlegement:
 *       "This product includes software developed by the
 *        Apache Software Foundation (http://www.apache.org/)."
 *    Alternately, this acknowlegement may appear in the software itself,
 *    if and wherever such third-party acknowlegements normally appear.
 *
 * 4. The names "The Jakarta Project", "Ant", and "Apache Software
 *    Foundation" must not be used to endorse or promote products derived
 *    from this software without prior written permission. For written
 *    permission, please contact apache@apache.org.
 *
 * 5. Products derived from this software may not be called "Apache"
 *    nor may "Apache" appear in their names without prior written
 *    permission of the Apache Group.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE APACHE SOFTWARE FOUNDATION OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Apache Software Foundation.  For more
 * information on the Apache Software Foundation, please see
 * <http://www.apache.org/>.
 */
package org.apache.ant.antlib.script;
import java.util.HashMap;
import java.util.Map;

import org.apache.ant.common.antlib.AntContext;
import org.apache.ant.common.antlib.StandardLibFactory;
import org.apache.ant.common.service.ComponentService;
import org.apache.ant.common.util.AntException;

/**
 * The ScriptFactory class is a factory for the Scripting tasks. It stores
 * the scripts as they are defined
 *
 * @author Conor MacNeill
 * @created 11 February 2002
 */
public class ScriptFactory extends StandardLibFactory {
    /**
     * An inner class used to record information about defined scripts.
     *
     * @author Conor MacNeill
     * @created 11 February 2002
     */
    private static class ScriptInfo {
        /** the scripting langauge to use */
        private String language;
        /** the script itself */
        private String script;

        /**
         * Constructor for the ScriptInfo object
         *
         * @param language the language the script is written in
         * @param script the script
         */
        public ScriptInfo(String language, String script) {
            this.language = language;
            this.script = script;
        }

        /**
         * Gets the language of the Script
         *
         * @return the language value
         */
        public String getLanguage() {
            return language;
        }

        /**
         * Gets the script.
         *
         * @return the script text
         */
        public String getScript() {
            return script;
        }
    }

    /** The core's Component Service instance */
    private ComponentService componentService;

    /** the scripts that have been defined */
    private Map scripts = new HashMap();

    /**
     * Initialise the factory
     *
     * @param context the factory's context
     * @exception AntException if the factory cannot be initialized
     */
    public void init(AntContext context) throws AntException {
        super.init(context);
        componentService = (ComponentService)
            context.getCoreService(ComponentService.class);
        try {
            Class.forName("com.ibm.bsf.BSFManager");
        } catch (ClassNotFoundException e) {
            throw new ScriptException("The script Ant library requires "
                 + "bsf.jar to be available");
        } catch (NoClassDefFoundError e) {
            throw new ScriptException("The script Ant library requires "
                 + "bsf.jar to be available. The class " + e.getMessage()
                 + "appears to be missing");
        }
    }

    /**
     * Create an instance of the given component class
     *
     * @param componentClass the class for which an instance is required
     * @param localName the name within the library undeer which the task is
     *      defined
     * @return an instance of the required class
     * @exception InstantiationException if the class cannot be instantiated
     * @exception IllegalAccessException if the instance cannot be accessed
     * @exception AntException if there is a problem creating the task
     */
    public Object createComponent(Class componentClass, String localName)
         throws InstantiationException, IllegalAccessException, AntException {
        Object component = super.createComponent(componentClass, localName);

        if (component instanceof ScriptDef) {
            ScriptDef scriptDef = (ScriptDef) component;
            scriptDef.setFactory(this);
        } else if (component instanceof ScriptBase) {
            ScriptBase scriptBase = (ScriptBase) component;
            scriptBase.setFactory(this);
            scriptBase.setScriptName(localName);
        }
        return component;
    }

    /**
     * Get the script language of a script
     *
     * @param scriptName the name the script is defined under
     * @return the script language name
     */
    protected String getScriptLanguage(String scriptName) {
        ScriptInfo scriptInfo = (ScriptInfo) scripts.get(scriptName);
        return scriptInfo.getLanguage();
    }

    /**
     * Get a script.
     *
     * @param scriptName the name the script is defined under
     * @return the script text
     */
    protected String getScript(String scriptName) {
        ScriptInfo scriptInfo = (ScriptInfo) scripts.get(scriptName);
        return scriptInfo.getScript();
    }

    /**
     * Define a script
     *
     * @param name the name the script is to be defined under
     * @param language the language of the scripr
     * @param script the script text
     * @exception AntException if the script cannot be defined
     */
    protected void defineScript(String name, String language, String script)
         throws AntException {
        ScriptInfo scriptDefinition = new ScriptInfo(language, script);
        scripts.put(name, scriptDefinition);
        componentService.taskdef(this, ScriptBase.class.getClassLoader(),
            name, ScriptBase.class.getName());
    }
}

