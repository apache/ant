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
import java.util.StringTokenizer;

/**
 * Class representing a build target.
 * 
 * @version $Revision$ 
 * @author Simeon Fitch 
 */
public class ACSTargetElement extends ACSNamedElement {

    /** Dependency property name. */
    public static final String DEPENDS = "depends";
    /** 'if' clause property name. */
    public static final String IF = "if";
    /** 'unless' clause property name. */
    public static final String UNLESS = "unless";

	/** 
	 * Default ctor.
	 * 
	 */
    public ACSTargetElement() {

    }

	/** 
	 * Get the set of dependency names. 
	 * 
	 * @return Dependency names.
	 */
    public String[] getDepends() {
        String depends = getAttribute(DEPENDS);
        StringTokenizer tok = new StringTokenizer(depends,",");
        String[] retval = new String[tok.countTokens()];
        for(int i = 0; i < retval.length; i++) {
            retval[i] = tok.nextToken().trim();
        }
        
        return retval;
    }

	/** 
	 * Set the list of dependency names.
	 * 
	 * @param depends Dependency names.
	 */
    public void setDepends(String[] depends) {
        String old = getAttribute(DEPENDS);
        StringBuffer buf = new StringBuffer();
        for(int i = 0; depends != null && i < depends.length; i++) {
            buf.append(depends[i]);
            if(i < depends.length - 1) {
                buf.append(", ");
            }
        }
        setAttribute(DEPENDS, buf.toString());
        firePropertyChange(DEPENDS, old, buf.toString());
    }

	/** 
	 * Get the 'if' clause.
	 * 
	 * @return 'if' clause.
	 */
    public String getIf() {
        return getAttribute(IF);
    }
    
	/** 
	 * Set the 'if' clause.
	 * 
	 * @param val 'if' clause value.
	 */
    public void setIf(String val) {
        String old = getIf();
        setAttribute(IF, val);
        firePropertyChange(IF, old, val);
    }

	/** 
	 * Get the 'unless' clause.
	 * 
	 * @return 'unless' clause.
	 */
    public String getUnless() {
        return getAttribute(UNLESS);
    }

	/** 
	 * Set the 'unless' clause.
	 * 
	 * @param val 'unless' clase value.
	 */
    public void setUnless(String val) {
        String old = getUnless();
        setAttribute(UNLESS, val);
        firePropertyChange(UNLESS, old, val);
    }
}
