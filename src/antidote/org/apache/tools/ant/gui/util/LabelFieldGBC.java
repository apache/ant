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
 * 4. The names "The Jakarta Project", "Tomcat", and "Apache Software
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
package org.apache.tools.ant.gui.util;

import java.awt.GridBagConstraints;
import java.awt.Insets;
import java.awt.Container;
import javax.swing.JLabel;

/**
 * Convenience specialization of the GridBagConstraints for laying
 * out label:field pairs.
 * 
 * @version $Revision$ 
 * @author Simeon Fitch 
 */
public class LabelFieldGBC extends GridBagConstraints {

	/** 
	 * Default ctor. Sets up the default settings.
	 * 
	 */
	public LabelFieldGBC() {
		// Add small abount of padding.
		insets = new Insets(1,3,1,3);
		// All vertical layout is relative
		gridy = RELATIVE;
		// Grid dimensions are equal (one label field pair per row).
		gridheight = 1;
		gridwidth = 1;
	}

	/** 
	 * Set up constraint values for placing a label before a field.
	 * 
	 * @return Constraints for a label.
	 */
	public LabelFieldGBC forLabel() {
		// Labels should only take up as much room as needed. 
		fill = NONE;
		// Set location to left side.
		gridx = 0;
		// Move it over to be as close to field as possible.
		anchor = NORTHEAST;
		// Don't take up any extra.
		weightx = 0.0;
		return this;
	}

	/** 
	 * Provide the same setup as forLabel(), but allow it to expand vertically
	 * to use up any extra space there may be.
	 * 
	 * @return Constraints for label that sucks up vertical space.
	 */
	public LabelFieldGBC forLastLabel() {
		forLabel();
		fill = VERTICAL;
		weighty = 1.0;
		return this;
	}

	/** 
	 * Set up constraint values for placing a field after a label.
	 * 
	 * @return Constraints for a field.
	 */
	public LabelFieldGBC forField() {
		// The field should take up as much space as is necessary.
		fill = HORIZONTAL;
		// Set the location to the right side.
		gridx = 1;
		// Center the field in the space available (a noop in this case).
		anchor = CENTER;
		// Take up any extra space.
		weightx = 1.0;
		return this;
	}

	/** 
	 * Provide the same setup as forField(), but allow it to expand vertically
	 *
	 * 
	 * @return Constraintes for field that sucks up vertical space.
	 */
	public LabelFieldGBC forLastField() {
		forField();
		fill = BOTH;
		weighty = 1.0;
		return this;
	}

}
