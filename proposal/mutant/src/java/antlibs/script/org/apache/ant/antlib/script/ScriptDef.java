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

import org.apache.ant.common.antlib.AbstractTask;
import org.apache.ant.common.util.AntException;

/**
 * Define a task using a script
 *
 * @author Conor MacNeill
 * @created 11 February 2002
 */
public class ScriptDef extends AbstractTask {
    /** The script factor to use */
    private ScriptFactory factory;

    /** the name by which this script will be activated */
    private String name;

    /** the scripting language used by the script */
    private String language;

    /** the script itself */
    private String script = "";

    /**
     * set the name under which this script will be activated in a build
     * file
     *
     * @param name the name of the script
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Set the scripting language used by this script
     *
     * @param language the scripting language used by this script.
     */
    public void setLanguage(String language) {
        this.language = language;
    }

    /**
     * Define the script. The script itself is stored in the factory where
     * it is retried by the ScriptBase instance
     *
     * @exception AntException if the script cannot be defined
     */
    public void execute() throws AntException {
        // tell the factory about this script, under this name.
        factory.defineScript(name, language, script);
    }

    /**
     * Defines the script.
     *
     * @param text Sets the value for the script variable.
     */
    public void addText(String text) {
        this.script += text;
    }

    /**
     * Set the script factory that will be used to store the script for
     * later execution
     *
     * @param factory the script factory used to store script information.
     */
    protected void setFactory(ScriptFactory factory) {
        this.factory = factory;
    }

}

