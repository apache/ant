/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.tools.ant.taskdefs;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.Properties;
import org.apache.tools.ant.AntClassLoader;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.types.Path;
import org.apache.tools.ant.types.Reference;

/**
 * Base class for Taskdef and Typedef - does all the classpath handling and and
 * class loading.
 *
 * @author Costin Manolache
 * @author <a href="stefan.bodewig@epost.de">Stefan Bodewig</a>
 */
public abstract class Definer extends Task
{
    private boolean reverseLoader = false;
    private Path classpath;
    private File file;
    private String name;
    private String resource;
    private String value;

    public void setClassname( String v )
    {
        value = v;
    }

    public void setClasspath( Path classpath )
    {
        if( this.classpath == null )
        {
            this.classpath = classpath;
        }
        else
        {
            this.classpath.append( classpath );
        }
    }

    public void setClasspathRef( Reference r )
    {
        createClasspath().setRefid( r );
    }

    public void setFile( File file )
    {
        this.file = file;
    }

    public void setName( String name )
    {
        this.name = name;
    }

    public void setResource( String res )
    {
        this.resource = res;
    }

    public String getClassname()
    {
        return value;
    }

    public Path createClasspath()
    {
        if( this.classpath == null )
        {
            this.classpath = new Path( project );
        }
        return this.classpath.createPath();
    }

    public void execute()
        throws BuildException
    {
        AntClassLoader al = createLoader();

        if( file == null && resource == null )
        {

            // simple case - one definition
            if( name == null || value == null )
            {
                String msg = "name or classname attributes of "
                     + getTaskName() + " element "
                     + "are undefined";
                throw new BuildException( msg );
            }
            addDefinition( al, name, value );

        }
        else
        {

            try
            {
                if( name != null || value != null )
                {
                    String msg = "You must not specify name or value "
                         + "together with file or resource.";
                    throw new BuildException( msg );
                }

                if( file != null && resource != null )
                {
                    String msg = "You must not specify both, file and resource.";
                    throw new BuildException( msg );
                }

                Properties props = new Properties();
                InputStream is = null;
                if( file != null )
                {
                    log( "Loading definitions from file " + file,
                        Project.MSG_VERBOSE );
                    is = new FileInputStream( file );
                    if( is == null )
                    {
                        log( "Could not load definitions from file " + file
                             + ". It doesn\'t exist.", Project.MSG_WARN );
                    }
                }
                if( resource != null )
                {
                    log( "Loading definitions from resource " + resource,
                        Project.MSG_VERBOSE );
                    is = al.getResourceAsStream( resource );
                    if( is == null )
                    {
                        log( "Could not load definitions from resource "
                             + resource + ". It could not be found.",
                            Project.MSG_WARN );
                    }
                }

                if( is != null )
                {
                    props.load( is );
                    Enumeration keys = props.keys();
                    while( keys.hasMoreElements() )
                    {
                        String n = ( String )keys.nextElement();
                        String v = props.getProperty( n );
                        addDefinition( al, n, v );
                    }
                }
            }
            catch( IOException ex )
            {
                throw new BuildException( "Error", ex);
            }
        }
    }

    protected abstract void addDefinition( String name, Class c );

    private void addDefinition( ClassLoader al, String name, String value )
        throws BuildException
    {
        try
        {
            Class c = al.loadClass( value );
            AntClassLoader.initializeClass( c );
            addDefinition( name, c );
        }
        catch( ClassNotFoundException cnfe )
        {
            String msg = getTaskName() + " class " + value +
                " cannot be found";
            throw new BuildException( msg, cnfe );
        }
        catch( NoClassDefFoundError ncdfe )
        {
            String msg = getTaskName() + " class " + value +
                " cannot be found";
            throw new BuildException( msg, ncdfe );
        }
    }


    private AntClassLoader createLoader()
    {
        AntClassLoader al = null;
        if( classpath != null )
        {
            al = new AntClassLoader( project, classpath, !reverseLoader );
        }
        else
        {
            al = new AntClassLoader( project, Path.systemClasspath, !reverseLoader );
        }
        // need to load Task via system classloader or the new
        // task we want to define will never be a Task but always
        // be wrapped into a TaskAdapter.
        al.addSystemPackageRoot( "org.apache.tools.ant" );
        return al;
    }
}
