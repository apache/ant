/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included  with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.myrmidon.interfaces.extensions;

import org.apache.avalon.excalibur.extension.Extension;
import org.apache.avalon.excalibur.extension.OptionalPackage;

/**
 * Maintains a set of optional packages.
 *
 * @author <a href="mailto:peter@apache.org">Peter Donald</a>
 * @version $Revision$ $Date$
 */
public interface ExtensionManager
{
    /** Role name for this interface. */
    String ROLE = ExtensionManager.class.getName();

    /**
     * Locates the optional package which best matches a required extension.
     *
     * @param extension the extension to locate an optional package
     * @return the optional package, or null if not found.
     */
    public OptionalPackage getOptionalPackage( Extension extension );
}
