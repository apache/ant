/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 1999, 2000 The Apache Software Foundation.  All rights
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
package org.apache.tools.ant.gui.acs;

import org.w3c.dom.Node;
import org.w3c.dom.NamedNodeMap;
import java.util.*;

/**
 * Element containing a property definition.
 * 
 * @version $Revision$ 
 * @author Simeon Fitch 
 */
public class ACSTaskElement extends ACSTreeNodeElement {
    /** Property name for the task type. */
    public static final String TASK_TYPE = "taskType";
    /** Property name for attributes. It's called "namedValues" so
     *  it doesn't collide with the Node.getAttributes() method. */
    public static final String NAMED_VALUES = "namedValues";

	/** 
	 * Default ctor.
	 * 
	 */
    public ACSTaskElement() {
    }

	/** 
	 * Get the task type.
	 * 
	 * @return Task type.
	 */
    public String getTaskType() {
        return getTagName();
    }


	/** 
	 * Get the attributes (named value mappings). This method is not named
     * getAttributes() because there is already a method of that name in
     * the Node interface.
	 * 
	 * @return Name-value mappings.
	 */
    public Properties getNamedValues() {
        Properties retval = new Properties();

        NamedNodeMap attribs = getAttributes();
        for(int i = 0, len = attribs.getLength(); i < len; i++) {
            Node n = attribs.item(i);
            retval.setProperty(n.getNodeName(), n.getNodeValue());
        }
        return retval;
    }


	/** 
	 * Set the attributes. This method sets the Node attirbutes using 
     * the given Map containing name-value pairs.
	 * 
	 * @param attributes New attribute set.
	 */
    public void setNamedValues(Properties props) {
        // XXX this code really sucks. It is really annoying that the 
        // DOM interfaces don't have a general "setAttributes()" or
        // "removeAllAttributes()" method, but instead make you 
        // remove each attribute individually, or require you to figure
        // out what the differences are between the two. 

        // Although this is very inefficient, I'm taking the conceptually
        // simplistic approach to this and brute force removing the existing 
        // set and replacing it with a brand new set. If this becomes a 
        // performance concern (which I doubt it will) it can be optimized 
        // later.

        Properties old = getNamedValues();

        Enumeration enum = old.propertyNames();
        while(enum.hasMoreElements()) {
            String name = (String) enum.nextElement();
            removeAttribute(name);
        }
        
        enum = props.propertyNames();
        while(enum.hasMoreElements()) {
            String key = (String) enum.nextElement();
            setAttribute(key, props.getProperty(key));
        }

        firePropertyChange(NAMED_VALUES, old, props);
    }

}
