/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.tools.ant.taskdefs.optional.perforce;

import org.apache.myrmidon.api.TaskException;
import org.apache.tools.ant.taskdefs.exec.ExecuteStreamHandler;

/**
 * Interface for p4 job output stream handler. Classes implementing this
 * interface can be called back by P4Base.execP4Command();
 *
 * @author <A HREF="mailto:leslie.hughes@rubus.com">Les Hughes</A>
 */
public interface P4Handler extends ExecuteStreamHandler
{

    public void process( String line )
        throws TaskException;

    public void setOutput( String line )
        throws TaskException;
}
