/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.tools.ant.util;
import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * A set of helper methods related to string manipulation.
 *
 * @author <a href="mailto:sbailliez@apache.org">Stephane Bailliez</a>
 */
public final class StringUtils
{

    /**
     * the line separator for this OS
     */
    public final static String LINE_SEP = System.getProperty( "line.separator" );

    /**
     * Convenient method to retrieve the full stacktrace from a given exception.
     *
     * @param t the exception to get the stacktrace from.
     * @return the stacktrace from the given exception.
     */
    public static String getStackTrace( Throwable t )
    {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter( sw, true );
        t.printStackTrace( pw );
        pw.flush();
        pw.close();
        return sw.toString();
    }
}
