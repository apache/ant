/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included  with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.tools.ant.taskdefs;

import java.util.Iterator;
import java.util.Vector;
import org.apache.avalon.framework.configuration.Configuration;
import org.apache.avalon.framework.configuration.DefaultConfiguration;
import org.apache.myrmidon.api.TaskException;
import org.apache.myrmidon.interfaces.executor.ExecutionFrame;
import org.apache.myrmidon.interfaces.executor.Executor;
import org.apache.tools.ant.Ant1CompatProject;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;

/**
 * A base class for Ant1 versions of &lt;ant&gt; and &lt;antcall&gt; tasks,
 * which delegate to the Myrmidon versions of these tasks.
 *
 * @author <a href="mailto:darrell@apache.org">Darrell DeBoer</a>
 * @version $Revision$ $Date$
 */
public abstract class AbstractAnt1AntTask
    extends Task
{
    /** the target to call if any */
    private String target = null;
    /** should we inherit properties from the parent ? */
    private boolean inheritAll = true;
    /** the properties to pass to the new project */
    private Vector properties = new Vector();
    /** the references to pass to the new project */
    private Vector references = new Vector();

    /**
     * If true, inherit all properties from parent Project
     * If false, inherit only userProperties and those defined
     * inside the ant call itself
     */
    public void setInheritAll( boolean value )
    {
        inheritAll = value;
    }

    /**
     * set the target to execute. If none is defined it will
     * execute the default target of the build file
     */
    public void setTarget( String s )
    {
        this.target = s;
    }

    /**
     * Create a nested property (ant) or param (antcall) element.
     */
    protected Property doCreateProperty()
    {
        Property p = new Property( true );
        properties.addElement( p );
        return p;
    }

    /**
     * create a reference element that identifies a data type that
     * should be carried over to the new project.
     */
    public void addReference( Reference r )
    {
        references.addElement( r );
    }

    /**
     * Helper class that implements the nested &lt;reference&gt;
     * element of &lt;ant&gt; and &lt;antcall&gt;.
     */
    public static class Reference
        extends org.apache.tools.ant.types.Reference
    {

        public Reference()
        {
            super();
        }

        private String targetid = null;

        public void setToRefid( String targetid )
        {
            this.targetid = targetid;
        }

        public String getToRefid()
        {
            return targetid;
        }
    }

    /**
     * Removes the Ant1CompatProject from the properties, builds a TaskModel for
     * executing the Myrmidon task, and executes that TaskModel.
     * @throws BuildException on error
     */
    public void execute() throws BuildException
    {
        Object ant1project = unsetAnt1Project();

        try
        {
            Configuration antConfig = constructTaskModel();

            executeTask( antConfig );
        }
        finally
        {
            resetAnt1Project( ant1project );
        }
    }

    /**
     * Executes the Myrmidon task detailed in the TaskModel provided.
     * @param taskModel the TaskModel for the task to execute.
     */
    private void executeTask( Configuration taskModel )
    {
        try
        {
            Executor executor = (Executor)m_context.getService( Executor.class );
            ExecutionFrame frame =
                (ExecutionFrame)m_context.getService( ExecutionFrame.class );
            executor.execute( taskModel, frame );
        }
        catch( TaskException e )
        {
            throw new BuildException( e );
        }
    }

    /**
     * Removes the Ant1CompatProject from the TaskContext properties.
     * @return the removed project
     * @throws BuildException
     */
    private Object unsetAnt1Project() throws BuildException
    {
        Object ant1project = null;
        try
        {
            ant1project =
                m_context.getProperty( Ant1CompatProject.ANT1_PROJECT_PROP );
            m_context.setProperty( Ant1CompatProject.ANT1_PROJECT_PROP, null );
        }
        catch( TaskException e )
        {
            throw new BuildException( e );
        }
        return ant1project;
    }

    /**
     * Adds the Ant1CompatProject back into the TaskContext properties.
     * @param ant1project the project to add.
     * @throws BuildException
     */
    private void resetAnt1Project( Object ant1project ) throws BuildException
    {
        try
        {
            m_context.setProperty( Ant1CompatProject.ANT1_PROJECT_PROP,
                                   ant1project );
        }
        catch( TaskException e )
        {
            throw new BuildException( e );
        }
    }

    /**
     * Builds the TaskModel for executing the Myrmidon version of a task.
     * @return a Configuration containing the TaskModel
     */
    protected Configuration constructTaskModel()
    {
        DefaultConfiguration antConfig = buildTaskModel();

        antConfig.setAttribute( "inherit-all", String.valueOf( inheritAll ) );

        // Ignore inheritRefs for now ( inheritAll == inheritRefs )

        if( target != null )
        {
            antConfig.setAttribute( "target", target );
        }

        addProperties( antConfig );
        addReferences( antConfig );

        return antConfig;
    }

    /**
     * Create the Myrmidon TaskModel, and configure with subclass-specific config.
     */
    protected abstract DefaultConfiguration buildTaskModel();

    /**
     * Adds all defined properties to the supplied Task model.
     * @param taskModel
     */
    protected void addProperties( DefaultConfiguration taskModel )
    {
        // Add all of the properties.
        Iterator iter = properties.iterator();
        while( iter.hasNext() )
        {
            DefaultConfiguration param = new DefaultConfiguration( "param", "" );
            Property property = (Property)iter.next();
            param.setAttribute( "name", property.getName() );
            param.setAttribute( "value", property.getValue() );
            taskModel.addChild( param );
        }
    }

    /**
     * Adds all defined references to the supplied Task model.
     * @param taskModel
     */
    protected void addReferences( DefaultConfiguration taskModel )
    {
        // TODO: Handle references.
    }

}
