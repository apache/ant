/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.ant.project;

import java.io.File;
import java.io.IOException;
import org.apache.ant.AntException;
import org.apache.log.Logger;

public interface ProjectBuilder
{
    void setLogger( Logger logger );

    Project build( File projectFile )
        throws IOException, AntException;
}


