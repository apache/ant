/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.tools.ant.types;

import java.util.Stack;
import org.apache.myrmidon.api.TaskException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.ProjectComponent;

/**
 * Base class for those classes that can appear inside the build file as stand
 * alone data types. <p>
 *
 * This class handles the common description attribute and provides a default
 * implementation for reference handling and checking for circular references
 * that is appropriate for types that can not be nested inside elements of the
 * same type (i.e. &lt;patternset&gt; but not &lt;path&gt;).</p>
 *
 * @author <a href="mailto:stefan.bodewig@epost.de">Stefan Bodewig</a>
 */
public abstract class DataType
    extends ProjectComponent
{
    /**
     * The descriptin the user has set.
     */
    protected String description;

    /**
     * Value to the refid attribute.
     */
    protected Reference ref;

    /**
     * Are we sure we don't hold circular references? <p>
     *
     * Subclasses are responsible for setting this value to false if we'd need
     * to investigate this condition (usually because a child element has been
     * added that is a subclass of DataType).</p>
     */
    protected boolean checked = true;

    /**
     * Sets a description of the current data type. It will be useful in
     * commenting what we are doing.
     *
     * @param desc The new Description value
     */
    public void setDescription( String desc )
    {
        description = desc;
    }

    /**
     * Set the value of the refid attribute. <p>
     *
     * Subclasses may need to check whether any other attributes have been set
     * as well or child elements have been created and thus override this
     * method. if they do the must call <code>super.setRefid</code>.</p>
     *
     * @param ref The new Refid value
     */
    public void setRefid( Reference ref )
        throws TaskException
    {
        this.ref = ref;
        checked = false;
    }

    /**
     * Return the description for the current data type.
     *
     * @return The Description value
     */
    public String getDescription()
    {
        return description;
    }

    /**
     * Has the refid attribute of this element been set?
     *
     * @return The Reference value
     */
    public boolean isReference()
    {
        return ref != null;
    }

    public void execute()
        throws TaskException
    {
        //HACK: NOOP execute - should be deleted in the future!
    }

    /**
     * Performs the check for circular references and returns the referenced
     * object.
     *
     * @param requiredClass Description of Parameter
     * @param dataTypeName Description of Parameter
     * @return The CheckedRef value
     */
    protected Object getCheckedRef( Class requiredClass, String dataTypeName )
        throws TaskException
    {
        if( !checked )
        {
            Stack stk = new Stack();
            stk.push( this );
            dieOnCircularReference( stk, getProject() );
        }

        Object o = ref.getReferencedObject( getProject() );
        if( !( requiredClass.isAssignableFrom( o.getClass() ) ) )
        {
            String msg = ref.getRefId() + " doesn\'t denote a " + dataTypeName;
            throw new TaskException( msg );
        }
        else
        {
            return o;
        }
    }

    /**
     * Creates an exception that indicates the user has generated a loop of data
     * types referencing each other.
     *
     * @return Description of the Returned Value
     */
    protected TaskException circularReference()
    {
        return new TaskException( "This data type contains a circular reference." );
    }

    /**
     * Check to see whether any DataType we hold references to is included in
     * the Stack (which holds all DataType instances that directly or indirectly
     * reference this instance, including this instance itself). <p>
     *
     * If one is included, throw a TaskException created by {@link
     * #circularReference circularReference}.</p> <p>
     *
     * This implementation is appropriate only for a DataType that cannot hold
     * other DataTypes as children.</p> <p>
     *
     * The general contract of this method is that it shouldn't do anything if
     * {@link #checked <code>checked</code>} is true and set it to true on exit.
     * </p>
     *
     * @param stk Description of Parameter
     * @param p Description of Parameter
     * @exception TaskException Description of Exception
     */
    protected void dieOnCircularReference( Stack stk, Project p )
        throws TaskException
    {

        if( checked || !isReference() )
        {
            return;
        }
        Object o = ref.getReferencedObject( p );

        if( o instanceof DataType )
        {
            if( stk.contains( o ) )
            {
                throw circularReference();
            }
            else
            {
                stk.push( o );
                ( (DataType)o ).dieOnCircularReference( stk, p );
                stk.pop();
            }
        }
        checked = true;
    }

    /**
     * Creates an exception that indicates that this XML element must not have
     * child elements if the refid attribute is set.
     *
     * @return Description of the Returned Value
     */
    protected TaskException noChildrenAllowed()
    {
        return new TaskException( "You must not specify nested elements when using refid" );
    }

    /**
     * Creates an exception that indicates that refid has to be the only
     * attribute if it is set.
     *
     * @return Description of the Returned Value
     */
    protected TaskException tooManyAttributes()
    {
        return new TaskException( "You must not specify more than one attribute" +
                                  " when using refid" );
    }
}
