/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included  with this distribution in
 * the LICENSE file.
 */
package org.apache.myrmidon.interfaces.extensions;

import org.apache.avalon.excalibur.extension.PackageRepository;
import org.apache.avalon.framework.component.Component;

/**
 * PackageRepository
 *
 * @author <a href="mailto:peter@apache.org">Peter Donald</a>
 * @version $Revision$ $Date$
 */
public interface ExtensionManager
    extends PackageRepository, Component
{
    String ROLE = "org.apache.myrmidon.interfaces.extensions.ExtensionManager";
}
