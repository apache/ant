/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included  with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.myrmidon.api.metadata;

/**
 * The Modeller interface specifies that the implementing object
 * wishes to handle its own configuration stage. In which case the
 * object is passed the ModelElement representing itself and it uses
 * the element to configure itself.
 *
 * @author <a href="mailto:peter@apache.org">Peter Donald</a>
 * @version $Revision$ $Date$
 * @see ModelElement
 */
public interface Modeller
{
    /**
     * Pass the object a read-only instance of it's own
     * model.
     *
     * @param element the ModelElement representing object
     */
    void model( ModelElement element );
}
