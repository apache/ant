/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included  with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.myrmidon.listeners;

import java.io.PrintStream;
import java.io.PrintWriter;

/**
 * A project listener that emulated the Ant 1.x -emacs mode.
 *
 * @author <a href="mailto:adammurdoch@apache.org">Adam Murdoch</a>
 * @version $Revision$ $Date$
 */
public class NoPrefixProjectListener
    extends DefaultProjectListener
{
    /**
     * Writes a message
     */
    protected void writeMessage( LogEvent event )
    {
        getWriter().println( event.getMessage() );
    }
}
