/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.ant;

/**
 * Interface that holds constants used to access variables from context.
 *
 * @author <a href="mailto:donaldp@apache.org">Peter Donald</a>
 */
public interface AntContextResources
{
    // the directory of ant
    String HOME_DIR     = "ant.install.dir"; 
    
    // the bin directory of ant
    String BIN_DIR      = "ant.install.bin"; 

    // the lib directory of ant
    String LIB_DIR      = "ant.install.lib"; 
    
    // the tasklib directory of ant
    String TASKLIB_DIR  = "ant.install.task-lib"; 

    // the directory to look for per user ant information
    String USER_DIR     = "ant.user.dir"; 
    
    // the directory to look for per project ant information
    String PROJECT_DIR  = "ant.project.dir";
}
