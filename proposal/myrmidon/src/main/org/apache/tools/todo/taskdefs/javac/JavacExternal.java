/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.tools.todo.taskdefs.javac;

import org.apache.myrmidon.api.TaskException;
import org.apache.myrmidon.api.TaskContext;
import org.apache.tools.todo.types.Commandline;
import org.apache.tools.todo.taskdefs.javac.DefaultCompilerAdapter;

/**
 * Performs a compile using javac externally.
 *
 * @author Brian Deitte
 */
public class JavacExternal extends DefaultCompilerAdapter
{

    /**
     * Performs a compile using the Javac externally.
     *
     * @return Description of the Returned Value
     * @exception org.apache.myrmidon.api.TaskException Description of Exception
     */
    public boolean execute()
        throws TaskException
    {
        getTaskContext().debug( "Using external javac compiler" );

        Commandline cmd = new Commandline();
        cmd.setExecutable( getJavac().getJavacExecutable() );
        setupModernJavacCommandlineSwitches( cmd );
        int firstFileName = cmd.size();
        logAndAddFilesToCompile( cmd );

        return executeExternalCompile( cmd.getCommandline(), firstFileName ) == 0;
    }

}

