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

    /** Name of the element to configure. */
    private String elementTag = null;
    /** List of child element wrappers. */
    private Vector children = new Vector();
    /** The element to configure. */
    private Object wrappedObject = null;
    /** XML attributes for the element. */
    private Attributes attributes;
    /** Text appearing within the element. */
    private StringBuffer characters = new StringBuffer();

    /**
     * Sole constructor creating a wrapper for the specified object.
     * 
     * @param proxy The element to configure. Must not be <code>null</code>.
     * @param elementTag The tag name generating this element.
     *                   Should not be <code>null</code>.
     */
    public RuntimeConfigurable2(Object proxy, String elementTag) {
        super( proxy, elementTag );
        wrappedObject = proxy;
        this.elementTag = elementTag;
        if( proxy instanceof Task )
            ((Task)proxy).setRuntimeConfigurableWrapper( this );
    }

    /**
     * Sets the element to configure. This is used when the real type of 
     * an element isn't known at the time of wrapper creation.
     * 
     * @param proxy The element to configure. Must not be <code>null</code>.
     */
    void setProxy(Object proxy) {
        wrappedObject = proxy;
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
    RuntimeConfigurable getChild(int index) {
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
        String id = null;

        if (attributes != null) {
            configure(wrappedObject, attributes, p);
            id = attributes.getValue("id");
            attributes = null;
        }
        
        if (characters.length() != 0) {
            ProjectHelper.addText(p, wrappedObject, characters.toString());
            characters.setLength(0);
        }
        Enumeration enum = children.elements();
        while (enum.hasMoreElements()) {
            RuntimeConfigurable2 child 
                = (RuntimeConfigurable2) enum.nextElement();
            if (child.wrappedObject instanceof Task) {
                Task childTask = (Task) child.wrappedObject;
                childTask.setRuntimeConfigurableWrapper(child);
                childTask.maybeConfigure();
            } else {
                child.maybeConfigure(p);
            }
            ProjectHelper.storeChild(p, wrappedObject, child.wrappedObject, 
                child.getElementTag().toLowerCase(Locale.US));
        }

        if (id != null) {
            p.addReference(id, wrappedObject);
        }
    }


    public static void configure( Object target, Attributes attrs, Project project )
        throws BuildException
    {
        if (target instanceof TaskAdapter) {
            target = ((TaskAdapter) target).getProxy();
        }
        
        IntrospectionHelper ih = 
            IntrospectionHelper.getHelper(target.getClass());
        
        project.addBuildListener(ih);
        
        for (int i = 0; i < attrs.getLength(); i++) {
            // reflect these into the target
            String value = RuntimeConfigurable2.replaceProperties(project, attrs.getValue(i));
            
            try {
                ih.setAttribute(project, target, 
                                attrs.getQName(i).toLowerCase(Locale.US), value);
            } catch (BuildException be) {
                // id attribute must be set externally
                if (!attrs.getQName(i).equals("id")) {
                    throw be;
                }
            }
        }
    }

    public static String replaceProperties( Project project ,String value ) {
        if (value == null) {
            return null;
        }

        Vector fragments = new Vector();
        Vector propertyRefs = new Vector();

        ProjectHelper.parsePropertyString(value, fragments, propertyRefs);

        StringBuffer sb = new StringBuffer();
        Enumeration i = fragments.elements();
        Enumeration j = propertyRefs.elements();
        while (i.hasMoreElements()) {
            
            String fragment = (String) i.nextElement();
            if (fragment == null) {
                String propertyName = (String) j.nextElement();
                Object repl=project.getProperty( propertyName );

                if( repl==null) {
                    // Try a dynamic substitiution using ref
                    repl=processReference( project, propertyName );
                }
                
                if (repl==null ) {
                    project.log("Property ${" + propertyName 
                        + "} has not been set", Project.MSG_VERBOSE);
                    fragment="${" + propertyName + "}"; 
                } else {
                    fragment = (String) repl;
                }
            }
            sb.append(fragment);
        }                        
        
        return sb.toString();

    }

    static Hashtable propertySources=new Hashtable();

    public static interface ProjectPropertySource {

	public String getProperty( Project project, String key );
	
    }
    
    public static void addPropertySource( String ns, ProjectPropertySource src ) {
        propertySources.put( ns, src );
    }

    
    /** Use the reference table to generate values for ${} substitution.
     *  To preserve backward compat ( as much as possible ) we'll only process
     *  ids with a 'namespace-like' syntax.
     *
     *  Currently we support:
     *    dom:idName:/xpath/like/syntax  - the referenced node must be a DOM, we'll use
     *                      XPath to extract a node. ( a simplified syntax is handled
     *                      directly, XXX used for 'real' xpaths ).
     *    toString:idName - we use toString on the referenced object
     *    bean:idName.propertyName - we get the idName and call the getter for the property. 
     */
    static String processReference( Project project, String name ) {
        if( name.startsWith( "toString:" )) {
            name=name.substring( "toString:".length());
            Object v=project.getReference( name );
            if( v==null ) return null;
            return v.toString();
        }

        int idx=name.indexOf(":");
        if( idx<0 ) return null;

        String ns=name.substring( 0, idx );
        String path=name.substring( idx );

        ProjectPropertySource ps=(ProjectPropertySource)propertySources.get( ns );
        if( ps == null )
            return null;

        return ps.getProperty( project, path );
    }
}
