/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included  with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.tools.todo.taskdefs.sitraka;

/**
 * default abstract filter element class
 */
public abstract class FilterElement
{
    protected String clazz = "*";// default is all classes
    protected String method = "*";// default is all methods

    public void setClass( String value )
    {
        clazz = value;
    }

    public void setMethod( String value )
    {
        method = value;
    }

    public String getAsPattern()
    {
        StringBuffer buf = new StringBuffer( toString() );
        replace( buf, ".", "\\." );
        replace( buf, "*", ".*" );
        replace( buf, "(", "\\(" );
        replace( buf, ")", "\\)" );
        return buf.toString();
    }

    public String toString()
    {
        return clazz + "." + method + "()";
    }

    /**
     * Replaces all occurences of <tt>find</tt> with <tt>replacement</tt> in the
     * source StringBuffer.
     *
     * @param src the original string buffer to modify.
     * @param find the string to be replaced.
     * @param replacement the replacement string for <tt>find</tt> matches.
     */
    public static void replace( StringBuffer src, String find, String replacement )
    {
        int index = 0;
        while( index < src.length() )
        {
            index = src.toString().indexOf( find, index );
            if( index == -1 )
            {
                break;
            }
            src.delete( index, index + find.length() );
            src.insert( index, replacement );
            index += replacement.length() + 1;
        }
    }
}
