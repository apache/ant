/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included  with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.myrmidon.interfaces.extensions;

import org.apache.avalon.excalibur.extension.PackageRepository;

/**
 * PackageRepository
 *
 * @author <a href="mailto:peter@apache.org">Peter Donald</a>
 * @version $Revision$ $Date$
 */
public interface ExtensionManager
    extends PackageRepository
{
    /** Role name for this interface. */
    String ROLE = ExtensionManager.class.getName();
}
