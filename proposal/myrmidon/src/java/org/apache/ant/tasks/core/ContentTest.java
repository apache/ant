/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.ant.tasks.core;

import org.apache.ant.AntException;
import org.apache.ant.tasklet.AbstractTasklet;

/**
 * This is abstract base class for tasklets.
 *
 * @author <a href="mailto:donaldp@apache.org">Peter Donald</a>
 */
public class ContentTest 
    extends AbstractTasklet
{
    public void addContent( final Integer value )
    {
        getLogger().info( "Integer content: " + value );
    }

    /*
      public void addContent( final String blah )
      {
      System.out.println( "String: " + blah );
      }
    */

    public void run()
        throws AntException
    {
    }
}
