/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.ant.configuration;

import org.apache.avalon.ConfigurationException;

/**
 * This interface should be implemented by classes that need to be
 * configured with custom parameters before initialization.
 * <br />
 *
 * The contract surrounding a <code>Configurable</code> is that the
 * instantiating entity must call the <code>configure</code>
 * method before it is valid.  The <code>configure</code> method
 * must be called after the constructor, and before any other method.
 *
 * @author <a href="mailto:fede@apache.org">Federico Barbieri</a>
 * @author <a href="mailto:pier@apache.org">Pierpaolo Fumagalli</a>
 * @author <a href="mailto:stefano@apache.org">Stefano Mazzocchi</a>
 * @author <a href="mailto:bloritsch@apache.org">Berin Loritsch</a>
 * @author <a href="mailto:donaldp@apache.org">Peter Donald</a>
 */
public interface Configurable
{
    /**
     * Pass the <code>Configuration</code> to the <code>Configurable</code>
     * class. This method must always be called after the constructor
     * and before any other method.
     *
     * @param configuration the class configurations.
     */
    void configure( Configuration configuration ) 
        throws ConfigurationException;
}
