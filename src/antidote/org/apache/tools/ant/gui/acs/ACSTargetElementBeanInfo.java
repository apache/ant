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
import org.apache.tools.ant.gui.customizer.DynamicCustomizer;

import java.beans.*;

/**
 * BeanInfo for the ACSTargetElement class.
 * 
 * @version $Revision$ 
 * @author Simeon Fitch 
 */
public class ACSTargetElementBeanInfo extends BaseBeanInfo {
	/** 
	 * Default ctor.
	 * 
	 */
    public ACSTargetElementBeanInfo() {
    }

	/** 
	 * Get the type that this BeanInfo represents.
	 * 
	 * @return Type.
	 */
    public Class getType() {
        return ACSTargetElement.class;
    }

	/** 
	 * Get the customizer type.
	 * 
	 * @return Customizer type.
	 */
    public Class getCustomizerType() {
        return Customizer.class;
    }

	/** 
	 * Get the property descriptors.
	 * 
     * @return Property descriptors.
	 */
    public PropertyDescriptor[] getPropertyDescriptors() {
        PropertyDescriptor[] retval = null;

        try {
            retval = new PropertyDescriptor[] {
                new PropertyDescriptor(ACSTargetElement.NAME, 
                                       ACSTargetElement.class),
                new PropertyDescriptor(ACSTargetElement.DESCRIPTION,
                                       ACSTargetElement.class),
                new PropertyDescriptor(ACSTargetElement.DEPENDS,
                                       ACSTargetElement.class,
                                       "getClone", "copyDependsFromTarget"),
                new PropertyDescriptor(ACSTargetElement.IF,
                                       ACSTargetElement.class),
                new PropertyDescriptor(ACSTargetElement.UNLESS,
                                       ACSTargetElement.class),
                new PropertyDescriptor(ACSTargetElement.XML_STRING, 
                                       ACSTargetElement.class,
                                       "getXMLString", null)
            };

            // Set display names.
            retval[0].setDisplayName(getResources().getString(
                getClass(),ACSTargetElement.NAME));
            retval[1].setDisplayName(getResources().getString(
                getClass(),ACSTargetElement.DESCRIPTION));
            retval[2].setDisplayName(getResources().getString(
                getClass(),ACSTargetElement.DEPENDS));
            retval[3].setDisplayName(getResources().getString(
                getClass(),ACSTargetElement.IF));
            retval[4].setDisplayName(getResources().getString(
                getClass(),ACSTargetElement.UNLESS));
            retval[5].setDisplayName(getResources().getString(
                getClass(),ACSTargetElement.XML_STRING));

            setSortingOrder(retval);
        }
        catch(IntrospectionException ex) {
            ex.printStackTrace();
            throw new Error(ex.toString());
        }

        return retval;
    }


    /** Customizer for this bean info. */
    public static class Customizer extends DynamicCustomizer {
        static {
            PropertyEditorManager.registerEditor(
		org.apache.tools.ant.gui.acs.ACSTargetElement.class, org.apache.tools.ant.gui.modules.edit.DependentTargetPropertyEditor.class);
        }
        
        public Customizer() {
            super(ACSTargetElement.class);
        }
    }
}
