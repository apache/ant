/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included  with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.tools.todo.taskdefs.archive;

import org.apache.myrmidon.api.TaskException;
import org.apache.tools.todo.types.EnumeratedAttribute;

/**
 * Valid Modes for LongFile attribute to Tar Task
 *
 * @author <a href="mailto:umagesh@apache.org">Magesh Umasankar</a>
 */
public class TarLongFileMode
    extends EnumeratedAttribute
{
    // permissable values for longfile attribute
    public final static String WARN = "warn";
    public final static String FAIL = "fail";
    public final static String TRUNCATE = "truncate";
    public final static String GNU = "gnu";
    public final static String OMIT = "omit";

    private final String[] validModes = {WARN, FAIL, TRUNCATE, GNU, OMIT};

    public TarLongFileMode()
        throws TaskException
    {
        super();
        setValue( WARN );
    }

    public String[] getValues()
    {
        return validModes;
    }

    public boolean isFailMode()
    {
        return FAIL.equalsIgnoreCase( getValue() );
    }

    public boolean isGnuMode()
    {
        return GNU.equalsIgnoreCase( getValue() );
    }

    public boolean isOmitMode()
    {
        return OMIT.equalsIgnoreCase( getValue() );
    }

    public boolean isTruncateMode()
    {
        return TRUNCATE.equalsIgnoreCase( getValue() );
    }

    public boolean isWarnMode()
    {
        return WARN.equalsIgnoreCase( getValue() );
    }
}
