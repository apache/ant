/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.ant.project;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import org.apache.ant.AntException;
import org.apache.ant.configuration.Configuration;
import org.apache.ant.configuration.ConfigurationBuilder;
import org.apache.ant.util.Condition;
import org.apache.ant.tasklet.TaskletContext;
import org.apache.avalon.AbstractLoggable;
import org.apache.avalon.ConfigurationException;
import org.apache.avalon.util.StringUtil;
import org.apache.log.Logger;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * Default implementation to construct project from a build file.
 *
 * @author <a href="mailto:donaldp@apache.org">Peter Donald</a>
 */
public class DefaultProjectBuilder
    extends AbstractLoggable
    implements ProjectBuilder
{
    protected ConfigurationBuilder  m_builder;

    public DefaultProjectBuilder()
    {
        m_builder = new ConfigurationBuilder();
    }

    /**
     * build a project from file.
     *
     * @param source the source
     * @return the constructed Project
     * @exception IOException if an error occurs
     * @exception AntException if an error occurs
     */
    public Project build( final File projectFile )
        throws IOException, AntException
    {
        try
        {
            final String location = projectFile.getCanonicalFile().toString();
            final Configuration configuration = buildConfiguration( location );
            return build( projectFile, configuration );
        }
        catch( final ConfigurationException ce )
        {
            throw new AntException( "ConfigurationException: " + ce.getMessage(), ce );
        }
    }

    /**
     * Utility method to build a Configuration tree from a source.
     * Overide this in sub-classes if you want to provide extra 
     * functionality (ie xslt/css).
     *
     * @param location the location
     * @return the created Configuration
     * @exception AntException if an error occurs
     * @exception IOException if an error occurs
     */
    protected Configuration buildConfiguration( final String location )
        throws AntException, IOException, ConfigurationException
    {
        try
        {
            return (Configuration)m_builder.build( location ); 
        }
        catch( final SAXException se )
        {
            throw new AntException( "SAXEception: " + se.getMessage(), se );
        }
    }

    /**
     * build project from configuration.
     *
     * @param file the file from which configuration was loaded
     * @param configuration the configuration loaded
     * @return the created Project
     * @exception IOException if an error occurs
     * @exception AntException if an error occurs
     * @exception ConfigurationException if an error occurs
     */
    protected Project build( final File file, final Configuration configuration )
        throws IOException, AntException, ConfigurationException
    {
        if( !configuration.getName().equals("project") )
        {
            throw new AntException( "Project file must be enclosed in project element" );
        }

        //get project-level attributes
        final String baseDirectoryName = configuration.getAttribute( "basedir" );
        final String defaultTarget = configuration.getAttribute( "default" );
        final String projectName = configuration.getAttribute( "name" );

        //determine base directory for project
        final File baseDirectory = 
            (new File( file.getParentFile(), baseDirectoryName )).getAbsoluteFile();

        getLogger().debug( "Project " + projectName + " base directory: " + baseDirectory );

        //create project and ...
        final DefaultProject project = new DefaultProject();
        project.setDefaultTargetName( defaultTarget );
        
        //setup basic context of project
        final TaskletContext context = project.getContext();
        context.setProperty( TaskletContext.BASE_DIRECTORY, baseDirectory );
        context.setProperty( Project.PROJECT_FILE, file );
        context.setProperty( Project.PROJECT, projectName );

        //build using all top-level attributes
        buildTopLevelProject( project, configuration );

        return project;
    }

    /**
     * Handle all top level elements in configuration.
     *
     * @param project the project
     * @param configuration the Configuration
     * @exception AntException if an error occurs
     */
    protected void buildTopLevelProject( final DefaultProject project, 
                                         final Configuration configuration )
        throws AntException
    {
        final Iterator elements = configuration.getChildren();

        while( elements.hasNext() )
        {
            final Configuration element = (Configuration)elements.next();
            final String name = element.getName();

            //handle individual elements
            if( name.equals( "target" ) ) buildTarget( project, element );
            else if( name.equals( "property" ) ) buildImplicitTask( project, element );
            else
            {
                throw new AntException( "Unknown top-level element " + name + 
                                        " at " + element.getLocation() );
            }
        }
    }

    /**
     * Build a target from configuration.
     *
     * @param project the project
     * @param task the Configuration
     */
    protected void buildTarget( final DefaultProject project, final Configuration target )
    {
        final String name = target.getAttribute( "name", null );
        final String depends = target.getAttribute( "depends", null );
        final String ifCondition = target.getAttribute( "if", null );
        final String unlessCondition = target.getAttribute( "unless", null );

        if( null == name )
        {
            throw new AntException( "Discovered un-named target at " + 
                                    target.getLocation() );
        } 

        getLogger().debug( "Parsing target: " + name );

        if( null != ifCondition && null != unlessCondition )
        {
            throw new AntException( "Discovered invalid target that has both a if and " +
                                    "unless condition at " + target.getLocation() );    
        }

        Condition condition = null;

        if( null != ifCondition )
        {
            getLogger().debug( "Target if condition: " + ifCondition );
            condition = new Condition( true, ifCondition );
        }
        else if( null != unlessCondition )
        {
            getLogger().debug( "Target unless condition: " + unlessCondition );
            condition = new Condition( false, unlessCondition );
        }

        final DefaultTarget defaultTarget = new DefaultTarget( condition );

        //apply depends attribute
        if( null != depends )
        {
            final String[] elements = StringUtil.splitString( depends, "," );

            for( int i = 0; i < elements.length; i++ )
            {
                final String dependency = elements[ i ].trim();

                if( 0 == dependency.length() )
                {
                    throw new AntException( "Discovered empty dependency in target " + 
                                            target.getName() + " at " + target.getLocation() ); 
                }

                getLogger().debug( "Target dependency: " + dependency );
                defaultTarget.addDependency( dependency );
            }    
        }

        //add all the targets from element
        final Iterator tasks = target.getChildren();
        while( tasks.hasNext() )
        {
            final Configuration task = (Configuration)tasks.next();
            getLogger().debug( "Parsed task: " + task.getName() );
            defaultTarget.addTask( task );
        }

        //add target to project
        project.addTarget( name, defaultTarget );
    }

    /**
     * Create an implict task from configuration
     *
     * @param project the project
     * @param task the configuration
     */
    protected void buildImplicitTask( final DefaultProject project, final Configuration task )
    {       
        DefaultTarget target = (DefaultTarget)project.getImplicitTarget();
        
        if( null == target )
        {
            target = new DefaultTarget();
            project.setImplicitTarget( target );
        }

        getLogger().debug( "Parsed implicit task: " + task.getName() );
        target.addTask( task );
    }
}
