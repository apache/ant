/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included  with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.tools.ant.taskdefs.manifest;

import org.apache.tools.ant.types.EnumeratedAttribute;

/**
 * Helper class for Manifest's mode attribute.
 *
 * @author <a href="mailto:conor@apache.org">Conor MacNeill</a>
 * @author <a href="mailto:stefan.bodewig@epost.de">Stefan Bodewig</a>
 * @author <a href="mailto:peter@apache.org">Peter Donald</a>
 * @version $Revision$ $Date$
 */
public class ManifestMode
    extends EnumeratedAttribute
{
    public String[] getValues()
    {
        return new String[]{"update", "replace"};
    }
}
