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
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.ant.antcore.antlib.AntLibDefinition;
import org.apache.ant.antcore.antlib.AntLibManager;
import org.apache.ant.antcore.antlib.AntLibrary;
import org.apache.ant.antcore.antlib.ComponentLibrary;
import org.apache.ant.antcore.antlib.DynamicLibrary;
import org.apache.ant.antcore.config.AntConfig;
import org.apache.ant.common.antlib.AntLibFactory;
import org.apache.ant.common.antlib.Aspect;
import org.apache.ant.common.antlib.Converter;
import org.apache.ant.common.antlib.DeferredTask;
import org.apache.ant.common.antlib.ExecutionComponent;
import org.apache.ant.common.antlib.StandardLibFactory;
import org.apache.ant.common.antlib.Task;
import org.apache.ant.common.antlib.TaskContainer;
import org.apache.ant.common.event.MessageLevel;
import org.apache.ant.common.model.BuildElement;
import org.apache.ant.common.service.ComponentService;
import org.apache.ant.common.util.AntException;
import org.apache.ant.common.util.Location;
import org.apache.ant.init.LoaderUtils;
import org.apache.ant.common.util.AttributeCollection;
import org.apache.ant.common.constants.Namespace;

/**
 * The instance of the ComponentServices made available by the core to the ant
 * libraries.
 *
 * @author Conor MacNeill
 * @created 27 January 2002
 */
public class ComponentManager implements ComponentService {

    /**
     * Type converters for this frame. Converters are used when configuring
     * Tasks to handle special type conversions.
     */
    private Map converters = new HashMap();

    /** This is the set of libraries whose converters have been loaded */
    private Set loadedConverters = new HashSet();

    /** This is the set of libraries whose aspects have been loaded */
    private Set loadedAspects = new HashSet();

    /** The factory objects for each library, indexed by the library Id */
    private Map libFactories = new HashMap();

    /** The Frame this service instance is working for */
    private Frame frame;

    /** The library manager instance used to configure libraries. */
    private AntLibManager libManager;

    /**
     * This is the list of aspects which have been loaded from the various Ant
     * libraries
     */
    private List aspects = new ArrayList();

    /** dynamic libraries which have been defined */
    private Map dynamicLibraries;

    /** The definitions which have been imported into this frame. */
    private Map imports = new HashMap();

    /** Reflector objects used to configure Tasks from the Task models. */
    private Map setters = new HashMap();


    /**
     * Constructor
     *
     * @param frame the frame containing this context
     * @param libManager the library manager with the library definitions
     *        which are shared across all component manager instances.
     * @exception ExecutionException if the loaded libraries could not be
     * imported.
     */
    protected ComponentManager(Frame frame, AntLibManager libManager)
         throws ExecutionException {
        this.frame = frame;
        AntConfig config = frame.getConfig();
        this.libManager = libManager;
        dynamicLibraries = new HashMap();
    }

    /**
     * Load a library or set of libraries from a location making them
     * available for use
     *
     * @param libLocation the file or URL of the library location
     * @param importAll if true all tasks are imported as the library is
     *      loaded
     * @exception AntException if the library cannot be loaded
     */
    public void loadLib(URL libLocation, boolean importAll)
         throws AntException {
        Map newLibraries = libManager.loadLibs(libLocation);
        Iterator i = newLibraries.keySet().iterator();
        while (i.hasNext()) {
            String libraryId = (String) i.next();
            if (importAll) {
                importLibrary(libraryId);
            }
        }
    }

    /**
     * Examine all the libraries defined in the Library manager and import
     * those which are in the ant library namespace.
     *
     * @exception AntException if the standard components cannot be imported.
     */
    protected void importStandardComponents() throws AntException {
        for (Iterator i = libManager.getLibraryIds(); i.hasNext();) {
            String libraryId = (String) i.next();
            if (libraryId.startsWith(Constants.ANT_LIB_PREFIX)) {
                importLibrary(libraryId);
            }
        }
    }

    /**
     * Experimental - define a new task
     *
     * @param taskName the name by which this task will be referred
     * @param factory the library factory object to create the task instances
     * @param loader the class loader to use to create the particular tasks
     * @param className the name of the class implementing the task
     * @exception ExecutionException if the task cannot be defined
     */
    public void taskdef(AntLibFactory factory, ClassLoader loader,
                        String taskName, String className)
         throws ExecutionException {
        defineComponent(factory, loader, ComponentLibrary.TASKDEF,
            taskName, className);
    }

    /**
     * Experimental - define a new type
     *
     * @param typeName the name by which this type will be referred
     * @param factory the library factory object to create the type instances
     * @param loader the class loader to use to create the particular types
     * @param className the name of the class implementing the type
     * @exception ExecutionException if the type cannot be defined
     */
    public void typedef(AntLibFactory factory, ClassLoader loader,
                        String typeName, String className)
         throws ExecutionException {
        defineComponent(factory, loader, ComponentLibrary.TYPEDEF,
            typeName, className);
    }

    /**
     * Add a library path for the given library
     *
     * @param libraryId the unique id of the library for which an additional
     *      path is being defined
     * @param libPath the library path (usually a jar)
     * @exception AntException if the path cannot be specified
     */
    public void addLibPath(String libraryId, URL libPath)
         throws AntException {
        libManager.addLibPath(libraryId, libPath);
    }

    /**
     * Import a complete library into the current execution frame
     *
     * @param libraryId The id of the library to be imported
     * @exception AntException if the library cannot be imported
     */
    public void importLibrary(String libraryId) throws AntException {
        AntLibrary library = libManager.getLibrary(libraryId);
        if (library == null) {
            throw new ExecutionException("Unable to import library " + libraryId
                 + " as it has not been loaded");
        }
        for (Iterator i = library.getDefinitionNames(); i.hasNext();) {
            String defName = (String) i.next();
            importLibraryDef(library, defName, null);
        }
        addConverters(library);
        addAspects(library);
    }

    /**
     * Import a single component from a library, optionally aliasing it to a
     * new name
     *
     * @param libraryId the unique id of the library from which the component
     *      is being imported
     * @param defName the name of the component within its library
     * @param alias the name under which this component will be used in the
     *      build scripts. If this is null, the components default name is
     *      used.
     * @exception AntException if the component cannot be imported
     */
    public void importComponent(String libraryId, String defName,
                                String alias) throws AntException {
        AntLibrary library = libManager.getLibrary(libraryId);
        if (library == null) {
            throw new ExecutionException("Unable to import component from "
                 + "library \"" + libraryId + "\" as it has not been loaded");
        }
        importLibraryDef(library, defName, alias);
        addConverters(library);
        addAspects(library);
    }

    /**
     * Imports a component defined in another frame.
     *
     * @param relativeName the qualified name of the component relative to
     *      this execution frame
     * @param alias the name under which this component will be used in the
     *      build scripts. If this is null, the components default name is
     *      used.
     * @exception ExecutionException if the component cannot be imported
     */
    public void importFrameComponent(String relativeName, String alias)
         throws ExecutionException {
        ImportInfo definition
             = frame.getReferencedDefinition(relativeName);

        if (definition == null) {
            throw new ExecutionException("The reference \"relativeName\" does"
                 + " not refer to a defined component");
        }

        String label = alias;
        if (label == null) {
            label = frame.getNameInFrame(relativeName);
        }

        frame.log("Adding referenced component <" + definition.getLocalName()
             + "> as <" + label + "> from library \""
             + definition.getComponentLibrary().getLibraryId() + "\", class: "
             + definition.getClassName(), MessageLevel.MSG_DEBUG);
        imports.put(label, definition);
    }

    /**
     * Create a component. The component will have a context but will not be
     * configured. It should be configured using the appropriate set methods
     * and then validated before being used.
     *
     * @param componentName the name of the component
     * @return the created component. The return type of this method depends
     *      on the component type.
     * @exception AntException if the component cannot be created
     */
    public Object createComponent(String componentName)
         throws AntException {
        return createComponent(componentName, (BuildElement) null);
    }

    /**
     * Create a component given its libraryId and local name within the
     * library. This method is unambiguous in the face of imports, aliases and
     * taskdefs performed in the build.
     *
     * @param libraryId the component's library identifier.
     * @param localName the name component within the library.
     * @return the created component. The return type of this method depends
     *      on the component type.
     * @exception AntException if the component cannot be created
     */
    public Object createComponent(String libraryId, String localName)
         throws AntException {
        AntLibrary library = libManager.getLibrary(libraryId);
        if (library == null) {
            throw new ExecutionException("No library with libraryId \""
                 + libraryId + "\" is available");
        }

        AntLibDefinition libDefinition = library.getDefinition(localName);
        if (libDefinition == null) {
            throw new ExecutionException("No component with name \""
                 + localName + "\" was found in library with libraryId \""
                 + libraryId + "\"");
        }
        return createComponentFromDef(localName, library, libDefinition, null);
    }

    /**
     * Get the collection ov converters currently configured
     *
     * @return A map of converter instances indexed on the class they can
     *      convert
     */
    protected Map getConverters() {
        return converters;
    }

    /**
     * Initialize a library.
     *
     * @param libraryId the library's identifier.
     *
     * @exception AntException if the library cannot be initalized.
     */
    protected void initializeLibrary(String libraryId)
         throws AntException {
        AntLibrary library = libManager.getLibrary(libraryId);
        if (library != null) {
            getLibFactory(library);
        }
    }

    /**
     * Get the collection of Ant Libraries defined for this frame Gets the
     * factory object for the given library
     *
     * @param componentLibrary the compnent library for which a factory objetc
     *      is required
     * @return the library's factory object
     * @exception AntException if the factory cannot be created
     */
    protected AntLibFactory getLibFactory(ComponentLibrary componentLibrary)
         throws AntException {
        String libraryId = componentLibrary.getLibraryId();
        if (libFactories.containsKey(libraryId)) {
            return (AntLibFactory) libFactories.get(libraryId);
        }
        ExecutionContext context
             = new ExecutionContext(frame, null, null);
        AntLibFactory libFactory = componentLibrary.getFactory(context);
        if (libFactory == null) {
            libFactory = new StandardLibFactory();
        }
        libFactories.put(libraryId, libFactory);
        return libFactory;
    }

    /**
     * Get an imported definition from the component manager
     *
     * @param name the name under which the component has been imported
     * @return the ImportInfo object detailing the import's library and other
     *      details
     */
    protected ImportInfo getImport(String name) {
        return (ImportInfo) imports.get(name);
    }

    /**
     * Create a component from a build model
     *
     * @param model the build model representing the component and its
     *      configuration
     * @return the configured component
     * @exception AntException if there is a problem creating or
     *      configuring the component
     */
    protected Object createComponent(BuildElement model)
         throws AntException {
        String componentName = model.getType();
        return createComponent(componentName, model);
    }

    /**
     * Create a component. This method creates a component and then configures
     * it from the given build model.
     *
     * @param componentName the name of the component which is used to select
     *      the object type to be created
     * @param model the build model of the component. If this is null, the
     *      component is created but not configured.
     * @return the configured component
     * @exception AntException if there is a problem creating or
     *      configuring the component
     */
    private Object createComponent(String componentName, BuildElement model)
         throws AntException {

        ImportInfo importInfo = getImport(componentName);
        if (importInfo == null) {
            throw new ExecutionException("There is no definition of the <"
            + componentName + "> component");
        }
        String className = importInfo.getClassName();

        ComponentLibrary componentLibrary
            = importInfo.getComponentLibrary();

        return createComponentFromDef(componentName, componentLibrary,
            importInfo.getDefinition(), model);
    }

    /**
     * Create a component from its library definition.
     *
     * @param componentName The component's name in the global context
     * @param componentLibrary the library which provides the deifnition of
     *      the component
     * @param libDefinition the component's definition
     * @param model the BuildElement model of the component's configuration.
     * @return the required component potentially wrapped in a wrapper object.
     * @exception AntException if the component cannot be created
     */
    private Object createComponentFromDef(String componentName,
                                          ComponentLibrary componentLibrary,
                                          AntLibDefinition libDefinition,
                                          BuildElement model)
         throws AntException {

        Location location = Location.UNKNOWN_LOCATION;
        String className = null;
        try {
            boolean isTask
                 = libDefinition.getDefinitionType() == AntLibrary.TASKDEF;


            Object component = null;
            if (model != null) {
                location = model.getLocation();
                for (Iterator i = aspects.iterator(); i.hasNext();) {
                    Aspect aspect = (Aspect) i.next();
                    component = aspect.preCreateComponent(component, model);
                }
            }

            AntLibFactory libFactory = getLibFactory(componentLibrary);
            ClassLoader componentLoader = null;
            if (component == null) {
                String localName = libDefinition.getDefinitionName();
                className = libDefinition.getClassName();
                componentLoader = componentLibrary.getClassLoader();
                Class componentClass
                    = Class.forName(className, true, componentLoader);
                // create the component using the factory
                component
                    = libFactory.createComponent(componentClass, localName);
            } else {
                className = component.getClass().getName();
                componentLoader = component.getClass().getClassLoader();
            }

            // wrap the component in an adapter if required.
            ExecutionComponent execComponent = null;
            if (isTask) {
                if (component instanceof Task) {
                    execComponent = (Task) component;
                } else {
                    execComponent = new TaskAdapter(componentName, component);
                }
            } else if (component instanceof ExecutionComponent) {
                execComponent = (ExecutionComponent) component;
            }

            // set the context loader to that for the component
            ClassLoader currentLoader
                 = LoaderUtils.setContextLoader(componentLoader);

            // if the component is an execution component create a context and
            // initialise the component with it.
            if (execComponent != null) {
                // give it a context unless it already has one
                if (execComponent.getAntContext() == null) {
                    ExecutionContext context
                         = new ExecutionContext(frame, execComponent, model);
                    context.setClassLoader(componentLoader);
                    execComponent.init(context, componentName);
                }
            }

            // if we have a model, use it to configure the component. Otherwise
            // the caller is expected to configure thre object
            if (model != null) {
                configureElement(libFactory, component, model);
                // if the component is an execution component and we have a
                // model, validate it
                if (execComponent != null) {
                    execComponent.validateComponent();
                }
                for (Iterator i = aspects.iterator(); i.hasNext();) {
                    Aspect aspect = (Aspect) i.next();
                    component = aspect.postCreateComponent(component, model);
                }
            }

            // reset the loader
            LoaderUtils.setContextLoader(currentLoader);

            // if we have an execution component, potentially a wrapper,
            // return it otherwise the component directly
            if (execComponent != null) {
                return execComponent;
            } else {
                return component;
            }
        } catch (ClassNotFoundException e) {
            throw new ExecutionException("Class " + className
                 + " for component <" + componentName + "> was not found", e,
                location);
        } catch (NoClassDefFoundError e) {
            throw new ExecutionException("Could not load a dependent class ("
                 + e.getMessage() + ") for component " + componentName, e,
                location);
        } catch (InstantiationException e) {
            throw new ExecutionException("Unable to instantiate component "
                 + "class " + className + " for component <"
                 + componentName + ">", e, location);
        } catch (IllegalAccessException e) {
            throw new ExecutionException("Unable to access task class "
                 + className + " for component <"
                 + componentName + ">", e, location);
        } catch (ExecutionException e) {
            e.setLocation(location, false);
            throw e;
        }
    }

    /**
     * Import a single component from the given library
     *
     * @param library the library which provides the component
     * @param defName the name of the component in the library
     * @param alias the name to be used for the component in build files. If
     *      this is null, the component's name within its library is used.
     */
    protected void importLibraryDef(ComponentLibrary library, String defName,
                                    String alias) {
        String label = alias;
        if (label == null) {
            label = defName;
        }

        AntLibDefinition libDef = library.getDefinition(defName);
        frame.log("Adding component <" + defName + "> as <" + label
             + "> from library \"" + library.getLibraryId() + "\", class: "
             + libDef.getClassName(), MessageLevel.MSG_DEBUG);
        imports.put(label, new ImportInfo(library, libDef));
    }

    /**
     * Gets the setter for the given class
     *
     * @param c the class for which the reflector is desired
     * @return the reflector
     */
    private Setter getSetter(Class c) {
        if (setters.containsKey(c)) {
            return (Setter) setters.get(c);
        }
        Setter setter = null;
        if (DeferredTask.class.isAssignableFrom(c)) {
            setter = new DeferredSetter();
        } else {
            ClassIntrospector introspector
                 = new ClassIntrospector(c, getConverters());
            setter = introspector.getReflector();
        }

        setters.put(c, setter);
        return setter;
    }

    /**
     * Create an instance of a type given its required class
     *
     * @param typeClass the class from which the instance should be created
     * @param model the model describing the required configuration of the
     *      instance
     * @param libFactory the factory object of the typeClass's Ant library
     * @param localName the name of the type within its Ant library
     * @return an instance of the given class appropriately configured
     * @exception AntException if there is a problem creating the type
     *      instance
     */
    private Object createTypeInstance(Class typeClass, AntLibFactory libFactory,
                                      BuildElement model, String localName)
         throws AntException {
        try {
            Object typeInstance
                 = libFactory.createComponent(typeClass, localName);

            if (typeInstance instanceof ExecutionComponent) {
                ExecutionComponent component
                     = (ExecutionComponent) typeInstance;
                ExecutionContext context
                    = new ExecutionContext(frame, component, model);
                component.init(context, localName);
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
            throw new ExecutionException(e, model.getLocation());
        }
    }

    /**
     * Create and add a nested element
     *
     * @param setter The Setter instance for the container element
     * @param element the container element in which the nested element will
     *      be created
     * @param model the model of the nested element
     * @param factory Ant Library factory associated with the element to which
     *      the attribute is to be added.
     * @exception AntException if the nested element cannot be created
     */
    private void addNestedElement(AntLibFactory factory, Setter setter,
                                  Object element, BuildElement model)
         throws AntException {
        String nestedElementName = model.getType();
        Class nestedType = setter.getType(nestedElementName);

        // is there a polymorph indicator - look in Ant aspects
        String typeName
            = model.getNamespaceAttributeValue(Namespace.ANT_META_URI, "type");

        Object typeInstance = null;
        if (typeName != null) {
            // the build file has specified the actual type of the element.
            // we need to look up that type and use it
            typeInstance = createComponent(typeName, model);
        } else if (nestedType != null) {
            // We need to create an instance of the class expected by the nested
            // element's adder method if that is possible
            if (nestedType.isInterface()) {
                throw new ExecutionException("No element can be created for "
                     + "nested element <" + nestedElementName + ">. Please "
                     + "provide a value by reference or specify the value type",
                    model.getLocation());
            }
            typeInstance = createTypeInstance(nestedType, factory, model, null);
        } else {
            throw new ExecutionException("The type of the <"
                 + nestedElementName + "> nested element is not known. "
                 + "Please specify by the type using the \"ant:type\" "
                 + "attribute or provide a reference to an instance with "
                 + "the \"ant:id\" attribute");
        }

        // is the typeInstance compatible with the type expected
        // by the element's add method
        if (!nestedType.isInstance(typeInstance)) {
            throw new ExecutionException("The type "
                + typeName + " is not compatible with the <"
                + nestedElementName + "> nested element", model.getLocation());
        }
        setter.addElement(element, nestedElementName, typeInstance);
    }

    /**
     * Create a nested element for the given object according to the model.
     *
     * @param setter the Setter instance of the container object
     * @param element the container object for which a nested element is
     *      required.
     * @param model the build model for the nestd element
     * @param factory Ant Library factory associated with the element creating
     *      the nested element
     * @exception AntException if the nested element cannot be created.
     */
    private void createNestedElement(AntLibFactory factory, Setter setter,
                                     Object element, BuildElement model)
         throws AntException {
        String nestedElementName = model.getType();
        try {
            Object nestedElement
                 = setter.createElement(element, nestedElementName);
            factory.registerCreatedElement(nestedElement);
            if (nestedElement instanceof ExecutionComponent) {
                ExecutionComponent component
                     = (ExecutionComponent) nestedElement;
                ExecutionContext context
                    = new ExecutionContext(frame, component, model);
                component.init(context, nestedElementName);
                configureElement(factory, nestedElement, model);
                component.validateComponent();
            } else {
                configureElement(factory, nestedElement, model);
            }
        } catch (ExecutionException e) {
            e.setLocation(model.getLocation(), false);
            throw e;
        } catch (RuntimeException e) {
            throw new ExecutionException(e, model.getLocation());
        }
    }

    /**
     * configure an object with attribtes from the given map
     *
     * @param object the object to be configured.
     * @param attributeValues a map containing named attribute values.
     * @param ignoreUnsupported if this is true, attribute names for which no
     *                          setter method exists are ignored.
     * @exception AntException if the object does not support an
     *            attribute in the map.
     */
    public void configureAttributes(Object object,
                                    AttributeCollection attributeValues,
                                    boolean ignoreUnsupported)
         throws AntException {
        Setter setter = getSetter(object.getClass());
        for (Iterator i = attributeValues.getAttributeNames(); i.hasNext();) {
            String attributeName = (String) i.next();
            String attributeValue = attributeValues.getAttribute(attributeName);
            if (!setter.supportsAttribute(attributeName)) {
                if (!ignoreUnsupported) {
                    throw new ExecutionException(object.getClass().getName()
                         + " does not support the \"" + attributeName
                         + "\" attribute");
                }
            } else {
                setter.setAttribute(object, attributeName,
                    frame.replacePropertyRefs(attributeValue));
            }
        }
    }

    /**
     * Configure an element according to the given model.
     *
     * @param element the object to be configured
     * @param model the BuildElement describing the object in the build file
     * @param factory Ant Library factory associated with the element being
     *      configured
     * @exception AntException if the element cannot be configured
     */
    private void configureElement(AntLibFactory factory, Object element,
                                  BuildElement model)
         throws AntException {
        Setter setter = getSetter(element.getClass());
        // do the nested elements
        for (Iterator i = model.getNestedElements(); i.hasNext();) {
            BuildElement nestedElementModel = (BuildElement) i.next();
            String nestedElementName = nestedElementModel.getType();
            ImportInfo info = getImport(nestedElementName);
            if (element instanceof TaskContainer
                 && info != null
                 && info.getDefinitionType() == AntLibrary.TASKDEF
                 && !setter.supportsNestedElement(nestedElementName)) {
                // it is a nested task
                Task nestedTask
                     = (Task) createComponent(nestedElementModel);
                TaskContainer container = (TaskContainer) element;
                container.addNestedTask(nestedTask);
            } else {
                if (setter.supportsNestedAdder(nestedElementName)) {
                    addNestedElement(factory, setter, element,
                        nestedElementModel);
                } else if (setter.supportsNestedCreator(nestedElementName)) {
                    createNestedElement(factory, setter, element,
                        nestedElementModel);
                } else {
                    throw new ExecutionException("<" + model.getType() + ">"
                         + " does not support the \"" + nestedElementName
                         + "\" nested element",
                        nestedElementModel.getLocation());
                }
            }
        }

        // Set the attributes of this element
        for (Iterator i = model.getAttributeNames(); i.hasNext();) {
            String attributeName = (String) i.next();
            String attributeValue = model.getAttributeValue(attributeName);
            if (!setter.supportsAttribute(attributeName)) {
                throw new ExecutionException("<" + model.getType() + ">"
                     + " does not support the \"" + attributeName
                     + "\" attribute", model.getLocation());
            }
            setter.setAttribute(element, attributeName,
                frame.replacePropertyRefs(attributeValue));
        }

        String modelText = model.getText();
        if (modelText.length() != 0) {
            if (!setter.supportsText()) {
                throw new ExecutionException("<" + model.getType() + ">"
                     + " does not support content", model.getLocation());
            }
            setter.addText(element,
                frame.replacePropertyRefs(modelText));
        }

    }

    /**
     * Define a new component
     *
     * @param componentName the name this component will take
     * @param defType the type of component being defined
     * @param factory the library factory object to create the component
     *      instances
     * @param loader the class loader to use to create the particular
     *      components
     * @param className the name of the class implementing the component
     * @exception ExecutionException if the component cannot be defined
     */
    private void defineComponent(AntLibFactory factory, ClassLoader loader,
                                 int defType, String componentName,
                                 String className)
         throws ExecutionException {
        DynamicLibrary dynamicLibrary
             = new DynamicLibrary(factory, loader);
        dynamicLibrary.addComponent(defType, componentName, className);
        dynamicLibraries.put(dynamicLibrary.getLibraryId(), dynamicLibrary);
        importLibraryDef(dynamicLibrary, componentName, null);
    }


    /**
     * Load any apsects from the given library.
     *
     * @param library the library from which the aspects are to be loaded.
     *
     * @exception AntException if an aspect cannot be loaded.
     */
    private void addAspects(AntLibrary library) throws AntException {
        if (!library.hasAspects()
            || loadedAspects.contains(library.getLibraryId())) {
            return;
        }

        String className = null;
        try {
            AntLibFactory libFactory = getLibFactory(library);
            ClassLoader aspectLoader = library.getClassLoader();
            for (Iterator i = library.getAspectClassNames(); i.hasNext();) {
                className = (String) i.next();
                Class aspectClass
                     = Class.forName(className, true, aspectLoader);
                if (!Aspect.class.isAssignableFrom(aspectClass)) {
                    throw new ExecutionException("In Ant library \""
                         + library.getLibraryId() + "\" the aspect class "
                         + aspectClass.getName()
                         + " does not implement the Aspect interface");
                }
                Aspect aspect = (Aspect) libFactory.createInstance(aspectClass);
                ExecutionContext context
                    = new ExecutionContext(frame, null, null);
                aspect.init(context);
                aspects.add(aspect);
            }
            loadedAspects.add(library.getLibraryId());
        } catch (ClassNotFoundException e) {
            throw new ExecutionException("In Ant library \""
                 + library.getLibraryId() + "\" aspect class "
                 + className + " was not found", e);
        } catch (NoClassDefFoundError e) {
            throw new ExecutionException("In Ant library \""
                 + library.getLibraryId()
                 + "\" could not load a dependent class ("
                 + e.getMessage() + ") for aspect " + className);
        } catch (InstantiationException e) {
            throw new ExecutionException("In Ant library \""
                 + library.getLibraryId()
                 + "\" unable to instantiate aspect class "
                 + className, e);
        } catch (IllegalAccessException e) {
            throw new ExecutionException("In Ant library \""
                 + library.getLibraryId()
                 + "\" unable to access aspect class "
                 + className, e);
        }
    }


    /**
     * Add the converters from the given library to those managed by this
     * frame.
     *
     * @param library the library from which the converters are required
     * @exception AntException if a converter defined in the library
     *      cannot be instantiated
     */
    private void addConverters(AntLibrary library)
         throws AntException {
        if (!library.hasConverters()
             || loadedConverters.contains(library.getLibraryId())) {
            return;
        }

        String className = null;
        try {
            AntLibFactory libFactory = getLibFactory(library);
            ClassLoader converterLoader = library.getClassLoader();
            for (Iterator i = library.getConverterClassNames(); i.hasNext();) {
                className = (String) i.next();
                Class converterClass
                     = Class.forName(className, true, converterLoader);
                if (!Converter.class.isAssignableFrom(converterClass)) {
                    throw new ExecutionException("In Ant library \""
                         + library.getLibraryId() + "\" the converter class "
                         + converterClass.getName()
                         + " does not implement the Converter interface");
                }
                Converter converter
                     = (Converter) libFactory.createInstance(converterClass);
                ExecutionContext context
                    = new ExecutionContext(frame, null, null);
                converter.init(context);
                Class[] converterTypes = converter.getTypes();
                for (int j = 0; j < converterTypes.length; ++j) {
                    converters.put(converterTypes[j], converter);
                }
            }
            loadedConverters.add(library.getLibraryId());
        } catch (ClassNotFoundException e) {
            throw new ExecutionException("In Ant library \""
                 + library.getLibraryId() + "\" converter class "
                 + className + " was not found", e);
        } catch (NoClassDefFoundError e) {
            throw new ExecutionException("In Ant library \""
                 + library.getLibraryId()
                 + "\" could not load a dependent class ("
                 + e.getMessage() + ") for converter " + className);
        } catch (InstantiationException e) {
            throw new ExecutionException("In Ant library \""
                 + library.getLibraryId()
                 + "\" unable to instantiate converter class "
                 + className, e);
        } catch (IllegalAccessException e) {
            throw new ExecutionException("In Ant library \""
                 + library.getLibraryId()
                 + "\" unable to access converter class "
                 + className, e);
        }
    }

    /**
     * Get the aspects which have been registered from ant libraries.
     *
     * @return the list of Aspect instances currently defined.
     */
    protected List getAspects() {
        return aspects;
    }
}

