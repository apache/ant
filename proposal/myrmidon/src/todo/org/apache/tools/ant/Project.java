/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.tools.ant;

import java.io.File;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import org.apache.avalon.framework.logger.AbstractLogEnabled;
import org.apache.avalon.framework.logger.Logger;
import org.apache.myrmidon.api.TaskException;
import org.apache.tools.ant.types.FilterSet;
import org.apache.tools.ant.types.FilterSetCollection;

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
    extends AbstractLogEnabled
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

    private ArrayList listeners = new ArrayList();

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

    public Logger hackGetLogger()
    {
        return super.getLogger();
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

        getLogger().debug( "Detected Java version: " + javaVersion + " in: " + System.getProperty( "java.home" ) );

        getLogger().debug( "Detected OS: " + System.getProperty( "os.name" ) );
    }

    /**
     * get the base directory of the project as a file object
     *
     * @return the base directory. If this is null, then the base dir is not
     *      valid
     */
    public File getBaseDir()
    {
        return baseDir;
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

    public FilterSet getGlobalFilterSet()
    {
        return globalFilterSet;
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
     * get the current task definition hashtable
     *
     * @return The TaskDefinitions value
     */
    public Hashtable getTaskDefinitions()
    {
        return taskClassDefinitions;
    }

    public void addBuildListener( BuildListener listener )
    {
        listeners.add( listener );
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
        throw new TaskException( "Task needs reimplementing" );
    }

    public void demuxOutput( String line, boolean isError )
    {
        Task task = (Task)threadTasks.get( Thread.currentThread() );
        if( task == null )
        {
            //fireMessageLogged( this, line, isError ? MSG_ERR : MSG_INFO );
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
     * Output a message to the log with the given log level and an event scope
     * of project
     *
     * @param msg text to log
     * @param msgLevel level to log at
     */
    public void log( String msg, int msgLevel )
    {
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
        return replaceProperties( this, value, getProperties() );
    }

    /**
     * Replace ${} style constructions in the given value with the string value
     * of the corresponding data types.
     *
     * @param value the string to be scanned for property references.
     * @param project Description of Parameter
     * @param keys Description of Parameter
     * @return Description of the Returned Value
     * @exception TaskException Description of Exception
     */
    private String replaceProperties( Project project, String value, Hashtable keys )
        throws TaskException
    {
        if( value == null )
        {
            return null;
        }

        ArrayList fragments = new ArrayList();
        ArrayList propertyRefs = new ArrayList();
        parsePropertyString( value, fragments, propertyRefs );

        StringBuffer sb = new StringBuffer();
        Iterator i = fragments.iterator();
        Iterator j = propertyRefs.iterator();
        while( i.hasNext() )
        {
            String fragment = (String)i.next();
            if( fragment == null )
            {
                String propertyName = (String)j.next();
                if( !keys.containsKey( propertyName ) )
                {
                    project.getLogger().debug( "Property ${" + propertyName + "} has not been set" );
                }
                fragment = ( keys.containsKey( propertyName ) ) ? (String)keys.get( propertyName )
                    : "${" + propertyName + "}";
            }
            sb.append( fragment );
        }

        return sb.toString();
    }

    /**
     * This method will parse a string containing ${value} style property values
     * into two lists. The first list is a collection of text fragments, while
     * the other is a set of string property names null entries in the first
     * list indicate a property reference from the second list.
     *
     * @param value Description of Parameter
     * @param fragments Description of Parameter
     * @param propertyRefs Description of Parameter
     * @exception TaskException Description of Exception
     */
    private void parsePropertyString( String value, ArrayList fragments, ArrayList propertyRefs )
        throws TaskException
    {
        int prev = 0;
        int pos;
        while( ( pos = value.indexOf( "$", prev ) ) >= 0 )
        {
            if( pos > 0 )
            {
                fragments.add( value.substring( prev, pos ) );
            }

            if( pos == ( value.length() - 1 ) )
            {
                fragments.add( "$" );
                prev = pos + 1;
            }
            else if( value.charAt( pos + 1 ) != '{' )
            {
                fragments.add( value.substring( pos + 1, pos + 2 ) );
                prev = pos + 2;
            }
            else
            {
                int endName = value.indexOf( '}', pos );
                if( endName < 0 )
                {
                    throw new TaskException( "Syntax error in property: "
                                             + value );
                }
                String propertyName = value.substring( pos + 2, endName );
                fragments.add( null );
                propertyRefs.add( propertyName );
                prev = endName + 1;
            }
        }

        if( prev < value.length() )
        {
            fragments.add( value.substring( prev ) );
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
}
