/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.ant.modules.test;

import org.apache.myrmidon.api.TaskException;
import org.apache.myrmidon.api.AbstractTask;

/**
 * This is to test whether content is added.
 *
 * @author <a href="mailto:donaldp@apache.org">Peter Donald</a>
 */
public class ContentTest 
    extends AbstractTask
{
    public void addContent( final Integer value )
    {
        getLogger().warn( "Integer content: " + value );
    }

    /*
      public void addContent( final String blah )
      {
      System.out.println( "String: " + blah );
      }
    */

    public void execute()
        throws TaskException
    {
    }
}
