/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.myrmidon.components.builder;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import org.apache.avalon.excalibur.i18n.ResourceManager;
import org.apache.avalon.excalibur.i18n.Resources;
import org.apache.myrmidon.interfaces.model.Project;
import org.apache.myrmidon.interfaces.model.Target;
import org.apache.myrmidon.interfaces.model.TypeLib;

/**
 * Default project implementation.
 *
 * @author <a href="mailto:peter@apache.org">Peter Donald</a>
 */
public class DefaultProject
    implements Project
{
    private final static Resources REZ =
        ResourceManager.getPackageResources( DefaultProject.class );

    ///The imports
    private final ArrayList m_imports = new ArrayList();

    ///The projects refferred to by this project
    private final HashMap m_projects = new HashMap();

    ///The targets contained by this project
    private final HashMap m_targets = new HashMap();

    ///The implicit target (not present in m_targets)
    private Target m_implicitTarget;

    ///The name of the default target
    private String m_defaultTarget;

    ///The base directory of project
    private File m_baseDirectory;

    /**
     * Get the imports for project.
     *
     * @return the imports
     */
    public TypeLib[] getTypeLibs()
    {
        return (TypeLib[])m_imports.toArray( new TypeLib[ 0 ] );
    }

    /**
     * Get names of projects referred to by this project.
     *
     * @return the names
     */
    public String[] getProjectNames()
    {
        return (String[])m_projects.keySet().toArray( new String[ 0 ] );
    }

    /**
     * Retrieve project reffered to by this project.
     *
     * @param name the project name
     * @return the Project or null if none by that name
     */
    public Project getProject( final String name )
    {
        return (Project)m_projects.get( name );
    }

    /**
     * Retrieve base directory of project.
     *
     * @return the projects base directory
     */
    public final File getBaseDirectory()
    {
        return m_baseDirectory;
    }

    /**
     * Retrieve implicit target.
     * The implicit target contains all the top level tasks.
     *
     * @return the Target
     */
    public final Target getImplicitTarget()
    {
        return m_implicitTarget;
    }

    /**
     * Set ImplicitTarget.
     *
     * @param target the implicit target
     */
    public final void setImplicitTarget( final Target target )
    {
        m_implicitTarget = target;
    }

    /**
     * Retrieve a target by name.
     *
     * @param name the name of target
     * @return the Target or null if no target exists with name
     */
    public final Target getTarget( final String targetName )
    {
        return (Target)m_targets.get( targetName );
    }

    /**
     * Get name of default target.
     *
     * @return the default target name
     */
    public final String getDefaultTargetName()
    {
        return m_defaultTarget;
    }

    /**
     * Retrieve names of all targets in project.
     *
     * @return an array target names
     */
    public final String[] getTargetNames()
    {
        return (String[])m_targets.keySet().toArray( new String[ 0 ] );
    }

    /**
     * Set DefaultTargetName.
     *
     * @param defaultTarget the default target name
     */
    public final void setDefaultTargetName( final String defaultTarget )
    {
        m_defaultTarget = defaultTarget;
    }

    /**
     * Retrieve base directory of project.
     *
     * @return the projects base directory
     */
    public final void setBaseDirectory( final File baseDirectory )
    {
        m_baseDirectory = baseDirectory;
    }

    public final void addTypeLib( final TypeLib typeLib )
    {
        m_imports.add( typeLib );
    }

    /**
     * Add a target.
     *
     * @param name the name of target
     * @param target the Target
     * @exception IllegalArgumentException if target already exists with same name
     */
    public final void addTarget( final String name, final Target target )
    {
        if( null != m_targets.get( name ) )
        {
            final String message = REZ.getString( "duplicate-target.error", name );
            throw new IllegalArgumentException( message );
        }
        else
        {
            m_targets.put( name, target );
        }
    }

    /**
     * Add a project reference.
     *
     * @param name the name of target
     * @param project the Project
     * @exception IllegalArgumentException if project already exists with same name
     */
    public final void addProject( final String name, final Project project )
    {
        if( null != m_projects.get( name ) )
        {
            final String message = REZ.getString( "duplicate-project.error", name );
            throw new IllegalArgumentException( message );
        }
        else
        {
            m_projects.put( name, project );
        }
    }
}
