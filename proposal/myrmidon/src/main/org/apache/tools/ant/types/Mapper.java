/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.tools.ant.types;

import java.util.Properties;
import java.util.Stack;
import org.apache.myrmidon.api.TaskException;
import org.apache.tools.ant.AntClassLoader;
import org.apache.tools.ant.util.FileNameMapper;

/**
 * Element to define a FileNameMapper.
 *
 * @author <a href="mailto:stefan.bodewig@epost.de">Stefan Bodewig</a>
 */
public class Mapper
    extends DataType
    implements Cloneable
{
    private MapperType m_type;
    private String m_classname;
    private Path m_classpath;
    private String m_from;
    private String m_to;

    /**
     * Set the class name of the FileNameMapper to use.
     *
     * @param classname The new Classname value
     */
    public void setClassname( String classname )
        throws TaskException
    {
        if( isReference() )
        {
            throw tooManyAttributes();
        }
        this.m_classname = classname;
    }

    /**
     * Set the classpath to load the FileNameMapper through (attribute).
     *
     * @param classpath The new Classpath value
     */
    public void setClasspath( Path classpath )
        throws TaskException
    {
        if( isReference() )
        {
            throw tooManyAttributes();
        }
        if( this.m_classpath == null )
        {
            this.m_classpath = classpath;
        }
        else
        {
            this.m_classpath.append( classpath );
        }
    }

    /**
     * Set the classpath to load the FileNameMapper through via reference
     * (attribute).
     *
     * @param r The new ClasspathRef value
     */
    public void setClasspathRef( Reference r )
        throws TaskException
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
        throws TaskException
    {
        if( isReference() )
        {
            throw tooManyAttributes();
        }
        this.m_from = from;
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
        if( m_type != null || m_from != null || m_to != null )
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
        throws TaskException
    {
        if( isReference() )
        {
            throw tooManyAttributes();
        }
        this.m_to = to;
    }

    /**
     * Set the type of FileNameMapper to use.
     *
     * @param type The new Type value
     */
    public void setType( MapperType type )
        throws TaskException
    {
        if( isReference() )
        {
            throw tooManyAttributes();
        }
        this.m_type = type;
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

        if( m_type == null && m_classname == null )
        {
            throw new TaskException( "one of the attributes type or classname is required" );
        }

        if( m_type != null && m_classname != null )
        {
            throw new TaskException( "must not specify both type and classname attribute" );
        }

        try
        {
            if( m_type != null )
            {
                m_classname = m_type.getImplementation();
            }

            Class c = null;
            if( m_classpath == null )
            {
                c = Class.forName( m_classname );
            }
            else
            {
                AntClassLoader al = new AntClassLoader( getProject(),
                                                        m_classpath );
                c = al.loadClass( m_classname );
                AntClassLoader.initializeClass( c );
            }

            FileNameMapper m = (FileNameMapper)c.newInstance();
            m.setFrom( m_from );
            m.setTo( m_to );
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
            if( m_type != null )
            {
                m_classname = null;
            }
        }
    }

    /**
     * Set the classpath to load the FileNameMapper through (nested element).
     *
     * @return Description of the Returned Value
     */
    public Path createClasspath()
        throws TaskException
    {
        if( isReference() )
        {
            throw noChildrenAllowed();
        }
        if( this.m_classpath == null )
        {
            this.m_classpath = new Path();
        }
        return this.m_classpath.createPath();
    }

    /**
     * Performs the check for circular references and returns the referenced
     * Mapper.
     *
     * @return The Ref value
     */
    protected Mapper getRef()
        throws TaskException
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

}
