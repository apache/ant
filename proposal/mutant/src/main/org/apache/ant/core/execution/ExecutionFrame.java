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
import org.apache.ant.core.types.*;

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
    /** State used in dependency analysis when a target's dependencies are being
        examined. */
    private static final String VISITING = "VISITING";

    /** State used in dependency analysis to indicate a target has been examined */
    private static final String VISITED = "VISITED";

    /** The Project that this execiton frame is processing */
    private Project project = null;
    
    /** The base URL for this frame. This is derived from the 
        Project's source URL and it's base attribute. */
    private URL baseURL = null;
    
    /** The imported frames of this frame. For each project imported by this frame's
        project, a corresponding ExecutionFrame is created. */
    private Map importedFrames = new HashMap();
    
    /** BuildEvent support used to fire events and manage listeners */
    private BuildEventSupport eventSupport = new BuildEventSupport();
    
    /** The context of this execution. This contains all data object's
        created by tasks that have been executed */
    private Map dataValues = new HashMap();
    
    /** Introspector objects used to configure Tasks from the Task models.*/
    private Map introspectors = new HashMap();
    
    /** The task defs that this frame will use to process tasks */
    private Map taskDefs = new HashMap();
    
    /** Type converters for this executionFrame. Converters are used when configuring
        Tasks to handle special type conversions. */
    private Map converters = new HashMap();
    
    /** The aspect handler active in this frame */
    private Map aspectHandlers = new HashMap();
    
    /** The namespace under which this execution frame lives in the hierarchical
        project namespace - null for the root namespace */
    private String namespace;
    
    /**
     * Construct an execution frame to process the given project model with 
     * the configuration represented by the libraries.
     *
     * @param project the model of the project to be built.
     * @param libraries an Array of AntLibrary objects containing the 
     *                  configuration of Ant for this build.
     *
     * @throws ConfigException when the project cannot be setup with the
     *                         given configuration
     */
    public ExecutionFrame(Project project, AntLibrary[] libraries) 
        throws ConfigException {
    
        this.namespace = null;
        setupFrame(project, libraries);
    }
    
    /**
     * Construct a subframe for managing a project imported into the main project.
     * @param project the model of the project to be built.
     * @param libraries an Array of AntLibrary objects containing the 
     *                  configuration of Ant for this build.
     * @param namespace the location of this project within the overall import
     *                  namespace.
     *
     * @throws ConfigException when the project cannot be setup with the
     *                         given configuration
     */     
    private ExecutionFrame(Project project, AntLibrary[] libraries, String namespace) 
        throws ConfigException {
    
        this.namespace = namespace;
        setupFrame(project, libraries);
    }
    

    /**
     * Set up the execution frame. 
     *
     * This method examines the project model and constructs the required
     * subframes to handle imported projects.
     * @param project the model of the project to be built.
     * @param libraries an Array of AntLibrary objects containing the 
     *                  configuration of Ant for this build.
     *
     * @throws ConfigException when the project cannot be setup with the
     *                         given configuration
     */
    private void setupFrame(Project project, AntLibrary[] libraries) 
        throws ConfigException {

        this.project = project;
        for (int i = 0; i < libraries.length; ++i) {
            addLibrary(libraries[i]);
        }

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
        
        for (Iterator i = project.getImportedProjectNames(); i.hasNext();) {
            String importName = (String)i.next();
            Project importedProject = project.getImportedProject(importName);
            String importNamespace 
                = namespace == null ? importName : namespace + ":" + importName;
            ExecutionFrame importedFrame 
                = new ExecutionFrame(importedProject, libraries, importNamespace);
            importedFrames.put(importName, importedFrame);
        }
    }

    /**
     * Add a configuration library to this execution frame. The library
     * will contain task definitions, converters, apsect handler definitions,
     * etc.
     *
     * @param library the configuration library to add to this frame.
     *
     * @throws ConfigException if the items in the library cannot be configured.
     */
    public void addLibrary(AntLibrary library) throws ConfigException {
        for (Iterator i = library.getTaskDefinitions(); i.hasNext(); ) {
            TaskDefinition taskDefinition = (TaskDefinition)i.next();
            addTaskDefinition(taskDefinition);
        }
        for (Iterator i = library.getConverterDefinitions(); i.hasNext(); ) {
            ConverterDefinition converterDef = (ConverterDefinition)i.next();
            addConverterDefinition(converterDef);
        }
        for (Iterator i = library.getAspectDefinitions(); i.hasNext(); ) {
            AspectDefinition aspectDef = (AspectDefinition)i.next();
            addAspectHandler(aspectDef);
        }
    }

    /**
     * Add a task definition to this execution frame
     *
     * @param taskDefinition the TaskDefinition to be added to the project.
     */
    public void addTaskDefinition(TaskDefinition taskDefinition) {
        String taskName = taskDefinition.getName();
        taskDefs.put(taskName, taskDefinition);
    }
    
    /**
     * Add a aspect handler definition to this execution frame
     *
     * @param taskDefinition the TaskDefinition to be added to the project.
     *
     * @throws ConfigException if the aspect handler cannot be created or configured.
     */
    public void addAspectHandler(AspectDefinition aspectDefinition) 
        throws ConfigException {
        String aspectPrefix = aspectDefinition.getAspectPrefix();
        try {
            Class aspectHandlerClass = aspectDefinition.getAspectHandlerClass();
            aspectHandlers.put(aspectPrefix, aspectHandlerClass);
        }    
        catch (ClassNotFoundException e) {
            throw new ConfigException("Unable to load aspect handler class for " 
                                      + aspectDefinition.getAspectHandlerClassName()
                                      + " in converter from " + aspectDefinition.getLibraryURL(),
                                      e);
        }
    }
    
    /**
     * Add a converter definition to this library.
     *
     * The converter is created immediately to handle conversions
     * when items are being configured. If the converter is an instance of
     * an AntConverter, the converter is configured with this execution
     * frame giving it the context it needs to resolve items relative to the
     * project's base, etc.
     *
     * @param converterDef the converter definition to load
     *
     * @throws ConfigException if the converter cannot be created or configured.
     */
    public void addConverterDefinition(ConverterDefinition converterDef) throws ConfigException {
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
                                          + " in converter from " + converterDef.getLibraryURL(),
                                          e);
            }
            else {
                throw new ConfigException("Unable to load target class " 
                                          + converterDef.getTargetClassName()
                                          + " in converter from " + converterDef.getLibraryURL(),
                                          e);
            }
        }
        catch (InstantiationException e) {
            throw new ConfigException("Unable to instantiate converter class " 
                                      + converterDef.getTargetClassName()
                                      + " in converter from " + converterDef.getLibraryURL(),
                                      e);
        }
        catch (IllegalAccessException e) {
            throw new ConfigException("Unable to access converter class " 
                                      + converterDef.getTargetClassName()
                                      + " in converter from " + converterDef.getLibraryURL(),
                                      e);
        }
    }

    
    /**
     * Get the bae URL of this frame. This will either be specified by the project's
     * base attribute or be derived implicitly from the project's location.
     */
    public URL getBaseURL() {
        return baseURL;
    }


    public void addBuildListener(BuildListener listener) {
        for (Iterator i = getImportedFrames(); i.hasNext(); ) {
            ExecutionFrame subFrame = (ExecutionFrame)i.next();
            subFrame.addBuildListener(listener);
        }
        eventSupport.addBuildListener(listener);
    }
    
    public void removeBuildListener(BuildListener listener) {
        for (Iterator i = getImportedFrames(); i.hasNext(); ) {
            ExecutionFrame subFrame = (ExecutionFrame)i.next();
            subFrame.removeBuildListener(listener);
        }
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
     * Initialize the frame by executing the project level tasks if any
     */
    public void initialize() throws ExecutionException, ConfigException {
        for (Iterator i = getImportedFrames(); i.hasNext(); ) {
            ExecutionFrame subFrame = (ExecutionFrame)i.next();
            subFrame.initialize();
        }
        Iterator taskIterator = project.getTasks();
        executeTasks(taskIterator);
    }

    public void fillinDependencyOrder(String targetName, List dependencyOrder, 
                                      Map state, Stack visiting) throws ConfigException {
        String fullTargetName = getQualifiedName(targetName); 
        if (state.get(fullTargetName) == VISITED) {
            return;
        }
        Target target = getProject().getTarget(targetName);                                       
        if (target == null) {
            StringBuffer sb = new StringBuffer("Target `");
            sb.append(targetName);
            sb.append("' does not exist in this project. ");
            if (!visiting.empty()) {
                String parent = (String)visiting.peek();
                sb.append("It is used from target `");
                sb.append(parent);
                sb.append("'.");
            }

            throw new ConfigException(new String(sb), getProject().getLocation());
        }
        
        state.put(fullTargetName, VISITING);
        visiting.push(fullTargetName);
        for (Iterator i = target.getDependencies(); i.hasNext(); ) {
            String dependency = (String)i.next();
            try {
                ExecutionFrame dependencyFrame = getRelativeFrame(dependency);
                if (dependencyFrame == null) {
                    StringBuffer sb = new StringBuffer("Target `");
                    sb.append(dependency);
                    sb.append("' does not exist in this project. ");
                    throw new ConfigException(new String(sb), target.getLocation());
                }
                
                String fullyQualifiedName = getQualifiedName(dependency);
                String dependencyState = (String)state.get(fullyQualifiedName);
                if (dependencyState == null) {
                    dependencyFrame.fillinDependencyOrder(getNameInFrame(dependency), dependencyOrder, 
                                                          state, visiting);
                }
                else if (dependencyState == VISITING) {
                    String circleDescription
                        = getCircularDesc(dependency, visiting);
                    throw new ConfigException(circleDescription, target.getLocation());
                }
            }
            catch (ExecutionException e) {
                throw new ConfigException(e.getMessage(), e, target.getLocation());
            }
        }
        
        state.put(fullTargetName, VISITED);
        String poppedNode = (String)visiting.pop();
        if (poppedNode != fullTargetName) {
            throw new ConfigException("Problem determining dependencies " + 
                                        " - expecting '" + fullTargetName + 
                                        "' but got '" + poppedNode + "'");
        }
        dependencyOrder.add(fullTargetName);                                        
    }
                                
    private String getCircularDesc(String end, Stack visitingNodes) {
        StringBuffer sb = new StringBuffer("Circular dependency: ");
        sb.append(end);
        String c;
        do {
            c = (String)visitingNodes.pop();
            sb.append(" <- ");
            sb.append(c);
        } while(!c.equals(end));
        return new String(sb);
    }

    /**
     * Check whether the targets in this frame and its subframes are OK
     */
    public void checkTargets(List dependencyOrder, Map state, Stack visiting) 
            throws ConfigException {
        // get the targets and just iterate through them.
        for (Iterator i = getProject().getTargets(); i.hasNext();) {
            Target target = (Target)i.next();
            fillinDependencyOrder(target.getName(),
                                  dependencyOrder, state, visiting);
        }
        
        // Now do the subframes.
        for (Iterator i = getImportedFrames(); i.hasNext();) {
            ExecutionFrame importedFrame = (ExecutionFrame)i.next();
            importedFrame.checkTargets(dependencyOrder, state, visiting);
        }
    }

        
    /**
     * Create a Task and configure it according to the given model.
     */        
    private Task configureTask(TaskElement model) 
        throws ConfigException, ExecutionException {

        String taskType = model.getType();
        TaskDefinition taskDefinition = (TaskDefinition)taskDefs.get(taskType);
        if (taskDefinition == null) {
            throw new ConfigException("There is no defintion for tasks of type <" 
                                      + taskType + ">", model.getLocation());
        }
        
        try {
            Class elementClass = taskDefinition.getExecutionTaskClass();
            Object element = elementClass.newInstance();
            Task task = null;
            if (element instanceof Task) {
                // create a Task context for the Task
                task = (Task)element;
            }
            else {
                task = new TaskAdapter(taskType, element);
            }
            
            configureElement(element, model);

            return task;
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

    private List getActiveAspects(BuildElement model) 
            throws ConfigException, ExecutionException, 
                   ClassIntrospectionException, ConversionException {
        List activeAspects = new ArrayList();
        for (Iterator i = model.getAspectNames(); i.hasNext();) {
            String aspectPrefix = (String)i.next();
            Class aspectHandlerClass = (Class)aspectHandlers.get(aspectPrefix);
            if (aspectHandlerClass != null) {
                try {
                    AspectHandler aspectHandler 
                        = (AspectHandler)aspectHandlerClass.newInstance();
                    ClassIntrospector introspector = getIntrospector(aspectHandlerClass);
                    
                    ExecutionContext context = new ExecutionContext(this, eventSupport, model);
                    aspectHandler.setAspectContext(context);
                    
                    Map aspectAttributes = model.getAspectAttributes(aspectPrefix);
                    for (Iterator j = aspectAttributes.keySet().iterator(); j.hasNext();) {
                        String attributeName = (String)j.next();
                        String attributeValue = (String)aspectAttributes.get(attributeName);
                        introspector.setAttribute(aspectHandler, attributeName, 
                                                  replacePropertyRefs(attributeValue));
                    }
                    activeAspects.add(aspectHandler);
                }
                catch (InstantiationException e) {
                    throw new ConfigException("Unable to instantiate aspect handler class " 
                                              + aspectHandlerClass,
                                              e);
                }
                catch (IllegalAccessException e) {
                    throw new ConfigException("Unable to access aspect handler class " 
                                              + aspectHandlerClass,
                                              e);
                }
            }
        }                                                          
        return activeAspects;
    }
    

    /**
     * Configure an element according to the given model.
     */   
    private void configureElement(Object element, TaskElement model) 
        throws ExecutionException, ConfigException {
        
        if (element instanceof Task) {
            Task task = (Task)element;
            ExecutionContext context = new ExecutionContext(this, eventSupport, model);
            task.setTaskContext(context);
        }
        try {
            ClassIntrospector introspector = getIntrospector(element.getClass());
                
            List aspects = getActiveAspects(model);
            for (Iterator i = aspects.iterator(); i.hasNext(); ) {
                AspectHandler aspectHandler = (AspectHandler)i.next();
                aspectHandler.beforeConfigElement(element);
            }
            
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
                    
                    Task nestedTask = configureTask(nestedElementModel);
                    TaskContainer container = (TaskContainer)element;
                    container.addTask(nestedTask);
                }
                else {                        
                    Object nestedElement 
                        = introspector.createElement(element, nestedElementModel.getType());
                    configureElement(nestedElement, nestedElementModel);
                }                    
            }
            for (Iterator i = aspects.iterator(); i.hasNext(); ) {
                AspectHandler aspectHandler = (AspectHandler)i.next();
                aspectHandler.afterConfigElement(element);
            }
        }
        catch (ClassIntrospectionException e) {
            throw new ExecutionException(e, model.getLocation());
        }
        catch (ConversionException e) {
            throw new ExecutionException(e, model.getLocation());
        }
    }

    /**
     * Run the tasks returned by the give iterator
     *
     * @param taskIterator the iterator giving the tasks to execute
     */
    public void executeTasks(Iterator taskIterator) throws ExecutionException, ConfigException {
        TaskElement task = null;
        try {
            while (taskIterator.hasNext()) {
                task = (TaskElement)taskIterator.next();
                try {
                    Task configuredTask = configureTask(task);
                    eventSupport.fireTaskStarted(this, task);
                    configuredTask.execute();
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
    public ExecutionFrame getRelativeFrame(String name) throws ExecutionException {
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
    public String getNameInFrame(String name) {
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
    public Object getDataValue(String name) throws ExecutionException {
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

    public void runBuild(List targetNames) throws AntException {
        Throwable buildFailureCause = null;
        try {
            eventSupport.fireBuildStarted(this, project);
            initialize();

            if (targetNames.isEmpty()) {
                // we just execute the default target if any
                String defaultTarget = project.getDefaultTarget();
                if (defaultTarget != null) {
                    executeTarget(defaultTarget);
                }
            }
            else {
                for (Iterator i = targetNames.iterator(); i.hasNext();) {
                    executeTarget((String)i.next());
                }
            }
            eventSupport.fireBuildFinished(this, project, null);
        }
        catch (RuntimeException e) {
            buildFailureCause = e;
            throw e;
        }
        catch (AntException e) {
            buildFailureCause = e;
            throw e;
        }
        finally {
            eventSupport.fireBuildFinished(this, project, buildFailureCause);
        }
    }

    public void executeTarget(String targetName) throws ExecutionException, ConfigException {
        // to execute a target we must determine its dependencies and 
        // execute them in order.
        Map state = new HashMap();
        Stack visiting = new Stack();
        List dependencyOrder = new ArrayList();
        ExecutionFrame startingFrame = getRelativeFrame(targetName);
        startingFrame.fillinDependencyOrder(getNameInFrame(targetName),
                                            dependencyOrder, state, visiting);

        // Now tell each frame to execute the targets required
        for (Iterator i = dependencyOrder.iterator(); i.hasNext();) {
            String fullTargetName = (String)i.next();
            ExecutionFrame frame = getRelativeFrame(fullTargetName);
            frame.executeTargetTasks(getNameInFrame(fullTargetName));
        }
    }
}
