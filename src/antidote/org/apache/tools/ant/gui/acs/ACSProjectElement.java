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
import java.net.URL;

/**
 * Class representing a project element in the build file.
 * 
 * @version $Revision$ 
 * @author Simeon Fitch 
 */
public class ACSProjectElement extends ACSNamedElement {
    /** The 'default' property name. */
    public static final String DEFAULT = "default";
    /** The 'basdir' property name. */
    public static final String BASEDIR = "basedir";
    /** Property name of the persistence location. */
    public static final String LOCATION = "location";
    /** The location where this project is persisted. */
    private URL _location = null;

	/** 
	 * Default ctor.
	 * 
	 */
    public ACSProjectElement() {
    }

	/** 
	 * Get the type that this BeanInfo represents.
	 * 
	 * @return Type.
	 */
    public Class getType() {
        return ACSProjectElement.class;
    }

	/** 
	 * Get the name of the default target.
	 * 
	 * @return Default target name.
	 */
    public String getDefault() {
        return getAttribute(DEFAULT);
    }

	/** 
	 * Set the name of the default target.
	 * 
	 * @param def Name of the default target.
	 */
    public void setDefault(String def) {
        String old = getDefault();
        setAttribute(DEFAULT, def);
        firePropertyChange(DEFAULT, old, def);
    }

	/** 
	 * Get the specified base directory for the build.
	 * 
	 * @return Base directory
	 */
    public String getBasedir() {
        return getAttribute(BASEDIR);
    }

	/** 
	 * Set the base directory for builds.
	 * 
	 * @param baseDir Build base directory.
	 */
    public void setBasedir(String baseDir) {
        String old = getBasedir();
        setAttribute(BASEDIR, baseDir);
        firePropertyChange(BASEDIR, old, baseDir);
    }

    /** 
     * Get the location where this project is persisted.
     * 
     * @return Saved location, or null if not persisted.
     */
    public URL getLocation() {
        return _location;
    }

    /** 
     * Set the loction where the project is persisted.
     * 
     * @param location Location of project.
     */
    public void setLocation(URL location) {
        URL old = _location;
        _location = location;
        firePropertyChange(LOCATION, old, _location);
    }


}
