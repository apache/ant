/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included  with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.myrmidon.listeners;

/**
 * A log message event.
 *
 * @author <a href="mailto:adammurdoch@apache.org">Adam Murdoch</a>
 * @version $Revision$ $Date$
 */
public interface LogEvent
    extends TaskEvent
{
    /**
     * Returns the message.
     */
    public String getMessage();

    /**
     * Returns the error that occurred.
     */
    public Throwable getThrowable();
}
