/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included  with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.tools.ant;

import java.util.Locale;
import org.apache.avalon.framework.configuration.Configurable;
import org.apache.avalon.framework.configuration.Configuration;
import org.apache.avalon.framework.configuration.ConfigurationException;
import org.apache.myrmidon.api.TaskContext;
import org.apache.myrmidon.api.TaskException;

/**
 * Ant1 Task proxy for Myrmidon.
 * Note that this class and OriginalAnt1Task (superclass) comprise a single logical
 * class, but the code is kept separate for ease of development. OriginalAnt1Task
 * is barely modified from the Ant1 original, whereas this class contains
 * all of the Myrmidon-specific adaptations.
 *
 * @author <a href="mailto:darrell@apache.org">Darrell DeBoer</a>
 * @version $Revision$ $Date$
 */
public class Task extends OriginalAnt1Task
    implements org.apache.myrmidon.api.Task, Configurable
{
    protected TaskContext m_context;

    /**
     * Specify the context in which the task operates in.
     * The Task will use the TaskContext to receive information
     * about it's environment.
     */
    public void contextualize( TaskContext context )
        throws TaskException
    {
        m_context = context;

        this.setTaskType( context.getName() );
        this.setTaskName( context.getName() );

        Project project = (Project)context.getProperty( "ant1.project" );
        if( project == null )
        {
            project = createProject();
            m_context.setProperty( "ant1.project", project );
        }
        this.setProject( project );
    }

    private Project createProject()
        throws TaskException
    {
        Project project = new Ant1CompatProject( m_context );
        project.init();
        return project;
    }

    public void configure( Configuration configuration ) throws ConfigurationException
    {
        configure( this, configuration );
        this.init();
    }

    protected void configure( Object target, Configuration configuration ) throws ConfigurationException
    {
        IntrospectionHelper helper = IntrospectionHelper.getHelper( target.getClass() );

        // Configure the id.
        String id = configuration.getAttribute( "id", null );
        if( id != null )
        {
            project.addReference( id, target );
        }

        // Configure the attributes.
        final String[] attribs = configuration.getAttributeNames();
        for( int i = 0; i < attribs.length; i++ )
        {
            final String name = attribs[ i ];
            final String value =
                project.replaceProperties( configuration.getAttribute( name ) );
            try
            {
                helper.setAttribute( project, target,
                                     name.toLowerCase( Locale.US ), value );
            }
            catch( BuildException be )
            {
                // id attribute must be set externally
                if( !name.equals( "id" ) )
                {
                    throw be;
                }
            }
        }

        // Configure the text content.
        String text = configuration.getValue( null );
        if( text != null )
        {
            helper.addText( project, target, text );
        }

        // Configure the nested elements
        Configuration[] nestedConfigs = configuration.getChildren();
        for( int i = 0; i < nestedConfigs.length; i++ )
        {
            Configuration nestedConfig = nestedConfigs[ i ];
            String name = nestedConfig.getName();
            Object nestedElement = helper.createElement( project, target, name );
            configure( nestedElement, nestedConfig );
            helper.storeElement( project, target, nestedElement, name );
        }

    }

    protected String getAnt1Name( String fullName )
    {
        return fullName.substring( Ant1CompatProject.ANT1_TASK_PREFIX.length() );
    }
}
