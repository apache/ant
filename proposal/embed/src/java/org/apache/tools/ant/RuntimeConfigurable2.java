/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2000-2002 The Apache Software Foundation.  All rights
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

package org.apache.tools.ant;

import org.apache.tools.ant.helper.*;
import java.util.Enumeration;
import java.util.Locale;
import java.util.Vector;
import java.util.Hashtable;
import org.xml.sax.AttributeList;
import org.xml.sax.Attributes;
import org.xml.sax.helpers.AttributeListImpl;
import org.xml.sax.helpers.AttributesImpl;

/**
 * Wrapper class that holds the attributes of an element, its children, and
 * any text within it. It then takes care of configuring that element at
 * runtime.
 *
 * This uses SAX2 and a more flexible substitution mechansim, based on
 * o.a.tomcat.util.IntrospectionUtil.
 *
 * @author <a href="mailto:stefan.bodewig@epost.de">Stefan Bodewig</a>
 * @author Costin Manolache
 */
public class RuntimeConfigurable2 extends RuntimeConfigurable {

    /** Name of the element to configure. elementName in UE */
    private String elementTag = null;  
    /** List of child element wrappers.  */
    private Vector children = new Vector();
    /** The element to configure. realThing in UE */
    private Object wrappedObject = null;
    /** XML attributes for the element. */
    private Attributes attributes;
    /** Text appearing within the element. */
    private StringBuffer characters = new StringBuffer();
    /** Indicates if the wrapped object has been configured */
    private boolean proxyConfigured = false;

    Project project;
    protected Location location = Location.UNKNOWN_LOCATION;
    
    /**
     * Sole constructor creating a wrapper for the specified object.
     *
     * @param proxy The element to configure. Must not be <code>null</code>.
     * @param elementTag The tag name generating this element.
     *                   Should not be <code>null</code>.
     */
    public RuntimeConfigurable2(Project project, Location location, Object proxy, String elementTag) {
        super( proxy, elementTag );
        wrappedObject = proxy;
        this.elementTag = elementTag;
        // This should never happen - all objects are lazy
        if( proxy instanceof Task )
            ((Task)proxy).setRuntimeConfigurableWrapper( this );
        proxyConfigured=false;
    }

    Project getProject() {
        return project;
    }

    Location getLocation() {
        return location;
    }    
    /**
     * Sets the element to configure. This is used when the real type of
     * an element isn't known at the time of wrapper creation.
     *
     * @param proxy The element to configure. Must not be <code>null</code>.
     */
    public void setProxy(Object proxy) {
        wrappedObject = proxy;
        proxyConfigured=false;
    }

    public Object getProxy() {
        return wrappedObject;
    }

    /**
     * Sets the attributes for the wrapped element.
     *
     * @param attributes List of attributes defined in the XML for this
     *                   element. May be <code>null</code>.
     * @deprecated It shouldn't be called by anyone except ProjectHelper
     */
    public void setAttributes(AttributeList attributes) {
        //    this.attributes = new AttributeListImpl(attributes);
    }

    public void setAttributes2(Attributes attributes) {
        this.attributes=new AttributesImpl( attributes );
    }
    
    /**
     * Returns the list of attributes for the wrapped element.
     * 
     * @return An AttributeList representing the attributes defined in the
     *         XML for this element. May be <code>null</code>.
     * @deprecated only for bkwd compatibility
     */
    public AttributeList getAttributes() {
        return sax1Attributes( attributes );
    }

    public Attributes getAttributes2() {
        return attributes;
    }

    public static AttributeList sax1Attributes( Attributes sax2Att ) {
        AttributeListImpl sax1Att=new AttributeListImpl();
        int length = sax2Att.getLength();
        if (length > 0) {
            for (int i = 0; i < length; i++) {
                // System.out.println("Attributes: " + sax2Att.getQName(i) + " " +
                //                    sax2Att.getValue(i));
                sax1Att.addAttribute( sax2Att.getQName(i), 
                                      sax2Att.getType(i),
                                      sax2Att.getValue(i));
            }
	}
        return sax1Att;
    }

    /**
     * Adds a child element to the wrapped element.
     * 
     * @param child The child element wrapper to add to this one.
     *              Must not be <code>null</code>.
     */
    public void addChild(RuntimeConfigurable child) {
        // addChild( UnknownElement ) in UE
        children.addElement(child);
    }

    /**
     * Returns the child wrapper at the specified position within the list.
     * 
     * @param index The index of the child to return.
     * 
     * @return The child wrapper at position <code>index</code> within the
     *         list.
     */
    public RuntimeConfigurable getChild(int index) {
        return (RuntimeConfigurable) children.elementAt(index);
    }

    /**
     * Adds characters from #PCDATA areas to the wrapped element.
     * 
     * @param data Text to add to the wrapped element. 
     *        Should not be <code>null</code>.
     */
    public void addText(String data) {
        characters.append(data);
    }

    /**
     * Adds characters from #PCDATA areas to the wrapped element.
     * 
     * @param buf A character array of the text within the element.
     *            Must not be <code>null</code>.
     * @param start The start element in the array.
     * @param count The number of characters to read from the array.
     * 
     */
    public void addText(char[] buf, int start, int count) {
        addText(new String(buf, start, count));
    }

    /**
     * Returns the tag name of the wrapped element.
     * 
     * @return The tag name of the wrapped element. This is unlikely
     *         to be <code>null</code>, but may be.
     */
    public String getElementTag() {
        // getTag in UE
        return elementTag;
    }

    /**
     * Configures the wrapped element and all its children.
     * The attributes and text for the wrapped element are configured,
     * and then each child is configured and added. Each time the
     * wrapper is configured, the attributes and text for it are
     * reset.
     * 
     * If the element has an <code>id</code> attribute, a reference
     * is added to the project as well.
     * 
     * @param p The project containing the wrapped element. 
     *          Must not be <code>null</code>.
     * 
     * @exception BuildException if the configuration fails, for instance due
     *            to invalid attributes or children, or text being added to
     *            an element which doesn't accept it.
     */
    public void maybeConfigure(Project p) throws BuildException {
        maybeConfigure(p, true);
    }

    /**
     * Configures the wrapped element.  The attributes and text for
     * the wrapped element are configured.  Each time the wrapper is
     * configured, the attributes and text for it are reset.
     *
     * If the element has an <code>id</code> attribute, a reference
     * is added to the project as well.
     *
     * @param p The project containing the wrapped element.
     *          Must not be <code>null</code>.
     *
     * @param configureChildren Whether to configure child elements as
     * well.  if true, child elements will be configured after the
     * wrapped element.
     *
     * @exception BuildException if the configuration fails, for instance due
     *            to invalid attributes or children, or text being added to
     *            an element which doesn't accept it.
     */
    public void maybeConfigure(Project p, boolean configureChildren) 
        throws BuildException {
        String id = null;

        if( proxyConfigured ) {
            return;
        }
        PropertyHelper2 ph=PropertyHelper2.getPropertyHelper(p);

        if (attributes != null) {
            ph.configure(wrappedObject, attributes, p);
            id = attributes.getValue("id");
            // No way - this will be used on future calls ( if the task is used
            // multiple times: attributes = null;
        }
        if (characters.length() != 0) {
            ProjectHelper.addText(p, wrappedObject, characters.toString());
        }
        Enumeration enum = children.elements();
        while (enum.hasMoreElements()) {
            RuntimeConfigurable2 child 
                = (RuntimeConfigurable2) enum.nextElement();
            if (child.wrappedObject instanceof Task) {
                Task childTask = (Task) child.wrappedObject;
                childTask.setRuntimeConfigurableWrapper(child);
            }

            if (configureChildren) {
                if (child.wrappedObject instanceof Task) {
                    Task childTask = (Task) child.wrappedObject;
                    childTask.maybeConfigure();
                } else {
                    child.maybeConfigure(p);
                }
                ProjectHelper.storeChild(p, wrappedObject, child.wrappedObject,
                                         child.getElementTag()
                                         .toLowerCase(Locale.US));
            }
        }

        if (id != null) {
            // p.addReference(id, wrappedObject);
            p.getReferences().put( id, wrappedObject );
            //System.out.println("XXX updating reference " + this + " " + id + " " + wrappedObject );
        }

        proxyConfigured = true; 
   }



}
