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
import org.apache.tools.ant.gui.command.NewElementCmd;
import org.apache.tools.ant.gui.util.Collections;
import org.w3c.dom.*;
import java.beans.*;
import java.util.*;

/**
 * Element defined by the DTD.
 *
 * @version $Revision$
 * @author Nick Davis<a href="mailto:nick_home_account@yahoo.com">nick_home_account@yahoo.com</a>
 */
public class ACSDtdDefinedElement extends ACSTreeNodeElement
implements ACSInfoProvider {
        
    /** Property name for the task type. */
    public static final String TASK_TYPE = "taskType";
    /** Property name for attributes. It's called "namedValues" so
     *  it doesn't collide with the Node.getAttributes() method. */
    public static final String NAMED_VALUES = "namedValues";
    /** The ANT DTD */
    static ACSDocumentType docType = new ACSDocumentType();
    /** Our menu string */
    public String[] menuString = null;
    /** The DTD element we represent */
    private ACSDocumentType.DtdElement _dtdElement = null;
    
	/** 
	 * Default ctor.
	 * 
	 */
    public ACSDtdDefinedElement() {
        // Load the DTD
        docType.init();
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
	 * Get the display name of this.
	 * 
	 * @return Display name.
	 */
    public String getDisplayName() {
        String name = getTagName();

        // Is there only one attribute?
        if (getAttributes().getLength() == 1) {
            Node onlyNode = getAttributes().item(0);

            // Display the only attribute
            name += ": " + onlyNode.getNodeValue();
        } else {
            
            // Display one of these attributes
            // if they are present.
            final String[] DISPLAY_ATTRIBUTES =
            {
                "name",
                "id",
                "property"
            };

            for(int i = 0; i < DISPLAY_ATTRIBUTES.length; i++) {
                Node testNode =
                    getAttributes().getNamedItem(DISPLAY_ATTRIBUTES[i]);
                if (testNode != null) {
                    name += ": " + testNode.getNodeValue();
                    break;
                }
            }
        }
        
        return name;
    }
    
        /** 
         * Set the task type.
         * 
         * @param type Type name.
         */
    public void setTaskType(String type) {
        setTag(type);
    }

	/** 
	 * Get the attributes (named value mappings). This method is not named
         * getAttributes() because there is already a method of that name in
         * the Node interface.
	 * 
	 * @return Name-value mappings.
	 */
    public ACSDtdDefinedAttributes getNamedValues() {
        ACSDtdDefinedAttributes retval =
            new ACSDtdDefinedAttributes(getDtdElement());

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
    public void setNamedValues(ACSDtdDefinedAttributes attributes) {
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

        ACSDtdDefinedAttributes old = (ACSDtdDefinedAttributes) getNamedValues();

        Enumeration enum = old.propertyNames();
        while(enum.hasMoreElements()) {
            String name = (String) enum.nextElement();
            removeAttribute(name);
        }

        enum = attributes.propertyNames();
        while(enum.hasMoreElements()) {
            String key = (String) enum.nextElement();
            setAttribute(key, attributes.getProperty(key));
        }

        firePropertyChange(NAMED_VALUES, old, attributes);
    }
    
    /**
     * Returns the menu items which may be used for this element.
     */
    public String[] getMenuString() {
        
        // If it already exists, use it.
        if (menuString != null) {
            return menuString;
        }

        // Find the DtdElement
        String name = getTagName();
        
        // Are we the project element?
        boolean isProject = false;
        if (name.equals("project")) {
            isProject = true;
        }
        
        ACSDocumentType.DtdElement e =
            docType.findElement(ACSDocumentType.CORE_ELEMENT, name);
        if (e == null) {
            e = docType.findElement(ACSDocumentType.OPTIONAL_ELEMENT, name);
        }

        if (e != null) {
            // Use the content model (all the possible
            // sub-elements) to create the menu.
            String[] temp = e.getContentModel();
            
            // Sort the items
            List list = Collections.fill(null, temp);
            java.util.Collections.sort(list);
            list.toArray(temp);
            
            int size = (temp.length > 5) ? 5 : temp.length;

            // The project doesn't need a delete menu
            if (isProject) {
                menuString = new String[size+1];
            } else {
                menuString = new String[size+2];
            }
            System.arraycopy(temp, 0, menuString, 0, size);
            
        } else {
            // This is for elements not in the DTD
            menuString = new String[2];
        }
        
        if (isProject) {
            menuString[menuString.length-1] = "newElement";
        } else {
            // Add the delete and generic create commands
            menuString[menuString.length-1] = "deleteElement";
            menuString[menuString.length-2] = "newElement";
        }
        
        return menuString;
    }
    
    /**
     * Returns the string to use if an action ID is not found.
     * In our case, the newElement command is used.
     */
    public String getDefaultActionID() {
        return "newElement";
    }
    
    /**
     * Returns a string array which contains this elements
     * possible children.
     * 
     * @param childType ACSDocumentType.CORE_ELEMENT or
     * ACSDocumentType.OPTIONAL_ELEMENT
     */
    public String[] getPossibleChildren(int childType) {
        String name = getTagName();
        
        ACSDocumentType.DtdElement e =
            docType.findElement(childType, name);
        if (e != null) {
            return e.getContentModel();
        }
        return null;
    }
    
    /**
     * Adds the attributes which are required by this node.
     */
    public void addRequiredAttributes() {
        ACSDocumentType.DtdElement e = getDtdElement();
        if (e == null) {
            return;
        }
        String[] attributes = e.getAttributes().getRequiredAttributes();
        if (attributes == null) {
            return;
        }
        for(int i = 0; i < attributes.length; i++) {
            if ( getAttributes().getNamedItem(attributes[i]) == null) {
                // XXX should set to default?
                setAttribute(attributes[i], "");
            }
        }
    }
    
    /**
     * Returns the DTD element we represent
     */
    private ACSDocumentType.DtdElement getDtdElement() {
        if (_dtdElement != null) {
            return _dtdElement;
        }

        String name = getNodeName();
        
        _dtdElement = docType.findElement(ACSDocumentType.CORE_ELEMENT, name);
        if (_dtdElement == null) {
            _dtdElement = docType.findElement(
                ACSDocumentType.OPTIONAL_ELEMENT, name);
        }
        
        return _dtdElement;
    }
}

