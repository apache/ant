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

import java.util.List;

/**
 * A set of utilities for handling Ant properties and values
 *
 * @author Conor MacNeill
 * @created 31 January 2002
 */
public class PropertyUtils {
    /**
     * This method will parse a string containing ${value} style property
     * values into two lists. The first list is a collection of text
     * fragments, while the other is a set of string property names. Null
     * entries in the first list indicate a property reference from the
     * second list.
     *
     * @param value the string to be parsed
     * @param fragments the fragments parsed out of the string
     * @param propertyRefs the property refs to be replaced
     * @exception GeneralException if there is a problem parsing out the
     *      values
     */
    public static void parsePropertyString(String value, List fragments,
                                           List propertyRefs)
         throws GeneralException {
        int prev = 0;
        int pos;
        while ((pos = value.indexOf("$", prev)) >= 0) {
            if (pos > 0) {
                fragments.add(value.substring(prev, pos));
            }

            if (pos == (value.length() - 1)) {
                fragments.add("$");
                prev = pos + 1;
            } else if (value.charAt(pos + 1) != '{') {
                fragments.add(value.substring(pos + 1, pos + 2));
                prev = pos + 2;
            } else {
                int endName = value.indexOf('}', pos);
                if (endName < 0) {
                    throw new GeneralException("Syntax error in property: "
                         + value);
                }
                String propertyName = value.substring(pos + 2, endName);
                fragments.add(null);
                propertyRefs.add(propertyName);
                prev = endName + 1;
            }
        }

        if (prev < value.length()) {
            fragments.add(value.substring(prev));
        }
    }

    /**
     * returns the boolean equivalent of a string, which is considered true
     * if either "on", "true", or "yes" is found, ignoring case.
     *
     * @param s the string value to be interpreted at a boolean
     * @return the value of s as a boolean
     */
    public static boolean toBoolean(String s) {
        return (s != null && (s.equalsIgnoreCase("on")
             || s.equalsIgnoreCase("true")
             || s.equalsIgnoreCase("yes")));
    }

}

