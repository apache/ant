/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.tools.ant.taskdefs.optional.junit;

import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * A couple of methods related to enumerations that might be useful. This class
 * should probably disappear once the required JDK is set to 1.2 instead of 1.1.
 *
 * @author <a href="mailto:sbailliez@imediation.com">Stephane Bailliez</a>
 */
public final class Iterators
{

    private Iterators()
    {
    }

    /**
     * creates an enumeration from an array of objects.
     *
     * @param array the array of object to enumerate.
     * @return the enumeration over the array of objects.
     */
    public static Iterator fromArray( Object[] array )
    {
        return new ArrayIterator( array );
    }

    /**
     * creates an enumeration from an array of enumeration. The created
     * enumeration will sequentially enumerate over all elements of each
     * enumeration and skip <tt>null</tt> enumeration elements in the array.
     *
     * @param enums the array of enumerations.
     * @return the enumeration over the array of enumerations.
     */
    public static Iterator fromCompound( Iterator[] enums )
    {
        return new CompoundIterator( enums );
    }

}

/**
 * Convenient enumeration over an array of objects.
 *
 * @author <a href="mailto:sbailliez@imediation.com">Stephane Bailliez</a>
 */
class ArrayIterator implements Iterator
{

    /**
     * object array
     */
    private Object[] array;

    /**
     * current index
     */
    private int pos;

    /**
     * Initialize a new enumeration that wraps an array.
     *
     * @param array the array of object to enumerate.
     */
    public ArrayIterator( Object[] array )
    {
        this.array = array;
        this.pos = 0;
    }

    /**
     * Tests if this enumeration contains more elements.
     *
     * @return <code>true</code> if and only if this enumeration object contains
     *      at least one more element to provide; <code>false</code> otherwise.
     */
    public boolean hasNext()
    {
        return ( pos < array.length );
    }

    /**
     * Returns the next element of this enumeration if this enumeration object
     * has at least one more element to provide.
     *
     * @return the next element of this enumeration.
     * @throws NoSuchElementException if no more elements exist.
     */
    public Object next()
        throws NoSuchElementException
    {
        if( hasNext() )
        {
            Object o = array[ pos ];
            pos++;
            return o;
        }
        throw new NoSuchElementException();
    }
}

/**
 * Convenient enumeration over an array of enumeration. For example: <pre>
 * Iterator e1 = v1.iterator();
 * while (e1.hasNext()){
 *    // do something
 * }
 * Iterator e2 = v2.iterator();
 * while (e2.hasNext()){
 *    // do the same thing
 * }
 * </pre> can be written as: <pre>
 * Iterator[] enums = { v1.iterator(), v2.iterator() };
 * Iterator e = Iterators.fromCompound(enums);
 * while (e.hasNext()){
 *    // do something
 * }
 * </pre> Note that the enumeration will skip null elements in the array. The
 * following is thus possible: <pre>
 * Iterator[] enums = { v1.iterator(), null, v2.iterator() }; // a null enumeration in the array
 * Iterator e = Iterators.fromCompound(enums);
 * while (e.hasNext()){
 *    // do something
 * }
 * </pre>
 *
 * @author <a href="mailto:sbailliez@imediation.com">Stephane Bailliez</a>
 */
class CompoundIterator implements Iterator
{

    /**
     * index in the enums array
     */
    private int index = 0;

    /**
     * enumeration array
     */
    private Iterator[] enumArray;

    public CompoundIterator( Iterator[] enumarray )
    {
        this.enumArray = enumarray;
    }

    /**
     * Tests if this enumeration contains more elements.
     *
     * @return <code>true</code> if and only if this enumeration object contains
     *      at least one more element to provide; <code>false</code> otherwise.
     */
    public boolean hasNext()
    {
        while( index < enumArray.length )
        {
            if( enumArray[ index ] != null && enumArray[ index ].hasNext() )
            {
                return true;
            }
            index++;
        }
        return false;
    }

    /**
     * Returns the next element of this enumeration if this enumeration object
     * has at least one more element to provide.
     *
     * @return the next element of this enumeration.
     * @throws NoSuchElementException if no more elements exist.
     */
    public Object next()
        throws NoSuchElementException
    {
        if( hasNext() )
        {
            return enumArray[ index ].next();
        }
        throw new NoSuchElementException();
    }
}

