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
import org.apache.ant.tasklet.TaskletContext;
import org.apache.avalon.ConfigurationException;
import org.apache.log.Logger;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class DefaultProjectBuilder
    implements ProjectBuilder
{
    protected final ConfigurationBuilder  m_configurationBuilder;
    protected Logger                      m_logger;

    public DefaultProjectBuilder()
    {
        ConfigurationBuilder builder = null;
        try { builder = new ConfigurationBuilder(); }
        catch( final SAXException se ) {}

        m_configurationBuilder = builder;
    }

    public void setLogger( final Logger logger )
    {
        m_logger = logger;
    }

    public Project build( final File projectFile )
        throws IOException, AntException
    {
        try
        {
            final String location = projectFile.getCanonicalFile().toURL().toString();
            final InputSource inputSource = new InputSource( location );
            final Configuration configuration = 
                (Configuration)m_configurationBuilder.build( inputSource );
            return build( projectFile, configuration );
        }
        catch( final SAXException se )
        {
            throw new AntException( "SAXEception: " + se.getMessage(), se );
        }
        catch( final ConfigurationException ce )
        {
            throw new AntException( "ConfigurationException: " + ce.getMessage(), ce );
        }
    }

    protected Project build( final File file, final Configuration configuration )
        throws IOException, AntException, ConfigurationException
    {
        if( !configuration.getName().equals("project") )
        {
            throw new AntException( "Project file must be enclosed in project element" );
        }
        
        final String baseDirectoryName = configuration.getAttribute( "basedir" );
        final String defaultTarget = configuration.getAttribute( "default" );
        final String projectName = configuration.getAttribute( "name" );

        final DefaultProject project = new DefaultProject();
        project.setDefaultTargetName( defaultTarget );

        final File baseDirectory = 
            (new File( file.getParentFile(), baseDirectoryName )).getAbsoluteFile();

        m_logger.debug( "Project " + projectName + " base directory: " + baseDirectory );
        
        final TaskletContext context = project.getContext();
        context.setProperty( TaskletContext.BASE_DIRECTORY, baseDirectory );
        context.setProperty( Project.PROJECT_FILE, file );
        context.setProperty( Project.PROJECT, projectName );

        buildTopLevelProject( project, configuration );

        return project;
    }

    protected void buildTopLevelProject( final DefaultProject project, 
                                         final Configuration configuration )
        throws AntException
    {
        final Iterator elements = configuration.getChildren();

        while( elements.hasNext() )
        {
            final Configuration element = (Configuration)elements.next();
            final String name = element.getName();

            if( name.equals( "target" ) ) buildTarget( project, element );
            else if( name.equals( "property" ) ) buildProperty( project, element );
            else
            {
                throw new AntException( "Unknown top-level element " + name + 
                                        " at " + element.getLocation() );
            }
        }
    }

    protected void buildTarget( final DefaultProject project, 
                                final Configuration configuration )
    {
        final String name = configuration.getAttribute( "name", null );
        final String depends = configuration.getAttribute( "depends", null );
        final String ifCondition = configuration.getAttribute( "if", null );
        final String unlessCondition = configuration.getAttribute( "unless", null );

        if( null == name )
        {
            throw new AntException( "Discovered un-named target at " + 
                                    configuration.getLocation() );
        } 

        m_logger.debug( "Parsing target: " + name );

        if( null != ifCondition && null != unlessCondition )
        {
            throw new AntException( "Discovered invalid target that has both a if and " +
                                    "unless condition at " + configuration.getLocation() );    
        }

        final DefaultTarget target = new DefaultTarget();
        
        if( null != ifCondition )
        {
            m_logger.debug( "Target if condition: " + ifCondition );
            target.setIfCondition( true );
            target.setCondition( ifCondition );
        }
        else if( null != unlessCondition )
        {
            m_logger.debug( "Target unless condition: " + unlessCondition );
            target.setIfCondition( false );
            target.setCondition( unlessCondition );
        }

        if( null != depends )
        {
            int start = 0;
            int end = depends.indexOf( ',' );

            while( -1 != end )
            {
                final String dependency = 
                    parseDependency( configuration, depends.substring( start, end ) );

                target.addDependency( dependency );
                start = end++;
                end = depends.indexOf( ',', start );
            }    

            final String dependency = 
                parseDependency( configuration, depends.substring( start ) );

            target.addDependency( dependency );
        }

        final Iterator tasks = configuration.getChildren();
        while( tasks.hasNext() )
        {
            final Configuration task = (Configuration)tasks.next();
            m_logger.debug( "Parsed task: " + task.getName() );
            target.addTask( task );
        }

        project.addTarget( name, target );
    }

    protected String parseDependency( final Configuration configuration, 
                                      String dependency ) 
        throws AntException
    {
        dependency = dependency.trim();
        
        if( 0 == dependency.length() )
        {
            throw new AntException( "Discovered empty dependency in target " + 
                                    configuration.getName() + " at " + 
                                    configuration.getLocation() ); 
        }
        
        m_logger.debug( "Target dependency: " + dependency );

        return dependency;
    }

    protected void buildProperty( final DefaultProject project, 
                                  final Configuration configuration )
    {       
        DefaultTarget target = (DefaultTarget)project.getImplicitTarget();
        
        if( null == target )
        {
            target = new DefaultTarget();
            project.setImplicitTarget( target );
        }

        m_logger.debug( "Parsed implicit task: " + configuration.getName() );
        target.addTask( configuration );
    }
}
