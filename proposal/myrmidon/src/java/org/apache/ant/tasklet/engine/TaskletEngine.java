/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.ant.tasklet.engine;

import org.apache.ant.AntException;
import org.apache.ant.configuration.Configuration;
import org.apache.avalon.Composer;
import org.apache.avalon.Contextualizable;
import org.apache.log.Logger;
 
public interface TaskletEngine
    extends Contextualizable, Composer
{
    void setLogger( Logger logger );
    void execute( final Configuration task )
        throws AntException;
}
