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

import org.apache.tools.ant.Project;
import org.apache.tools.ant.MagicNames;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.BuildException;

import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Set;
import java.util.HashSet;

import org.apache.bsf.BSFException;
import org.apache.bsf.BSFManager;

/**
 * Define a task using a script
 *
 * @author Conor MacNeill
 * @since Ant 1.6
 */
public class ScriptDef extends Task {
    /** the name by which this script will be activated */
    private String name;

    /** the scripting language used by the script */
    private String language;

    /** the script itself */
    private String script = "";

    /** Attributes definitions of this script */
    private List attributes = new ArrayList();

    /** Nested Element definitions of this script */
    private List nestedElements = new ArrayList();

    /** The attribute names as a set */
    private Set attributeSet;

    /** The nested element definitions indexed by their names */
    private Map nestedElementMap;

    /**
     * set the name under which this script will be activated in a build
     * file
     *
     * @param name the name of the script
     */
    public void setName(String name) {
        this.name = name;
    }

    public boolean isAttributeSupported(String attributeName) {
        return attributeSet.contains(attributeName);
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
            this.name = name;
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
            this.name = name;
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

        if (language == null) {
            throw new BuildException("scriptdef requires a language attribute "
                + "to specify the script language");
        }

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
        Map scriptRepository = null;
        Project project = getProject();
        synchronized (project) {
            scriptRepository =
                (Map) project.getReference(MagicNames.SCRIPT_REPOSITORY);
            if (scriptRepository == null) {
                scriptRepository = new HashMap();
                project.addReference(MagicNames.SCRIPT_REPOSITORY,
                    scriptRepository);
            }
        }

        scriptRepository.put(name, this);
        project.addTaskDefinition(name, ScriptDefBase.class);
    }

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
            // try the context classloader
            ClassLoader loader
                = Thread.currentThread().getContextClassLoader();

            Class instanceClass = null;
            try {
                instanceClass = Class.forName(classname, true, loader);
            } catch (Throwable e) {
                // try normal method
                try {
                    instanceClass = Class.forName(classname);
                } catch (Throwable e2) {
                    throw new BuildException("scriptdef: Unable to load "
                        + "class " + classname + " for nested element <"
                        + elementName + ">", e2);
                }
            }

            try {
                instance = instanceClass.newInstance();
            } catch (Throwable e) {
                throw new BuildException("scriptdef: Unable to create "
                    + "element of class " + classname + " for nested "
                    + "element <" + elementName + ">", e);
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
     *
     * @param elements a list of nested element values.
     */
    public void executeScript(Map attributes, Map elements) {
        try {
            BSFManager manager = new BSFManager();
            // execute the script
            manager.declareBean("attributes", attributes,
                attributes.getClass());
            manager.declareBean("elements", elements,
                elements.getClass());
            manager.declareBean("project", getProject(), Project.class);
            manager.exec(language, "scriptdef <" + name + ">", 0, 0, script);
        } catch (BSFException e) {
            Throwable t = e;
            Throwable te = e.getTargetException();
            if (te != null) {
                if (te instanceof BuildException) {
                    throw (BuildException) te;
                } else {
                    t = te;
                }
            }
            throw new BuildException(t);
        }
    }

    /**
     * Ass the scipt text.
     *
     * @param text appended to the script text.
     */
    public void addText(String text) {
        this.script += text;
    }
}

