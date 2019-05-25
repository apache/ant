/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
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
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.DynamicAttribute;
import org.apache.tools.ant.ProjectHelper;
import org.apache.tools.ant.RuntimeConfigurable;
import org.apache.tools.ant.Target;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.TaskContainer;
import org.apache.tools.ant.UnknownElement;
import org.apache.tools.ant.property.LocalProperties;
import org.apache.tools.ant.taskdefs.MacroDef.Attribute;

/**
 * The class to be placed in the ant type definition.
 * It is given a pointer to the template definition,
 * and makes a copy of the unknown element, substituting
 * the parameter values in attributes and text.
 * @since Ant 1.6
 */
public class MacroInstance extends Task implements DynamicAttribute, TaskContainer {
    private MacroDef macroDef;
    private Map<String, String> map = new HashMap<>();
    private Map<String, MacroDef.TemplateElement> nsElements = null;
    private Map<String, UnknownElement> presentElements;
    private Map<String, String> localAttributes;
    private String text = null;
    private String implicitTag = null;
    private List<Task> unknownElements = new ArrayList<>();

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
    @Override
    public void setDynamicAttribute(String name, String value) {
        map.put(name.toLowerCase(Locale.ENGLISH), value);
    }

    /**
     * Method present for BC purposes.
     * @param name not used
     * @return nothing
     * @deprecated since 1.6.x.
     * @throws BuildException always
     */
    @Deprecated
    public Object createDynamicElement(String name) throws BuildException {
        throw new BuildException("Not implemented any more");
    }

    private Map<String, MacroDef.TemplateElement> getNsElements() {
        if (nsElements == null) {
            nsElements = new HashMap<>();
            for (Map.Entry<String, MacroDef.TemplateElement> entry : macroDef
                .getElements().entrySet()) {
                nsElements.put(entry.getKey(), entry.getValue());
                MacroDef.TemplateElement te = entry.getValue();
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
    @Override
    public void addTask(Task nestedTask) {
        unknownElements.add(nestedTask);
    }

    private void processTasks() {
        if (implicitTag != null) {
            return;
        }
        for (Task task : unknownElements) {
            UnknownElement ue = (UnknownElement) task;
            String name = ProjectHelper.extractNameFromComponentName(
                ue.getTag()).toLowerCase(Locale.ENGLISH);
            if (getNsElements().get(name) == null) {
                throw new BuildException("unsupported element %s", name);
            }
            if (presentElements.get(name) != null) {
                throw new BuildException("Element %s already present", name);
            }
            presentElements.put(name, ue);
        }
    }

    /**
     * Embedded element in macro instance
     */
    public static class Element implements TaskContainer {
        private List<Task> unknownElements = new ArrayList<>();

        /**
         * Add an unknown element (to be snipped into the macroDef instance)
         *
         * @param nestedTask an unknown element
         */
        @Override
        public void addTask(Task nestedTask) {
            unknownElements.add(nestedTask);
        }

        /**
         * @return the list of unknown elements
         */
        public List<Task> getUnknownElements() {
            return unknownElements;
        }
    }

    private static final int STATE_NORMAL         = 0;
    private static final int STATE_EXPECT_BRACKET = 1;
    private static final int STATE_EXPECT_NAME    = 2;

    private String macroSubs(String s, Map<String, String> macroMapping) {
        if (s == null) {
            return null;
        }
        StringBuilder ret = new StringBuilder();
        StringBuilder macroName = null;

        int state = STATE_NORMAL;
        for (final char ch : s.toCharArray()) {
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
                        macroName = new StringBuilder();
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
                    // macroName cannot be null as this state is only
                    // ever reached from STATE_EXPECT_BRACKET after it
                    // has been set
                    if (ch == '}') {
                        state = STATE_NORMAL;
                        String name = macroName.toString().toLowerCase(Locale.ENGLISH); //NOSONAR
                        String value = macroMapping.get(name);
                        if (value == null) {
                            ret.append("@{");
                            ret.append(name);
                            ret.append("}");
                        } else {
                            ret.append(value);
                        }
                        macroName = null;
                    } else {
                        macroName.append(ch); //NOSONAR
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
                // macroName cannot be null as this state is only
                // ever reached from STATE_EXPECT_BRACKET after it
                // has been set
                ret.append("@{");
                ret.append(macroName.toString()); //NOSONAR
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
        Map<String, Object> m = ue.getWrapper().getAttributeMap();
        for (Map.Entry<String, Object> entry : m.entrySet()) {
            rc.setAttribute(
                entry.getKey(),
                macroSubs((String) entry.getValue(), localAttributes));
        }
        rc.addText(macroSubs(ue.getWrapper().getText().toString(),
                             localAttributes));

        for (RuntimeConfigurable r : Collections.list(ue.getWrapper().getChildren())) {
            UnknownElement unknownElement = (UnknownElement) r.getProxy();
            String tag = unknownElement.getTaskType();
            if (tag != null) {
                tag = tag.toLowerCase(Locale.ENGLISH);
            }
            MacroDef.TemplateElement templateElement =
                getNsElements().get(tag);
            if (templateElement == null || nested) {
                UnknownElement child = copy(unknownElement, nested);
                rc.addChild(child.getWrapper());
                ret.addChild(child);
            } else if (templateElement.isImplicit()) {
                if (unknownElements.isEmpty() && !templateElement.isOptional()) {
                    throw new BuildException(
                        "Missing nested elements for implicit element %s",
                        templateElement.getName());
                }
                for (Task task : unknownElements) {
                    UnknownElement child = copy((UnknownElement) task, true);
                    rc.addChild(child.getWrapper());
                    ret.addChild(child);
                }
            } else {
                UnknownElement presentElement =
                    presentElements.get(tag);
                if (presentElement == null) {
                    if (!templateElement.isOptional()) {
                        throw new BuildException(
                            "Required nested element %s missing",
                            templateElement.getName());
                    }
                    continue;
                }
                String presentText =
                    presentElement.getWrapper().getText().toString();
                if (!presentText.isEmpty()) {
                    rc.addText(macroSubs(presentText, localAttributes));
                }
                List<UnknownElement> list = presentElement.getChildren();
                if (list != null) {
                    for (UnknownElement unknownElement2 : list) {
                        UnknownElement child = copy(unknownElement2, true);
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
    @Override
    public void execute() {
        presentElements = new HashMap<>();
        getNsElements();
        processTasks();
        localAttributes = new Hashtable<>();
        Set<String> copyKeys = new HashSet<>(map.keySet());
        for (Attribute attribute : macroDef.getAttributes()) {
            String value = map.get(attribute.getName());
            if (value == null && "description".equals(attribute.getName())) {
                value = getDescription();
            }
            if (value == null) {
                value = attribute.getDefault();
                value = macroSubs(value, localAttributes);
            }
            if (value == null) {
                throw new BuildException("required attribute %s not set",
                    attribute.getName());
            }
            localAttributes.put(attribute.getName(), value);
            copyKeys.remove(attribute.getName());
        }
        copyKeys.remove("id");

        if (macroDef.getText() != null) {
            if (text == null) {
                String defaultText =  macroDef.getText().getDefault();
                if (!macroDef.getText().getOptional() && defaultText == null) {
                    throw new BuildException("required text missing");
                }
                text = defaultText == null ? "" : defaultText;
            }
            if (macroDef.getText().getTrim()) {
                text = text.trim();
            }
            localAttributes.put(macroDef.getText().getName(), text);
        } else if (text != null && !text.trim().isEmpty()) {
            throw new BuildException(
                "The \"%s\" macro does not support nested text data.",
                getTaskName());
        }
        if (!copyKeys.isEmpty()) {
            throw new BuildException("Unknown attribute"
                + (copyKeys.size() > 1 ? "s " : " ") + copyKeys);
        }

        // need to set the project on unknown element
        UnknownElement c = copy(macroDef.getNestedTask(), false);
        c.init();
        LocalProperties localProperties = LocalProperties.get(getProject());
        localProperties.enterScope();
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
            localProperties.exitScope();
        }
    }
}
