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
package org.apache.ant.antcore.execution;
import java.io.File;
import java.net.URL;
import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import org.apache.ant.antcore.config.AntConfig;
import org.apache.ant.common.antlib.Task;
import org.apache.ant.common.antlib.Aspect;
import org.apache.ant.common.antlib.AntContext;
import org.apache.ant.common.event.BuildListener;
import org.apache.ant.common.event.MessageLevel;
import org.apache.ant.common.model.BuildElement;
import org.apache.ant.common.model.Project;
import org.apache.ant.common.model.Target;
import org.apache.ant.common.service.ComponentService;
import org.apache.ant.common.service.DataService;
import org.apache.ant.common.service.EventService;
import org.apache.ant.common.service.ExecService;
import org.apache.ant.common.service.FileService;
import org.apache.ant.common.service.InputService;
import org.apache.ant.common.service.MagicProperties;
import org.apache.ant.common.util.DemuxOutputReceiver;
import org.apache.ant.common.util.ExecutionException;
import org.apache.ant.common.util.FileUtils;
import org.apache.ant.init.InitConfig;
import org.apache.ant.init.LoaderUtils;

/**
 * An Frame maintains the state of a project during an execution. The Frame
 * contains the data values set by Ant tasks as they are executed, including
 * task definitions, property values, etc.
 *
 * @author Conor MacNeill
 * @created 14 January 2002
 */
public class Frame implements DemuxOutputReceiver {
    /** the base dir of the project */
    private File baseDir;

    /** The Project that this execution frame is processing */
    private Project project = null;

    /** The referenced frames corresponding to the referenced projects */
    private Map referencedFrames = new HashMap();

    /** 
     * This is a Map of Maps. This map is keyed on an executing task.
     * Each entry is itself a Map of Aspects to their context for the
     * particular task.
     */
    private Map aspectContextsMap = new HashMap();
    
    /** 
     * The property overrides for the referenced frames. This map is indexed 
     * by the reference names of the frame. Each entry is another Map of 
     * property values indexed by their relative name.
     */
    private Map overrides = new HashMap();
    
    /**
     * The context of this execution. This contains all data object's created
     * by tasks that have been executed
     */
    private Map dataValues = new HashMap();

    /**
     * Ant's initialization configuration with information on the location of
     * Ant and its libraries.
     */
    private InitConfig initConfig;

    /** BuildEvent support used to fire events and manage listeners */
    private BuildEventSupport eventSupport = new BuildEventSupport();

    /**
     * The services map is a map of service interface classes to instances
     * which provide the service.
     */
    private Map services = new HashMap();

    /**
     * The configuration to be used in this execution of Ant. It is formed
     * from the system, user and any runtime configs.
     */
    private AntConfig config;

    /**
     * The Data Service instance used by the frame for data management
     */
    private DataService dataService;

    /** The execution file service instance */
    private FileService fileService;

    /**
     * the Component Manager used to manage the importing of library
     * components from the Ant libraries
     */
    private ComponentManager componentManager;

    /** The core's execution Service */
    private CoreExecService execService;


    /**
     * Create an Execution Frame for the given project
     *
     * @param config the user config to use for this execution of Ant
     * @param initConfig Ant's initialisation config
     * @exception ExecutionException if a component of the library cannot be
     *      imported
     */
    protected Frame(InitConfig initConfig,
                    AntConfig config) throws ExecutionException {
        this.config = config;
        this.initConfig = initConfig;
    }


    /**
     * Replace ${} style constructions in the given value with the string
     * value of the corresponding data values in the frame
     *
     * @param value the string to be scanned for property references.
     * @return the string with all property references replaced
     * @exception ExecutionException if any of the properties do not exist
     */
    protected String replacePropertyRefs(String value) 
         throws ExecutionException {
        return dataService.replacePropertyRefs(value);
    }


    /**
     * Sets the Project of the Frame
     *
     * @param project The new Project value
     * @exception ExecutionException if any required sub-frames cannot be
     *      created and configured
     */
    protected void setProject(Project project) throws ExecutionException {
        this.project = project;
        referencedFrames.clear();
    }


    /**
     * get the name of the project associated with this frame.
     *
     * @return the project's name
     */
    protected String getProjectName() {
        if (project != null) {
            return project.getName();
        }
        return null;
    }


    /**
     * Set a value in this frame or any of its imported frames.
     *
     * @param name the name of the value
     * @param value the actual value
     * @param mutable if true, existing values can be changed
     * @exception ExecutionException if the value name is invalid
     */
    protected void setDataValue(String name, Object value, boolean mutable)
         throws ExecutionException {
        Frame frame = getContainingFrame(name);

        if (frame == null) {
            setOverrideProperty(name, value, mutable);
            return;
        }

        if (frame == this) {
            if (dataValues.containsKey(name) && !mutable) {
                log("Ignoring oveeride for data value " + name,
                    MessageLevel.MSG_VERBOSE);
            } else {
                dataValues.put(name, value);
            }
        } else {
            frame.setDataValue(getNameInFrame(name), value, mutable);
        }
    }

    /**
     * When a frame has not yet been referenced, this method is used
     * to set the initial properties for the frame when it is introduced.
     *
     * @param name the name of the value
     * @param value the actual value
     * @param mutable if true, existing values can be changed
     * @exception ExecutionException if attempting to override a property in 
     *                               the current frame. 
     */
    private void setOverrideProperty(String name, Object value, 
                                     boolean mutable) 
         throws ExecutionException {
        int refIndex = name.indexOf(Project.REF_DELIMITER);
        if (refIndex == -1) {
            throw new ExecutionException("Property overrides can only be set" 
                + " for properties in referenced projects - not " 
                + name);
        }
        
        String firstFrameName = name.substring(0, refIndex);
        
        String relativeName 
            = name.substring(refIndex + Project.REF_DELIMITER.length());

        Map frameOverrides = (Map) overrides.get(firstFrameName);
        if (frameOverrides == null) {
            frameOverrides = new HashMap();
            overrides.put(firstFrameName, frameOverrides);
        }

        if (mutable || !frameOverrides.containsKey(relativeName)) {
            frameOverrides.put(relativeName, value);
        }            
    }
    
    /**
     * Get a value which exists in the frame property overrides awaiting 
     * the frame to be introduced.
     *
     * @param name the name of the value
     * @return the value of the property or null if the property does not 
     * exist.
     * @exception ExecutionException if attempting to get an override in 
     *                               the current frame. 
     */
    private Object getOverrideProperty(String name) throws ExecutionException {
        int refIndex = name.indexOf(Project.REF_DELIMITER);
        if (refIndex == -1) {
            throw new ExecutionException("Property overrides can only be" 
                + " returned for properties in referenced projects - not " 
                + name);
        }
        
        String firstFrameName = name.substring(0, refIndex);
        
        String relativeName 
            = name.substring(refIndex + Project.REF_DELIMITER.length());

        Map frameOverrides = (Map) overrides.get(firstFrameName);
        if (frameOverrides == null) {
            return null;
        }

        return frameOverrides.get(relativeName);
    }
    
    /**
     * Get a value which exists in the frame property overrides awaiting 
     * the frame to be introduced.
     *
     * @param name the name of the value
     * @return the value of the property or null if the property does not 
     * exist.
     * @exception ExecutionException if attempting to check an override in 
     *                               the current frame. 
     */
    private boolean isOverrideSet(String name) throws ExecutionException {
        int refIndex = name.indexOf(Project.REF_DELIMITER);
        if (refIndex == -1) {
            throw new ExecutionException("Property overrides can only be" 
                + " returned for properties in referenced projects - not " 
                + name);
        }
        
        String firstFrameName = name.substring(0, refIndex);
        
        String relativeName 
            = name.substring(refIndex + Project.REF_DELIMITER.length());

        Map frameOverrides = (Map) overrides.get(firstFrameName);
        if (frameOverrides == null) {
            return false;
        }

        return frameOverrides.containsKey(relativeName);
    }
    

    /**
     * Set the initial properties to be used when the frame starts execution
     *
     * @param properties a Map of named properties which may in fact be any
     *      object
     * @exception ExecutionException if the properties cannot be set
     */
    protected void setInitialProperties(Map properties)
         throws ExecutionException {
        if (properties != null) {
            addProperties(properties);
        }

        // add in system properties
        addProperties(System.getProperties());
    }


    /**
     * Set the values of various magic properties
     *
     * @exception ExecutionException if the properties cannot be set
     */
    protected void setMagicProperties() throws ExecutionException {
        URL antHomeURL = initConfig.getAntHome();
        String antHomeString = null;

        if (antHomeURL.getProtocol().equals("file")) {
            File antHome = new File(antHomeURL.getFile());

            antHomeString = antHome.getAbsolutePath();
        } else {
            antHomeString = antHomeURL.toString();
        }
        setDataValue(MagicProperties.ANT_HOME, antHomeString, true);
    }


    /**
     * Get a definition from a referenced frame
     *
     * @param definitionName the name of the definition relative to this frame
     * @return the appropriate import info object from the referenced frame's
     *      imports
     * @exception ExecutionException if the referenced definition cannot be
     *      found
     */
    protected ImportInfo getReferencedDefinition(String definitionName)
         throws ExecutionException {
        Frame containingFrame = getContainingFrame(definitionName);
        String localName = getNameInFrame(definitionName);

        if (containingFrame == null) {
            throw new ExecutionException("There is no project corresponding "
                 + "to the name \"" + definitionName + "\"");
        }
        if (containingFrame == this) {
            return componentManager.getImport(localName);
        } else {
            return containingFrame.getReferencedDefinition(localName);
        }
    }


    /**
     * Gets the project model this frame is working with
     *
     * @return the project model
     */
    protected Project getProject() {
        return project;
    }


    /**
     * Get all the properties from the frame and any references frames. This
     * is an expensive operation since it must clone all of the property
     * stores in all frames
     *
     * @return a Map containing the frames properties indexed by their full
     *      name.
     */
    protected Map getAllProperties() {
        Map allProperties = new HashMap(dataValues);
        Iterator i = referencedFrames.keySet().iterator();

        while (i.hasNext()) {
            String refName = (String) i.next();
            Frame refFrame = getReferencedFrame(refName);
            Map refProperties = refFrame.getAllProperties();
            Iterator j = refProperties.keySet().iterator();

            while (j.hasNext()) {
                String name = (String) j.next();
                Object value = refProperties.get(name);

                allProperties.put(refName + Project.REF_DELIMITER + name,
                    value);
            }
        }

        return allProperties;
    }


    /**
     * Get the Ant initialization configuration for this frame.
     *
     * @return Ant's initialization configuration
     */
    protected InitConfig getInitConfig() {
        return initConfig;
    }


    /**
     * Get the config instance being used by this frame.
     *
     * @return the config associated with this frame.
     */
    protected AntConfig getConfig() {
        return config;
    }


    /**
     * Get the core's implementation of the given service interface.
     *
     * @param serviceInterfaceClass the service interface for which an
     *      implementation is require
     * @return the core's implementation of the service interface
     * @exception ExecutionException if the core does not provide an
     *      implementatin of the requested interface
     */
    protected Object getCoreService(Class serviceInterfaceClass)
         throws ExecutionException {
        Object service = services.get(serviceInterfaceClass);

        if (service == null) {
            throw new ExecutionException("No service of interface class "
                 + serviceInterfaceClass);
        }
        return service;
    }


    /**
     * Get the EventSupport instance for this frame. This tracks the build
     * listeners on this frame
     *
     * @return the EventSupport instance
     */
    protected BuildEventSupport getEventSupport() {
        return eventSupport;
    }


    /**
     * Gets the baseDir of the Frame
     *
     * @return the baseDir value
     */
    protected File getBaseDir() {
        return baseDir;
    }


    /**
     * Get a referenced frame by its reference name
     *
     * @param referenceName the name under which the frame was imported.
     * @return the Frame asscociated with the given reference name or null if
     *      there is no such project.
     */
    protected Frame getReferencedFrame(String referenceName) {
        return (Frame) referencedFrames.get(referenceName);
    }


    /**
     * Get the frames representing referenced projects.
     *
     * @return an iterator which returns the referenced ExeuctionFrames..
     */
    protected Iterator getReferencedFrames() {
        return referencedFrames.values().iterator();
    }


    /**
     * Get the name of an object in its frame
     *
     * @param fullname The name of the object
     * @return the name of the object within its containing frame
     */
    protected String getNameInFrame(String fullname) {
        int index = fullname.lastIndexOf(Project.REF_DELIMITER);

        if (index == -1) {
            return fullname;
        }
        return fullname.substring(index + Project.REF_DELIMITER.length());
    }


    /**
     * Get a value from this frame or any imported frame
     *
     * @param name the name of the data value - may contain reference
     *      delimiters
     * @return the data value fetched from the appropriate frame
     * @exception ExecutionException if the value is not defined
     */
    protected Object getDataValue(String name) throws ExecutionException {
        Frame frame = getContainingFrame(name);

        if (frame == null) {
            return getOverrideProperty(name);
        }
        if (frame == this) {
            return dataValues.get(name);
        } else {
            return frame.getDataValue(getNameInFrame(name));
        }
    }


    /**
     * Indicate if a data value has been set
     *
     * @param name the name of the data value - may contain reference
     *      delimiters
     * @return true if the value exists
     * @exception ExecutionException if the containing frame for the value
     *      does not exist
     */
    protected boolean isDataValueSet(String name) throws ExecutionException {
        Frame frame = getContainingFrame(name);

        if (frame == null) {
            return isOverrideSet(name);
        }
        if (frame == this) {
            return dataValues.containsKey(name);
        } else {
            return frame.isDataValueSet(getNameInFrame(name));
        }
    }


    /**
     * Get the execution frame which contains, directly, the named element
     * where the name is relative to this frame
     *
     * @param elementName The name of the element
     * @return the execution frame for the project that contains the given
     *      target
     */
    protected Frame getContainingFrame(String elementName) {
        int index = elementName.lastIndexOf(Project.REF_DELIMITER);

        if (index == -1) {
            return this;
        }

        Frame currentFrame = this;
        String relativeName = elementName.substring(0, index);
        StringTokenizer tokenizer
             = new StringTokenizer(relativeName, Project.REF_DELIMITER);

        while (tokenizer.hasMoreTokens()) {
            String refName = tokenizer.nextToken();

            currentFrame = currentFrame.getReferencedFrame(refName);
            if (currentFrame == null) {
                return null;
            }
        }

        return currentFrame;
    }


    /**
     * Add a collection of properties to this frame
     *
     * @param properties the collection of property values, indexed by their
     *      names
     * @exception ExecutionException if the frame cannot be created.
     */
    protected void addProperties(Map properties) throws ExecutionException {
        for (Iterator i = properties.keySet().iterator(); i.hasNext();) {
            String name = (String) i.next();
            Object value = properties.get(name);

            setDataValue(name, value, false);
        }
    }

    /**
     * Create a project reference.
     *
     * @param name the name under which the project will be
     *      referenced.
     * @param project the project model.
     * @param initialData the project's initial data load.
     * @exception ExecutionException if the project cannot be referenced.
     */
    protected void createProjectReference(String name, Project project,
                                          Map initialData) 
        throws ExecutionException {
       Frame referencedFrame = createFrame(project);

       if (initialData != null) {
           referencedFrame.setInitialProperties(initialData);
       }
       
       // does the frame have any overrides?
       Map initialProperties = (Map) overrides.get(name);
       if (initialProperties != null) {
           referencedFrame.setInitialProperties(initialProperties);
           overrides.remove(name);
       }
        
       referencedFrames.put(name, referencedFrame);
       referencedFrame.initialize();
    }

    /**
     * Create a new frame for a given project
     *
     * @param project the project model the frame will deal with
     * @return an Frame ready to build the project
     * @exception ExecutionException if the frame cannot be created.
     */
    protected Frame createFrame(Project project)
         throws ExecutionException {
        Frame newFrame
             = new Frame(initConfig, config);

        newFrame.setProject(project);
        for (Iterator j = eventSupport.getListeners(); j.hasNext();) {
            BuildListener listener = (BuildListener) j.next();

            newFrame.addBuildListener(listener);
        }
        
        return newFrame;
    }


    /**
     * Log a message as a build event
     *
     * @param message the message to be logged
     * @param level the priority level of the message
     */
    protected void log(String message, int level) {
        eventSupport.fireMessageLogged(project, message, level);
    }


    /**
     * Add a build listener to this execution frame
     *
     * @param listener the listener to be added to the frame
     */
    protected void addBuildListener(BuildListener listener) {
        for (Iterator i = getReferencedFrames(); i.hasNext();) {
            Frame referencedFrame = (Frame) i.next();

            referencedFrame.addBuildListener(listener);
        }
        eventSupport.addBuildListener(listener);
    }


    /**
     * Remove a build listener from the execution
     *
     * @param listener the listener to be removed
     */
    protected void removeBuildListener(BuildListener listener) {
        for (Iterator i = getReferencedFrames(); i.hasNext();) {
            Frame subFrame = (Frame) i.next();

            subFrame.removeBuildListener(listener);
        }
        eventSupport.removeBuildListener(listener);
    }


    /**
     * Run the given list of targets
     *
     * @param targets a list of target names which are to be evaluated
     * @exception ExecutionException if there is a problem in the build
     */
    protected void runBuild(List targets) throws ExecutionException {
        initialize();
        if (targets.isEmpty()) {
            // we just execute the default target if any
            String defaultTarget = project.getDefaultTarget();

            if (defaultTarget != null) {
                log("Executing default target: " + defaultTarget,
                    MessageLevel.MSG_DEBUG);
                executeTarget(defaultTarget);
            }
        } else {
            for (Iterator i = targets.iterator(); i.hasNext();) {
                String targetName = (String) i.next();

                log("Executing target: " + targetName, MessageLevel.MSG_DEBUG);
                executeTarget(targetName);
            }
        }
    }



    /**
     * Given a fully qualified target name, this method returns the fully
     * qualified name of the project
     *
     * @param fullTargetName the full qualified target name
     * @return the full name of the containing project
     */
    private String getFullProjectName(String fullTargetName) {
        int index = fullTargetName.lastIndexOf(Project.REF_DELIMITER);
        if (index == -1) {
            return null;
        }

        return fullTargetName.substring(0, index);
    }

    /**
     * Flatten the dependencies to the given target
     *
     * @param flattenedList the List of targets that must be executed before
     *      the given target
     * @param fullTargetName the fully qualified name of the target
     * @exception ExecutionException if the given target does not exist in the
     *      project hierarchy
     */
    private void flattenDependency(List flattenedList, String fullTargetName)
         throws ExecutionException {
        if (flattenedList.contains(fullTargetName)) {
            return;
        }
        String fullProjectName = getFullProjectName(fullTargetName);
        Frame frame = getContainingFrame(fullTargetName);
        String localTargetName = getNameInFrame(fullTargetName);
        Target target = frame.getProject().getTarget(localTargetName);
        if (target == null) {
            throw new ExecutionException("Target " + fullTargetName
                 + " does not exist");
        }
        for (Iterator i = target.getDependencies(); i.hasNext();) {
            String localDependencyName = (String) i.next();
            String fullDependencyName = localDependencyName;
            if (fullProjectName != null) {
                fullDependencyName = fullProjectName + Project.REF_DELIMITER 
                    + localDependencyName;
            }
            flattenDependency(flattenedList, fullDependencyName);
            if (!flattenedList.contains(fullDependencyName)) {
                flattenedList.add(fullDependencyName);
            }
        }
    }

    /**
     * get the list of dependent targets which must be evaluated for the
     * given target.
     *
     * @param fullTargetName the full name (in reference space) of the
     *      target
     * @return the flattened list of targets
     * @exception ExecutionException if the given target could not be found
     */
    protected List getTargetDependencies(String fullTargetName)
         throws ExecutionException {
        List flattenedList = new ArrayList();
        flattenDependency(flattenedList, fullTargetName);
        flattenedList.add(fullTargetName);
        return flattenedList;
    }


    /**
     * Execute the tasks of a target in this frame with the given name
     *
     * @param targetName the name of the target whose tasks will be evaluated
     * @exception ExecutionException if there is a problem executing the tasks
     *      of the target
     */
    protected void executeTarget(String targetName) throws ExecutionException {

        // to execute a target we must determine its dependencies and
        // execute them in order.

        // firstly build a list of fully qualified target names to execute.
        List dependencyOrder = getTargetDependencies(targetName);

        for (Iterator i = dependencyOrder.iterator(); i.hasNext();) {
            String fullTargetName = (String) i.next();
            Frame frame = getContainingFrame(fullTargetName);
            String localTargetName = getNameInFrame(fullTargetName);

            frame.executeTargetTasks(localTargetName);
        }
    }

    /**
     * Execute a task notifiying all registered aspects of the fact
     *
     * @param task the Task instance to execute.
     *
     * @exception ExecutionException if the task has a problem.
     */
    protected void executeTask(Task task) throws ExecutionException {
        List aspects = componentManager.getAspects();
        Map aspectContexts = new HashMap();
        for (Iterator i = aspects.iterator(); i.hasNext();) {
            Aspect aspect = (Aspect) i.next();
            Object context = aspect.preExecuteTask(task);
            aspectContexts.put(aspect, context);
        }
        if (aspectContexts.size() != 0) {
            aspectContextsMap.put(task, aspectContexts);
        }
        
        AntContext context = task.getAntContext();

        if (!(context instanceof ExecutionContext)) {
            throw new ExecutionException("The Task was not configured with an"
                 + " appropriate context");
        }
        ExecutionContext execContext = (ExecutionContext) context;

        eventSupport.fireTaskStarted(task);

        Throwable failureCause = null;

        try {
            ClassLoader currentLoader
                 = LoaderUtils.setContextLoader(execContext.getClassLoader());

            task.execute();
            LoaderUtils.setContextLoader(currentLoader);
        } catch (Throwable e) {
            failureCause = e;
        }

        // Now call back the aspects that registered interest
        Set activeAspects = aspectContexts.keySet();
        for (Iterator i = activeAspects.iterator(); i.hasNext();) {
            Aspect aspect = (Aspect) i.next();
            Object aspectContext = aspectContexts.get(aspect);
            failureCause 
                = aspect.postExecuteTask(aspectContext, failureCause);
        }
        eventSupport.fireTaskFinished(task, failureCause);
        if (aspectContexts.size() != 0) {
            aspectContextsMap.remove(task);
        }
        
        if (failureCause != null) {
            if (failureCause instanceof ExecutionException) {
                throw (ExecutionException) failureCause;
            }
            throw new ExecutionException(failureCause);
        }
    }
    

    /**
     * Run the tasks returned by the given iterator
     *
     * @param taskIterator the iterator giving the tasks to execute
     * @exception ExecutionException if there is execution problem while
     *      executing tasks
     */
    protected void executeTasks(Iterator taskIterator)
         throws ExecutionException {
        
        while (taskIterator.hasNext()) {
            BuildElement model = (BuildElement) taskIterator.next();

            // what sort of element is this.
            List aspects = componentManager.getAspects();
            try {
                Object component = componentManager.createComponent(model);
                for (Iterator i = aspects.iterator(); i.hasNext();) {
                    Aspect aspect = (Aspect) i.next();
                    Object replacement 
                        = aspect.postCreateComponent(component, model);
                    if (replacement != null) {
                        component = replacement;
                    }
                }

                if (component instanceof Task) {
                    executeTask((Task) component);
                } 
            } catch (ExecutionException e) {
                e.setLocation(model.getLocation(), false);
                throw e;
            } catch (RuntimeException e) {
                ExecutionException ee =
                    new ExecutionException(e, model.getLocation());

                throw ee;
            }
        }

    }


    /**
     * Execute the given target's tasks. The target must be local to this
     * frame's project
     *
     * @param targetName the name of the target within this frame that is to
     *      be executed.
     * @exception ExecutionException if there is a problem executing tasks
     */
    protected void executeTargetTasks(String targetName)
         throws ExecutionException {
        Throwable failureCause = null;
        Target target = project.getTarget(targetName);
        String ifCondition = target.getIfCondition();
        String unlessCondition = target.getUnlessCondition();

        if (ifCondition != null) {
            ifCondition = dataService.replacePropertyRefs(ifCondition.trim());
            if (!isDataValueSet(ifCondition)) {
                return;
            }
        }

        if (unlessCondition != null) {
            unlessCondition
                 = dataService.replacePropertyRefs(unlessCondition.trim());
            if (isDataValueSet(unlessCondition)) {
                return;
            }
        }

        try {
            Iterator taskIterator = target.getTasks();

            eventSupport.fireTargetStarted(target);
            executeTasks(taskIterator);
        } catch (ExecutionException e) {
            e.setLocation(target.getLocation(), false);
            failureCause = e;
            throw e;
        } catch (RuntimeException e) {
            ExecutionException ee =
                new ExecutionException(e, target.getLocation());

            failureCause = ee;
            throw ee;
        } finally {
            eventSupport.fireTargetFinished(target, failureCause);
        }
    }


    /**
     * Initialize the frame by executing the project level tasks if any
     *
     * @exception ExecutionException if the top level tasks of the frame
     *      failed
     */
    protected void initialize() throws ExecutionException {
        configureServices();
        setMagicProperties();
        determineBaseDir();

        try {        
            // load system ant lib
            URL systemLibs 
                = new URL(initConfig.getLibraryURL(), "syslibs/");
            componentManager.loadLib(systemLibs.toString(), true, true);
            
            // execute any config tasks
            executeTasks(config.getTasks());
    
            // now load other system libraries
            URL antLibs = new URL(initConfig.getLibraryURL(), "antlibs/");
            componentManager.loadLib(antLibs.toString(), false, true);
            
            executeTasks(project.getTasks());
        } catch (MalformedURLException e) {
            throw new ExecutionException("Unable to initialize antlibs", e);
        }
    }


    /**
     * Determine the base directory for each frame in the frame hierarchy
     *
     * @exception ExecutionException if the base directories cannot be
     *      determined
     */
    private void determineBaseDir() throws ExecutionException {
        if (isDataValueSet(MagicProperties.BASEDIR)) {
            baseDir
                 = new File(getDataValue(MagicProperties.BASEDIR).toString());
        } else {
            URL projectURL = project.getSourceURL();

            if (projectURL.getProtocol().equals("file")) {
                File projectFile = new File(projectURL.getFile());
                File projectFileParent = projectFile.getParentFile();
                String base = project.getBase();

                if (base == null) {
                    baseDir = projectFileParent;
                } else {
                    FileUtils fileUtils = FileUtils.newFileUtils();

                    baseDir = fileUtils.resolveFile(projectFileParent, base);
                }
            } else {
                baseDir = new File(".");
            }
        }
        setDataValue(MagicProperties.BASEDIR, baseDir.getAbsolutePath(), true);
    }


    /**
     * Configure the services that the frame makes available to its library
     * components
     *
     * @exception ExecutionException if the services required by the core 
     * could not be configured.
     */
    private void configureServices() throws ExecutionException {
        // create services and make them available in our services map
        fileService = new CoreFileService(this);
        componentManager = new ComponentManager(this);
        dataService = new CoreDataService(this,
            config.isUnsetPropertiesAllowed());
        execService = new CoreExecService(this);

        services.put(FileService.class, fileService);
        services.put(ComponentService.class, componentManager);
        services.put(DataService.class, dataService);
        services.put(EventService.class, new CoreEventService(this));
        services.put(ExecService.class, execService);
        services.put(InputService.class, new CoreInputService(this));
    }


    /**
     * Handle the content from a single thread. This method will be called by
     * the thread producing the content. The content is broken up into
     * separate lines
     *
     * @param line the content produce by the current thread.
     * @param isErr true if this content is from the thread's error stream.
     */
    public void threadOutput(String line, boolean isErr) {
        eventSupport.threadOutput(line, isErr);
    }
}


