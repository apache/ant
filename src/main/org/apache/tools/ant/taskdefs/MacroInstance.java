/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2003 The Apache Software Foundation.  All rights
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

package org.apache.tools.ant.taskdefs;

import java.util.ArrayList;
import java.util.List;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Enumeration;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.DynamicConfigurator;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.TaskContainer;
import org.apache.tools.ant.UnknownElement;
import org.apache.tools.ant.RuntimeConfigurable;

/**
 * The class to be placed in the ant type definition.
 * It is given a pointer to the template definition,
 * and makes a copy of the unknown element, substituting
 * the the parameter values in attributes and text.
 * @author Peter Reilly
 * @since Ant 1.6
 */
public class MacroInstance extends Task implements DynamicConfigurator {
    private MacroDef template;
    private Map      map = new HashMap();
    private Map      elements = new HashMap();
    private Hashtable localProperties = new Hashtable();

    /**
     * Called from MacroDef.MyAntTypeDefinition#create()
     *
     * @param template a <code>MacroDef</code> value
     */
    protected void setTemplate(MacroDef template) {
        this.template = template;
    }

    /**
     * A parameter name value pair as a xml attribute.
     *
     * @param name the name of the attribute
     * @param value the value of the attribute
     */
    public void setDynamicAttribute(String name, String value) {
        map.put(name, value);
    }

    /**
     * Add an element.
     * @param name the name of the element
     * @return an inner Element type
     * @throws BuildException if the name is not known or if this element
     *                        has already been seen
     */
    public Object createDynamicElement(String name) throws BuildException {
        if (template.getElements().get(name) == null) {
            throw new BuildException("unsupported element " + name);
        }
        if (elements.get(name) != null) {
            throw new BuildException("Element " + name + " already present");
        }
        Element ret = new Element();
        elements.put(name, ret);
        return ret;
    }

    /**
     * Embedded element in macro instance
     */
    public static class Element implements TaskContainer {
        private List unknownElements = new ArrayList();

        /**
         * Add an unknown element (to be snipped into the template instance)
         *
         * @param nestedTask an unknown element
         */
        public void addTask(Task nestedTask) {
            unknownElements.add(nestedTask);
        }

        /**
         * @return the list of unknown elements
         */
        public List getUnknownElements() {
            return unknownElements;
        }
    }

    private static String macroSubs(String s, Map macroMapping) {
        StringBuffer ret = new StringBuffer();
        StringBuffer macroName = new StringBuffer();
        boolean inMacro = false;
        for (int i = 0; i < s.length(); ++i) {
            if (s.charAt(i) == '$') {
                inMacro = true;
            } else {
                if (inMacro) {
                    if (s.charAt(i) == '{') {
                        continue;
                    } else if (s.charAt(i) == '}') {
                        String name = macroName.toString();
                        String value = (String) macroMapping.get(name);
                        if (value == null) {
                            ret.append("${" + name + "}");
                        } else {
                            ret.append(value);
                        }
                        macroName = new StringBuffer();
                        inMacro = false;
                    } else {
                        macroName.append(s.charAt(i));
                    }
                } else {
                    ret.append(s.charAt(i));
                }
            }
        }

        return ret.toString();
    }

    private UnknownElement copy(UnknownElement ue) {
        UnknownElement ret = new UnknownElement(ue.getTag());
        ret.setNamespace(ue.getNamespace());
        ret.setProject(getProject());
        ret.setQName(ue.getQName());
        ret.setTaskName(ue.getTaskName());
        ret.setLocation(ue.getLocation());
        ret.setOwningTarget(getOwningTarget());
        RuntimeConfigurable rc = new RuntimeConfigurable(
            ret, ue.getTaskName());
        rc.setPolyType(ue.getWrapper().getPolyType());
        Map map = ue.getWrapper().getAttributeMap();
        for (Iterator i = map.entrySet().iterator(); i.hasNext();) {
            Map.Entry entry = (Map.Entry) i.next();
            rc.setAttribute(
                (String) entry.getKey(),
                macroSubs((String) entry.getValue(), localProperties));
        }
        rc.addText(macroSubs(ue.getWrapper().getText().toString(),
                             localProperties));

        Enumeration e = ue.getWrapper().getChildren();
        while (e.hasMoreElements()) {
            RuntimeConfigurable r = (RuntimeConfigurable) e.nextElement();
            UnknownElement unknownElement = (UnknownElement) r.getProxy();
            String tag = unknownElement.getTag();
            MacroDef.TemplateElement templateElement =
                (MacroDef.TemplateElement) template.getElements().get(tag);
            if (templateElement == null) {
                UnknownElement child = copy(unknownElement);
                rc.addChild(child.getWrapper());
                ret.addChild(child);
            } else {
                Element element = (Element) elements.get(tag);
                if (element == null) {
                    if (!templateElement.isOptional()) {
                        throw new BuildException(
                            "Required nested element "
                            + templateElement.getName() + " missing");
                    }
                    continue;
                }
                for (Iterator i = element.getUnknownElements().iterator();
                     i.hasNext();) {
                    UnknownElement child = (UnknownElement) i.next();
                    rc.addChild(child.getWrapper());
                    ret.addChild(child);
                }
            }
        }
        return ret;
    }

    /**
     * Execute the templates instance.
     * Copies the unknown element, substitutes the parameters,
     * and calls perform on the unknown element.
     *
     */
    public void execute() {
        localProperties = new Hashtable();
        Set copyKeys = new HashSet(map.keySet());
        for (int i = 0; i < template.getParams().size(); ++i) {
            MacroDef.Param param = (MacroDef.Param) template.getParams().get(i);
            String value = (String) map.get(param.getName());
            if (value == null) {
                value = param.getDefault();
            }
            if (value == null) {
                throw new BuildException(
                    "required parameter " + param.getName() + " not set");
            }
            localProperties.put(param.getName(), value);
            copyKeys.remove(param.getName());
        }
        if (copyKeys.size() != 0) {
            throw new BuildException(
                "Unknown attribute" + (copyKeys.size() > 1 ? "s " : " ")
                + copyKeys);
        }

        // need to set the project on unknown element
        UnknownElement c = copy(template.getNestedTask());
        c.init();
        c.perform();
    }
}
