/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.tools.ant.taskdefs;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;

/**
 * Call another target in the same project. <pre>
 *    &lt;target name="foo"&gt;
 *      &lt;antcall target="bar"&gt;
 *        &lt;param name="property1" value="aaaaa" /&gt;
 *        &lt;param name="foo" value="baz" /&gt;
 *       &lt;/antcall&gt;
 *    &lt;/target&gt;
 *
 *    &lt;target name="bar" depends="init"&gt;
 *      &lt;echo message="prop is ${property1} ${foo}" /&gt;
 *    &lt;/target&gt;
 * </pre> <p>
 *
 * This only works as expected if neither property1 nor foo are defined in the
 * project itself.
 *
 * @author <a href="mailto:stefan.bodewig@epost.de">Stefan Bodewig</a>
 */
public class CallTarget extends Task
{
    private boolean initialized = false;
    private boolean inheritAll = true;

    private Ant callee;
    private String subTarget;

    /**
     * If true, inherit all properties from parent Project If false, inherit
     * only userProperties and those defined inside the antcall call itself
     *
     * @param inherit The new InheritAll value
     */
    public void setInheritAll( boolean inherit )
    {
        inheritAll = inherit;
    }

    public void setTarget( String target )
    {
        subTarget = target;
    }

    /**
     * create a reference element that identifies a data type that should be
     * carried over to the new project.
     *
     * @param r The feature to be added to the Reference attribute
     */
    public void addReference( Ant.Reference r )
    {
        callee.addReference( r );
    }

    public Property createParam()
    {
        return callee.createProperty();
    }

    public void execute()
    {
        if( !initialized )
        {
            init();
        }

        if( subTarget == null )
        {
            throw new BuildException( "Attribute target is required.",
                location );
        }

        callee.setDir( project.getBaseDir() );
        callee.setAntfile( project.getProperty( "ant.file" ) );
        callee.setTarget( subTarget );
        callee.setInheritAll( inheritAll );
        callee.execute();
    }//-- setInheritAll

    public void init()
    {
        callee = ( Ant )project.createTask( "ant" );
        callee.setOwningTarget( target );
        callee.setTaskName( getTaskName() );
        callee.setLocation( location );
        callee.init();
        initialized = true;
    }

    protected void handleErrorOutput( String line )
    {
        if( callee != null )
        {
            callee.handleErrorOutput( line );
        }
        else
        {
            super.handleErrorOutput( line );
        }
    }

    protected void handleOutput( String line )
    {
        if( callee != null )
        {
            callee.handleOutput( line );
        }
        else
        {
            super.handleOutput( line );
        }
    }

}
