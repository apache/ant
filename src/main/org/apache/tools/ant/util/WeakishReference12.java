/*
 * User: slo
 * Date: Sep 10, 2002
 * Time: 11:02:16 PM
 * Status:      Experimental (Do Not Distribute)
 *
 * (c) Copyright 2002, Hewlett-Packard Company, all rights reserved.
 */

package org.apache.tools.ant.util;

import java.lang.ref.WeakReference;

/**
 *
 */
public class WeakishReference12 extends WeakishReference  {

    private WeakReference weakref;

    /**
     * create a new soft reference, which is bound to a
     * Weak reference inside
     * @param reference
     * @see java.lang.ref.WeakReference
     */
    public WeakishReference12(Object reference) {
        this.weakref = new WeakReference(reference);
    }

    /**
     * Returns this reference object's referent.
     */
    public Object get() {
        return weakref.get();
    }

}
