/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.tools.ant;

import java.util.Vector;
import org.apache.myrmidon.api.TaskException;

/**
 * Wrapper class that holds all information necessary to create a task or data
 * type that did not exist when Ant started.
 *
 * @author <a href="mailto:stefan.bodewig@epost.de">Stefan Bodewig</a>
 */
public class UnknownElement extends Task
{

    /**
     * Childelements, holds UnknownElement instances.
     */
    private Vector children = new Vector();

    /**
     * Holds the name of the task/type or nested child element of a task/type
     * that hasn't been defined at parser time.
     */
    private String elementName;

    /**
     * The real object after it has been loaded.
     */
    private Object realThing;

    public UnknownElement( String elementName )
    {
        this.elementName = elementName;
    }

    /**
     * return the corresponding XML element name.
     *
     * @return The Tag value
     */
    public String getTag()
    {
        return elementName;
    }

    /**
     * Return the task instance after it has been created (and if it is a task.
     *
     * @return The Task value
     */
    public Task getTask()
    {
        if( realThing != null && realThing instanceof Task )
        {
            return (Task)realThing;
        }
        return null;
    }

    /**
     * Adds a child element to this element.
     *
     * @param child The feature to be added to the Child attribute
     */
    public void addChild( UnknownElement child )
    {
        children.addElement( child );
    }

    /**
     * Called when the real task has been configured for the first time.
     */
    public void execute()
        throws TaskException
    {
        if( realThing == null )
        {
            // plain impossible to get here, maybeConfigure should
            // have thrown an exception.
            throw new TaskException( "Could not create task of type: "
                                     + elementName );
        }

        if( realThing instanceof Task )
        {
            ( (Task)realThing ).perform();
        }
    }

    /**
     * creates the real object instance, creates child elements, configures the
     * attributes of the real object.
     *
     * @exception BuildException Description of Exception
     */
    public void maybeConfigure()
        throws TaskException
    {
    }

    protected BuildException getNotFoundException( String what,
                                                   String elementName )
    {
        String lSep = System.getProperty( "line.separator" );
        String msg = "Could not create " + what + " of type: " + elementName
            + "." + lSep
            + "Ant could not find the task or a class this" + lSep
            + "task relies upon." + lSep
            + "Common solutions are to use taskdef to declare" + lSep
            + "your task, or, if this is an optional task," + lSep
            + "to put the optional.jar and all required libraries of" + lSep
            + "this task in the lib directory of" + lSep
            + "your ant installation (ANT_HOME)." + lSep
            + "There is also the possibility that your build file " + lSep
            + "is written to work with a more recent version of ant " + lSep
            + "than the one you are using, in which case you have to " + lSep
            + "upgrade.";
        return new BuildException( msg );
    }

    /**
     * Creates child elements, creates children of the children, sets attributes
     * of the child elements.
     *
     * @param parent Description of Parameter
     * @param parentWrapper Description of Parameter
     * @exception BuildException Description of Exception
     */
    protected void handleChildren( Object parent,
                                   RuntimeConfigurable parentWrapper )
        throws TaskException
    {

        if( parent instanceof TaskAdapter )
        {
            parent = ( (TaskAdapter)parent ).getProxy();
        }

        Class parentClass = parent.getClass();
        IntrospectionHelper ih = IntrospectionHelper.getHelper( parentClass );

        for( int i = 0; i < children.size(); i++ )
        {
            RuntimeConfigurable childWrapper = parentWrapper.getChild( i );
            UnknownElement child = (UnknownElement)children.elementAt( i );
            Object realChild = null;

            if( parent instanceof TaskContainer )
            {
                realChild = makeTask( child, childWrapper, false );
                ( (TaskContainer)parent ).addTask( (Task)realChild );
            }
            else
            {
                realChild = ih.createElement( project, parent, child.getTag() );
            }

            childWrapper.setProxy( realChild );

            child.handleChildren( realChild, childWrapper );

            if( parent instanceof TaskContainer )
            {
                ( (Task)realChild ).maybeConfigure();
            }
        }
    }

    /**
     * Creates a named task or data type - if it is a task, configure it up to
     * the init() stage.
     *
     * @param ue Description of Parameter
     * @param w Description of Parameter
     * @return Description of the Returned Value
     */
    protected Object makeObject( UnknownElement ue, RuntimeConfigurable w )
        throws TaskException
    {
        Object o = makeTask( ue, w, true );
        if( o == null )
        {
            o = project.createDataType( ue.getTag() );
        }
        if( o == null )
        {
            throw getNotFoundException( "task or type", ue.getTag() );
        }
        return o;
    }

    /**
     * Create a named task and configure it up to the init() stage.
     *
     * @param ue Description of Parameter
     * @param w Description of Parameter
     * @param onTopLevel Description of Parameter
     * @return Description of the Returned Value
     */
    protected Task makeTask( UnknownElement ue, RuntimeConfigurable w,
                             boolean onTopLevel )
        throws TaskException
    {
        Task task = project.createTask( ue.getTag() );
        if( task == null && !onTopLevel )
        {
            throw getNotFoundException( "task", ue.getTag() );
        }

        if( task != null )
        {
            // UnknownElement always has an associated target
            task.setOwningTarget( target );
            task.init();
        }
        return task;
    }

}// UnknownElement
