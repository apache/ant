/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included  with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.myrmidon.framework.exec;

/**
 * This class is used to receive notifications of what the native
 * process outputs to standard output and standard error.
 *
 * @author <a href="mailto:peter@apache.org">Peter Donald</a>
 * @version $Revision$ $Date$
 */
public interface ExecOutputHandler
{
    /**
     * Receive notification about the process writing
     * to standard output.
     */
    void stdout( String line );

    /**
     * Receive notification about the process writing
     * to standard error.
     */
    void stderr( String line );
}
