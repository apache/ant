/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2001 The Apache Software Foundation.  All rights
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
package org.apache.ant.core.execution;

import org.apache.ant.core.model.*;
import org.apache.ant.core.support.*;

import java.util.*;
import java.net.*;

/**
 * An ExecutionFrame is the state of a project during an execution. 
 * The ExecutionFrame contains the data values set by Ant tasks as
 * they are executed, including task definitions, property values, etc.
 *
 * @author <a href="mailto:conor@apache.org">Conor MacNeill</a>
 */
public class ExecutionFrame {
    /** The Project that this execiton frame is processing */
    private Project project = null;
    
    /** The base URL for this frame. This is derived from the 
        Project's source URL and it's base attribute. */
    private URL baseURL = null;
    
    /** The task defs that this frame will use to process tasks */
    private Map taskDefs = new HashMap();
    
    /** The imported frames of this frame. For each project imported by this frame's
        project, a corresponding ExecutionFrame is created. */
    private Map importedFrames = new HashMap();
    
    /** BuildEvent support used to fire events and manage listeners */
    private BuildEventSupport eventSupport = new BuildEventSupport();
    
    /** The context of this execution. This contains all data object's
        created by tasks that have been executed */
    private Map dataValues = new HashMap();
    
    /** Introspector objects used to configure ExecutionTasks from the Task models.*/
    private Map introspectors = new HashMap();
    
    /** Type converters for this executionFrame. Converters are used when configuring
        Tasks to handle special type conversions. */
    private Map converters = new HashMap();
    
    /** The namespace under which this execution frame lives in the hierarchical
        project namespace - null for the root namespace */
    private String namespace;
    
    public ExecutionFrame(Project project, Map taskDefs, Map converterDefs,
                          String namespace) throws ConfigException {
        this.project = project;
        this.taskDefs = taskDefs;
        this.namespace = namespace;

        try {        
            String base = project.getBase();
            if (base == null) {
                baseURL = project.getSourceURL();
            }
            else {
                base = base.trim();
                if (!base.endsWith("/")) {
                    base += "/";
                }
                baseURL = new URL(project.getSourceURL(), base);
            }            
        }
        catch (MalformedURLException e) {
            throw new ConfigException("Project's base value \"" + project.getBase() 
                                      + "\" is not valid", e, project.getLocation());
        }                                      
        
        // We create a set of converters from the converter definitions we
        // have been given and initialise them. They should be AntConverters
        setupConverters(converterDefs);
        
        for (Iterator i = project.getImportedProjectNames(); i.hasNext();) {
            String importName = (String)i.next();
            Project importedProject = project.getImportedProject(importName);
            String importNamespace 
                = namespace == null ? importName : namespace + ":" + importName;
            ExecutionFrame importedFrame 
                = new ExecutionFrame(importedProject, taskDefs, converterDefs, importNamespace);
            importedFrames.put(importName, importedFrame);
        }
    }

    public URL getBaseURL() {
        return baseURL;
    }

    private void setupConverters(Map converterDefs) throws ConfigException {
        converters = new HashMap();
        for (Iterator i = converterDefs.values().iterator(); i.hasNext(); ) {
            ConverterDefinition converterDef = (ConverterDefinition)i.next();
            boolean targetLoaded = false;
            try {
                Class targetClass = converterDef.getTargetClass();
                targetLoaded = false;
                Class converterClass = converterDef.getConverterClass();
                Converter converter = (AntConverter)converterClass.newInstance();
                if (converter instanceof AntConverter) {
                    ((AntConverter)converter).init(this);
                }
                converters.put(targetClass, converter);
            }
            catch (ClassNotFoundException e) {
                if (targetLoaded) {
                    throw new ConfigException("Unable to load converter class for " 
                                              + converterDef.getConverterClassName()
                                              + " in converter from " + converterDef.getLibraryURL()
                                              , e);
                }
                else {
                    throw new ConfigException("Unable to load target class " 
                                              + converterDef.getTargetClassName()
                                              + " in converter from " + converterDef.getLibraryURL()
                                              , e);
                }
            }
            catch (InstantiationException e) {
                throw new ConfigException("Unable to instantiate converter class " 
                                          + converterDef.getTargetClassName()
                                          + " in converter from " + converterDef.getLibraryURL()
                                          , e);
            }
            catch (IllegalAccessException e) {
                throw new ConfigException("Unable to access converter class " 
                                          + converterDef.getTargetClassName()
                                          + " in converter from " + converterDef.getLibraryURL()
                                          , e);
            }
        }
    }
    
    public void addBuildListener(BuildListener listener) {
        eventSupport.addBuildListener(listener);
    }
    
    public void removeBuildListener(BuildListener listener) {
        eventSupport.removeBuildListener(listener);
    }

    /**
     * Get the project associated with this execution frame.
     *
     * @return the project associated iwth this execution frame.
     */
    public Project getProject() {
        return project;
    }
    

    /**
     * Get the names of the frames representing imported projects.
     *
     * @return an iterator which returns the names of the imported frames.
     */
    public Iterator getImportedFrameNames() {
        return importedFrames.keySet().iterator();
    }
    

    /**
     * Get the frames representing imported projects.
     *
     * @return an iterator which returns the imported ExeuctionFrames..
     */
    public Iterator getImportedFrames() {
        return importedFrames.values().iterator();
    }
    
    /**
     * Get an imported frame by name
     *
     * @param importName the name under which the frame was imported.
     *
     * @return the ExecutionFrame asscociated with the given import name or null 
     *         if there is no such project.
     */
    public ExecutionFrame getImportedFrame(String importName) {
        return (ExecutionFrame)importedFrames.get(importName);
    }
    
    /**
     * Get the location of this frame in the namespace hierarchy
     *
     * @return the location of this frame within the project import
     *         namespace hierarchy.
     */
    public String getNamespace() {
        return namespace;
    }
    
    /**
     * Get the fully qualified name of something with respect to this 
     * execution frame.
     * 
     * @param name the unqualified name.
     * 
     * @return the fully qualified version of the given name
     */
    public String getQualifiedName(String name) {
        return namespace == null ? name : namespace + ":" + name;
    }
    
    /**
     * Get the relative name of something with respect to this 
     * execution frame.
     * 
     * @param fullname the fully qualified name.
     * 
     * @return the relative version of the given name
     */
    public String getRelativeName(String fullname) {
        if (namespace == null) {
            return fullname;
        }
        int index = fullname.indexOf(namespace);
        if (index != 0) {
            return fullname;
        }
        
        return fullname.substring(namespace.length() + 1);
    }
    
    /**
     * Execute the given target's tasks
     *
     * @param the name of the target within this frame that is to be executed.
     */
    public void executeTargetTasks(String targetName) throws ExecutionException, ConfigException {
        Target target = project.getTarget(targetName);
        try {
            Iterator taskIterator = target.getTasks();
            eventSupport.fireTargetStarted(this, target);
            executeTasks(taskIterator);
            eventSupport.fireTargetFinished(this, target, null);
        }
        catch (RuntimeException e) {
            eventSupport.fireTargetFinished(this, target, e);
            throw e;
        }
    }
    
    /**
     * Initialise the frame by executing the project level tasks if any
     */
    public void initialise() throws ExecutionException, ConfigException {
        Iterator taskIterator = project.getTasks();
        executeTasks(taskIterator);
    }


    private ExecutionTask getConfiguredExecutionTask(TaskElement model) 
        throws ConfigException, ExecutionException {

        String taskType = model.getType();
        TaskDefinition taskDefinition = (TaskDefinition)taskDefs.get(taskType);
        if (taskDefinition == null) {
            throw new ConfigException("There is no task defintion for tasks of type <" 
                                      + taskType + ">", model.getLocation());
        }
        
        try {
            Class executionTaskClass = taskDefinition.getExecutionTaskClass();
            ExecutionTask executionTask = (ExecutionTask)executionTaskClass.newInstance();
            executionTask.setExecutionFrame(this);
            executionTask.setBuildEventSupport(eventSupport);
            executionTask.setBuildElement(model);
            configureElement(executionTask, model);
            return executionTask;
        }
        catch (ClassNotFoundException e) {
            throw new ConfigException("Execution class " + taskDefinition.getTaskClassName()
                                         + " was not found", e, model.getLocation());
        }
        catch (InstantiationException e) {
            throw new ConfigException("Unable to instantiate execution class "
                                          + taskDefinition.getTaskClassName(), 
                                          e, model.getLocation());
        }
        catch (IllegalAccessException e) {
            throw new ConfigException("Unable to access execution class "
                                          + taskDefinition.getTaskClassName(), 
                                          e, model.getLocation());
        }
    }        
   
    /**
     * Run the tasks returned by the give iterator
     *
     * @param taskIterator the iterator giving the tasks to execute
     */
    public void executeTasks(Iterator taskIterator) throws ExecutionException, ConfigException {
        Task task = null;
        try {
            while (taskIterator.hasNext()) {
                task = (Task)taskIterator.next();
                try {
                    ExecutionTask executionTask = getConfiguredExecutionTask(task);
                    eventSupport.fireTaskStarted(this, task);
                    executionTask.execute();
                }
                catch (ExecutionException e) {
                    if (e.getLocation() == null || e.getLocation() == Location.UNKNOWN_LOCATION) {
                        e.setLocation(task.getLocation());
                    }
                    throw e;
                }
                catch (ConfigException e) {
                    if (e.getLocation() == null || e.getLocation() == Location.UNKNOWN_LOCATION) {
                        e.setLocation(task.getLocation());
                    }
                    throw e;
                }
                eventSupport.fireTaskFinished(this, task, null);
            }
        }
        catch (RuntimeException e) {
            eventSupport.fireTaskFinished(this, task, e);
            throw e;
        }
    }        

    private void configureElement(Object element, TaskElement model) 
        throws ExecutionException, ConfigException {

        try {
            ClassIntrospector introspector = getIntrospector(element.getClass());
                
            // start by setting the attributes of this element
            for (Iterator i = model.getAttributeNames(); i.hasNext();) {
                String attributeName = (String)i.next();
                String attributeValue = model.getAttributeValue(attributeName);
                introspector.setAttribute(element, attributeName, 
                                          replacePropertyRefs(attributeValue));
            }
            
            String modelText = model.getText().trim();
            if (modelText.length() != 0) {
                introspector.addText(element, replacePropertyRefs(modelText));
            }

            // now do the nested elements
            for (Iterator i = model.getNestedElements(); i.hasNext();) {
                TaskElement nestedElementModel = (TaskElement)i.next();
                if (element instanceof TaskContainer &&
                    !introspector.supportsNestedElement(nestedElementModel.getType())) {
                    
                    ExecutionTask nestedExecutionTask 
                        = getConfiguredExecutionTask(nestedElementModel);
                        
                    TaskContainer container = (TaskContainer)element;
                    container.addExecutionTask(nestedExecutionTask);
                }
                else {                        
                    Object nestedElement 
                        = introspector.createElement(element, nestedElementModel.getType());
                    configureElement(nestedElement, nestedElementModel);
                }                    
            }
        }
        catch (ClassIntrospectionException e) {
            throw new ExecutionException(e, model.getLocation());
        }
        catch (ConversionException e) {
            throw new ExecutionException(e, model.getLocation());
        }
    }

    private ClassIntrospector getIntrospector(Class c) {
        if (introspectors.containsKey(c)) {
            return (ClassIntrospector)introspectors.get(c);
        }
        ClassIntrospector introspector = new ClassIntrospector(c, converters);
        introspectors.put(c, introspector);
        return introspector;
    }
                            
    /**
     * Replace ${} style constructions in the given value with the string value of
     * the corresponding data types.
     *
     * @param value the string to be scanned for property references.
     */
    public String replacePropertyRefs(String value) throws ExecutionException {
        if (value == null) {
            return null;
        }

        List fragments = new ArrayList();
        List propertyRefs = new ArrayList();
        parsePropertyString(value, fragments, propertyRefs);

        StringBuffer sb = new StringBuffer();
        Iterator i = fragments.iterator();
        Iterator j = propertyRefs.iterator();
        while (i.hasNext()) {
            String fragment = (String)i.next();
            if (fragment == null) {
                String propertyName = (String)j.next();
                if (!isDataValueSet(propertyName)) {
                    throw new ExecutionException("Property " + propertyName + " has not been set");
                }
                fragment = getDataValue(propertyName).toString();
            }
            sb.append(fragment);
        }                        
        
        return sb.toString();
    }
    

    /**
     * This method will parse a string containing ${value} style 
     * property values into two list. The first list is a collection
     * of text fragments, while the other is a set of string property names
     * null entries in the first list indicate a property reference from the
     * second list.
     */
    static public void parsePropertyString(String value, List fragments, List propertyRefs) 
        throws ExecutionException {
        int prev = 0;
        int pos;
        while ((pos = value.indexOf("$", prev)) >= 0) {
            if (pos > 0) {
                fragments.add(value.substring(prev, pos));
            }

            if( pos == (value.length() - 1)) {
                fragments.add("$");
                prev = pos + 1;
            }
            else if (value.charAt(pos + 1) != '{' ) {
                fragments.add(value.substring(pos + 1, pos + 2));
                prev = pos + 2;
            } else {
                int endName = value.indexOf('}', pos);
                if (endName < 0) {
                    throw new ExecutionException("Syntax error in property: " 
                                                 + value );
                }
                String propertyName = value.substring(pos + 2, endName);
                fragments.add(null);
                propertyRefs.add(propertyName);
                prev = endName + 1;
            }
        }

        if (prev < value.length()) {
            fragments.add(value.substring(prev));
        }
    }

    /**
     * Given a name of an object, get the frame relative from this frame that
     * contains that object.
     */
    private ExecutionFrame getRelativeFrame(String name) throws ExecutionException {
        int index = name.lastIndexOf(":");
        if (index == -1) {
            return this;
        }
        
        ExecutionFrame currentFrame = this;
        String relativeFrameName = name.substring(0, index);
        StringTokenizer tokenizer = new StringTokenizer(relativeFrameName, ":");
        while (tokenizer.hasMoreTokens()) {
            String frameName = tokenizer.nextToken();
            currentFrame = currentFrame.getImportedFrame(frameName);
            if (currentFrame == null) {
                throw new ExecutionException("The project " + frameName + " in " 
                                             + name + " was not found");
            }
        }
        
        return currentFrame;
    }

    /**
     * Get the name of an object in its frame
     */
    private String getNameInFrame(String name) {
        int index = name.lastIndexOf(":");
        if (index == -1) {
            return name;
        }
        return name.substring(index+1);
    }

    /**
     * Set a value in this frame or any of its imported frames
     */
    public void setDataValue(String name, Object value) throws ExecutionException {
        ExecutionFrame frame = getRelativeFrame(name);
        frame.setDirectDataValue(getNameInFrame(name), value);
    }
    
    /**
     * Get a value from this frame or any imported frame
     */
    private Object getDataValue(String name) throws ExecutionException {
        ExecutionFrame frame = getRelativeFrame(name);
        return frame.getDirectDataValue(getNameInFrame(name));
    }
    
    /**
     * Set a value in this frame only
     */
    private void setDirectDataValue(String name, Object value) {
        dataValues.put(name, value);
    }
    
    /**
     * Get a value from this frame
     */
    private Object getDirectDataValue(String name) {
        return dataValues.get(name);
    }

    /**
     * Indicate if a data value has been set
     */
    public boolean isDataValueSet(String name) throws ExecutionException {
        ExecutionFrame frame = getRelativeFrame(name);
        return frame.isDirectDataValueSet(getNameInFrame(name));
    }
        
    /**
     * Indicate if a data value has been set in this frame
     */
    private boolean isDirectDataValueSet(String name) {
        return dataValues.containsKey(name);
    }
        
}
