/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.tools.ant.taskdefs;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import org.apache.myrmidon.api.TaskException;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.taskdefs.condition.Condition;
import org.apache.tools.ant.types.EnumeratedAttribute;
import org.apache.tools.ant.types.Path;

/**
 * Will set the given property if the requested resource is available at
 * runtime.
 *
 * @author Stefano Mazzocchi <a href="mailto:stefano@apache.org">
 *      stefano@apache.org</a>
 * @author <a href="mailto:umagesh@apache.org">Magesh Umasankar</a>
 */

public class Available
    extends Task
    implements Condition
{
    private String m_value = "true";
    private String m_classname;
    private Path m_classpath;
    private String m_file;
    private Path m_filepath;
    private ClassLoader m_classLoader;

    private String m_property;
    private String m_resource;
    private FileDir m_type;

    public void setClassname( String classname )
    {
        if( !"".equals( classname ) )
        {
            m_classname = classname;
        }
    }

    /**
     * Adds a classpath element.
     */
    public void addClasspath( Path classpath )
        throws TaskException
    {
        if ( m_classpath == null )
        {
            m_classpath = classpath;
        }
        else
        {
            m_classpath.addPath(classpath);
        }
    }

    public void setFile( String file )
    {
        m_file = file;
    }

    public void setProperty( String property )
    {
        m_property = property;
    }

    public void setResource( String resource )
    {
        m_resource = resource;
    }

    public void setType( FileDir type )
    {
        m_type = type;
    }

    public void setValue( String value )
    {
        m_value = value;
    }

    /**
     * Adds a file search path element.
     */
    public void addFilepath( Path path )
        throws TaskException
    {
        if( m_filepath == null )
        {
            m_filepath = path;
        }
        else
        {
            m_filepath.addPath( path );
        }
    }

    public boolean eval()
        throws TaskException
    {
        if( m_classname == null && m_file == null && m_resource == null )
        {
            throw new TaskException( "At least one of (classname|file|resource) is required" );
        }

        if( m_type != null )
        {
            if( m_file == null )
            {
                throw new TaskException( "The type attribute is only valid when specifying the file attribute." );
            }
        }

        if( m_classpath != null )
        {
            final URL[] urls = m_classpath.toURLs();
            m_classLoader = new URLClassLoader( urls );
        }

        if( ( m_classname != null ) && !checkClass( m_classname ) )
        {
            getLogger().debug( "Unable to load class " + m_classname + " to set property " + m_property );
            return false;
        }

        if( ( m_file != null ) && !checkFile() )
        {
            if( m_type != null )
            {
                getLogger().debug( "Unable to find " + m_type + " " + m_file + " to set property " + m_property );
            }
            else
            {
                getLogger().debug( "Unable to find " + m_file + " to set property " + m_property );
            }
            return false;
        }

        if( ( m_resource != null ) && !checkResource( m_resource ) )
        {
            getLogger().debug( "Unable to load resource " + m_resource + " to set property " + m_property );
            return false;
        }

        return true;
    }

    public void execute()
        throws TaskException
    {
        if( m_property == null )
        {
            throw new TaskException( "property attribute is required" );
        }

        if( eval() )
        {
            setProperty( m_property, m_value );
        }
    }

    private boolean checkClass( String classname )
    {
        try
        {
            final ClassLoader classLoader = getClassLoader();
            classLoader.loadClass( classname );
            return true;
        }
        catch( ClassNotFoundException e )
        {
            return false;
        }
        catch( NoClassDefFoundError e )
        {
            return false;
        }
    }

    private boolean checkFile()
        throws TaskException
    {
        if( m_filepath == null )
        {
            return checkFile( resolveFile( m_file ), m_file );
        }
        else
        {
            String[] paths = m_filepath.list();
            for( int i = 0; i < paths.length; ++i )
            {
                getLogger().debug( "Searching " + paths[ i ] );
                /*
                 * filepath can be a list of directory and/or
                 * file names (gen'd via <fileset>)
                 *
                 * look for:
                 * full-pathname specified == path in list
                 * full-pathname specified == parent dir of path in list
                 * simple name specified   == path in list
                 * simple name specified   == path in list + name
                 * simple name specified   == parent dir + name
                 * simple name specified   == parent of parent dir + name
                 *
                 */
                File path = new File( paths[ i ] );

                // **   full-pathname specified == path in list
                // **   simple name specified   == path in list
                if( path.exists() && m_file.equals( paths[ i ] ) )
                {
                    if( m_type == null )
                    {
                        getLogger().debug( "Found: " + path );
                        return true;
                    }
                    else if( m_type.isDir()
                        && path.isDirectory() )
                    {
                        getLogger().debug( "Found directory: " + path );
                        return true;
                    }
                    else if( m_type.isFile()
                        && path.isFile() )
                    {
                        getLogger().debug( "Found file: " + path );
                        return true;
                    }
                    // not the requested type
                    return false;
                }

                File parent = path.getParentFile();
                // **   full-pathname specified == parent dir of path in list
                if( parent != null && parent.exists()
                    && m_file.equals( parent.getAbsolutePath() ) )
                {
                    if( m_type == null )
                    {
                        getLogger().debug( "Found: " + parent );
                        return true;
                    }
                    else if( m_type.isDir() )
                    {
                        getLogger().debug( "Found directory: " + parent );
                        return true;
                    }
                    // not the requested type
                    return false;
                }

                // **   simple name specified   == path in list + name
                if( path.exists() && path.isDirectory() )
                {
                    if( checkFile( new File( path, m_file ),
                                   m_file + " in " + path ) )
                    {
                        return true;
                    }
                }

                // **   simple name specified   == parent dir + name
                if( parent != null && parent.exists() )
                {
                    if( checkFile( new File( parent, m_file ),
                                   m_file + " in " + parent ) )
                    {
                        return true;
                    }
                }

                // **   simple name specified   == parent of parent dir + name
                if( parent != null )
                {
                    File grandParent = parent.getParentFile();
                    if( grandParent != null && grandParent.exists() )
                    {
                        if( checkFile( new File( grandParent, m_file ),
                                       m_file + " in " + grandParent ) )
                        {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    private boolean checkFile( File f, String text )
    {
        if( m_type != null )
        {
            if( m_type.isDir() )
            {
                if( f.isDirectory() )
                {
                    getLogger().debug( "Found directory: " + text );
                }
                return f.isDirectory();
            }
            else if( m_type.isFile() )
            {
                if( f.isFile() )
                {
                    getLogger().debug( "Found file: " + text );
                }
                return f.isFile();
            }
        }
        if( f.exists() )
        {
            getLogger().debug( "Found: " + text );
        }
        return f.exists();
    }

    private boolean checkResource( String resource )
    {
        final ClassLoader classLoader = getClassLoader();
        return ( null != classLoader.getResourceAsStream( resource ) );
    }

    private ClassLoader getClassLoader()
    {
        if( null == m_classLoader )
        {
            return ClassLoader.getSystemClassLoader();
        }
        else
        {
            return m_classLoader;
        }
    }

    public static class FileDir extends EnumeratedAttribute
    {

        private final static String[] values = {"file", "dir"};

        public String[] getValues()
        {
            return values;
        }

        public boolean isDir()
        {
            return "dir".equalsIgnoreCase( getValue() );
        }

        public boolean isFile()
        {
            return "file".equalsIgnoreCase( getValue() );
        }

        public String toString()
        {
            return getValue();
        }
    }
}
