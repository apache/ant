/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.tools.ant;

import java.util.Enumeration;
import java.util.Locale;
import java.util.Vector;
import org.apache.myrmidon.api.TaskException;
import org.xml.sax.AttributeList;
import org.xml.sax.helpers.AttributeListImpl;

/**
 * Wrapper class that holds the attributes of a Task (or elements nested below
 * that level) and takes care of configuring that element at runtime.
 *
 * @author <a href="mailto:stefan.bodewig@epost.de">Stefan Bodewig</a>
 */
public class RuntimeConfigurable
{

    private String elementTag = null;
    private Vector children = new Vector();
    private Object wrappedObject = null;
    private StringBuffer characters = new StringBuffer();
    private AttributeList attributes;

    /**
     * @param proxy The element to wrap.
     * @param elementTag Description of Parameter
     */
    public RuntimeConfigurable( Object proxy, String elementTag )
    {
        wrappedObject = proxy;
        this.elementTag = elementTag;
    }

    /**
     * Set's the attributes for the wrapped element.
     *
     * @param attributes The new Attributes value
     */
    public void setAttributes( AttributeList attributes )
    {
        this.attributes = new AttributeListImpl( attributes );
    }

    /**
     * Returns the AttributeList of the wrapped element.
     *
     * @return The Attributes value
     */
    public AttributeList getAttributes()
    {
        return attributes;
    }

    public String getElementTag()
    {
        return elementTag;
    }

    /**
     * Adds child elements to the wrapped element.
     *
     * @param child The feature to be added to the Child attribute
     */
    public void addChild( RuntimeConfigurable child )
    {
        children.addElement( child );
    }

    /**
     * Add characters from #PCDATA areas to the wrapped element.
     *
     * @param data The feature to be added to the Text attribute
     */
    public void addText( String data )
    {
        characters.append( data );
    }

    /**
     * Add characters from #PCDATA areas to the wrapped element.
     *
     * @param buf The feature to be added to the Text attribute
     * @param start The feature to be added to the Text attribute
     * @param end The feature to be added to the Text attribute
     */
    public void addText( char[] buf, int start, int end )
    {
        addText( new String( buf, start, end ) );
    }

    /**
     * Configure the wrapped element and all children.
     *
     * @param p Description of Parameter
     * @exception BuildException Description of Exception
     */
    public void maybeConfigure( Project p )
        throws TaskException
    {
        String id = null;

        if( attributes != null )
        {
            ProjectHelper.configure( wrappedObject, attributes, p );
            id = attributes.getValue( "id" );
            attributes = null;
        }
        if( characters.length() != 0 )
        {
            ProjectHelper.addText( p, wrappedObject, characters.toString() );
            characters.setLength( 0 );
        }
        Enumeration enum = children.elements();
        while( enum.hasMoreElements() )
        {
            RuntimeConfigurable child = (RuntimeConfigurable)enum.nextElement();
            if( child.wrappedObject instanceof Task )
            {
                Task childTask = (Task)child.wrappedObject;
                childTask.setRuntimeConfigurableWrapper( child );
                childTask.maybeConfigure();
            }
            else
            {
                child.maybeConfigure( p );
            }
            ProjectHelper.storeChild( p, wrappedObject, child.wrappedObject, child.getElementTag().toLowerCase( Locale.US ) );
        }

        if( id != null )
        {
            p.addReference( id, wrappedObject );
        }
    }

    void setProxy( Object proxy )
    {
        wrappedObject = proxy;
    }

    /**
     * Returns the child with index <code>index</code>.
     *
     * @param index Description of Parameter
     * @return The Child value
     */
    RuntimeConfigurable getChild( int index )
    {
        return (RuntimeConfigurable)children.elementAt( index );
    }

}
