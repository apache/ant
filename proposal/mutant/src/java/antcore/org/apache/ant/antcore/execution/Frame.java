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
import java.util.Map;
import java.util.StringTokenizer;
import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import org.apache.ant.antcore.config.AntConfig;
import org.apache.ant.antcore.antlib.AntLibManager;
import org.apache.ant.common.antlib.Task;
import org.apache.ant.common.antlib.Aspect;
import org.apache.ant.common.event.BuildListener;
import org.apache.ant.common.event.MessageLevel;
import org.apache.ant.common.model.BuildElement;
import org.apache.ant.common.model.ModelException;
import org.apache.ant.common.model.Project;
import org.apache.ant.common.model.Target;
import org.apache.ant.common.model.NamespaceValueCollection;
import org.apache.ant.common.service.ComponentService;
import org.apache.ant.common.service.DataService;
import org.apache.ant.common.service.EventService;
import org.apache.ant.common.service.ExecService;
import org.apache.ant.common.service.FileService;
import org.apache.ant.common.service.InputService;
import org.apache.ant.common.service.MagicProperties;
import org.apache.ant.common.util.DemuxOutputReceiver;
import org.apache.ant.common.util.DataValue;
import org.apache.ant.common.util.FileUtils;
import org.apache.ant.common.util.Location;
import org.apache.ant.common.util.AntException;
import org.apache.ant.init.AntEnvironment;
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
     * Ant's initialization configuration with information on the location of
     * Ant and its libraries.
     */
    private AntEnvironment antEnv;

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
    private CoreDataService dataService;

    /** The execution file service instance */
    private CoreFileService fileService;

    /**
     * the Component Manager used to manage the importing of library
     * components from the Ant libraries
     */
    private ComponentManager componentManager;

    /** The core's execution Service */
    private CoreExecService execService;

    /** The parent frame of this frame - may be null. */
    private Frame parent = null;

    /** The currently executing target in this frame */
    private String currentTarget = null;

    /** The global library manager */
    private AntLibManager libManager;

    /**
     * Create the main or root Execution Frame.
     *
     * @param config the user config to use for this execution of Ant
     * @param antEnv Ant's initialisation config
     */
    public Frame(AntEnvironment antEnv, AntConfig config) {
        this.config = config;
        this.antEnv = antEnv;
        this.parent = null;
        this.libManager
            = new AntLibManager(antEnv, config.isRemoteLibAllowed());
    }

    /**
     * Create an Execution Frame.
     *
     * @param config the user config to use for this execution of Ant
     * @param antEnv Ant's initialisation config
     * @param parent the frame creating this frame.
     */
    private Frame(AntEnvironment antEnv, AntConfig config, Frame parent) {
        this.config = config;
        this.antEnv = antEnv;
        this.parent = parent;
        this.libManager = parent.libManager;
    }


    /**
     * Replace ${} style constructions in the given value with the string
     * value of the corresponding data values in the frame
     *
     * @param value the string to be scanned for property references.
     * @return the string with all property references replaced
     * @exception AntException if any of the properties do not exist
     */
    protected String replacePropertyRefs(String value)
         throws AntException {
        return dataService.replacePropertyRefs(value);
    }


    /**
     * Sets the Project of the Frame
     *
     * @param project The new Project value
     * @exception ModelException if the project is not valid.
     */
    public void setProject(Project project) throws ModelException {
        this.project = project;
        referencedFrames.clear();
        project.validate();
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
     * Initialize the frame.
     *
     * @param initialDataValues a Map of named DataValue instances.
     * @exception AntException if the frame cannot be initialized.
     */
    public void initialize(Map initialDataValues) throws AntException {
        configureServices();
        // add in system properties
        dataService.addProperties(System.getProperties(),
            DataValue.PRIORITY_BASE);
        if (initialDataValues != null) {
            dataService.addDataValues(initialDataValues);
        }
        setMagicProperties();
    }

    /**
     * Set the values of various magic properties
     *
     * @exception AntException if the properties cannot be set
     */
    protected void setMagicProperties() throws AntException {
        // ant.home
        URL antHomeURL = antEnv.getAntHome();
        String antHomeString = null;

        if (antHomeURL.getProtocol().equals("file")) {
            File antHome = new File(antHomeURL.getFile());

            antHomeString = antHome.getAbsolutePath();
        } else {
            antHomeString = antHomeURL.toString();
        }
        DataValue antHomeValue
            = new DataValue(antHomeString, DataValue.PRIORITY_USER);
        dataService.setDataValue(MagicProperties.ANT_HOME, antHomeValue,
            false);

        // ant.file
        URL projectSource = project.getSourceURL();
        if (projectSource != null
             && projectSource.getProtocol().equals("file")) {
            DataValue antFileValue = new DataValue(projectSource.getFile(),
                DataValue.PRIORITY_USER);
            dataService.setDataValue(MagicProperties.ANT_FILE,
                antFileValue, true);
        }

        // basedir
        determineBaseDir();

        // ant.project.name
        String projectName = project.getName();
        if (projectName != null) {
            dataService.setDataValue(MagicProperties.ANT_PROJECT_NAME,
                new DataValue(projectName, DataValue.PRIORITY_USER),
                true);
        }

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
     * Get the Ant initialization configuration for this frame.
     *
     * @return Ant's initialization configuration
     */
    protected AntEnvironment getAntEnvironment() {
        return antEnv;
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
     * @return an iterator which returns the referenced ExeuctionFrames.
     */
    protected Iterator getReferencedFrames() {
        return referencedFrames.values().iterator();
    }

    /**
     * Get the names used for referenced projects
     *
     * @return an iterator which returns the referenced frame names.
     */
    protected Iterator getRefNames() {
        return referencedFrames.keySet().iterator();
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
     * Create a project reference.
     *
     * @param name the name under which the project will be
     *      referenced.
     * @param project the project model.
     * @param initialData the project's initial data load.
     * @exception AntException if the project cannot be referenced.
     */
    protected void createProjectReference(String name, Project project,
                                          Map initialData)
        throws AntException {
        Frame referencedFrame = createFrame(project);
        addListeners(referencedFrame);


        Map overrideProperties = dataService.getOverrides(name);
        Map values = new HashMap();
        dataService.mergeDataValues(values, initialData);
        dataService.mergeDataValues(values, overrideProperties);
        referencedFrame.initialize(values);
        dataService.removeOverrides(name);

        referencedFrames.put(name, referencedFrame);
        referencedFrame.importStandardComponents();
        referencedFrame.runGlobalTasks();
    }

    /**
     * Create a new frame for a given project
     *
     * @param project the project model the frame will deal with
     * @return an Frame ready to build the project
     * @exception ModelException if the given project is not valid.
     */
    protected Frame createFrame(Project project)
         throws ModelException {
        Frame newFrame
             = new Frame(antEnv, config, this);

        newFrame.setProject(project);

        return newFrame;
    }

    /**
     * Add all build listeners from this frame to the given sub frame.
     *
     * @param subFrame the subFrame to which all the listeners of this frame
     *        will be added.
     */
    protected void addListeners(Frame subFrame) {
        List listeners = eventSupport.getListeners();
        for (Iterator j = listeners.iterator(); j.hasNext();) {
            BuildListener listener = (BuildListener) j.next();

            subFrame.addBuildListener(listener);
        }
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
    public void addBuildListener(BuildListener listener) {
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
     * Import any standard components from the libraries which have been loaded.
     * A standard component is a component provided by a library in the ant
     * namespace.
     *
     * @exception AntException if the standard components cannot be imported.
     */
    private void importStandardComponents() throws AntException {
        componentManager.importStandardComponents();
    }

    /**
     * Run the given list of targets
     *
     * @param targets a list of target names which are to be evaluated
     * @exception AntException if there is a problem in the build
     */
    protected void runBuild(List targets) throws AntException {
        log("Running build.", MessageLevel.DEBUG);
        importStandardComponents();
        runGlobalTasks();
        if (targets.isEmpty()) {
            // we just execute the default target if any
            String defaultTarget = project.getDefaultTarget();

            if (defaultTarget != null) {
                log("Executing default target: " + defaultTarget,
                    MessageLevel.DEBUG);
                executeTarget(defaultTarget);
            }
        } else {
            for (Iterator i = targets.iterator(); i.hasNext();) {
                String targetName = (String) i.next();

                log("Executing target: " + targetName, MessageLevel.DEBUG);
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
     * @param targetRefLocation the location requesting this dependency.
     * @exception ExecutionException if the given target does not exist in the
     *      project hierarchy
     */
    private void flattenDependency(List flattenedList, String fullTargetName,
                                   Location targetRefLocation)
         throws ExecutionException {
        if (flattenedList.contains(fullTargetName)) {
            return;
        }
        String fullProjectName = getFullProjectName(fullTargetName);
        Frame frame = getContainingFrame(fullTargetName);
        if (frame == null) {
            throw new ExecutionException("No project available under the "
                + "referenced name \"" + fullTargetName, targetRefLocation);
        }
        String localTargetName = getNameInFrame(fullTargetName);
        Target target = frame.getProject().getTarget(localTargetName);
        if (target == null) {
            throw new ExecutionException("Target \"" + fullTargetName
                 + "\" does not exist", targetRefLocation);
        }
        for (Iterator i = target.getDependencies(); i.hasNext();) {
            String localDependencyName = (String) i.next();
            String fullDependencyName = localDependencyName;
            if (fullProjectName != null) {
                fullDependencyName = fullProjectName + Project.REF_DELIMITER
                    + localDependencyName;
            }
            flattenDependency(flattenedList, fullDependencyName,
                target.getLocation());
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
        flattenDependency(flattenedList, fullTargetName, null);
        flattenedList.add(fullTargetName);
        return flattenedList;
    }


    /**
     * Execute the tasks of a target in this frame with the given name
     *
     * @param targetName the name of the target whose tasks will be evaluated
     * @exception AntException if there is a problem executing the tasks
     *      of the target
     */
    protected void executeTarget(String targetName) throws AntException {

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
     * Execute a task with the given aspect values.
     *
     * @param task the task to be executed.
     * @param namespaceValues the collection of namespace attribute values.
     * @exception AntException if the task has a problem.
     */
    protected void executeTask(Task task,
                               NamespaceValueCollection namespaceValues)
         throws AntException {

        List aspects = componentManager.getAspects();
        Map aspectContexts = new HashMap();
        for (Iterator i = aspects.iterator(); i.hasNext();) {
            Aspect aspect = (Aspect) i.next();
            Object aspectContext = aspect.preExecuteTask(task, namespaceValues);
            if (aspectContext != null) {
                aspectContexts.put(aspect, aspectContext);
            }
        }
        if (aspectContexts.size() != 0) {
            aspectContextsMap.put(task, aspectContexts);
        }

        eventSupport.fireTaskStarted(task);

        Throwable failureCause = null;

        ExecutionContext execContext = (ExecutionContext) task.getAntContext();
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
            if (failureCause instanceof AntException) {
                throw (AntException) failureCause;
            }
            throw new ExecutionException(failureCause);
        }
    }


    /**
     * Run the tasks returned by the given iterator
     *
     * @param taskIterator the iterator giving the tasks to execute
     * @exception AntException if there is execution problem while
     *      executing tasks
     */
    protected void executeTasks(Iterator taskIterator)
         throws AntException {

        if (taskIterator == null) {
            return;
        }

        while (taskIterator.hasNext()) {
            BuildElement model = (BuildElement) taskIterator.next();

            // what sort of element is this.
            try {
                Object component = componentManager.createComponent(model);
                if (component instanceof Task) {
                    execService.executeTask((Task) component);
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
     * Get the parent frame of this frame.
     *
     * @return the parent frame - may be null if this frame has no parent.
     */
    private Frame getParent() {
        return parent;
    }

    /**
     * Get the currently executing target of this frame
     *
     * @return the name of the current target.
     */
    private String getCurrentTarget() {
        return currentTarget;
    }

    /**
     * Check for recursion - executing the same target in the same buildfile
     * with the same base directory
     *
     * @param targetName the target to check
     *
     * @exception ExecutionException if the target is already being evaluated
     * in a parent frame.
     */
    private void checkRecursion(String targetName) throws ExecutionException {
        Frame checkFrame = getParent();
        while (checkFrame != null) {
            File checkDir = checkFrame.getBaseDir();
            String checkTarget = checkFrame.getCurrentTarget();
            URL checkURL = checkFrame.getProject().getSourceURL();
            if (targetName.equals(checkTarget)
                && checkDir.equals(getBaseDir())
                && checkURL != null
                && checkURL.equals(getProject().getSourceURL())) {
                throw new ExecutionException("Recursive execution of "
                    + "target \"" + targetName + "\" in "
                    + "project \"" + checkURL + "\"");
            }
            checkFrame = checkFrame.getParent();
        }
    }

    /**
     * Initialize a library in this frame.
     *
     * @param libraryId the library's global identifier.
     *
     * @exception AntException if the library cannot be initialized.
     */
    protected void initializeLibrary(String libraryId)
         throws AntException {
        componentManager.initializeLibrary(libraryId);
    }


    /**
     * Execute the given target's tasks. The target must be local to this
     * frame's project
     *
     * @param targetName the name of the target within this frame that is to
     *      be executed.
     * @exception AntException if there is a problem executing tasks
     */
    protected void executeTargetTasks(String targetName)
         throws AntException {
        checkRecursion(targetName);
        currentTarget = targetName;

        Throwable failureCause = null;
        Target target = project.getTarget(targetName);
        String ifCondition = target.getIfCondition();
        String unlessCondition = target.getUnlessCondition();

        if (ifCondition != null) {
            ifCondition = dataService.replacePropertyRefs(ifCondition.trim());
            if (!dataService.isDataValueSet(ifCondition)) {
                return;
            }
        }

        if (unlessCondition != null) {
            unlessCondition
                 = dataService.replacePropertyRefs(unlessCondition.trim());
            if (dataService.isDataValueSet(unlessCondition)) {
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
            currentTarget = null;
        }

    }

    /**
     * Start the build. This is only called on the
     * main frame of the build. All subordinate frames use runBuild to
     * process sub builds.
     *
     * This method performs all global config tasks and then starts the
     * build
     *
     * @param targets the targets to be evaluated in this build
     *
     * @exception AntException if there is a problem running the build.
     */
    public void startBuild(List targets) throws AntException {
        eventSupport.fireBuildStarted(project);

        Throwable buildFailureCause = null;
        try {
            // load system ant lib
            log("Loading system antlibs.", MessageLevel.DEBUG);
            URL systemLibsURL = antEnv.getSyslibsURL();
            componentManager.loadLib(systemLibsURL, false);
            log("Importing standard components.", MessageLevel.DEBUG);
            importStandardComponents();

            log("Executing global configuration tasks", MessageLevel.DEBUG);
            executeTasks(config.getGlobalTasks());

            // now load other system libraries
            log("Loading standard antlibs.", MessageLevel.DEBUG);
            URL antLibsURL = antEnv.getAntlibsURL();
            componentManager.loadLib(antLibsURL, false);

            runBuild(targets);
        } catch (RuntimeException e) {
            buildFailureCause = e;
            throw e;
        } catch (AntException e) {
            buildFailureCause = e;
            throw e;
        } catch (Throwable e) {
            ExecutionException ee =
                new ExecutionException("Unable to initialize antlibs", e);
            buildFailureCause = ee;
            throw ee;
        } finally {
            eventSupport.fireBuildFinished(project, buildFailureCause);
        }
    }

    /**
     * Execute any config and project level tasks
     *
     * @exception AntException if the top level tasks of the frame
     *      failed
     */
    private void runGlobalTasks() throws AntException {
        executeTasks(config.getFrameTasks());
        executeTasks(project.getTasks());
    }

    /**
     * Set the base director for this frame's execution.
     *
     * @param baseDir the new base directory
     *
     * @exception AntException if the base directory cannot be set.
     */
    protected void setBaseDir(File baseDir) throws AntException {
        FileUtils fileUtils = FileUtils.newFileUtils();

        baseDir = fileUtils.normalize(baseDir.getAbsolutePath());
        if (!baseDir.exists()) {
            throw new ExecutionException("Basedir " + baseDir.getAbsolutePath()
                + " does not exist");
        }
        if (!baseDir.isDirectory()) {
            throw new ExecutionException("Basedir " + baseDir.getAbsolutePath()
                + " is not a directory");
        }
        this.baseDir = baseDir;
        dataService.setDataValue(MagicProperties.BASEDIR,
            new DataValue(baseDir.getPath(), DataValue.PRIORITY_USER),
            false);
        log("Project base dir set to: " + this.baseDir,
            MessageLevel.VERBOSE);
    }


    /**
     * Determine the base directory.
     *
     * @exception AntException if the base directories cannot be
     *      determined
     */
    private void determineBaseDir() throws AntException {
        if (dataService.isDataValueSet(MagicProperties.BASEDIR)) {
            String baseDirString
                = dataService.getDataValue(MagicProperties.BASEDIR).toString();
            setBaseDir(new File(baseDirString));
        } else {
            URL projectURL = project.getSourceURL();

            if (projectURL.getProtocol().equals("file")) {
                File projectFile = new File(projectURL.getFile());
                File projectFileParent = projectFile.getParentFile();
                String base = project.getBase();

                if (base == null) {
                    setBaseDir(projectFileParent);
                } else {
                    FileUtils fileUtils = FileUtils.newFileUtils();

                    setBaseDir(fileUtils.resolveFile(projectFileParent, base));
                }
            } else {
                setBaseDir(new File("."));
            }
        }
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
        componentManager = new ComponentManager(this, libManager);
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


