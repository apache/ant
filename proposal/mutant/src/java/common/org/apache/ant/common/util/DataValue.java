/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2002 The Apache Software Foundation.  All rights
 * reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution, if
 *    any, must include the following acknowlegement:
 *       "This product includes software developed by the
 *        Apache Software Foundation (http://www.apache.org/)."
 *    Alternately, this acknowlegement may appear in the software itself,
 *    if and wherever such third-party acknowlegements normally appear.
 *
 * 4. The names "The Jakarta Project", "Ant", and "Apache Software
 *    Foundation" must not be used to endorse or promote products derived
 *    from this software without prior written permission. For written
 *    permission, please contact apache@apache.org.
 *
 * 5. Products derived from this software may not be called "Apache"
 *    nor may "Apache" appear in their names without prior written
 *    permission of the Apache Group.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE APACHE SOFTWARE FOUNDATION OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Apache Software Foundation.  For more
 * information on the Apache Software Foundation, please see
 * <http://www.apache.org/>.
 */
package org.apache.ant.common.util;
import java.util.HashMap;
import java.util.Iterator;

import java.util.Map;

/**
 * A DataValue is an arbitrary value with an associated priority.
 *
 * @author Conor MacNeill
 * @created 26 June 2002
 */
public class DataValue {
    /** Base priority level */
    public static final int PRIORITY_BASE = 0;
    /** Prioirty of values inherited from a super build. */
    public static final int PRIORITY_INHERIT = 10;
    /** Priority of values specified by the user. */
    public static final int PRIORITY_USER = 20;

    /** The DataValue's priority */
    private int priority;
    /** The actual data. */
    private Object value;

    /**
     * Create a DataValue with the given data and priority.
     *
     * @param value the actual value
     * @param priority the priority associated with this value.
     */
    public DataValue(Object value, int priority) {
        this.priority = priority;
        this.value = value;
    }

    /**
     * Convert plain named values into a collection of DataValues with the
     * given priority
     *
     * @param values A collection of values named by String keys
     * @param priority The required data value to be applied to the values.
     * @return A collection of datavalues corresponding to the input collection
     * and having the specified priority.
     */
    public static Map makeDataValues(Map values, int priority) {
        Map dataValues = new HashMap();
        for (Iterator i = values.keySet().iterator(); i.hasNext();) {
            Object key = i.next();
            Object value = values.get(key);
            dataValues.put(key, new DataValue(value, priority));
        }
        return dataValues;
    }

    /**
     * Gets the priority of the DataValue object
     *
     * @return the priority value
     */
    public int getPriority() {
        return priority;
    }

    /**
     * Gets the value of the DataValue object
     *
     * @return the value value
     */
    public Object getValue() {
        return value;
    }
}

