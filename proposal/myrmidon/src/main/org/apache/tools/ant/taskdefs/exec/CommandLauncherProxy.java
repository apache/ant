/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included  with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.tools.ant.taskdefs.exec;

import org.apache.tools.ant.Project;
import org.apache.myrmidon.api.TaskException;
import java.io.IOException;

/**
 * A command launcher that proxies another command launcher. Sub-classes
 * override exec(args, env, workdir)
 */
class CommandLauncherProxy
    extends CommandLauncher
{

    private CommandLauncher _launcher;

    CommandLauncherProxy( CommandLauncher launcher )
    {
        _launcher = launcher;
    }

    /**
     * Launches the given command in a new process. Delegates this method to
     * the proxied launcher
     *
     * @param project Description of Parameter
     * @param cmd Description of Parameter
     * @param env Description of Parameter
     * @return Description of the Returned Value
     * @exception IOException Description of Exception
     */
    public Process exec( Project project, String[] cmd, String[] env )
        throws IOException, TaskException
    {
        return _launcher.exec( project, cmd, env );
    }
}
