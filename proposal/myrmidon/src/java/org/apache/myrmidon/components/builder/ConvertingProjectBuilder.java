/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included  with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.myrmidon.components.builder;

import java.util.HashSet;
import java.util.Set;
import org.apache.avalon.framework.configuration.Configuration;
import org.apache.avalon.framework.configuration.DefaultConfiguration;
import org.apache.myrmidon.interfaces.builder.ProjectException;

/**
 * A simple ProjectBuilder, which programmatically converts an Ant1 Project
 * configuration into a Myrmidon one.
 *
 * @author <a href="mailto:darrell@apache.org">Darrell DeBoer</a>
 * @version $Revision$ $Date$
 *
 * @ant.type type="project-builder" name="xml"
 */
public class ConvertingProjectBuilder
    extends DefaultProjectBuilder
{
    private static final String VERSION_ATTRIBUTE = "version";

    protected Configuration parseProject( String systemID )
        throws ProjectException
    {
        Configuration originalConfig = super.parseProject( systemID );

        // Check the version, if it's present, just use this config.
        // TODO: check for version < 2.0
        if( originalConfig.getAttribute( VERSION_ATTRIBUTE, null ) != null )
        {
            return originalConfig;
        }

        // Convert the config by prepending "ant1." on tasks,
        // and using <if> tasks instead of target 'if=' and 'unless='
        DefaultConfiguration newConfig = copyConfiguration( originalConfig );

        // Put a new version attribute.
        newConfig.setAttribute( VERSION_ATTRIBUTE, "2.0" );

        // Copy the remaining attributes.
        Set omitAttributes = new HashSet();
        omitAttributes.add( VERSION_ATTRIBUTE );
        copyAttributes( originalConfig, newConfig, omitAttributes );

        // Now copy/convert the children
        Configuration[] children = originalConfig.getChildren();
        for( int i = 0; i < children.length; i++ )
        {
            Configuration child = children[ i ];

            if( child.getName().equals( "target" ) )
            {
                newConfig.addChild( convertTarget( child ) );
            }
            else
            {
                newConfig.addChild( convertTask( child ) );
            }
        }

        return newConfig;
    }

    /**
     * Converts Configuration for an Ant1 Target into a Myrmidon version.
     * @param originalTarget The Ant1 Target
     * @return the converted target
     */
    private Configuration convertTarget( Configuration originalTarget )
    {
        DefaultConfiguration newTarget = copyConfiguration( originalTarget );

        // Copy all attributes except 'if' and 'unless'
        Set omitAttributes = new HashSet();
        omitAttributes.add( "if" );
        omitAttributes.add( "unless" );
        copyAttributes( originalTarget, newTarget, omitAttributes );

        DefaultConfiguration containerElement = newTarget;

        // For 'if="prop-name"', replace with <if> task.
        String ifAttrib = originalTarget.getAttribute( "if", null );
        if ( ifAttrib != null )
        {
            DefaultConfiguration ifElement =
                buildIfElement( ifAttrib, false, originalTarget.getLocation() );
            containerElement.addChild( ifElement );
            // Treat the ifElement as the enclosing target.
            containerElement = ifElement;
        }

        // For 'unless="prop-name"', replace with <if> task (negated).
        String unlessAttrib = originalTarget.getAttribute( "unless", null );
        if ( unlessAttrib != null )
        {
            DefaultConfiguration unlessElement =
                buildIfElement( unlessAttrib, true, originalTarget.getLocation() );
            containerElement.addChild( unlessElement );
            // Treat the unlessElement as the enclosing target.
            containerElement = unlessElement;
        }

        // Now copy in converted tasks.
        Configuration[] tasks = originalTarget.getChildren();
        for( int i = 0; i < tasks.length; i++ )
        {
            containerElement.addChild( convertTask( tasks[ i ] ) );
        }

        return newTarget;
    }

    /**
     * Builds configuration for an <if> task, to replace a "if" or "unless"
     * attribute on a Ant1 target.
     * @param ifProperty the name of the property from the Ant1 attribute.
     * @param unless if the attribute is actually an "unless" attribute.
     * @param location the configuration location to use
     * @return The configuration for an <if> task
     */
    private DefaultConfiguration buildIfElement( String ifProperty,
                                                 boolean unless,
                                                 final String location )
    {
        // <if>
        //      <condition>
        //          <is-set property="prop-name"/>
        //      </condition>
        //      .. tasks
        // </if>
        DefaultConfiguration isSetElement =
            new DefaultConfiguration( "is-set", location );
        isSetElement.setAttribute( "property", ifProperty );

        DefaultConfiguration conditionElement =
            new DefaultConfiguration( "condition", location );

        if ( unless )
        {
            // Surround <is-set> with <not>
            DefaultConfiguration notElement =
                new DefaultConfiguration( "not", location );
            notElement.addChild( isSetElement );
            conditionElement.addChild( notElement );
        }
        else
        {
            conditionElement.addChild( isSetElement );
        }


        DefaultConfiguration ifElement =
            new DefaultConfiguration( "if", location );
        ifElement.addChild( conditionElement );

        return ifElement;
    }

    /**
     * Converts Configuration for an Ant1 Task into a Myrmidon version.
     * @param originalTask The Ant1 Task
     * @return the converted task
     */
    private Configuration convertTask( Configuration originalTask )
    {
        // Create a new configuration with the "ant1." prefix.
        String newTaskName = "ant1." + originalTask.getName();
        DefaultConfiguration newTask =
            new DefaultConfiguration( newTaskName, originalTask.getLocation() );

        // Copy all attributes and elements of the task.
        copyAttributes( originalTask, newTask, new HashSet() );
        copyChildren( originalTask, newTask );

        return newTask;
    }

    /**
     * Copies all child elements from one configuration to another
     * @param from Configuration to copy from
     * @param to Configuration to copy to
     */
    private void copyChildren( Configuration from, DefaultConfiguration to )
    {
        Configuration[] children = from.getChildren();
        for( int i = 0; i < children.length; i++ )
        {
            to.addChild( children[ i ] );
        }
    }

    /**
     * Copies all attributes from one configuration to another, excluding
     * specified attribute names.
     * @param from Configuration to copy from
     * @param to Configuration to copy to
     * @param omitAttributes a Set of attribute names to exclude
     */
    private void copyAttributes( Configuration from,
                                 DefaultConfiguration to,
                                 Set omitAttributes )
    {
        // Copy other attributes
        String[] attribs = from.getAttributeNames();
        for( int i = 0; i < attribs.length; i++ )
        {
            String name = attribs[ i ];
            if( omitAttributes.contains( name ) )
            {
                continue;
            }
            String value = from.getAttribute( name, "" );
            to.setAttribute( name, value );
        }
    }

    /**
     * Creates a DefaultConfiguration with the same name and location as
     * the one supplied.
     * @param originalConfig the COnfiguration to copy.
     * @return the new Configuration
     */
    private DefaultConfiguration copyConfiguration( Configuration originalConfig )
    {
        return new DefaultConfiguration( originalConfig.getName(),
                                         originalConfig.getLocation() );
    }
}
