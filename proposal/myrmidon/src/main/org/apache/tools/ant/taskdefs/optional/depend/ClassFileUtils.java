/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.tools.ant.taskdefs.optional.depend;

/**
 * Utility class file routines. This class porovides a number of static utility
 * methods to convert between the formats used in the Java class file format and
 * those commonly used in Java programming.
 *
 * @author Conor MacNeill
 */
public class ClassFileUtils
{

    /**
     * Convert a class name from java source file dot notation to class file
     * slash notation..
     *
     * @param dotName the class name in dot notation (eg. java.lang.Object).
     * @return the class name in slash notation (eg. java/lang/Object).
     */
    public static String convertDotName( String dotName )
    {
        return dotName.replace( '.', '/' );
    }

    /**
     * Convert a class name from class file slash notation to java source file
     * dot notation.
     *
     * @param name Description of Parameter
     * @return the class name in dot notation (eg. java.lang.Object).
     */
    public static String convertSlashName( String name )
    {
        return name.replace( '\\', '.' ).replace( '/', '.' );
    }
}

