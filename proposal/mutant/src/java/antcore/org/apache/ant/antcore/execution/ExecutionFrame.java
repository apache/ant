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
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import org.apache.ant.antcore.antlib.AntLibrary;
import org.apache.ant.antcore.config.AntConfig;
import org.apache.ant.common.antlib.AntLibFactory;
import org.apache.ant.common.antlib.ExecutionComponent;
import org.apache.ant.common.antlib.Task;
import org.apache.ant.common.antlib.TaskContainer;
import org.apache.ant.common.event.BuildListener;
import org.apache.ant.common.model.BuildElement;
import org.apache.ant.common.model.Project;
import org.apache.ant.common.model.Target;
import org.apache.ant.common.service.ComponentService;
import org.apache.ant.common.service.DataService;
import org.apache.ant.common.service.FileService;
import org.apache.ant.common.service.EventService;
import org.apache.ant.common.service.MagicProperties;
import org.apache.ant.common.util.AntException;
import org.apache.ant.common.util.ConfigException;
import org.apache.ant.common.util.ExecutionException;
import org.apache.ant.common.util.FileUtils;
import org.apache.ant.common.util.MessageLevel;
import org.apache.ant.init.InitConfig;

/**
 * An ExecutionFrame maintains the state of a project during an execution.
 * The ExecutionFrame contains the data values set by Ant tasks as they are
 * executed, including task definitions, property values, etc.
 *
 * @author <a href="mailto:conor@apache.org">Conor MacNeill</a>
 * @created 14 January 2002
 */
public class ExecutionFrame {
    /** The Ant aspect used to identify Ant metadata */
    public final static String ANT_ASPECT = "ant";

    /** the base dir of the project */
    private File baseDir;

    /** The Project that this execution frame is processing */
    private Project project = null;

    /** The referenced frames corresponding to the referenced projects */
    private Map referencedFrames = new HashMap();

    /** Reflector objects used to configure Tasks from the Task models. */
    private Map reflectors = new HashMap();

    /**
     * The context of this execution. This contains all data object's
     * created by tasks that have been executed
     */
    private Map dataValues = new HashMap();

    /**
     * Ant's initialization configuration with information on the location
     * of Ant and its libraries.
     */
    private InitConfig initConfig;

    /**
     * These are the standard libraries from which taskdefs, typedefs, etc
     * may be imported.
     */
    private Map standardLibs;

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

    /**
     * Create an Execution Frame for the given project
     *
     * @param standardLibs The libraries of tasks and types available to
     *      this frame
     * @param config the user config to use for this execution of Ant
     * @param initConfig Ant's initialisation config
     * @exception ExecutionException if a component of the library cannot be
     *      imported
     */
    protected ExecutionFrame(Map standardLibs, InitConfig initConfig,
                             AntConfig config) throws ExecutionException {
        this.standardLibs = standardLibs;
        this.config = config;
        this.initConfig = initConfig;
    }

    /**
     * Set the context loader of the current thread and returns the existing
     * classloader
     *
     * @param newLoader the new context loader
     * @return the old context loader
     */
    private static ClassLoader setContextLoader(ClassLoader newLoader) {
        Thread thread = Thread.currentThread();
        ClassLoader currentLoader = thread.getContextClassLoader();
        thread.setContextClassLoader(newLoader);
        return currentLoader;
    }

    /**
     * Sets the Project of the ExecutionFrame
     *
     * @param project The new Project value
     * @exception ExecutionException if any required sub-frames cannot be
     *      created and configured
     */
    protected void setProject(Project project) throws ExecutionException {
        this.project = project;
        referencedFrames = new HashMap();

        for (Iterator i = project.getReferencedProjectNames(); i.hasNext(); ) {
            String referenceName = (String)i.next();
            Project referencedProject
                 = project.getReferencedProject(referenceName);
            ExecutionFrame referencedFrame = createFrame(referencedProject);
            referencedFrames.put(referenceName, referencedFrame);

        }

        configureServices();
        componentManager.setStandardLibraries(standardLibs);
        setMagicProperties();
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
        ExecutionFrame frame = getContainingFrame(name);
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
        // set up various magic properties
        setDataValue(MagicProperties.ANT_HOME,
            initConfig.getAntHome().toString(), true);
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
            String refName = (String)i.next();
            ExecutionFrame refFrame = getReferencedFrame(refName);
            Map refProperties = refFrame.getAllProperties();
            Iterator j = refProperties.keySet().iterator();
            while (j.hasNext()) {
                String name = (String)j.next();
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
     * Gets the baseDir of the ExecutionFrame
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
     * @return the ExecutionFrame asscociated with the given reference name
     *      or null if there is no such project.
     */
    protected ExecutionFrame getReferencedFrame(String referenceName) {
        return (ExecutionFrame)referencedFrames.get(referenceName);
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
        ExecutionFrame frame = getContainingFrame(name);
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
        ExecutionFrame frame = getContainingFrame(name);
        if (frame == this) {
            return dataValues.containsKey(name);
        } else {
            return frame.isDataValueSet(getNameInFrame(name));
        }
    }

    /**
     * Add a collection of properties to this frame
     *
     * @param properties the collection of property values, indexed by their
     *      names
     * @exception ExecutionException if the frame cannot be created.
     */
    protected void addProperties(Map properties) throws ExecutionException {
        for (Iterator i = properties.keySet().iterator(); i.hasNext(); ) {
            String name = (String)i.next();
            Object value = properties.get(name);
            setDataValue(name, value, false);
        }
    }

    /**
     * Create a new frame for a given project
     *
     * @param project the project model the frame will deal with
     * @return an ExecutionFrame ready to build the project
     * @exception ExecutionException if the frame cannot be created.
     */
    protected ExecutionFrame createFrame(Project project)
         throws ExecutionException {
        ExecutionFrame newFrame
             = new ExecutionFrame(standardLibs, initConfig, config);
        newFrame.setProject(project);
        for (Iterator j = eventSupport.getListeners(); j.hasNext(); ) {
            BuildListener listener = (BuildListener)j.next();
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
        for (Iterator i = getReferencedFrames(); i.hasNext(); ) {
            ExecutionFrame referencedFrame = (ExecutionFrame)i.next();
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
        for (Iterator i = getReferencedFrames(); i.hasNext(); ) {
            ExecutionFrame subFrame = (ExecutionFrame)i.next();
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
        determineBaseDirs();

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
            for (Iterator i = targets.iterator(); i.hasNext(); ) {
                String targetName = (String)i.next();
                log("Executing target: " + targetName, MessageLevel.MSG_DEBUG);
                executeTarget(targetName);
            }
        }
    }

    /**
     * Execute the tasks of a target in this frame with the given name
     *
     * @param targetName the name of the target whose tasks will be
     *      evaluated
     * @exception ExecutionException if there is a problem executing the
     *      tasks of the target
     */
    protected void executeTarget(String targetName) throws ExecutionException {
        // to execute a target we must determine its dependencies and
        // execute them in order.

        try {
            // firstly build a list of fully qualified target names to execute.
            List dependencyOrder = project.getTargetDependencies(targetName);
            for (Iterator i = dependencyOrder.iterator(); i.hasNext(); ) {
                String fullTargetName = (String)i.next();
                ExecutionFrame frame = getContainingFrame(fullTargetName);
                String localTargetName = getNameInFrame(fullTargetName);
                frame.executeTargetTasks(localTargetName);
            }
        } catch (ConfigException e) {
            throw new ExecutionException(e);
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
            Throwable failureCause = null;
            BuildElement model = (BuildElement)taskIterator.next();
            // what sort of element is this.
            ImportInfo importInfo
                 = componentManager.getDefinition(model.getType());
            if (importInfo == null) {
                throw new ExecutionException("There is no definition for the <"
                     + model.getType() + "> element", model.getLocation());
            }

            try {
                if (importInfo.getDefinitionType() == AntLibrary.TASKDEF) {
                    TaskContext taskContext = configureTask(model);
                    eventSupport.fireTaskStarted(model);

                    ClassLoader currentLoader
                         = setContextLoader(taskContext.getLoader());
                    taskContext.execute();
                    setContextLoader(currentLoader);
                    taskContext.destroy();
                } else {
                    // typedef
                    String typeId = model.getAspectValue(ANT_ASPECT, "id");
                    Object typeInstance = configureType(model.getType(), model);
                    if (typeId != null) {
                        setDataValue(typeId, typeInstance, true);
                    }
                }
            } catch (AntException te) {
                ExecutionException e
                     = new ExecutionException(te, te.getLocation());
                e.setLocation(model.getLocation(), false);
                failureCause = e;
                throw e;
            } catch (RuntimeException e) {
                ExecutionException ee =
                    new ExecutionException(e.getClass().getName() + ": "
                     + e.getMessage(), e, model.getLocation());
                failureCause = ee;
                throw ee;
            } finally {
                eventSupport.fireTaskFinished(model, failureCause);
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
                new ExecutionException(e.getClass().getName() + ": "
                 + e.getMessage(), e, target.getLocation());
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
        for (Iterator i = getReferencedFrames(); i.hasNext(); ) {
            ExecutionFrame referencedFrame = (ExecutionFrame)i.next();
            referencedFrame.initialize();
        }
        Iterator taskIterator = project.getTasks();
        executeTasks(taskIterator);
    }


    /**
     * Gets the reflector for the given class
     *
     * @param c the class for which the reflector is desired
     * @return the reflector
     */
    private Reflector getReflector(Class c) {
        if (reflectors.containsKey(c)) {
            return (Reflector)reflectors.get(c);
        }
        ClassIntrospector introspector
             = new ClassIntrospector(c, componentManager.getConverters());
        Reflector reflector = introspector.getReflector();
        reflectors.put(c, reflector);
        return reflector;
    }


    /**
     * Get the execution frame which contains, directly, the named target
     * where the name is relative to this frame
     *
     * @param targetName The name of the target
     * @return the execution frame for the project that contains the given
     *      target
     */
    private ExecutionFrame getContainingFrame(String targetName) {
        int index = targetName.lastIndexOf(Project.REF_DELIMITER);
        if (index == -1) {
            return this;
        }

        ExecutionFrame currentFrame = this;
        String relativeName = targetName.substring(0, index);
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
     * Determine the base directory for each frame in the frame hierarchy
     *
     * @exception ExecutionException if the base directories cannot be
     *      determined
     */
    private void determineBaseDirs() throws ExecutionException {
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
                    FileUtils fileUtils = new FileUtils();
                    baseDir = fileUtils.resolveFile(projectFileParent, base);
                }
            } else {
                baseDir = new File(".");
            }
        }
        setDataValue(MagicProperties.BASEDIR, baseDir.getAbsolutePath(), true);

        for (Iterator i = getReferencedFrames(); i.hasNext(); ) {
            ExecutionFrame refFrame = (ExecutionFrame)i.next();
            refFrame.determineBaseDirs();
        }
    }

    /**
     * Configure the services that the frame makes available to its library
     * components
     */
    private void configureServices() {
        // create services and make them available in our services map
        fileService = new ExecutionFileService(this);
        componentManager
             = new ComponentManager(this, config.isRemoteLibAllowed());
        dataService = new ExecutionDataService(this,
            config.isUnsetPropertiesAllowed());

        services.put(FileService.class, fileService);
        services.put(ComponentService.class, componentManager);
        services.put(DataService.class, dataService);
        services.put(EventService.class,  new CoreEventService(this));
    }

    /**
     * Configure an element according to the given model.
     *
     * @param element the object to be configured
     * @param model the BuildElement describing the object in the build file
     * @param factory Ant Library factory associated with the element being
     *      configured
     * @exception ExecutionException if the element cannot be configured
     */
    private void configureElement(AntLibFactory factory, Object element,
                                  BuildElement model)
         throws ExecutionException {

        Reflector reflector = getReflector(element.getClass());

        // start by setting the attributes of this element
        for (Iterator i = model.getAttributeNames(); i.hasNext(); ) {
            String attributeName = (String)i.next();
            String attributeValue = model.getAttributeValue(attributeName);
            if (!reflector.supportsAttribute(attributeName)) {
                throw new ExecutionException(model.getType()
                     + " does not support the \"" + attributeName
                     + "\" attribute", model.getLocation());
            }
            reflector.setAttribute(element, attributeName,
                dataService.replacePropertyRefs(attributeValue));
        }
        String modelText = model.getText().trim();
        if (modelText.length() != 0) {
            if (!reflector.supportsText()) {
                throw new ExecutionException(model.getType()
                     + " does not support content", model.getLocation());
            }
            reflector.addText(element,
                dataService.replacePropertyRefs(modelText));
        }

        // now do the nested elements
        for (Iterator i = model.getNestedElements(); i.hasNext(); ) {
            BuildElement nestedElementModel = (BuildElement)i.next();
            String nestedElementName = nestedElementModel.getType();

            ImportInfo info = componentManager.getDefinition(nestedElementName);
            if (element instanceof TaskContainer
                 && info != null
                 && info.getDefinitionType() == AntLibrary.TASKDEF
                 && !reflector.supportsNestedElement(nestedElementName)) {
                // it is a nested task
                TaskContext nestedContext
                     = configureTask(nestedElementModel);
                TaskContainer container = (TaskContainer)element;
                // XXX what should we be adding - need to understand container
                // method of executing tasks
                container.addTask(nestedContext.getTask());
            } else {
                if (reflector.supportsNestedAdder(nestedElementName)) {
                    addNestedElement(factory, reflector, element,
                        nestedElementModel);
                } else if (reflector.supportsNestedCreator(nestedElementName)) {
                    createNestedElement(factory, reflector, element,
                        nestedElementModel);
                } else {
                    throw new ExecutionException(model.getType()
                         + " does not support the \"" + nestedElementName
                         + "\" nested element",
                        nestedElementModel.getLocation());
                }
            }
        }

    }

    /**
     * Create a nested element for the given object according to the model.
     *
     * @param reflector the reflector instance of the container object
     * @param element the container object for which a nested element is
     *      required.
     * @param model the build model for the nestd element
     * @param factory Ant Library factory associated with the element
     *      creating the nested element
     * @exception ExecutionException if the nested element cannot be
     *      created.
     */
    private void createNestedElement(AntLibFactory factory, Reflector reflector,
                                     Object element, BuildElement model)
         throws ExecutionException {
        log("The use of create methods is deprecated - class: "
             + element.getClass().getName(), MessageLevel.MSG_INFO);

        String nestedElementName = model.getType();
        try {
            Object nestedElement
                 = reflector.createElement(element, nestedElementName);
            factory.registerCreatedElement(nestedElement);
            if (nestedElement instanceof ExecutionComponent) {
                ExecutionComponent component
                     = (ExecutionComponent)nestedElement;
                ExecutionContext context
                     = new ExecutionContext(this);
                context.setModelElement(model);
                component.init(context);
                configureElement(factory, nestedElement, model);
                component.validateComponent();
            } else {
                configureElement(factory, nestedElement, model);
            }
        } catch (ExecutionException e) {
            e.setLocation(model.getLocation(), false);
            throw e;
        } catch (RuntimeException e) {
            throw new ExecutionException(e.getClass().getName() + ": "
                 + e.getMessage(), e, model.getLocation());
        }
    }


    /**
     * Create and add a nested element
     *
     * @param reflector The reflector instance for the container element
     * @param element the container element in which the nested element will
     *      be created
     * @param model the model of the nested element
     * @param factory Ant Library factory associated with the element to
     *      which the attribute is to be added.
     * @exception ExecutionException if the nested element cannot be created
     */
    private void addNestedElement(AntLibFactory factory, Reflector reflector,
                                  Object element, BuildElement model)
         throws ExecutionException {

        String nestedElementName = model.getType();
        Class nestedType = reflector.getType(nestedElementName);

        // is there a polymorph indicator - look in Ant aspects
        String typeName = model.getAspectValue(ANT_ASPECT, "type");
        String refId = model.getAspectValue(ANT_ASPECT, "refid");
        if (refId != null && typeName != null) {
            throw new ExecutionException("Only one of " + ANT_ASPECT
                 + ":type and " + ANT_ASPECT
                 + ":refid may be specified at a time", model.getLocation());
        }

        Object typeInstance = null;
        if (typeName != null) {
            // the build file has specified the actual type of the element.
            // we need to look up that type and use it
            typeInstance = configureType(typeName, model);
        } else if (refId != null) {
            // We have a reference to an existing instance. Need to check if
            // it is compatible with the type expected by the nested element's
            // adder method
            typeInstance = getDataValue(refId);
            if (model.getAttributeNames().hasNext() ||
                model.getNestedElements().hasNext() ||
                model.getText().length() != 0) {
                throw new ExecutionException("Element <" + nestedElementName
                     + "> is defined by reference and hence may not specify "
                     + "any attributes, nested elements or content",
                    model.getLocation());
            }
            if (typeInstance == null) {
                throw new ExecutionException("The given ant:refid value '"
                     + refId + "' is not defined", model.getLocation());
            }
        } else {
            // We need to create an instance of the class expected by the nested
            // element's adder method if that is possible
            if (nestedType.isInterface()) {
                throw new ExecutionException("No element can be created for "
                     + "nested element <" + nestedElementName + ">. Please "
                     + "provide a value by reference or specify the value type",
                    model.getLocation());
            }

            typeInstance = createTypeInstance(nestedType, factory, model);
        }

        // is the typeInstance compatible with the type expected
        // by the element's add method
        if (!nestedType.isInstance(typeInstance)) {
            if (refId != null) {
                throw new ExecutionException("The value specified by refId "
                     + refId + " is not compatible with the <"
                     + nestedElementName + "> nested element",
                    model.getLocation());
            } else if (typeName != null) {
                throw new ExecutionException("The type "
                     + typeName + " is not compatible with the <"
                     + nestedElementName + "> nested element",
                    model.getLocation());
            }
        }
        reflector.addElement(element, nestedElementName, typeInstance);
    }


    /**
     * Create a Task and configure it according to the given model.
     *
     * @param model the model for the task from the build file
     * @return an execution context for managing the task
     * @exception ExecutionException if there is a problem configuring the
     *      task
     */
    private TaskContext configureTask(BuildElement model)
         throws ExecutionException {

        String taskType = model.getType();
        ImportInfo taskDefInfo = componentManager.getDefinition(taskType);
        if (taskDefInfo == null
             || taskDefInfo.getDefinitionType() != AntLibrary.TASKDEF) {
            throw new ExecutionException("There is no defintion for a "
                 + "task of type <" + taskType + ">", model.getLocation());
        }

        String className = taskDefInfo.getClassName();
        AntLibrary antLibrary = taskDefInfo.getAntLibrary();

        try {
            ClassLoader taskClassLoader = antLibrary.getClassLoader();
            Class elementClass
                 = Class.forName(className, true, taskClassLoader);
            AntLibFactory libFactory
                 = componentManager.getLibFactory(antLibrary);
            Object element = libFactory.createTaskInstance(elementClass);

            Task task = null;
            if (element instanceof Task) {
                // create a Task context for the Task
                task = (Task)element;
            } else {
                task = new TaskAdapter(taskType, element);
            }

            // set the context loader while configuring the element
            ClassLoader currentLoader = setContextLoader(taskClassLoader);
            TaskContext taskContext = new TaskContext(this);
            taskContext.init(taskClassLoader, task, model);
            configureElement(libFactory, element, model);
            task.validateComponent();
            setContextLoader(currentLoader);
            return taskContext;
        } catch (ClassNotFoundException e) {
            throw new ExecutionException("Class " + className
                 + " for task <" + taskType + "> was not found", e,
                model.getLocation());
        } catch (NoClassDefFoundError e) {
            throw new ExecutionException("Could not load a dependent class ("
                 + e.getMessage() + ") for task " + taskType,
                e, model.getLocation());
        } catch (InstantiationException e) {
            throw new ExecutionException("Unable to instantiate task class "
                 + className + " for task <" + taskType + ">",
                e, model.getLocation());
        } catch (IllegalAccessException e) {
            throw new ExecutionException("Unable to access task class "
                 + className + " for task <" + taskType + ">",
                e, model.getLocation());
        } catch (ExecutionException e) {
            e.setLocation(model.getLocation(), false);
            throw e;
        } catch (RuntimeException e) {
            throw new ExecutionException(e.getClass().getName() + ": "
                 + e.getMessage(), e, model.getLocation());
        }
    }


    /**
     * Configure a type instance from the given build model. The name given
     * may not match the name in the model type value. This allows the
     * caller to provide a polymorphic type for the type model
     *
     * @param typeName the name of the type which should be created
     * @param model the model describing the type
     * @return an instance of the type, configured from the model
     * @exception ExecutionException if the type could not be created
     */
    private Object configureType(String typeName, BuildElement model)
         throws ExecutionException {
        ImportInfo typeDefInfo = componentManager.getDefinition(typeName);
        if (typeDefInfo == null
             || typeDefInfo.getDefinitionType() != AntLibrary.TYPEDEF) {
            throw new ExecutionException("There is no defintion for a "
                 + "type <" + typeName + ">", model.getLocation());
        }

        String className = typeDefInfo.getClassName();
        AntLibrary antLibrary = typeDefInfo.getAntLibrary();

        try {
            ClassLoader typeClassLoader = antLibrary.getClassLoader();
            Class typeClass
                 = Class.forName(className, true, typeClassLoader);

            ClassLoader currentLoader = setContextLoader(typeClassLoader);
            AntLibFactory libFactory
                 = componentManager.getLibFactory(antLibrary);
            Object typeInstance
                 = createTypeInstance(typeClass, libFactory, model);
            setContextLoader(currentLoader);

            return typeInstance;
        } catch (ClassNotFoundException e) {
            throw new ExecutionException("Class " + className
                 + " for type <" + typeName + "> was not found", e,
                model.getLocation());
        } catch (NoClassDefFoundError e) {
            throw new ExecutionException("Could not load a dependent class ("
                 + e.getMessage() + ") for type " + typeName);
        }
    }

    /**
     * Create an instance of a type given its required class
     *
     * @param typeClass the class from which the instance should be created
     * @param model the model describing the required configuration of the
     *      instance
     * @param libFactory the factory object of the typeClass's Ant library
     * @return an instance of the given class appropriately configured
     * @exception ExecutionException if there is a problem creating the type
     *      instance
     */
    private Object createTypeInstance(Class typeClass, AntLibFactory libFactory,
                                      BuildElement model)
         throws ExecutionException {
        try {
            Object typeInstance = libFactory.createTypeInstance(typeClass);

            if (typeInstance instanceof ExecutionComponent) {
                ExecutionComponent component = (ExecutionComponent)typeInstance;
                ExecutionContext context
                     = new ExecutionContext(this);
                context.setModelElement(model);
                component.init(context);
                configureElement(libFactory, typeInstance, model);
                component.validateComponent();
            } else {
                configureElement(libFactory, typeInstance, model);
            }
            return typeInstance;
        } catch (InstantiationException e) {
            throw new ExecutionException("Unable to instantiate type class "
                 + typeClass.getName() + " for type <" + model.getType() + ">",
                e, model.getLocation());
        } catch (IllegalAccessException e) {
            throw new ExecutionException("Unable to access type class "
                 + typeClass.getName() + " for type <" + model.getType() + ">",
                e, model.getLocation());
        } catch (ExecutionException e) {
            e.setLocation(model.getLocation(), false);
            throw e;
        } catch (RuntimeException e) {
            throw new ExecutionException(e.getClass().getName() + ": "
                 + e.getMessage(), e, model.getLocation());
        }
    }
}

