/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.tools.ant.taskdefs.optional.pvcs;


/**
 * class to handle &lt;pvcsprojec&gt; elements
 *
 * @author RT
 */
public class PvcsProject
{
    private String name;

    public PvcsProject()
    {
        super();
    }

    public void setName( String name )
    {
        PvcsProject.this.name = name;
    }

    public String getName()
    {
        return name;
    }
}
