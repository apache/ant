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
package org.apache.ant.antcore.execution;
import org.apache.ant.common.antlib.DeferredTask;

/**
 * An implementation of the Setter interface for configuring instances of
 * the DeferredTask interface
 *
 * @author Conor MacNeill
 * @created 11 February 2002
 */
public class DeferredSetter implements Setter {
    /**
     * Set an attribute value on an object
     *
     * @param obj the object on which the value is being set
     * @param attributeName the name of the attribute
     * @param value the string represenation of the attribute's value
     * @exception ExecutionException if the object does not support the
     *      attribute or the object has a problem setting the value
     */
    public void setAttribute(Object obj, String attributeName,
                             String value) throws ExecutionException {
        DeferredTask task = (DeferredTask) obj;
        task.setAttribute(attributeName, value);
    }


    /**
     * Get the type of the given nested element
     *
     * @param elementName the nested element whose type is desired
     * @return the class instance representing the type of the element adder
     */
    public Class getType(String elementName) {
        return null;
    }

    /**
     * Adds PCDATA to the element
     *
     * @param obj the instance whose content is being provided
     * @param text the required content
     * @exception ExecutionException if the object does not support
     *      contentor the object has a problem setting the content
     */
    public void addText(Object obj, String text)
         throws ExecutionException {
        DeferredTask task = (DeferredTask) obj;
        task.addText(text);
    }

    /**
     * Add an element to the given object
     *
     * @param obj The object to which the element is being added
     * @param elementName the name of the element
     * @param value the object to be added - the nested element
     * @exception ExecutionException if the object does not support content
     *      or the object has a problem setting the content
     */
    public void addElement(Object obj, String elementName, Object value)
         throws ExecutionException {
        DeferredTask task = (DeferredTask) obj;
        task.addElement(elementName, value);
    }

    /**
     * Create a nested element using the object's element factory method.
     *
     * @param container the object in which the nested element is required.
     * @param elementName the name of the nested element
     * @return the new instance of the nested element
     * @exception ExecutionException if the nested element cannot be
     *      created.
     */
    public Object createElement(Object container, String elementName)
         throws ExecutionException {
        throw new ExecutionException("Deferred Tasks do not support "
             + "creation of nested elements");
    }

    /**
     * Indicate if the class assocated with this reflector supports the
     * addition of text content.
     *
     * @return true if the class supports an addText method
     */
    public boolean supportsText() {
        return true;
    }

    /**
     * Indicate if the class assocated with this reflector supports the
     * given attribute
     *
     * @param attributeName the name of the attribute
     * @return true if the given attribute is supported
     */
    public boolean supportsAttribute(String attributeName) {
        return true;
    }

    /**
     * Determine if the class associated with this reflector supports a
     * particular nested element via a create factory method
     *
     * @param elementName the name of the element
     * @return true if the class supports creation of that element
     */
    public boolean supportsNestedCreator(String elementName) {
        return false;
    }

    /**
     * Determine if the class associated with this reflector supports a
     * particular nested element via an add method
     *
     * @param elementName the name of the element
     * @return true if the class supports addition of that element
     */
    public boolean supportsNestedAdder(String elementName) {
        return true;
    }

    /**
     * Determine if the class associated with this reflector supports a
     * particular nested element
     *
     * @param elementName the name of the element
     * @return true if the class supports the given type of nested element
     */
    public boolean supportsNestedElement(String elementName) {
        return true;
    }

}

