/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.myrmidon.components.builder;

import org.xml.sax.SAXException;

/**
 * Dummy exception to stop parsing "safely".
 *
 * @author <a href="mailto:peter@apache.org">Peter Donald</a>
 */
class StopParsingException
    extends SAXException
{
    public StopParsingException()
    {
        super( "" );
    }
}
