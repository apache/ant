/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included  with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.tools.ant.taskdefs.optional.sitraka;

import org.apache.tools.ant.taskdefs.exec.LogStreamHandler;
import java.io.OutputStream;
import java.io.IOException;

/**
 * specific pumper to avoid those nasty stdin issues
 */
class CoverageStreamHandler
    extends LogStreamHandler
{
    CoverageStreamHandler( OutputStream output, OutputStream error )
    {
        super( output, error );
    }

    /**
     * there are some issues concerning all JProbe executable In our case a
     * 'Press ENTER to close this window..." will be displayed in the
     * current window waiting for enter. So I'm closing the stream right
     * away to avoid problems.
     *
     * @param os The new ProcessInputStream value
     */
    public void setProcessInputStream( OutputStream os )
    {
        try
        {
            os.close();
        }
        catch( IOException ignored )
        {
        }
    }
}
