/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.tools.ant;

import java.io.PrintStream;

/**
 * Interface used by Ant to log the build output. A build logger is a build
 * listener which has the 'right' to send output to the ant log, which is
 * usually System.out unles redirected by the -logfile option.
 *
 * @author Conor MacNeill
 */
public interface BuildLogger extends BuildListener
{
    /**
     * Set the msgOutputLevel this logger is to respond to. Only messages with a
     * message level lower than or equal to the given level are output to the
     * log. <P>
     *
     * Constants for the message levels are in Project.java. The order of the
     * levels, from least to most verbose, is MSG_ERR, MSG_WARN, MSG_INFO,
     * MSG_VERBOSE, MSG_DEBUG.
     *
     * @param level the logging level for the logger.
     */
    void setMessageOutputLevel( int level );

    /**
     * Set the output stream to which this logger is to send its output.
     *
     * @param output the output stream for the logger.
     */
    void setOutputPrintStream( PrintStream output );

    /**
     * Set this logger to produce emacs (and other editor) friendly output.
     *
     * @param emacsMode true if output is to be unadorned so that emacs and
     *      other editors can parse files names, etc.
     */
    void setEmacsMode( boolean emacsMode );

    /**
     * Set the output stream to which this logger is to send error messages.
     *
     * @param err the error stream for the logger.
     */
    void setErrorPrintStream( PrintStream err );
}
