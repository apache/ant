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
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import org.apache.ant.common.converter.Converter;
import org.apache.ant.common.task.DataType;
import org.apache.ant.common.task.Task;
import org.apache.ant.common.task.TaskContainer;
import org.apache.ant.common.task.TaskException;
import org.apache.ant.common.util.Location;
import org.apache.ant.antcore.antlib.AntLibDefinition;
import org.apache.ant.antcore.antlib.AntLibrary;
import org.apache.ant.antcore.event.BuildEventSupport;
import org.apache.ant.antcore.event.BuildListener;
import org.apache.ant.antcore.model.BuildElement;
import org.apache.ant.antcore.model.Project;
import org.apache.ant.antcore.model.Target;
import org.apache.ant.antcore.util.ConfigException;

/**
 * An ExecutionFrame maintains the state of a project during an execution.
 * The ExecutionFrame contains the data values set by Ant tasks as they are
 * executed, including task definitions, property values, etc.
 *
 * @author <a href="mailto:conor@apache.org">Conor MacNeill</a>
 * @created 14 January 2002
 */
public class ExecutionFrame {

    /**
     * This class is used to maintain information about imports
     *
     * @author <a href="mailto:conor@apache.org">Conor MacNeill</a>
     * @created 16 January 2002
     */
    private static class ImportInfo {
        /** the ant library from which the import is made */
        private AntLibrary library;
        /** the library definition information */
        private AntLibDefinition libDefinition;

        /**
         * ImportInfo records what has been imported from an Ant Library
         *
         * @param library The library from which the import was made
         * @param libDefinition the library definition information
         */
        public ImportInfo(AntLibrary library, AntLibDefinition libDefinition) {
            this.library = library;
            this.libDefinition = libDefinition;
        }

        /**
         * Get the classname that has been imported
         *
         * @return the classname that was imported.
         */
        public String getClassName() {
            return libDefinition.getClassName();
        }

        /**
         * Get the library from which the import was made
         *
         * @return the library from which the import was made
         */
        public AntLibrary getAntLibrary() {
            return library;
        }

        /**
         * Get the type of the definition that was imported
         *
         * @return the type of definition
         */
        public int getDefinitionType() {
            return libDefinition.getDefinitionType();
        }

    }

    /** The Ant aspect used to identify Ant metadata */
    public final static String ANT_ASPECT = "ant";

    /** The prefix for library ids that are automatically imported */
    public final static String ANT_LIB_PREFIX = "ant.";

    /** the base dir of the project expressed as a URL */
    private URL baseURL;

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
     * The available libraries from which taskdefs, typedefs, etc may be
     * imported
     */
    private Map antLibraries;

    /** The definitions which have been imported into this frame. */
    private Map definitions = new HashMap();

    /** BuildEvent support used to fire events and manage listeners */
    private BuildEventSupport eventSupport = new BuildEventSupport();

    /**
     * Type converters for this executionFrame. Converters are used when
     * configuring Tasks to handle special type conversions.
     */
    private Map converters = new HashMap();

    /**
     * Create an Execution Frame for the given project
     *
     * @param antLibraries The libraries of tasks and types available to
     *      this frame
     * @exception ConfigException if a component of the library cannot be
     *      imported
     */
    public ExecutionFrame(Map antLibraries) throws ConfigException {
        this.antLibraries = antLibraries;

        // go through the libraries and import all standard ant libraries
        for (Iterator i = antLibraries.keySet().iterator(); i.hasNext(); ) {
            String libraryId = (String)i.next();
            if (libraryId.startsWith(ANT_LIB_PREFIX)) {
                // standard library - import whole library
                importLibrary(libraryId);
            }
        }
    }

    /**
     * This method will parse a string containing ${value} style property
     * values into two lists. The first list is a collection of text
     * fragments, while the other is a set of string property names. Null
     * entries in the first list indicate a property reference from the
     * second list.
     *
     * @param value the string to be parsed
     * @param fragments the fragments parsed out of the string
     * @param propertyRefs the property refs to be replaced
     * @exception ExecutionException if there is a problem getting property
     *      values
     */
    public static void parsePropertyString(String value, List fragments,
                                           List propertyRefs)
         throws ExecutionException {
        int prev = 0;
        int pos;
        while ((pos = value.indexOf("$", prev)) >= 0) {
            if (pos > 0) {
                fragments.add(value.substring(prev, pos));
            }

            if (pos == (value.length() - 1)) {
                fragments.add("$");
                prev = pos + 1;
            } else if (value.charAt(pos + 1) != '{') {
                fragments.add(value.substring(pos + 1, pos + 2));
                prev = pos + 2;
            } else {
                int endName = value.indexOf('}', pos);
                if (endName < 0) {
                    throw new ExecutionException("Syntax error in property: "
                         + value);
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
     * @exception ConfigException if any required sub-frames cannot be
     *      created and configured
     */
    public void setProject(Project project) throws ConfigException {
        try {
            this.project = project;
            baseURL = new URL(project.getSourceURL(), project.getBase());
        } catch (MalformedURLException e) {
            throw new ConfigException("Unable to determine project base dir",
                e, project.getLocation());
        }

        referencedFrames = new HashMap();

        for (Iterator i = project.getReferencedProjectNames(); i.hasNext(); ) {
            String referenceName = (String)i.next();
            Project referencedProject
                 = project.getReferencedProject(referenceName);
            ExecutionFrame referencedFrame
                 = new ExecutionFrame(antLibraries);
            referencedFrame.setProject(referencedProject);
            referencedFrames.put(referenceName, referencedFrame);

            for (Iterator j = eventSupport.getListeners(); j.hasNext(); ) {
                BuildListener listener = (BuildListener)j.next();
                referencedFrame.addBuildListener(listener);
            }
        }
    }

    /**
     * Set a value in this frame or any of its imported frames
     *
     * @param name the name of the value
     * @param value the actual value
     * @exception ExecutionException if the value name is invalid
     */
    public void setDataValue(String name, Object value)
         throws ExecutionException {
        ExecutionFrame frame = getContainingFrame(name);
        if (frame == this) {
            dataValues.put(name, value);
        } else {
            frame.setDataValue(getNameInFrame(name), value);
        }
    }

    /**
     * Gets the baseURL of the ExecutionFrame
     *
     * @return the baseURL value
     */
    public URL getBaseURL() {
        return baseURL;
    }

    /**
     * Get a referenced frame by its reference name
     *
     * @param referenceName the name under which the frame was imported.
     * @return the ExecutionFrame asscociated with the given reference name
     *      or null if there is no such project.
     */
    public ExecutionFrame getReferencedFrame(String referenceName) {
        return (ExecutionFrame)referencedFrames.get(referenceName);
    }

    /**
     * Get the frames representing referenced projects.
     *
     * @return an iterator which returns the referenced ExeuctionFrames..
     */
    public Iterator getReferencedFrames() {
        return referencedFrames.values().iterator();
    }

    /**
     * Get the name of an object in its frame
     *
     * @param fullname The name of the object
     * @return the name of the object within its containing frame
     */
    public String getNameInFrame(String fullname) {
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
    public Object getDataValue(String name) throws ExecutionException {
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
    public boolean isDataValueSet(String name) throws ExecutionException {
        ExecutionFrame frame = getContainingFrame(name);
        if (frame == null) {
            return dataValues.containsKey(name);
        } else {
            return frame.isDataValueSet(getNameInFrame(name));
        }
    }

    /**
     * Add a build listener to this execution frame
     *
     * @param listener the listener to be added to the frame
     */
    public void addBuildListener(BuildListener listener) {
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
    public void removeBuildListener(BuildListener listener) {
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
    public void runBuild(List targets) throws ExecutionException {
        initialize();
        if (targets.isEmpty()) {
            // we just execute the default target if any
            String defaultTarget = project.getDefaultTarget();
            if (defaultTarget != null) {
                executeTarget(defaultTarget);
            }
        } else {
            for (Iterator i = targets.iterator(); i.hasNext(); ) {
                executeTarget((String)i.next());
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
    public void executeTarget(String targetName) throws ExecutionException {
        // to execute a target we must determine its dependencies and
        // execute them in order.

        // firstly build a list of fully qualified target names to execute.
        try {
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
    public void executeTasks(Iterator taskIterator) throws ExecutionException {
        while (taskIterator.hasNext()) {
            Throwable failureCause = null;
            BuildElement model = (BuildElement)taskIterator.next();
            // what sort of element is this.
            ImportInfo importInfo
                 = (ImportInfo)definitions.get(model.getType());
            if (importInfo == null) {
                throw new ExecutionException("There is no definition for the "
                     + model.getType() + " element", model.getLocation());
            }

            try {
                if (importInfo.getDefinitionType() == AntLibrary.TASKDEF) {
                    TaskContext taskContext = configureTask(model);
                    eventSupport.fireTaskStarted(model);

                    ClassLoader currentLoader
                         = setContextLoader(taskContext.getLoader());
                    taskContext.execute();
                    setContextLoader(currentLoader);
                    releaseTaskContext(taskContext);
                } else {
                    // typedef
                    String typeId = model.getAspectValue(ANT_ASPECT, "id");
                    Object typeInstance = configureType(model.getType(), model);
                    if (typeId != null) {
                        setDataValue(typeId, typeInstance);
                    }
                }

            } catch (TaskException te) {
                ExecutionException e
                     = new ExecutionException(te, te.getLocation());
                if (e.getLocation() == null
                     || e.getLocation() == Location.UNKNOWN_LOCATION) {
                    e.setLocation(model.getLocation());
                }
                failureCause = e;
                throw e;
            } catch (RuntimeException e) {
                failureCause = e;
                throw e;
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
    public void executeTargetTasks(String targetName)
         throws ExecutionException {
        Throwable failureCause = null;
        Target target = project.getTarget(targetName);
        try {
            Iterator taskIterator = target.getTasks();
            eventSupport.fireTargetStarted(target);
            executeTasks(taskIterator);
        } catch (ExecutionException e) {
            if (e.getLocation() == null
                 || e.getLocation() == Location.UNKNOWN_LOCATION) {
                e.setLocation(target.getLocation());
            }
            failureCause = e;
            throw e;
        } catch (RuntimeException e) {
            failureCause = e;
            throw e;
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
    public void initialize() throws ExecutionException {
        for (Iterator i = getReferencedFrames(); i.hasNext(); ) {
            ExecutionFrame referencedFrame = (ExecutionFrame)i.next();
            referencedFrame.initialize();
        }
//        Iterator taskIterator = project.getTasks();
//        executeTopLevelTasks(taskIterator);
    }

    /**
     * Replace ${} style constructions in the given value with the string
     * value of the corresponding data types.
     *
     * @param value the string to be scanned for property references.
     * @return the string with all property references replaced
     * @exception ExecutionException if any of the properties do not exist
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
                    throw new ExecutionException("Property " + propertyName
                         + " has not been set");
                }
                fragment = getDataValue(propertyName).toString();
            }
            sb.append(fragment);
        }

        return sb.toString();
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
        ClassIntrospector introspector = new ClassIntrospector(c, converters);
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
     * Add the converters from the given library to those managed by this
     * frame.
     *
     * @param library the library from which the converters are required
     * @exception ConfigException if a converter defined in the library
     *      cannot be instantiated
     */
    private void addLibraryConverters(AntLibrary library)
         throws ConfigException {
        if (!library.hasConverters()) {
            return;
        }

        String className = null;
        try {
            ClassLoader converterLoader = library.getClassLoader();
            for (Iterator i = library.getConverterClassNames(); i.hasNext(); ) {
                className = (String)i.next();
                Class converterClass
                     = Class.forName(className, true, converterLoader);
                if (!Converter.class.isAssignableFrom(converterClass)) {
                    throw new ConfigException("The converter class "
                         + converterClass.getName()
                         + " does not implement the Converter interface");
                }
                Converter converter = (Converter)converterClass.newInstance();
                ExecutionContext context = new ExecutionContext();
                context.initEnvironment(this, eventSupport);
                converter.init(context);
                Class[] converterTypes = converter.getTypes();
                for (int j = 0; j < converterTypes.length; ++j) {
                    converters.put(converterTypes[j], converter);
                }
            }
        } catch (ClassNotFoundException e) {
            throw new ConfigException("Converter Class " + className
                 + " was not found", e);
        } catch (NoClassDefFoundError e) {
            throw new ConfigException("Could not load a dependent class ("
                 + e.getMessage() + ") for converter " + className);
        } catch (InstantiationException e) {
            throw new ConfigException("Unable to instantiate converter class "
                 + className, e);
        } catch (IllegalAccessException e) {
            throw new ConfigException("Unable to access converter class "
                 + className, e);
        }
    }

    /**
     * Configure an element according to the given model.
     *
     * @param element the object to be configured
     * @param model the BuildElement describing the object in the build file
     * @exception ExecutionException if the element cannot be configured
     * @exception TaskException if a nested task has a problem
     */
    private void configureElement(Object element, BuildElement model)
         throws ExecutionException, TaskException {

        Reflector reflector = getReflector(element.getClass());

        // start by setting the attributes of this element
        for (Iterator i = model.getAttributeNames(); i.hasNext(); ) {
            String attributeName = (String)i.next();
            String attributeValue = model.getAttributeValue(attributeName);
            reflector.setAttribute(element, attributeName,
                replacePropertyRefs(attributeValue));
        }
        String modelText = model.getText().trim();
        if (modelText.length() != 0) {
            reflector.addText(element, replacePropertyRefs(modelText));
        }

        // now do the nested elements
        for (Iterator i = model.getNestedElements(); i.hasNext(); ) {
            BuildElement nestedElementModel = (BuildElement)i.next();
            String nestedElementName = nestedElementModel.getType();

            ImportInfo info = (ImportInfo)definitions.get(nestedElementName);
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
                Object nestedElement = createNestedElement(reflector, element,
                    nestedElementModel);
                reflector.addElement(element, nestedElementName, nestedElement);
            }
        }
    }

    /**
     * Create a nested element
     *
     * @param reflector The reflector instance for the container element
     * @param element the container element in which the nested element will
     *      be created
     * @param model the model of the nested element
     * @return a configured nested element
     * @exception ExecutionException if the nested element cannot be created
     * @exception TaskException if the nested element has a problem
     */
    private Object createNestedElement(Reflector reflector, Object element,
                                       BuildElement model)
         throws ExecutionException, TaskException {

        String nestedElementName = model.getType();
        if (!reflector.supportsNestedElement(nestedElementName)) {
            throw new ExecutionException("The element name " + nestedElementName
                 + " is not a supported nested element of "
                 + element.getClass().getName());
        }
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

            typeInstance = createTypeInstance(nestedType, model);
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
        return typeInstance;
    }


    /**
     * Create a Task and configure it according to the given model.
     *
     * @param model the model for the task from the build file
     * @return an execution context for managing the task
     * @exception ExecutionException if there is a problem configuring the
     *      task
     * @exception TaskException if the task or a nested task has a problem
     */
    private TaskContext configureTask(BuildElement model)
         throws ExecutionException, TaskException {

        String taskType = model.getType();
        ImportInfo taskDefInfo = (ImportInfo)definitions.get(taskType);
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
            Object element = elementClass.newInstance();
            Task task = null;
            if (element instanceof Task) {
                // create a Task context for the Task
                task = (Task)element;
            } else {
                task = new TaskAdapter(taskType, element);
            }

            // set the context loader while configuring the element
            ClassLoader currentLoader = setContextLoader(taskClassLoader);
            TaskContext taskContext = allocateTaskContext();
            taskContext.init(taskClassLoader, task, model);
            configureElement(element, model);
            setContextLoader(currentLoader);
            return taskContext;
        } catch (ClassNotFoundException e) {
            throw new ExecutionException("Class " + className
                 + " for task <" + taskType + "> was not found", e,
                model.getLocation());
        } catch (NoClassDefFoundError e) {
            throw new ExecutionException("Could not load a dependent class ("
                 + e.getMessage() + ") for task " + taskType);
        } catch (InstantiationException e) {
            throw new ExecutionException("Unable to instantiate task class "
                 + className + " for task <" + taskType + ">",
                e, model.getLocation());
        } catch (IllegalAccessException e) {
            throw new ExecutionException("Unable to access task class "
                 + className + " for task <" + taskType + ">",
                e, model.getLocation());
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
     * @exception TaskException there was a problem within the type
     */
    private Object configureType(String typeName, BuildElement model)
         throws ExecutionException, TaskException {
        ImportInfo typeDefInfo = (ImportInfo)definitions.get(typeName);
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
            Object typeInstance = createTypeInstance(typeClass, model);
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
     * @return an instance of the given class appropriately configured
     * @exception ExecutionException if there is a problem creating the type
     *      instance
     * @exception TaskException if there is a problem configuring the type
     *      instance.
     */
    private Object createTypeInstance(Class typeClass, BuildElement model)
         throws ExecutionException, TaskException {
        try {
            Object typeInstance = typeClass.newInstance();
            if (typeInstance instanceof DataType) {
                DataType dataType = (DataType)typeInstance;
                TypeContext typeContext = new TypeContext();
                typeContext.initEnvironment(this, eventSupport);
                typeContext.init(dataType, model);
            }
            configureElement(typeInstance, model);
            return typeInstance;
        } catch (InstantiationException e) {
            throw new ExecutionException("Unable to instantiate type class "
                 + typeClass.getName() + " for type <" + model.getType() + ">",
                e, model.getLocation());
        } catch (IllegalAccessException e) {
            throw new ExecutionException("Unable to access type class "
                 + typeClass.getName() + " for type <" + model.getType() + ">",
                e, model.getLocation());
        }
    }


    /**
     * Allocate a context for use
     *
     * @return ExecutionContext for use
     */
    private TaskContext allocateTaskContext() {
        TaskContext context = new TaskContext();
        context.initEnvironment(this, eventSupport);
        return context;
    }

    /**
     * Release a context. Any associated tasks are destroyed.
     *
     * @param context the cotext to be released
     */
    private void releaseTaskContext(TaskContext context) {
        context.destroy();
    }


    /**
     * Import a complete library into this frame
     *
     * @param libraryId The id of the library to be imported
     * @exception ConfigException if the library cannot be imported
     */
    private void importLibrary(String libraryId) throws ConfigException {
        AntLibrary library = (AntLibrary)antLibraries.get(libraryId);
        Map libDefs = library.getDefinitions();
        for (Iterator i = libDefs.keySet().iterator(); i.hasNext(); ) {
            String defName = (String)i.next();
            AntLibDefinition libdef
                 = (AntLibDefinition)libDefs.get(defName);
            definitions.put(defName, new ImportInfo(library, libdef));
        }
        addLibraryConverters(library);
    }
}

