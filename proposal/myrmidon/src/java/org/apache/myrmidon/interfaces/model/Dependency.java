/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included  with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.myrmidon.interfaces.model;

/**
 * A dependency for a target.
 *
 * @author <a href="mailto:adammurdoch@apache.org">Adam Murdoch</a>
 * @version $Revision$ $Date$
 */
public class Dependency
{
    private final String m_projectName;
    private final String m_targetName;

    public Dependency( final String projectName, final String targetName )
    {
        m_projectName = projectName;
        m_targetName = targetName;
    }

    public String getProjectName()
    {
        return m_projectName;
    }

    public String getTargetName()
    {
        return m_targetName;
    }
}
