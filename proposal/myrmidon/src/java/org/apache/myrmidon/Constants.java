/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.myrmidon;

/**
 * Abstract interface to hold constants.
 *
 * @author <a href="mailto:peter@apache.org">Peter Donald</a>
 * @version $Revision$ $Date$
 */
public interface Constants
{
    //Constants to indicate the build of Myrmidon
    String BUILD_DATE = "@@DATE@@";
    String BUILD_VERSION = "@@VERSION@@";

    String BUILD_DESCRIPTION = "Myrmidon " + BUILD_VERSION + " compiled on " + BUILD_DATE;
}
