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

import com.sun.xml.tree.ElementNode;

/**
 * Element containing a property definition.
 * 
 * @version $Revision$ 
 * @author Simeon Fitch 
 */
public class ACSPropertyElement extends ACSDtdDefinedElement {
    /** The 'name' property name. */
    public static final String NAME = "name";
    /** The 'value' property name. */
    public static final String VALUE = "value";
    /** The file to load properties from. */
    public static final String FILE = "file";

	/** 
	 * Default ctor.
	 * 
	 */
    public ACSPropertyElement() {
    }

	/** 
	 * Get the display name of this.
	 * 
	 * @return Display name.
	 */
    public String getDisplayName() {
        String file = getFile();

        if(file == null || file.trim().length() == 0) {
            return getName();
        }
        else {
            return "file: " + file;
        }
    }

	/** 
	 * Get the property name.
	 * 
	 * @return Property name.
	 */
    public String getName() {
        return getAttribute(NAME);
    }

	/** 
	 * Set the property name.
	 * 
	 * @param name Property name.
	 */
    public void setName(String name) {
        String old = getName();
        setAttribute(NAME, name);
        firePropertyChange(NAME, old, name);
    }

	/** 
	 * Get the property value.
	 * 
	 * @return Property value.
	 */
    public String getValue() {
        return getAttribute(VALUE);
    }

	/** 
	 * Set the property value.
	 * 
	 * @param name Property value.
	 */
    public void setValue(String value) {
        String old = getValue();
        setAttribute(VALUE, value);
        firePropertyChange(VALUE, old, value);
    }

	/** 
	 * Get the external property file.
	 * 
	 * @return Property file.
	 */
    public String getFile() {
        return getAttribute(FILE);
    }

	/** 
	 * Set the external property file.
	 * 
	 * @param name Property file.
	 */
    public void setFile(String file) {
        String old = getFile();
        setAttribute(FILE, file);
        firePropertyChange(FILE, old, file);
    }
}
