/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.ant.tasklet.engine;

import org.apache.ant.tasklet.Tasklet;
import org.apache.avalon.camelot.Loader;

/**
 * Class used to load tasks et al from a source.
 * 
 * @author <a href="mailto:donaldp@apache.org">Peter Donald</a>
 */
public interface TaskletLoader
    extends Loader
{
    /**
     * Load a tasklet with a particular classname.
     *
     * @param tasklet the tasklet classname
     * @return the tasklet
     * @exception Exception if an error occurs
     */
    Tasklet loadTasklet( String tasklet )
        throws Exception;
}
