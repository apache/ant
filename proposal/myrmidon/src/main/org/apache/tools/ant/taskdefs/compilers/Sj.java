/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.tools.ant.taskdefs.compilers;

import org.apache.myrmidon.api.TaskException;
import org.apache.tools.ant.types.Commandline;

/**
 * The implementation of the sj compiler. Uses the defaults for
 * DefaultCompilerAdapter
 *
 * @author <a href="mailto:don@bea.com">Don Ferguson</a>
 */
public class Sj extends DefaultCompilerAdapter
{

    /**
     * Performs a compile using the sj compiler from Symantec.
     *
     * @return Description of the Returned Value
     * @exception TaskException Description of Exception
     * @author don@bea.com
     */
    public boolean execute()
        throws TaskException
    {
        getLogger().debug( "Using symantec java compiler" );

        Commandline cmd = setupJavacCommand();
        cmd.setExecutable( "sj" );

        int firstFileName = cmd.size() - m_compileList.length;

        return executeExternalCompile( cmd.getCommandline(), firstFileName ) == 0;
    }

}

