/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.myrmidon.interfaces.converter;

import org.apache.avalon.framework.component.Component;
import org.apache.myrmidon.converter.Converter;

/**
 * Master Converter to handle converting between types.
 *
 * @author <a href="mailto:peter@apache.org">Peter Donald</a>
 * @version $Revision$ $Date$
 */
public interface MasterConverter
    extends Component, Converter
{
    String ROLE = MasterConverter.class.getName();
}
