/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
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
import org.apache.tools.ant.DynamicAttribute;
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
 * @since Ant 1.6
 */
public class MacroInstance extends Task implements DynamicAttribute, TaskContainer {
    private MacroDef macroDef;
    private Map      map = new HashMap();
    private Map      nsElements = null;
    private Map      presentElements;
    private Hashtable localAttributes;
    private String    text = null;
    private String    implicitTag =     null;
    private List      unknownElements = new ArrayList();

    /**
     * Called from MacroDef.MyAntTypeDefinition#create()
     *
     * @param macroDef a <code>MacroDef</code> value
     */
    public void setMacroDef(MacroDef macroDef) {
        this.macroDef = macroDef;
    }

    /**
     * @return the macro definition object for this macro instance.
     */
    public MacroDef getMacroDef() {
        return macroDef;
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
     * Method present for BC purposes.
     * @param name not used
     * @return nothing
     * @deprecated since 1.6.x.
     * @throws BuildException always
     */
    public Object createDynamicElement(String name) throws BuildException {
        throw new BuildException("Not implemented any more");
    }

    private Map getNsElements() {
        if (nsElements == null) {
            nsElements = new HashMap();
            for (Iterator i = macroDef.getElements().entrySet().iterator();
                 i.hasNext();) {
                Map.Entry entry = (Map.Entry) i.next();
                nsElements.put((String) entry.getKey(),
                               entry.getValue());
                MacroDef.TemplateElement te = (MacroDef.TemplateElement)
                    entry.getValue();
                if (te.isImplicit()) {
                    implicitTag = te.getName();
                }
            }
        }
        return nsElements;
    }

    /**
     * Add a unknownElement for the macro instances nested elements.
     *
     * @param nestedTask a nested element.
     */
    public void addTask(Task nestedTask) {
        unknownElements.add(nestedTask);
    }

    private void processTasks() {
        if (implicitTag != null) {
            return;
        }
        for (Iterator i = unknownElements.iterator(); i.hasNext();) {
            UnknownElement ue = (UnknownElement) i.next();
            String name = ProjectHelper.extractNameFromComponentName(
                ue.getTag()).toLowerCase(Locale.US);
            if (getNsElements().get(name) == null) {
                throw new BuildException("unsupported element " + name);
            }
            if (presentElements.get(name) != null) {
                throw new BuildException("Element " + name + " already present");
            }
            presentElements.put(name, ue);
        }
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

    private String macroSubs(String s, Map macroMapping) {
        if (s == null) {
            return null;
        }
        StringBuffer ret = new StringBuffer();
        StringBuffer macroName = null;

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
                        state = STATE_NORMAL;
                        ret.append('@');
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
                            ret.append("@{");
                            ret.append(name);
                            ret.append("}");
                        } else {
                            ret.append(value);
                        }
                        macroName = null;
                    } else {
                        macroName.append(ch);
                    }
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

    private UnknownElement copy(UnknownElement ue, boolean nested) {
        UnknownElement ret = new UnknownElement(ue.getTag());
        ret.setNamespace(ue.getNamespace());
        ret.setProject(getProject());
        ret.setQName(ue.getQName());
        ret.setTaskType(ue.getTaskType());
        ret.setTaskName(ue.getTaskName());
        ret.setLocation(
            macroDef.getBackTrace() ? ue.getLocation() : getLocation());
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
        Map m = ue.getWrapper().getAttributeMap();
        for (Iterator i = m.entrySet().iterator(); i.hasNext();) {
            Map.Entry entry = (Map.Entry) i.next();
            rc.setAttribute(
                (String) entry.getKey(),
                macroSubs((String) entry.getValue(), localAttributes));
        }
        rc.addText(macroSubs(ue.getWrapper().getText().toString(),
                             localAttributes));

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
            if (templateElement == null || nested) {
                UnknownElement child = copy(unknownElement, nested);
                rc.addChild(child.getWrapper());
                ret.addChild(child);
            } else if (templateElement.isImplicit()) {
                if (unknownElements.size() == 0 && !templateElement.isOptional()) {
                    throw new BuildException(
                        "Missing nested elements for implicit element "
                        + templateElement.getName());
                }
                for (Iterator i = unknownElements.iterator();
                     i.hasNext();) {
                    UnknownElement child
                        = copy((UnknownElement) i.next(), true);
                    rc.addChild(child.getWrapper());
                    ret.addChild(child);
                }
            } else {
                UnknownElement presentElement =
                    (UnknownElement) presentElements.get(tag);
                if (presentElement == null) {
                    if (!templateElement.isOptional()) {
                        throw new BuildException(
                            "Required nested element "
                            + templateElement.getName() + " missing");
                    }
                    continue;
                }
                String presentText =
                    presentElement.getWrapper().getText().toString();
                if (!"".equals(presentText)) {
                    rc.addText(macroSubs(presentText, localAttributes));
                }
                List list = presentElement.getChildren();
                if (list != null) {
                    for (Iterator i = list.iterator();
                         i.hasNext();) {
                        UnknownElement child
                            = copy((UnknownElement) i.next(), true);
                        rc.addChild(child.getWrapper());
                        ret.addChild(child);
                    }
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
        presentElements = new HashMap();
        getNsElements();
        processTasks();
        localAttributes = new Hashtable();
        Set copyKeys = new HashSet(map.keySet());
        for (Iterator i = macroDef.getAttributes().iterator(); i.hasNext();) {
            MacroDef.Attribute attribute = (MacroDef.Attribute) i.next();
            String value = (String) map.get(attribute.getName());
            if (value == null && "description".equals(attribute.getName())) {
                value = getDescription();
            }
            if (value == null) {
                value = attribute.getDefault();
                value = macroSubs(value, localAttributes);
            }
            if (value == null) {
                throw new BuildException(
                    "required attribute " + attribute.getName() + " not set");
            }
            localAttributes.put(attribute.getName(), value);
            copyKeys.remove(attribute.getName());
        }
        if (copyKeys.contains("id")) {
            copyKeys.remove("id");
        }
        if (macroDef.getText() != null) {
            if (text == null) {
                String defaultText =  macroDef.getText().getDefault();
                if (!macroDef.getText().getOptional() && defaultText == null) {
                    throw new BuildException(
                        "required text missing");
                }
                text = defaultText == null ? "" : defaultText;
            }
            if (macroDef.getText().getTrim()) {
                text = text.trim();
            }
            localAttributes.put(macroDef.getText().getName(), text);
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
        UnknownElement c = copy(macroDef.getNestedTask(), false);
        c.init();
        try {
            c.perform();
        } catch (BuildException ex) {
            if (macroDef.getBackTrace()) {
                throw ProjectHelper.addLocationToBuildException(
                    ex, getLocation());
            } else {
                ex.setLocation(getLocation());
                throw ex;
            }
        } finally {
            presentElements = null;
            localAttributes = null;
        }
    }
}
