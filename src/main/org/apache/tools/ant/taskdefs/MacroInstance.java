/* 
 * Copyright  2003-2004 Apache Software Foundation
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

package org.apache.tools.ant.taskdefs;

import java.util.ArrayList;
import java.util.List;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Enumeration;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.DynamicConfigurator;
import org.apache.tools.ant.ProjectHelper;
import org.apache.tools.ant.RuntimeConfigurable;
import org.apache.tools.ant.Target;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.TaskContainer;
import org.apache.tools.ant.UnknownElement;

/**
 * The class to be placed in the ant type definition.
 * It is given a pointer to the template definition,
 * and makes a copy of the unknown element, substituting
 * the parameter values in attributes and text.
 * @author Peter Reilly
 * @since Ant 1.6
 */
public class MacroInstance extends Task implements DynamicConfigurator {
    private MacroDef macroDef;
    private Map      map = new HashMap();
    private Map      nsElements = null;
    private Map      presentElements = new HashMap();
    private Hashtable localProperties = new Hashtable();
    private String    text = null;

    /**
     * Called from MacroDef.MyAntTypeDefinition#create()
     *
     * @param macroDef a <code>MacroDef</code> value
     */
    public void setMacroDef(MacroDef macroDef) {
        this.macroDef = macroDef;
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
        if (getNsElements().get(name) == null) {
            throw new BuildException("unsupported element " + name);
        }
        if (presentElements.get(name) != null) {
            throw new BuildException("Element " + name + " already present");
        }
        Element ret = new Element();
        presentElements.put(name, ret);
        return ret;
    }

    private Map getNsElements() {
        if (nsElements == null) {
            nsElements = new HashMap();
            for (Iterator i = macroDef.getElements().entrySet().iterator();
                 i.hasNext();) {
                Map.Entry entry = (Map.Entry) i.next();
                nsElements.put((String) entry.getKey(),
                               entry.getValue());
            }
        }
        return nsElements;
    }

    /**
     * Embedded element in macro instance
     */
    public static class Element implements TaskContainer {
        private List unknownElements = new ArrayList();

        /**
         * Add an unknown element (to be snipped into the macroDef instance)
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

    private static final int STATE_NORMAL         = 0;
    private static final int STATE_EXPECT_BRACKET = 1;
    private static final int STATE_EXPECT_NAME    = 2;
    private static final int STATE_EXPECT_EXCAPE  = 3;

    private String macroSubs(String s, Map macroMapping) {
        if (s == null) {
            return null;
        }
        StringBuffer ret = new StringBuffer();
        StringBuffer macroName = null;
        boolean inMacro = false;
        int state = STATE_NORMAL;
        for (int i = 0; i < s.length(); ++i) {
            char ch = s.charAt(i);
            switch (state) {
                case STATE_NORMAL:
                    if (ch == '@') {
                        state = STATE_EXPECT_BRACKET;
                    } else {
                        ret.append(ch);
                    }
                    break;
                case STATE_EXPECT_BRACKET:
                    if (ch == '{') {
                        state = STATE_EXPECT_NAME;
                        macroName = new StringBuffer();
                    } else if (ch == '@') {
                        state = STATE_EXPECT_EXCAPE;
                    } else {
                        state = STATE_NORMAL;
                        ret.append('@');
                        ret.append(ch);
                    }
                    break;
                case STATE_EXPECT_NAME:
                    if (ch == '}') {
                        state = STATE_NORMAL;
                        String name = macroName.toString().toLowerCase(Locale.US);
                        String value = (String) macroMapping.get(name);
                        if (value == null) {
                            ret.append("@{" + name + "}");
                        } else {
                            ret.append(value);
                        }
                        macroName = null;
                    } else {
                        macroName.append(ch);
                    }
                    break;
                case STATE_EXPECT_EXCAPE:
                    state = STATE_NORMAL;
                    if (ch == '{') {
                        ret.append("@");
                    } else {
                        ret.append("@@");
                    }
                    ret.append(ch);
                    break;
                default:
                    break;
            }
        }
        switch (state) {
            case STATE_NORMAL:
                break;
            case STATE_EXPECT_BRACKET:
                ret.append('@');
                break;
            case STATE_EXPECT_NAME:
                ret.append("@{");
                ret.append(macroName.toString());
                break;
            case STATE_EXPECT_EXCAPE:
                ret.append("@@");
                break;
            default:
                break;
        }

        return ret.toString();
    }

    /**
     * Set the text contents for the macro.
     * @param text the text to be added to the macro.
     */

    public void addText(String text) {
        this.text = text;
    }

    private UnknownElement copy(UnknownElement ue) {
        UnknownElement ret = new UnknownElement(ue.getTag());
        ret.setNamespace(ue.getNamespace());
        ret.setProject(getProject());
        ret.setQName(ue.getQName());
        ret.setTaskType(ue.getTaskType());
        ret.setTaskName(ue.getTaskName());
        ret.setLocation(ue.getLocation());
        if (getOwningTarget() == null) {
            Target t = new Target();
            t.setProject(getProject());
            ret.setOwningTarget(t);
        } else {
            ret.setOwningTarget(getOwningTarget());
        }
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
            String tag = unknownElement.getTaskType();
            if (tag != null) {
                tag = tag.toLowerCase(Locale.US);
            }
            MacroDef.TemplateElement templateElement =
                (MacroDef.TemplateElement) getNsElements().get(tag);
            if (templateElement == null) {
                UnknownElement child = copy(unknownElement);
                rc.addChild(child.getWrapper());
                ret.addChild(child);
            } else {
                Element element = (Element) presentElements.get(tag);
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
     * Copies the unknown element, substitutes the attributes,
     * and calls perform on the unknown element.
     *
     */
    public void execute() {
        localProperties = new Hashtable();
        Set copyKeys = new HashSet(map.keySet());
        for (Iterator i = macroDef.getAttributes().iterator(); i.hasNext();) {
            MacroDef.Attribute attribute = (MacroDef.Attribute) i.next();
            String value = (String) map.get(attribute.getName());
            if (value == null) {
                value = attribute.getDefault();
                value = macroSubs(value, localProperties);
            }
            if (value == null) {
                throw new BuildException(
                    "required attribute " + attribute.getName() + " not set");
            }
            localProperties.put(attribute.getName(), value);
            copyKeys.remove(attribute.getName());
        }
        if (copyKeys.contains("id")) {
            copyKeys.remove("id");
        }
        if (macroDef.getText() != null) {
            if (text == null) {
                if (!macroDef.getText().getOptional()) {
                    throw new BuildException(
                        "required text missing");
                }
                text = "";
            }
            if (macroDef.getText().getTrim()) {
                text = text.trim();
            }
            localProperties.put(macroDef.getText().getName(), text);
        } else {
            if (text != null && !text.trim().equals("")) {
                throw new BuildException(
                    "The \"" + getTaskName() + "\" macro does not support"
                    + " nested text data.");
            }
        }
        if (copyKeys.size() != 0) {
            throw new BuildException(
                "Unknown attribute" + (copyKeys.size() > 1 ? "s " : " ")
                + copyKeys);
        }

        // need to set the project on unknown element
        UnknownElement c = copy(macroDef.getNestedTask());
        c.init();
        try {
            c.perform();
        } catch (BuildException ex) {
            throw ProjectHelper.addLocationToBuildException(
                ex, getLocation());
        }
    }
}
