/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included  with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.aut.nativelib.impl;

import java.io.IOException;
import org.apache.aut.nativelib.ExecMetaData;
import org.apache.aut.nativelib.ExecException;

/**
 * This is the interface implemented by objects which are capable of
 * lauching a native command. Each different implementation is likely
 * to have a different strategy or be restricted to specific platform.
 *
 * <p>It is expected that the user will get a reference to the
 * <code>CommandLauncher</code> most appropriate for their environment.</p>
 *
 * @author <a href="mailto:peter@apache.org">Peter Donald</a>
 * @version $Revision$ $Date$
 */
public interface CommandLauncher
{
    /**
     * Execute the specified native command.
     *
     * @param metaData the native command to execute
     * @return the Process launched by the CommandLauncher
     * @exception IOException is thrown when the native code can not
     *            launch the application for some reason. Usually due
     *            to the command not being fully specified and not in
     *            the PATH env var.
     * @exception ExecException if the command launcher detects that
     *            it can not execute the native command for some reason.
     */
    Process exec( ExecMetaData metaData )
        throws IOException, ExecException;
}
