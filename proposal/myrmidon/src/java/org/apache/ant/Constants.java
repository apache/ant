/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.ant;

/**
 * Abstract interface to hold constants.
 * 
 * @author <a href="mailto:donaldp@apache.org">Peter Donald</a>
 */
interface Constants
{
    //Constants to indicate the build of Ant/Myrmidon
    String  BUILD_DATE     = "@@DATE@@";
    String  BUILD_VERSION  = "@@VERSION@@";
}
