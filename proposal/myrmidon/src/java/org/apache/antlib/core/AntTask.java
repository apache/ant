/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.antlib.core;

import java.io.File;
import org.apache.avalon.framework.parameters.Parameters;
import org.apache.myrmidon.interfaces.model.Project;

/**
 * Executes a target in a named build file.
 *
 * @author <a href="mailto:peter@apache.org">Peter Donald</a>
 * @ant.task name="ant"
 */
public class AntTask

    extends AbstractAntTask
{
    /**
     * Default build file.
     */
    private static final String DEFAULT_BUILD_FILE = "build.ant";

    /**
     * The build file which to execute. If not set defaults to
     * using "build.ant" in the basedir of current project.
     */
    private File m_file;

    /**
     * The "type" of the build file. By default this is null which
     * means the type will be determined by the build file extension.
     */
    private String m_type;

    /**
     * set the build file to process.
     *
     * @param file the build file
     */
    public void setFile( final File file )
    {
        m_file = file;
    }

    /**
     * set the type of build file.
     *
     * @param type the type of build file
     */
    public void setType( final String type )
    {
        m_type = type;
    }

    /**
     * @return The project containing the target to execute.
     * @throws Exception If a problem occurred building the project.
     */
    protected Project getProject() throws Exception
    {
        if( null == m_file )
        {
            m_file = getContext().resolveFile( DEFAULT_BUILD_FILE );
        }

        final Project project =
            getEmbeddor().createProject( m_file.toString(),
                                         m_type,
                                         new Parameters() );
        return project;
    }

}
