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
package org.apache.tools.ant.taskdefs.optional.script;

import org.apache.tools.ant.AntTypeDefinition;
import org.apache.tools.ant.ComponentHelper;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.MagicNames;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.ProjectHelper;
import org.apache.tools.ant.types.ResourceCollection;
import org.apache.tools.ant.taskdefs.DefBase;

import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Set;
import java.util.HashSet;
import java.io.File;

import org.apache.tools.ant.util.ClasspathUtils;
import org.apache.tools.ant.util.ScriptRunnerBase;
import org.apache.tools.ant.util.ScriptRunnerHelper;

/**
 * Define a task using a script
 *
 * @since Ant 1.6
 */
public class ScriptDef extends DefBase {
    /**
     * script runner helper
     */
    private ScriptRunnerHelper helper = new ScriptRunnerHelper();
    /**
     * script runner.
     */
    /** Used to run the script */
    private ScriptRunnerBase   runner = null;

    /** the name by which this script will be activated */
    private String name;

    /** Attributes definitions of this script */
    private List attributes = new ArrayList();

    /** Nested Element definitions of this script */
    private List nestedElements = new ArrayList();

    /** The attribute names as a set */
    private Set attributeSet;

    /** The nested element definitions indexed by their names */
    private Map nestedElementMap;

    /**
     * Set the project.
     * @param project the project that this def belows to.
     */
    public void setProject(Project project) {
        super.setProject(project);
        helper.setProjectComponent(this);
        helper.setSetBeans(false);
    }

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
     * Indicates whether the task supports a given attribute name
     *
     * @param attributeName the name of the attribute.
     *
     * @return true if the attribute is supported by the script.
     */
    public boolean isAttributeSupported(String attributeName) {
        return attributeSet.contains(attributeName);
    }

    /**
     * Class representing an attribute definition
     */
    public static class Attribute {
        /** The attribute name */
        private String name;

        /**
         * Set the attribute name
         *
         * @param name the attribute name
         */
        public void setName(String name) {
            this.name = name.toLowerCase(Locale.US);
        }
    }

    /**
     * Add an attribute definition to this script.
     *
     * @param attribute the attribute definition.
     */
    public void addAttribute(Attribute attribute) {
        attributes.add(attribute);
    }

    /**
     * Class to represent a nested element definition
     */
    public static class NestedElement {
        /** The name of the neseted element */
        private String name;

        /** The Ant type to which this nested element corresponds. */
        private String type;

        /** The class to be created for this nested element */
        private String className;

        /**
         * set the tag name for this nested element
         *
         * @param name the name of this nested element
         */
        public void setName(String name) {
            this.name = name.toLowerCase(Locale.US);
        }

        /**
         * Set the type of this element. This is the name of an
         * Ant task or type which is to be used when this element is to be
         * created. This is an alternative to specifying the class name directly
         *
         * @param type the name of an Ant type, or task, to use for this nested
         * element.
         */
        public void setType(String type) {
            this.type = type;
        }

        /**
         * Set the classname of the class to be used for the nested element.
         * This specifies the class directly and is an alternative to specifying
         * the Ant type name.
         *
         * @param className the name of the class to use for this nested
         * element.
         */
        public void setClassName(String className) {
            this.className = className;
        }
    }

    /**
     * Add a nested element definition.
     *
     * @param nestedElement the nested element definition.
     */
    public void addElement(NestedElement nestedElement) {
        nestedElements.add(nestedElement);
    }

    /**
     * Define the script.
     */
    public void execute() {
        if (name == null) {
            throw new BuildException("scriptdef requires a name attribute to "
                + "name the script");
        }

        if (helper.getLanguage() == null) {
            throw new BuildException("<scriptdef> requires a language attribute "
                + "to specify the script language");
        }

        // Check if need to set the loader
        if (getAntlibClassLoader() != null || hasCpDelegate()) {
            helper.setClassLoader(createLoader());
        }

        // Now create the scriptRunner
        runner = helper.getScriptRunner();

        attributeSet = new HashSet();
        for (Iterator i = attributes.iterator(); i.hasNext();) {
            Attribute attribute = (Attribute) i.next();
            if (attribute.name == null) {
                throw new BuildException("scriptdef <attribute> elements "
                    + "must specify an attribute name");
            }

            if (attributeSet.contains(attribute.name)) {
                throw new BuildException("scriptdef <" + name + "> declares "
                    + "the " + attribute.name + " attribute more than once");
            }
            attributeSet.add(attribute.name);
        }

        nestedElementMap = new HashMap();
        for (Iterator i = nestedElements.iterator(); i.hasNext();) {
            NestedElement nestedElement = (NestedElement) i.next();
            if (nestedElement.name == null) {
                throw new BuildException("scriptdef <element> elements "
                    + "must specify an element name");
            }
            if (nestedElementMap.containsKey(nestedElement.name)) {
                throw new BuildException("scriptdef <" + name + "> declares "
                    + "the " + nestedElement.name + " nested element more "
                    + "than once");
            }

            if (nestedElement.className == null
                && nestedElement.type == null) {
                throw new BuildException("scriptdef <element> elements "
                    + "must specify either a classname or type attribute");
            }
            if (nestedElement.className != null
                && nestedElement.type != null) {
                throw new BuildException("scriptdef <element> elements "
                    + "must specify only one of the classname and type "
                    + "attributes");
            }


            nestedElementMap.put(nestedElement.name, nestedElement);
        }

        // find the script repository - it is stored in the project
        Map scriptRepository = lookupScriptRepository();
        name = ProjectHelper.genComponentName(getURI(), name);
        scriptRepository.put(name, this);
        AntTypeDefinition def = new AntTypeDefinition();
        def.setName(name);
        def.setClass(ScriptDefBase.class);
        ComponentHelper.getComponentHelper(
            getProject()).addDataTypeDefinition(def);
    }

    /**
     * Find or create the script repository - it is stored in the project.
     * This method is synchronized on the project under {@link MagicNames#SCRIPT_REPOSITORY}
     * @return the current script repository registered as a refrence.
     */
    private Map lookupScriptRepository() {
        Map scriptRepository = null;
        Project p = getProject();
        synchronized (p) {
            scriptRepository =
                    (Map) p.getReference(MagicNames.SCRIPT_REPOSITORY);
            if (scriptRepository == null) {
                scriptRepository = new HashMap();
                p.addReference(MagicNames.SCRIPT_REPOSITORY,
                        scriptRepository);
            }
        }
        return scriptRepository;
    }

    /**
     * Create a nested element to be configured.
     *
     * @param elementName the name of the nested element.
     * @return object representing the element name.
     */
    public Object createNestedElement(String elementName) {
        NestedElement definition
            = (NestedElement) nestedElementMap.get(elementName);
        if (definition == null) {
            throw new BuildException("<" + name + "> does not support "
                + "the <" + elementName + "> nested element");
        }

        Object instance = null;
        String classname = definition.className;
        if (classname == null) {
            instance = getProject().createTask(definition.type);
            if (instance == null) {
                instance = getProject().createDataType(definition.type);
            }
        } else {
            /*
            // try the context classloader
            ClassLoader loader
                = Thread.currentThread().getContextClassLoader();
            */
            ClassLoader loader = createLoader();

            try {
                instance = ClasspathUtils.newInstance(classname, loader);
            } catch (BuildException e) {
                instance = ClasspathUtils.newInstance(classname, ScriptDef.class.getClassLoader());
            }

            getProject().setProjectReference(instance);
        }

        if (instance == null) {
            throw new BuildException("<" + name + "> is unable to create "
                + "the <" + elementName + "> nested element");
        }
        return instance;
    }

    /**
     * Execute the script.
     *
     * @param attributes collection of attributes
     * @param elements a list of nested element values.
     * @deprecated since 1.7.
     *             Use executeScript(attribute, elements, instance) instead.
     */
    public void executeScript(Map attributes, Map elements) {
        executeScript(attributes, elements, null);
    }

    /**
     * Execute the script.
     * This is called by the script instance to execute the script for this
     * definition.
     *
     * @param attributes collection of attributes
     * @param elements   a list of nested element values.
     * @param instance   the script instance; can be null
     */
    public void executeScript(Map attributes, Map elements, ScriptDefBase instance) {
        runner.addBean("attributes", attributes);
        runner.addBean("elements", elements);
        runner.addBean("project", getProject());
        if (instance != null) {
            runner.addBean("self", instance);
        }
        runner.executeScript("scriptdef_" + name);
    }

    /**
     * Defines the manager.
     *
     * @param manager the scripting manager.
     */
    public void setManager(String manager) {
        helper.setManager(manager);
    }

    /**
     * Defines the language (required).
     *
     * @param language the scripting language name for the script.
     */
    public void setLanguage(String language) {
        helper.setLanguage(language);
    }

    /**
     * Load the script from an external file ; optional.
     *
     * @param file the file containing the script source.
     */
    public void setSrc(File file) {
        helper.setSrc(file);
    }

    /**
     * Set the script text.
     *
     * @param text a component of the script text to be added.
     */
    public void addText(String text) {
        helper.addText(text);
    }

    /**
     * Add any source resource.
     * @since Ant1.7.1
     * @param resource source of script
     */
    public void add(ResourceCollection resource) {
        helper.add(resource);
    }
}

