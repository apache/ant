/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.tools.ant.taskdefs;



/**
 * Exception thrown indicating problems in a JAR Manifest
 *
 * @author <a href="mailto:conor@apache.org">Conor MacNeill</a>
 */
public class ManifestException extends Exception
{

    /**
     * Constructs an exception with the given descriptive message.
     *
     * @param msg Description of or information about the exception.
     */
    public ManifestException( String msg )
    {
        super( msg );
    }
}
