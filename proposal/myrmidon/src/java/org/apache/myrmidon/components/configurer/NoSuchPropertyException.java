/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included  with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.myrmidon.components.configurer;

import org.apache.avalon.framework.CascadingException;

/**
 * An exception thrown when an unknown property is encountered.
 *
 * TODO - this should extend ConfigurationException, however
 * ConfigurationException is final.
 *
 * @author Adam Murdoch
 */
public class NoSuchPropertyException extends CascadingException
{
    public NoSuchPropertyException( String message )
    {
        super( message );
    }
}
