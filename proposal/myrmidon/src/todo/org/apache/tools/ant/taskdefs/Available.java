/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.tools.ant.taskdefs;

import java.io.File;
import org.apache.myrmidon.api.TaskException;
import org.apache.tools.ant.AntClassLoader;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.taskdefs.condition.Condition;
import org.apache.tools.ant.types.EnumeratedAttribute;
import org.apache.tools.ant.types.Path;
import org.apache.tools.ant.types.Reference;
import org.apache.tools.ant.util.FileUtils;

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
    private String value = "true";
    private String classname;
    private Path classpath;
    private String file;
    private Path filepath;
    private AntClassLoader loader;

    private String property;
    private String resource;
    private FileDir type;

    public void setClassname( String classname )
    {
        if( !"".equals( classname ) )
        {
            this.classname = classname;
        }
    }

    public void setClasspath( Path classpath )
        throws TaskException
    {
        createClasspath().append( classpath );
    }

    public void setClasspathRef( Reference r )
        throws TaskException
    {
        createClasspath().setRefid( r );
    }

    public void setFile( String file )
    {
        this.file = file;
    }

    public void setFilepath( Path filepath )
        throws TaskException
    {
        createFilepath().append( filepath );
    }

    public void setProperty( String property )
    {
        this.property = property;
    }

    public void setResource( String resource )
    {
        this.resource = resource;
    }

    public void setType( FileDir type )
    {
        this.type = type;
    }

    public void setValue( String value )
    {
        this.value = value;
    }

    public Path createClasspath()
        throws TaskException
    {
        if( this.classpath == null )
        {
            this.classpath = new Path( project );
        }
        return this.classpath.createPath();
    }

    public Path createFilepath()
        throws TaskException
    {
        if( this.filepath == null )
        {
            this.filepath = new Path( project );
        }
        return this.filepath.createPath();
    }

    public boolean eval()
        throws TaskException
    {
        if( classname == null && file == null && resource == null )
        {
            throw new TaskException( "At least one of (classname|file|resource) is required" );
        }

        if( type != null )
        {
            if( file == null )
            {
                throw new TaskException( "The type attribute is only valid when specifying the file attribute." );
            }
        }

        if( classpath != null )
        {
            classpath.setProject( project );
            this.loader = new AntClassLoader( project, classpath );
        }

        if( ( classname != null ) && !checkClass( classname ) )
        {
            log( "Unable to load class " + classname + " to set property " + property, Project.MSG_VERBOSE );
            return false;
        }

        if( ( file != null ) && !checkFile() )
        {
            if( type != null )
            {
                log( "Unable to find " + type + " " + file + " to set property " + property, Project.MSG_VERBOSE );
            }
            else
            {
                log( "Unable to find " + file + " to set property " + property, Project.MSG_VERBOSE );
            }
            return false;
        }

        if( ( resource != null ) && !checkResource( resource ) )
        {
            log( "Unable to load resource " + resource + " to set property " + property, Project.MSG_VERBOSE );
            return false;
        }

        if( loader != null )
        {
            loader.cleanup();
        }

        return true;
    }

    public void execute()
        throws TaskException
    {
        if( property == null )
        {
            throw new TaskException( "property attribute is required" );
        }

        if( eval() )
        {
            String lSep = System.getProperty( "line.separator" );
            if( null == project.getProperty( property ) )
            {
                setProperty( property, value );
            }
            //else ignore
        }
    }

    private boolean checkClass( String classname )
    {
        try
        {
            if( loader != null )
            {
                loader.loadClass( classname );
            }
            else
            {
                ClassLoader l = this.getClass().getClassLoader();
                // Can return null to represent the bootstrap class loader.
                // see API docs of Class.getClassLoader.
                if( l != null )
                {
                    l.loadClass( classname );
                }
                else
                {
                    Class.forName( classname );
                }
            }
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
        if( filepath == null )
        {
            return checkFile( resolveFile( file ), file );
        }
        else
        {
            String[] paths = filepath.list();
            for( int i = 0; i < paths.length; ++i )
            {
                log( "Searching " + paths[ i ], Project.MSG_DEBUG );
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
                if( path.exists() && file.equals( paths[ i ] ) )
                {
                    if( type == null )
                    {
                        log( "Found: " + path, Project.MSG_VERBOSE );
                        return true;
                    }
                    else if( type.isDir()
                        && path.isDirectory() )
                    {
                        log( "Found directory: " + path, Project.MSG_VERBOSE );
                        return true;
                    }
                    else if( type.isFile()
                        && path.isFile() )
                    {
                        log( "Found file: " + path, Project.MSG_VERBOSE );
                        return true;
                    }
                    // not the requested type
                    return false;
                }

                FileUtils fileUtils = FileUtils.newFileUtils();
                File parent = path.getParentFile();
                // **   full-pathname specified == parent dir of path in list
                if( parent != null && parent.exists()
                    && file.equals( parent.getAbsolutePath() ) )
                {
                    if( type == null )
                    {
                        log( "Found: " + parent, Project.MSG_VERBOSE );
                        return true;
                    }
                    else if( type.isDir() )
                    {
                        log( "Found directory: " + parent, Project.MSG_VERBOSE );
                        return true;
                    }
                    // not the requested type
                    return false;
                }

                // **   simple name specified   == path in list + name
                if( path.exists() && path.isDirectory() )
                {
                    if( checkFile( new File( path, file ),
                                   file + " in " + path ) )
                    {
                        return true;
                    }
                }

                // **   simple name specified   == parent dir + name
                if( parent != null && parent.exists() )
                {
                    if( checkFile( new File( parent, file ),
                                   file + " in " + parent ) )
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
                        if( checkFile( new File( grandParent, file ),
                                       file + " in " + grandParent ) )
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
        if( type != null )
        {
            if( type.isDir() )
            {
                if( f.isDirectory() )
                {
                    log( "Found directory: " + text, Project.MSG_VERBOSE );
                }
                return f.isDirectory();
            }
            else if( type.isFile() )
            {
                if( f.isFile() )
                {
                    log( "Found file: " + text, Project.MSG_VERBOSE );
                }
                return f.isFile();
            }
        }
        if( f.exists() )
        {
            log( "Found: " + text, Project.MSG_VERBOSE );
        }
        return f.exists();
    }

    private boolean checkResource( String resource )
    {
        if( loader != null )
        {
            return ( loader.getResourceAsStream( resource ) != null );
        }
        else
        {
            ClassLoader cL = this.getClass().getClassLoader();
            if( cL != null )
            {
                return ( cL.getResourceAsStream( resource ) != null );
            }
            else
            {
                return
                    ( ClassLoader.getSystemResourceAsStream( resource ) != null );
            }
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
