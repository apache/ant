/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2000-2003 The Apache Software Foundation.  All rights
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
 * 4. The names "Ant" and "Apache Software
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
package org.apache.tools.ant.taskdefs.optional.script;

import org.apache.tools.ant.Task;
import org.apache.tools.ant.MagicNames;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.DynamicConfigurator;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;

/**
 * The script execution class. This class finds the defining script task
 * and passes control to that task's executeScript method. This class
 * implements the TaskCOntainer interface primarily to stop Ant's core from
 * configuring the nested elements - this is done by the script task itself.
 *
 * @author Conor MacNeill
 * @since Ant 1.6
 */
public class ScriptDefBase extends Task implements DynamicConfigurator {

    /** Nested elements */
    private Map nestedElementMap = new HashMap();

    /** Attributes */
    private Map attributes = new HashMap();

    /**
     * Locate the script defining task and execute the script by passing
     * control to it
     */
    public void execute() {
        getScript().executeScript(attributes, nestedElementMap);
    }

    private ScriptDef getScript() {
        String name = getTaskType();
        Map scriptRepository
            = (Map) getProject().getReference(MagicNames.SCRIPT_REPOSITORY);
        if (scriptRepository == null) {
            throw new BuildException("Script repository not found for " + name);
        }

        ScriptDef definition = (ScriptDef) scriptRepository.get(getTaskType());
        if (definition == null) {
            throw new BuildException("Script definition not found for " + name);
        }
        return definition;
    }

    /**
     * Create a nested element
     *
     * @param name the nested element name
     * @return the element to be configured
     */
    public Object createDynamicElement(String name)  {
        List nestedElementList = (List) nestedElementMap.get(name);
        if (nestedElementList == null) {
            nestedElementList = new ArrayList();
            nestedElementMap.put(name, nestedElementList);
        }
        Object element = getScript().createNestedElement(name);
        nestedElementList.add(element);
        return element;
    }

    /**
     * Set a task attribute
     *
     * @param name the attribute name.
     * @param value the attribute's string value
     */
    public void setDynamicAttribute(String name, String value) {
        ScriptDef definition = getScript();
        if (!definition.isAttributeSupported(name)) {
                throw new BuildException("<" + getTaskType()
                    + "> does not support the \"" + name + "\" attribute");
        }

        attributes.put(name, value);
    }
}

