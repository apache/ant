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
package org.apache.ant.common.service;
import java.util.Map;

import org.apache.ant.common.util.AntException;

/**
 * Service interface for Data value manipulation operations provided by the
 * core.
 *
 * @author Conor MacNeill
 * @created 31 January 2002
 */
public interface DataService {
    /**
     * Get a data value
     *
     * @param valueName the name of the data value
     * @return the current object associated with the name or null if no
     *      value is currently associated with the name
     * @exception AntException if the value cannot be retrieved.
     */
    Object getDataValue(String valueName) throws AntException;

    /**
     * Indicate if a data value has been set
     *
     * @param name the name of the data value - may contain reference
     *      delimiters
     * @return true if the value exists
     * @exception AntException if the containing frame for the value
     *      does not exist
     */
    boolean isDataValueSet(String name) throws AntException;

    /**
     * Set a data value. If an existing data value exists, associated with
     * the given name, the value will not be changed
     *
     * @param valueName the name of the data value
     * @param value the value to be associated with the name
     * @exception AntException if the value cannot be set
     */
    void setDataValue(String valueName, Object value) throws AntException;

    /**
     * Set a data value which can be overwritten
     *
     * @param valueName the name of the data value
     * @param value the value to be associated with the name
     * @exception AntException if the value cannot be set
     */
    void setMutableDataValue(String valueName, Object value)
         throws AntException;

    /**
     * Replace ${} style constructions in the given value with the string
     * value of the corresponding data values in the frame
     *
     * @param value the string to be scanned for property references.
     * @return the string with all property references replaced
     * @exception AntException if any of the properties do not exist
     */
    String replacePropertyRefs(String value) throws AntException;

    /**
     * Replace ${} style constructions in the given value with the string
     * value of the objects in the given map. Any values which are not found
     * are left unchanged.
     *
     * @param value the string to be scanned for property references.
     * @param replacementValues the collection of replacement values
     * @return the string with all property references replaced
     * @exception AntException if any of the properties do not exist
     */
    String replacePropertyRefs(String value, Map replacementValues)
         throws AntException;

    /**
     * Get all the properties from the frame and any references frames. This
     * is an expensive operation since it must clone all of the property
     * stores in all frames
     *
     * @return a Map containing the frames properties indexed by their full
     *      name.
     */
    Map getAllProperties();
}

