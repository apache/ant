/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.tools.ant.types;

import java.util.Properties;
import java.util.Stack;
import org.apache.tools.ant.AntClassLoader;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.util.FileNameMapper;
import org.apache.myrmidon.api.TaskException;

/**
 * Element to define a FileNameMapper.
 *
 * @author <a href="mailto:stefan.bodewig@epost.de">Stefan Bodewig</a>
 */
public class Mapper extends DataType implements Cloneable
{

    protected MapperType type = null;

    protected String classname = null;

    protected Path classpath = null;

    protected String from = null;

    protected String to = null;

    public Mapper( Project p )
    {
        setProject( p );
    }

    /**
     * Set the class name of the FileNameMapper to use.
     *
     * @param classname The new Classname value
     */
    public void setClassname( String classname )
    {
        if( isReference() )
        {
            throw tooManyAttributes();
        }
        this.classname = classname;
    }

    /**
     * Set the classpath to load the FileNameMapper through (attribute).
     *
     * @param classpath The new Classpath value
     */
    public void setClasspath( Path classpath )
    {
        if( isReference() )
        {
            throw tooManyAttributes();
        }
        if( this.classpath == null )
        {
            this.classpath = classpath;
        }
        else
        {
            this.classpath.append( classpath );
        }
    }

    /**
     * Set the classpath to load the FileNameMapper through via reference
     * (attribute).
     *
     * @param r The new ClasspathRef value
     */
    public void setClasspathRef( Reference r )
    {
        if( isReference() )
        {
            throw tooManyAttributes();
        }
        createClasspath().setRefid( r );
    }

    /**
     * Set the argument to FileNameMapper.setFrom
     *
     * @param from The new From value
     */
    public void setFrom( String from )
    {
        if( isReference() )
        {
            throw tooManyAttributes();
        }
        this.from = from;
    }

    /**
     * Make this Mapper instance a reference to another Mapper. <p>
     *
     * You must not set any other attribute if you make it a reference.</p>
     *
     * @param r The new Refid value
     * @exception TaskException Description of Exception
     */
    public void setRefid( Reference r )
        throws TaskException
    {
        if( type != null || from != null || to != null )
        {
            throw tooManyAttributes();
        }
        super.setRefid( r );
    }

    /**
     * Set the argument to FileNameMapper.setTo
     *
     * @param to The new To value
     */
    public void setTo( String to )
    {
        if( isReference() )
        {
            throw tooManyAttributes();
        }
        this.to = to;
    }

    /**
     * Set the type of FileNameMapper to use.
     *
     * @param type The new Type value
     */
    public void setType( MapperType type )
    {
        if( isReference() )
        {
            throw tooManyAttributes();
        }
        this.type = type;
    }

    /**
     * Returns a fully configured FileNameMapper implementation.
     *
     * @return The Implementation value
     * @exception TaskException Description of Exception
     */
    public FileNameMapper getImplementation()
        throws TaskException
    {
        if( isReference() )
        {
            return getRef().getImplementation();
        }

        if( type == null && classname == null )
        {
            throw new TaskException( "one of the attributes type or classname is required" );
        }

        if( type != null && classname != null )
        {
            throw new TaskException( "must not specify both type and classname attribute" );
        }

        try
        {
            if( type != null )
            {
                classname = type.getImplementation();
            }

            Class c = null;
            if( classpath == null )
            {
                c = Class.forName( classname );
            }
            else
            {
                AntClassLoader al = new AntClassLoader( getProject(),
                                                        classpath );
                c = al.loadClass( classname );
                AntClassLoader.initializeClass( c );
            }

            FileNameMapper m = (FileNameMapper)c.newInstance();
            m.setFrom( from );
            m.setTo( to );
            return m;
        }
        catch( TaskException be )
        {
            throw be;
        }
        catch( Throwable t )
        {
            throw new TaskException( "Error", t );
        }
        finally
        {
            if( type != null )
            {
                classname = null;
            }
        }
    }

    /**
     * Set the classpath to load the FileNameMapper through (nested element).
     *
     * @return Description of the Returned Value
     */
    public Path createClasspath()
    {
        if( isReference() )
        {
            throw noChildrenAllowed();
        }
        if( this.classpath == null )
        {
            this.classpath = new Path( getProject() );
        }
        return this.classpath.createPath();
    }

    /**
     * Performs the check for circular references and returns the referenced
     * Mapper.
     *
     * @return The Ref value
     */
    protected Mapper getRef()
    {
        if( !checked )
        {
            Stack stk = new Stack();
            stk.push( this );
            dieOnCircularReference( stk, getProject() );
        }

        Object o = ref.getReferencedObject( getProject() );
        if( !( o instanceof Mapper ) )
        {
            String msg = ref.getRefId() + " doesn\'t denote a mapper";
            throw new TaskException( msg );
        }
        else
        {
            return (Mapper)o;
        }
    }

    /**
     * Class as Argument to FileNameMapper.setType.
     *
     * @author RT
     */
    public static class MapperType extends EnumeratedAttribute
    {
        private Properties implementations;

        public MapperType()
        {
            implementations = new Properties();
            implementations.put( "identity",
                                 "org.apache.tools.ant.util.IdentityMapper" );
            implementations.put( "flatten",
                                 "org.apache.tools.ant.util.FlatFileNameMapper" );
            implementations.put( "glob",
                                 "org.apache.tools.ant.util.GlobPatternMapper" );
            implementations.put( "merge",
                                 "org.apache.tools.ant.util.MergingMapper" );
            implementations.put( "regexp",
                                 "org.apache.tools.ant.util.RegexpPatternMapper" );
        }

        public String getImplementation()
        {
            return implementations.getProperty( getValue() );
        }

        public String[] getValues()
        {
            return new String[]{"identity", "flatten", "glob", "merge", "regexp"};
        }
    }

}
