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
package org.apache.ant.common.model;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * The AspectValueCollection holds aspect values for a range of aspects.
 * Values can be retrieved for a particular aspect attribute or all attributes
 * of a given aspect.
 * 
 * @author Conor MacNeill
 * @created 11 January 2002
 */
public class AspectValueCollection {
    /** The aspects defined for this element. */
    private Map aspectMaps = new HashMap();

    /**
     * Set the aspect attribute values.
     *
     * The attributes are sorted into their various aspects 
     *
     * @param attributes a Map of aspect attributes values. The keys are the
     *        aspect
     */
    public void addAttributes(Map attributes) {
        for (Iterator i = attributes.keySet().iterator(); i.hasNext();) {
            String attributeName = (String) i.next();
            int separator = attributeName.indexOf(":");
            if (separator != -1) {
                String aspectName = attributeName.substring(0, separator);
                String name = attributeName.substring(separator + 1);
                if (aspectName.length() != 0 && name.length() != 0) {
                    Map prefixMap = (Map) aspectMaps.get(aspectName);
                    if (prefixMap == null) {
                        prefixMap = new HashMap();
                        aspectMaps.put(aspectName, prefixMap);
                    }
                    prefixMap.put(name, attributes.get(attributeName));
                }
            }
        }
    }

    /**
     * Get an iterator on the aspects which have been given values on this
     * element
     *
     * @return an iterator of Strings , being the aspects which have been
     *      given values on this element.
     */
    public Iterator getNames() {
        return aspectMaps.keySet().iterator();
    }

    /**
     * Get the set of attribute values related to the given aspect
     *
     * @param aspectName the aspect name
     * @return a map of the attribute values for the given aspect.
     */
    public Map getAttributes(String aspectName) {
        return (Map) aspectMaps.get(aspectName);
    }

    /**
     * Get the value of a single aspect attribute
     *
     * @param aspectName the prefix which identifies the aspectr
     * @param keyName the attribute name
     * @return the aspect value
     */
    public String getAttributeValue(String aspectName, String keyName) {
        Map aspectAttributes = getAttributes(aspectName);
        if (aspectAttributes == null) {
            return null;
        }
        return (String) aspectAttributes.get(keyName);
    }
}

