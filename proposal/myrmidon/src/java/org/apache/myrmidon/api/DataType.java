/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.myrmidon.api;

import org.apache.avalon.framework.component.Component;

/**
 * Base class for those classes that can appear inside the build file
 * as stand alone data types.  
 *
 * @author <a href="mailto:donaldp@apache.org">Peter Donald</a>
 */
public interface DataType
    extends Component
{
    String ROLE = "org.apache.myrmidon.api.DataType";
}
