/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included  with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.myrmidon.interfaces.model;

/**
 * Determines the validity of names used in projects.
 *
 * @author <a href="mailto:darrell@apache.org">Darrell DeBoer</a>
 * @version $Revision$ $Date$
 */
public interface NameValidator
{
    /**
     * Validates the supplied name, failing if it is not.
     * @throws Exception is the supplied name is not valid.
     */
    void validate( String name ) throws Exception;
}
