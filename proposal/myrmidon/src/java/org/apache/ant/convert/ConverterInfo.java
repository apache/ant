/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.ant.convert;

import java.net.URL;
import org.apache.avalon.camelot.Info;

/**
 * This info represents meta-information about a converter.
 *
 * @author <a href="mailto:donaldp@apache.org">Peter Donald</a>
 */
public interface ConverterInfo
    extends Info
{
    /**
     * Retrieve the source type from which it can convert.
     * NB: Should this be an array ????
     *
     * @return the classname from which object produced
     */
    String getSource();

    /**
     * Retrieve the type to which the converter converts.
     * NB: Should this be an array ????
     *
     * @return the classname of the produced object
     */
    String getDestination();

    /**
     * Retrieve classname for concerter.
     *
     * @return the taskname
     */
    String getClassname();

    /**
     * Retrieve location of task library where task is contained.
     *
     * @return the location of task library
     */
    URL getLocation();
}

