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
package org.apache.tools.ant.gui.acs;

import org.apache.tools.ant.gui.core.ResourceManager;
import org.apache.tools.ant.gui.customizer.DynamicCustomizer;
import java.beans.*;
import javax.swing.ImageIcon;
import java.awt.Image;

/**
 * Abstract base class for ACS BeanInfo classes.
 * 
 * @version $Revision$ 
 * @author Simeon Fitch 
 */
abstract class BaseBeanInfo extends SimpleBeanInfo {
    /** Property name for specifiying a sorting order. */
    public static final String SORT_ORDER = DynamicCustomizer.SORT_ORDER;
    /** Resource provider for bean info. */
    private static ResourceManager _resources = new ResourceManager();

    /** Icon for this. */
    private Image _icon = null;

	/** 
	 * Default ctor.
	 * 
	 */
    protected BaseBeanInfo() {
        // Load the icon.
        ImageIcon img = _resources.getImageIcon(getClass(), "icon");
        if(img != null) {
            _icon = img.getImage();
        }
    }

	/** 
	 * Get the local resources.
	 * 
	 * @return Resources.
	 */
    ResourceManager getResources() {
        return _resources;
    }

	/** 
	 * Get the bean descriptor.
	 * 
	 * @return Bean descriptor.
	 */
    public BeanDescriptor getBeanDescriptor() {
        return new ACSBeanDescriptor(this);
    }

	/** 
	 * Get the icon for displaying this bean.
	 * 
	 * @param kind Kind of icon. XXX currently ignored
	 * @return Image for bean, or null if none.
	 */
    public Image getIcon(int kind) {
        // XXX kind is currently ignored.
        return _icon;
    }

	/** 
	 * Set the sorting order property of the given objects based
     * on the order that they appear in the array. 
	 * 
	 * @param vals FeatureDescriptors to set sorting order property for.
	 */
    protected void setSortingOrder(FeatureDescriptor[] vals) {
        for(int i = 0; i < vals.length; i++) {
            vals[i].setValue(SORT_ORDER, new Integer(i));
        }
    }

	/** 
	 * Get the type that this BeanInfo represents.
	 * 
	 * @return Type.
	 */
    public abstract Class getType();

	/** 
	 * Get the type of the customizer to use. 
	 * 
	 * @return Customizer to use.
	 */
    public abstract Class getCustomizerType();

    /**
     * Gets the beans <code>PropertyDescriptor</code>s.
     * 
     * @return An array of PropertyDescriptors describing the editable
     * properties supported by this bean.  May return null if the
     * information should be obtained by automatic analysis.
     * <p>
     * If a property is indexed, then its entry in the result array will
     * belong to the IndexedPropertyDescriptor subclass of PropertyDescriptor.
     * A client of getPropertyDescriptors can use "instanceof" to check
     * if a given PropertyDescriptor is an IndexedPropertyDescriptor.
     */
    public abstract PropertyDescriptor[] getPropertyDescriptors();


}
