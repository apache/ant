/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.tools.ant;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Modifier;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Properties;
import java.util.Stack;
import java.util.Vector;
import org.apache.myrmidon.api.TaskException;
import org.apache.tools.ant.types.FilterSet;
import org.apache.tools.ant.types.FilterSetCollection;
import org.apache.tools.ant.util.FileUtils;

/**
 * Central representation of an Ant project. This class defines a Ant project
 * with all of it's targets and tasks. It also provides the mechanism to kick
 * off a build using a particular target name. <p>
 *
 * This class also encapsulates methods which allow Files to be refered to using
 * abstract path names which are translated to native system file paths at
 * runtime as well as defining various project properties.
 *
 * @author duncan@x180.com
 */
public class Project
{
    public final static int MSG_ERR = 0;
    public final static int MSG_WARN = 1;
    public final static int MSG_INFO = 2;
    public final static int MSG_VERBOSE = 3;
    public final static int MSG_DEBUG = 4;

    // private set of constants to represent the state
    // of a DFS of the Target dependencies
    private final static String VISITING = "VISITING";
    private final static String VISITED = "VISITED";

    public final static String JAVA_1_0 = "1.0";
    public final static String JAVA_1_1 = "1.1";
    public final static String JAVA_1_2 = "1.2";
    public final static String JAVA_1_3 = "1.3";
    public final static String JAVA_1_4 = "1.4";

    public final static String TOKEN_START = FilterSet.DEFAULT_TOKEN_START;
    public final static String TOKEN_END = FilterSet.DEFAULT_TOKEN_END;

    private static String javaVersion;

    private Hashtable properties = new Hashtable();
    private Hashtable userProperties = new Hashtable();
    private Hashtable references = new Hashtable();
    private Hashtable dataClassDefinitions = new Hashtable();
    private Hashtable taskClassDefinitions = new Hashtable();
    private Hashtable createdTasks = new Hashtable();
    private Hashtable targets = new Hashtable();
    private FilterSet globalFilterSet = new FilterSet();
    private FilterSetCollection globalFilters = new FilterSetCollection( globalFilterSet );

    private Vector listeners = new Vector();

    /**
     * The Ant core classloader - may be null if using system loader
     */
    private ClassLoader coreLoader;

    /**
     * Records the latest task on a thread
     */
    private Hashtable threadTasks = new Hashtable();
    private File baseDir;
    private String defaultTarget;
    private String description;

    private String name;

    static
    {

        // Determine the Java version by looking at available classes
        // java.lang.StrictMath was introduced in JDK 1.3
        // java.lang.ThreadLocal was introduced in JDK 1.2
        // java.lang.Void was introduced in JDK 1.1
        // Count up version until a NoClassDefFoundError ends the try

        try
        {
            javaVersion = JAVA_1_0;
            Class.forName( "java.lang.Void" );
            javaVersion = JAVA_1_1;
            Class.forName( "java.lang.ThreadLocal" );
            javaVersion = JAVA_1_2;
            Class.forName( "java.lang.StrictMath" );
            javaVersion = JAVA_1_3;
            Class.forName( "java.lang.CharSequence" );
            javaVersion = JAVA_1_4;
        }
        catch( ClassNotFoundException cnfe )
        {
            // swallow as we've hit the max class version that
            // we have
        }
    }

    /**
     * static query of the java version
     *
     * @return something like "1.1" or "1.3"
     */
    public static String getJavaVersion()
    {
        return javaVersion;
    }

    /**
     * returns the boolean equivalent of a string, which is considered true if
     * either "on", "true", or "yes" is found, ignoring case.
     *
     * @param s Description of Parameter
     * @return Description of the Returned Value
     */
    public static boolean toBoolean( String s )
    {
        return ( s.equalsIgnoreCase( "on" ) ||
            s.equalsIgnoreCase( "true" ) ||
            s.equalsIgnoreCase( "yes" ) );
    }

    /**
     * Translate a path into its native (platform specific) format. <p>
     *
     * This method uses the PathTokenizer class to separate the input path into
     * its components. This handles DOS style paths in a relatively sensible
     * way. The file separators are then converted to their platform specific
     * versions.
     *
     * @param to_process the path to be converted
     * @return the native version of to_process or an empty string if to_process
     *      is null or empty
     */
    public static String translatePath( String to_process )
    {
        if( to_process == null || to_process.length() == 0 )
        {
            return "";
        }

        StringBuffer path = new StringBuffer( to_process.length() + 50 );
        PathTokenizer tokenizer = new PathTokenizer( to_process );
        while( tokenizer.hasMoreTokens() )
        {
            String pathComponent = tokenizer.nextToken();
            pathComponent = pathComponent.replace( '/', File.separatorChar );
            pathComponent = pathComponent.replace( '\\', File.separatorChar );
            if( path.length() != 0 )
            {
                path.append( File.pathSeparatorChar );
            }
            path.append( pathComponent );
        }

        return path.toString();
    }

    private static TaskException makeCircularException( String end, Stack stk )
    {
        StringBuffer sb = new StringBuffer( "Circular dependency: " );
        sb.append( end );
        String c;
        do
        {
            c = (String)stk.pop();
            sb.append( " <- " );
            sb.append( c );
        } while( !c.equals( end ) );
        return new TaskException( new String( sb ) );
    }

    /**
     * set the base directory; XML attribute. checks for the directory existing
     * and being a directory type
     *
     * @param baseDir project base directory.
     * @throws TaskException if the directory was invalid
     */
    public void setBaseDir( File baseDir )
        throws TaskException
    {
        baseDir = FileUtils.newFileUtils().normalize( baseDir.getAbsolutePath() );
        if( !baseDir.exists() )
            throw new TaskException( "Basedir " + baseDir.getAbsolutePath() + " does not exist" );
        if( !baseDir.isDirectory() )
            throw new TaskException( "Basedir " + baseDir.getAbsolutePath() + " is not a directory" );
        this.baseDir = baseDir;
        setPropertyInternal( "basedir", this.baseDir.getPath() );
        String msg = "Project base dir set to: " + this.baseDir;
        log( msg, MSG_VERBOSE );
    }

    /**
     * match basedir attribute in xml
     *
     * @param baseD project base directory.
     * @throws TaskException if the directory was invalid
     */
    public void setBasedir( String baseD )
        throws TaskException
    {
        setBaseDir( new File( baseD ) );
    }

    public void setCoreLoader( ClassLoader coreLoader )
    {
        this.coreLoader = coreLoader;
    }

    /**
     * set the default target of the project XML attribute name.
     *
     * @param defaultTarget The new Default value
     */
    public void setDefault( String defaultTarget )
    {
        this.defaultTarget = defaultTarget;
    }

    /**
     * set the project description
     *
     * @param description text
     */
    public void setDescription( String description )
    {
        this.description = description;
    }

    /**
     * set the ant.java.version property, also tests for unsupported JVM
     * versions, prints the verbose log messages
     *
     * @throws TaskException if this Java version is not supported
     */
    public void setJavaVersionProperty()
        throws TaskException
    {
        setPropertyInternal( "ant.java.version", javaVersion );

        // sanity check
        if( javaVersion == JAVA_1_0 )
        {
            throw new TaskException( "Ant cannot work on Java 1.0" );
        }

        log( "Detected Java version: " + javaVersion + " in: " + System.getProperty( "java.home" ), MSG_VERBOSE );

        log( "Detected OS: " + System.getProperty( "os.name" ), MSG_VERBOSE );
    }

    /**
     * ant xml property. Set the project name as an attribute of this class, and
     * of the property ant.project.name
     *
     * @param name The new Name value
     */
    public void setName( String name )
    {
        setUserProperty( "ant.project.name", name );
        this.name = name;
    }

    /**
     * set a property. An existing property of the same name will not be
     * overwritten.
     *
     * @param name name of property
     * @param value new value of the property
     * @since 1.5
     */
    public void setNewProperty( String name, String value )
    {
        if( null != properties.get( name ) )
        {
            log( "Override ignored for property " + name, MSG_VERBOSE );
            return;
        }
        log( "Setting project property: " + name + " -> " +
             value, MSG_DEBUG );
        properties.put( name, value );
    }

    /**
     * set a property. Any existing property of the same name is overwritten,
     * unless it is a user property.
     *
     * @param name name of property
     * @param value new value of the property
     */
    public void setProperty( String name, String value )
    {
        // command line properties take precedence
        if( null != userProperties.get( name ) )
        {
            log( "Override ignored for user property " + name, MSG_VERBOSE );
            return;
        }

        if( null != properties.get( name ) )
        {
            log( "Overriding previous definition of property " + name,
                 MSG_VERBOSE );
        }

        log( "Setting project property: " + name + " -> " +
             value, MSG_DEBUG );
        properties.put( name, value );
    }

    /**
     * turn all the system properties into ant properties. user properties still
     * override these values
     */
    public void setSystemProperties()
    {
        Properties systemP = System.getProperties();
        Enumeration e = systemP.keys();
        while( e.hasMoreElements() )
        {
            Object name = e.nextElement();
            String value = systemP.get( name ).toString();
            this.setPropertyInternal( name.toString(), value );
        }
    }

    /**
     * set a user property, which can not be overwritten by set/unset property
     * calls
     *
     * @param name The new UserProperty value
     * @param value The new UserProperty value
     * @see #setProperty(String,String)
     */
    public void setUserProperty( String name, String value )
    {
        log( "Setting ro project property: " + name + " -> " +
             value, MSG_DEBUG );
        userProperties.put( name, value );
        properties.put( name, value );
    }

    /**
     * get the base directory of the project as a file object
     *
     * @return the base directory. If this is null, then the base dir is not
     *      valid
     */
    public File getBaseDir()
    {
        if( baseDir == null )
        {
            try
            {
                setBasedir( "." );
            }
            catch( TaskException ex )
            {
                ex.printStackTrace();
            }
        }
        return baseDir;
    }

    public Vector getBuildListeners()
    {
        return listeners;
    }

    public ClassLoader getCoreLoader()
    {
        return coreLoader;
    }

    /**
     * get the current task definition hashtable
     *
     * @return The DataTypeDefinitions value
     */
    public Hashtable getDataTypeDefinitions()
    {
        return dataClassDefinitions;
    }

    /**
     * get the default target of the project
     *
     * @return default target or null
     */
    public String getDefaultTarget()
    {
        return defaultTarget;
    }

    /**
     * get the project description
     *
     * @return description or null if no description has been set
     */
    public String getDescription()
    {
        return description;
    }

    public FilterSet getGlobalFilterSet()
    {
        return globalFilterSet;
    }

    /**
     * get the project name
     *
     * @return name string
     */
    public String getName()
    {
        return name;
    }

    /**
     * get a copy of the property hashtable
     *
     * @return the hashtable containing all properties, user included
     */
    public Hashtable getProperties()
    {
        Hashtable propertiesCopy = new Hashtable();

        Enumeration e = properties.keys();
        while( e.hasMoreElements() )
        {
            Object name = e.nextElement();
            Object value = properties.get( name );
            propertiesCopy.put( name, value );
        }

        return propertiesCopy;
    }

    /**
     * query a property.
     *
     * @param name the name of the property
     * @return the property value, or null for no match
     */
    public String getProperty( String name )
    {
        if( name == null )
            return null;
        String property = (String)properties.get( name );
        return property;
    }

    /**
     * @param key Description of Parameter
     * @return The object with the "id" key.
     */
    public Object getReference( String key )
    {
        return references.get( key );
    }

    public Hashtable getReferences()
    {
        return references;
    }

    /**
     * get the target hashtable
     *
     * @return hashtable, the contents of which can be cast to Target
     */
    public Hashtable getTargets()
    {
        return targets;
    }

    /**
     * get the current task definition hashtable
     *
     * @return The TaskDefinitions value
     */
    public Hashtable getTaskDefinitions()
    {
        return taskClassDefinitions;
    }

    /**
     * get a copy of the user property hashtable
     *
     * @return the hashtable user properties only
     */
    public Hashtable getUserProperties()
    {
        Hashtable propertiesCopy = new Hashtable();

        Enumeration e = userProperties.keys();
        while( e.hasMoreElements() )
        {
            Object name = e.nextElement();
            Object value = properties.get( name );
            propertiesCopy.put( name, value );
        }

        return propertiesCopy;
    }

    /**
     * query a user property.
     *
     * @param name the name of the property
     * @return the property value, or null for no match
     */
    public String getUserProperty( String name )
    {
        if( name == null )
            return null;
        String property = (String)userProperties.get( name );
        return property;
    }

    /**
     * Topologically sort a set of Targets.
     *
     * @param root is the (String) name of the root Target. The sort is created
     *      in such a way that the sequence of Targets uptil the root target is
     *      the minimum possible such sequence.
     * @param targets is a Hashtable representing a "name to Target" mapping
     * @return a Vector of Strings with the names of the targets in sorted
     *      order.
     * @exception TaskException if there is a cyclic dependency among the
     *      Targets, or if a Target does not exist.
     */
    public final Vector topoSort( String root, Hashtable targets )
        throws TaskException
    {
        Vector ret = new Vector();
        Hashtable state = new Hashtable();
        Stack visiting = new Stack();

        // We first run a DFS based sort using the root as the starting node.
        // This creates the minimum sequence of Targets to the root node.
        // We then do a sort on any remaining unVISITED targets.
        // This is unnecessary for doing our build, but it catches
        // circular dependencies or missing Targets on the entire
        // dependency tree, not just on the Targets that depend on the
        // build Target.

        tsort( root, targets, state, visiting, ret );
        log( "Build sequence for target `" + root + "' is " + ret, MSG_VERBOSE );
        for( Enumeration en = targets.keys(); en.hasMoreElements(); )
        {
            String curTarget = (String)( en.nextElement() );
            String st = (String)state.get( curTarget );
            if( st == null )
            {
                tsort( curTarget, targets, state, visiting, ret );
            }
            else if( st == VISITING )
            {
                throw new RuntimeException( "Unexpected node in visiting state: " + curTarget );
            }
        }
        log( "Complete build sequence is " + ret, MSG_VERBOSE );
        return ret;
    }

    public void addBuildListener( BuildListener listener )
    {
        listeners.addElement( listener );
    }

    /**
     * add a new datatype
     *
     * @param typeName name of the datatype
     * @param typeClass full datatype classname
     */
    public void addDataTypeDefinition( String typeName, Class typeClass )
    {
        if( null != dataClassDefinitions.get( typeName ) )
        {
            log( "Trying to override old definition of datatype " + typeName,
                 MSG_WARN );
        }

        String msg = " +User datatype: " + typeName + "     " + typeClass.getName();
        log( msg, MSG_DEBUG );
        dataClassDefinitions.put( typeName, typeClass );
    }

    /**
     * @param target is the Target to be added or replaced in the current
     *      Project.
     */
    public void addOrReplaceTarget( Target target )
    {
        addOrReplaceTarget( target.getName(), target );
    }

    /**
     * @param target is the Target to be added/replaced in the current Project.
     * @param targetName is the name to use for the Target
     */
    public void addOrReplaceTarget( String targetName, Target target )
    {
        String msg = " +Target: " + targetName;
        log( msg, MSG_DEBUG );
        target.setProject( this );
        targets.put( targetName, target );
    }

    public void addReference( String name, Object value )
    {
        if( null != references.get( name ) )
        {
            log( "Overriding previous definition of reference to " + name,
                 MSG_WARN );
        }
        log( "Adding reference: " + name + " -> " + value, MSG_DEBUG );
        references.put( name, value );
    }

    /**
     * This call expects to add a <em>new</em> Target.
     *
     * @param target is the Target to be added to the current Project.
     * @see Project#addOrReplaceTarget to replace existing Targets.
     */
    public void addTarget( Target target )
        throws TaskException
    {
        String name = target.getName();
        if( targets.get( name ) != null )
        {
            throw new TaskException( "Duplicate target: `" + name + "'" );
        }
        addOrReplaceTarget( name, target );
    }

    /**
     * This call expects to add a <em>new</em> Target.
     *
     * @param target is the Target to be added to the current Project.
     * @param targetName is the name to use for the Target
     * @exception TaskException if the Target already exists in the project.
     * @see Project#addOrReplaceTarget to replace existing Targets.
     */
    public void addTarget( String targetName, Target target )
        throws TaskException
    {
        if( targets.get( targetName ) != null )
        {
            throw new TaskException( "Duplicate target: `" + targetName + "'" );
        }
        addOrReplaceTarget( targetName, target );
    }

    /**
     * add a new task definition, complain if there is an overwrite attempt
     *
     * @param taskName name of the task
     * @param taskClass full task classname
     * @throws TaskException and logs as Project.MSG_ERR for conditions, that
     *      will cause the task execution to fail.
     */
    public void addTaskDefinition( String taskName, Class taskClass )
        throws TaskException
    {
        Class old = (Class)taskClassDefinitions.get( taskName );
        if( null != old )
        {
            if( old.equals( taskClass ) )
            {
                log( "Ignoring override for task " + taskName
                     + ", it is already defined by the same class.",
                     MSG_VERBOSE );
                return;
            }
            else
            {
                log( "Trying to override old definition of task " + taskName,
                     MSG_WARN );

            }
        }

        String msg = " +User task: " + taskName + "     " + taskClass.getName();
        log( msg, MSG_DEBUG );
        checkTaskClass( taskClass );
        taskClassDefinitions.put( taskName, taskClass );
    }

    /**
     * Checks a class, whether it is suitable for serving as ant task.
     *
     * @param taskClass Description of Parameter
     * @throws TaskException and logs as Project.MSG_ERR for conditions, that
     *      will cause the task execution to fail.
     */
    public void checkTaskClass( final Class taskClass )
        throws TaskException
    {
        if( !Modifier.isPublic( taskClass.getModifiers() ) )
        {
            final String message = taskClass + " is not public";
            log( message, Project.MSG_ERR );
            throw new TaskException( message );
        }
        if( Modifier.isAbstract( taskClass.getModifiers() ) )
        {
            final String message = taskClass + " is abstract";
            log( message, Project.MSG_ERR );
            throw new TaskException( message );
        }
        try
        {
            taskClass.getConstructor( null );
            // don't have to check for public, since
            // getConstructor finds public constructors only.
        }
        catch( NoSuchMethodException e )
        {
            final String message = "No public default constructor in " + taskClass;
            log( message, Project.MSG_ERR );
            throw new TaskException( message );
        }
        if( !Task.class.isAssignableFrom( taskClass ) )
            TaskAdapter.checkTaskClass( taskClass, this );
    }

    /**
     * create a new DataType instance
     *
     * @param typeName name of the datatype
     * @return null if the datatype name is unknown
     * @throws TaskException when datatype creation goes bad
     */
    public Object createDataType( String typeName )
        throws TaskException
    {
        Class c = (Class)dataClassDefinitions.get( typeName );

        if( c == null )
            return null;

        try
        {
            java.lang.reflect.Constructor ctor = null;
            boolean noArg = false;
            // DataType can have a "no arg" constructor or take a single
            // Project argument.
            try
            {
                ctor = c.getConstructor( new Class[ 0 ] );
                noArg = true;
            }
            catch( NoSuchMethodException nse )
            {
                ctor = c.getConstructor( new Class[]{Project.class} );
                noArg = false;
            }

            Object o = null;
            if( noArg )
            {
                o = ctor.newInstance( new Object[ 0 ] );
            }
            else
            {
                o = ctor.newInstance( new Object[]{this} );
            }
            if( o instanceof ProjectComponent )
            {
                ( (ProjectComponent)o ).setProject( this );
            }
            String msg = "   +DataType: " + typeName;
            log( msg, MSG_DEBUG );
            return o;
        }
        catch( java.lang.reflect.InvocationTargetException ite )
        {
            Throwable t = ite.getTargetException();
            String msg = "Could not create datatype of type: "
                + typeName + " due to " + t;
            throw new TaskException( msg, t );
        }
        catch( Throwable t )
        {
            String msg = "Could not create datatype of type: "
                + typeName + " due to " + t;
            throw new TaskException( msg, t );
        }
    }

    /**
     * create a new task instance
     *
     * @param taskType name of the task
     * @return null if the task name is unknown
     * @throws TaskException when task creation goes bad
     */
    public Task createTask( String taskType )
        throws TaskException
    {
        Class c = (Class)taskClassDefinitions.get( taskType );

        if( c == null )
            return null;
        try
        {
            Object o = c.newInstance();
            Task task = null;
            if( o instanceof Task )
            {
                task = (Task)o;
            }
            else
            {
                // "Generic" Bean - use the setter pattern
                // and an Adapter
                TaskAdapter taskA = new TaskAdapter();
                taskA.setProxy( o );
                task = taskA;
            }
            task.setProject( this );
            task.setTaskType( taskType );

            // set default value, can be changed by the user
            task.setTaskName( taskType );

            String msg = "   +Task: " + taskType;
            log( msg, MSG_DEBUG );
            addCreatedTask( taskType, task );
            return task;
        }
        catch( Throwable t )
        {
            String msg = "Could not create task of type: "
                + taskType + " due to " + t;
            throw new TaskException( msg, t );
        }
    }

    public void demuxOutput( String line, boolean isError )
    {
        Task task = (Task)threadTasks.get( Thread.currentThread() );
        if( task == null )
        {
            fireMessageLogged( this, line, isError ? MSG_ERR : MSG_INFO );
        }
        else
        {
            if( isError )
            {
                task.handleErrorOutput( line );
            }
            else
            {
                task.handleOutput( line );
            }
        }
    }

    /**
     * execute the targets and any targets it depends on
     *
     * @param targetName the target to execute
     * @throws TaskException if the build failed
     */
    public void executeTarget( String targetName )
        throws TaskException
    {

        // sanity check ourselves, if we've been asked to build nothing
        // then we should complain

        if( targetName == null )
        {
            String msg = "No target specified";
            throw new TaskException( msg );
        }

        // Sort the dependency tree, and run everything from the
        // beginning until we hit our targetName.
        // Sorting checks if all the targets (and dependencies)
        // exist, and if there is any cycle in the dependency
        // graph.
        Vector sortedTargets = topoSort( targetName, targets );

        int curidx = 0;
        Target curtarget;

        do
        {
            curtarget = (Target)sortedTargets.elementAt( curidx++ );
            curtarget.performTasks();
        } while( !curtarget.getName().equals( targetName ) );
    }

    /**
     * execute the sequence of targets, and the targets they depend on
     *
     * @param targetNames Description of Parameter
     * @throws TaskException if the build failed
     */
    public void executeTargets( Vector targetNames )
        throws TaskException
    {
        Throwable error = null;

        for( int i = 0; i < targetNames.size(); i++ )
        {
            executeTarget( (String)targetNames.elementAt( i ) );
        }
    }

    /**
     * Initialise the project. This involves setting the default task
     * definitions and loading the system properties.
     *
     * @exception TaskException Description of Exception
     */
    public void init()
        throws TaskException
    {
        setJavaVersionProperty();

        String defs = "/org/apache/tools/ant/taskdefs/defaults.properties";

        try
        {
            Properties props = new Properties();
            InputStream in = this.getClass().getResourceAsStream( defs );
            if( in == null )
            {
                throw new TaskException( "Can't load default task list" );
            }
            props.load( in );
            in.close();

            Enumeration enum = props.propertyNames();
            while( enum.hasMoreElements() )
            {
                String key = (String)enum.nextElement();
                String value = props.getProperty( key );
                try
                {
                    Class taskClass = Class.forName( value );
                    addTaskDefinition( key, taskClass );
                }
                catch( NoClassDefFoundError ncdfe )
                {
                    // ignore...
                }
                catch( ClassNotFoundException cnfe )
                {
                    // ignore...
                }
            }
        }
        catch( IOException ioe )
        {
            throw new TaskException( "Can't load default task list" );
        }

        String dataDefs = "/org/apache/tools/ant/types/defaults.properties";

        try
        {
            Properties props = new Properties();
            InputStream in = this.getClass().getResourceAsStream( dataDefs );
            if( in == null )
            {
                throw new TaskException( "Can't load default datatype list" );
            }
            props.load( in );
            in.close();

            Enumeration enum = props.propertyNames();
            while( enum.hasMoreElements() )
            {
                String key = (String)enum.nextElement();
                String value = props.getProperty( key );
                try
                {
                    Class dataClass = Class.forName( value );
                    addDataTypeDefinition( key, dataClass );
                }
                catch( NoClassDefFoundError ncdfe )
                {
                    // ignore...
                }
                catch( ClassNotFoundException cnfe )
                {
                    // ignore...
                }
            }
        }
        catch( IOException ioe )
        {
            throw new TaskException( "Can't load default datatype list" );
        }

        setSystemProperties();
    }

    /**
     * Output a message to the log with the default log level of MSG_INFO
     *
     * @param msg text to log
     */

    public void log( String msg )
    {
        log( msg, MSG_INFO );
    }

    /**
     * Output a message to the log with the given log level and an event scope
     * of project
     *
     * @param msg text to log
     * @param msgLevel level to log at
     */
    public void log( String msg, int msgLevel )
    {
        fireMessageLogged( this, msg, msgLevel );
    }

    /**
     * Output a message to the log with the given log level and an event scope
     * of a task
     *
     * @param task task to use in the log
     * @param msg text to log
     * @param msgLevel level to log at
     */
    public void log( Task task, String msg, int msgLevel )
    {
        fireMessageLogged( task, msg, msgLevel );
    }

    /**
     * Output a message to the log with the given log level and an event scope
     * of a target
     *
     * @param target target to use in the log
     * @param msg text to log
     * @param msgLevel level to log at
     */
    public void log( Target target, String msg, int msgLevel )
    {
        fireMessageLogged( target, msg, msgLevel );
    }

    public void removeBuildListener( BuildListener listener )
    {
        listeners.removeElement( listener );
    }

    /**
     * Replace ${} style constructions in the given value with the string value
     * of the corresponding data types.
     *
     * @param value the string to be scanned for property references.
     * @return Description of the Returned Value
     * @exception TaskException Description of Exception
     */
    public String replaceProperties( String value )
        throws TaskException
    {
        return ProjectHelper.replaceProperties( this, value );
    }

    /**
     * send build finished event to the listeners
     *
     * @param exception exception which indicates failure if not null
     */
    protected void fireBuildFinished( Throwable exception )
    {
        BuildEvent event = new BuildEvent( this );
        event.setException( exception );
        for( int i = 0; i < listeners.size(); i++ )
        {
            BuildListener listener = (BuildListener)listeners.elementAt( i );
            listener.buildFinished( event );
        }
    }

    /**
     * send build started event to the listeners
     */
    protected void fireBuildStarted()
    {
        BuildEvent event = new BuildEvent( this );
        for( int i = 0; i < listeners.size(); i++ )
        {
            BuildListener listener = (BuildListener)listeners.elementAt( i );
            listener.buildStarted( event );
        }
    }

    protected void fireMessageLogged( Project project, String message, int priority )
    {
        BuildEvent event = new BuildEvent( project );
        fireMessageLoggedEvent( event, message, priority );
    }

    protected void fireMessageLogged( Target target, String message, int priority )
    {
        BuildEvent event = new BuildEvent( target );
        fireMessageLoggedEvent( event, message, priority );
    }

    protected void fireMessageLogged( Task task, String message, int priority )
    {
        BuildEvent event = new BuildEvent( task );
        fireMessageLoggedEvent( event, message, priority );
    }

    /**
     * send build finished event to the listeners
     *
     * @param exception exception which indicates failure if not null
     * @param target Description of Parameter
     */
    protected void fireTargetFinished( Target target, Throwable exception )
    {
        BuildEvent event = new BuildEvent( target );
        event.setException( exception );
        for( int i = 0; i < listeners.size(); i++ )
        {
            BuildListener listener = (BuildListener)listeners.elementAt( i );
            listener.targetFinished( event );
        }
    }

    /**
     * send target started event to the listeners
     *
     * @param target Description of Parameter
     */
    protected void fireTargetStarted( Target target )
    {
        BuildEvent event = new BuildEvent( target );
        for( int i = 0; i < listeners.size(); i++ )
        {
            BuildListener listener = (BuildListener)listeners.elementAt( i );
            listener.targetStarted( event );
        }
    }

    protected void fireTaskFinished( Task task, Throwable exception )
    {
        threadTasks.remove( Thread.currentThread() );
        System.out.flush();
        System.err.flush();
        BuildEvent event = new BuildEvent( task );
        event.setException( exception );
        for( int i = 0; i < listeners.size(); i++ )
        {
            BuildListener listener = (BuildListener)listeners.elementAt( i );
            listener.taskFinished( event );
        }
    }

    protected void fireTaskStarted( Task task )
    {
        // register this as the current task on the current thread.
        threadTasks.put( Thread.currentThread(), task );
        BuildEvent event = new BuildEvent( task );
        for( int i = 0; i < listeners.size(); i++ )
        {
            BuildListener listener = (BuildListener)listeners.elementAt( i );
            listener.taskStarted( event );
        }
    }

    /**
     * Allows Project and subclasses to set a property unless its already
     * defined as a user property. There are a few cases internally to Project
     * that need to do this currently.
     *
     * @param name The new PropertyInternal value
     * @param value The new PropertyInternal value
     */
    private void setPropertyInternal( String name, String value )
    {
        if( null != userProperties.get( name ) )
        {
            return;
        }
        properties.put( name, value );
    }

    // one step in a recursive DFS traversal of the Target dependency tree.
    // - The Hashtable "state" contains the state (VISITED or VISITING or null)
    // of all the target names.
    // - The Stack "visiting" contains a stack of target names that are
    // currently on the DFS stack. (NB: the target names in "visiting" are
    // exactly the target names in "state" that are in the VISITING state.)
    // 1. Set the current target to the VISITING state, and push it onto
    // the "visiting" stack.
    // 2. Throw a TaskException if any child of the current node is
    // in the VISITING state (implies there is a cycle.) It uses the
    // "visiting" Stack to construct the cycle.
    // 3. If any children have not been VISITED, tsort() the child.
    // 4. Add the current target to the Vector "ret" after the children
    //   have been visited. Move the current target to the VISITED state.
    //   "ret" now contains the sorted sequence of Targets upto the current
    //   Target.

    private final void tsort( String root, Hashtable targets,
                              Hashtable state, Stack visiting,
                              Vector ret )
        throws TaskException
    {
        state.put( root, VISITING );
        visiting.push( root );

        Target target = (Target)( targets.get( root ) );

        // Make sure we exist
        if( target == null )
        {
            StringBuffer sb = new StringBuffer( "Target `" );
            sb.append( root );
            sb.append( "' does not exist in this project. " );
            visiting.pop();
            if( !visiting.empty() )
            {
                String parent = (String)visiting.peek();
                sb.append( "It is used from target `" );
                sb.append( parent );
                sb.append( "'." );
            }

            throw new TaskException( new String( sb ) );
        }

        for( Enumeration en = target.getDependencies(); en.hasMoreElements(); )
        {
            String cur = (String)en.nextElement();
            String m = (String)state.get( cur );
            if( m == null )
            {
                // Not been visited
                tsort( cur, targets, state, visiting, ret );
            }
            else if( m == VISITING )
            {
                // Currently visiting this node, so have a cycle
                throw makeCircularException( cur, visiting );
            }
        }

        String p = (String)visiting.pop();
        if( root != p )
        {
            throw new RuntimeException( "Unexpected internal error: expected to pop " + root + " but got " + p );
        }
        state.put( root, VISITED );
        ret.addElement( target );
    }

    /**
     * Keep a record of all tasks that have been created so that they can be
     * invalidated if a taskdef overrides the definition.
     *
     * @param type The feature to be added to the CreatedTask attribute
     * @param task The feature to be added to the CreatedTask attribute
     */
    private void addCreatedTask( String type, Task task )
    {
        synchronized( createdTasks )
        {
            Vector v = (Vector)createdTasks.get( type );
            if( v == null )
            {
                v = new Vector();
                createdTasks.put( type, v );
            }
            v.addElement( task );
        }
    }

    private void fireMessageLoggedEvent( BuildEvent event, String message, int priority )
    {
        event.setMessage( message, priority );
        for( int i = 0; i < listeners.size(); i++ )
        {
            BuildListener listener = (BuildListener)listeners.elementAt( i );
            listener.messageLogged( event );
        }
    }
}
