/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package org.apache.tools.ant;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Iterator;

import org.apache.tools.ant.util.CollectionUtils;
import org.xml.sax.AttributeList;
import org.xml.sax.helpers.AttributeListImpl;

/**
 * Wrapper class that holds the attributes of an element, its children, and
 * any text within it. It then takes care of configuring that element at
 * runtime.
 *
 */
public class RuntimeConfigurable implements Serializable {

    /** Empty Hashtable. */
    private static final Hashtable EMPTY_HASHTABLE = new Hashtable(0);

    /** Name of the element to configure. */
    private String elementTag = null;

    /** List of child element wrappers. */
    private List/*<RuntimeConfigurable>*/ children = null;

    /** The element to configure. It is only used during
     * maybeConfigure.
     */
    private transient Object wrappedObject = null;

    /** the creator used to make the wrapped object */
    private transient IntrospectionHelper.Creator creator;

    /**
     * XML attributes for the element.
     * @deprecated since 1.6.x
     */
    private transient AttributeList attributes;

    /** Attribute names and values. While the XML spec doesn't require
     *  preserving the order ( AFAIK ), some ant tests do rely on the
     *  exact order. The following code is copied from AttributeImpl.
     *  We could also just use SAX2 Attributes and convert to SAX1 ( DOM
     *  attribute Nodes can also be stored in SAX2 Attributes )
     *  XXX under JDK 1.4 you can just use a LinkedHashMap for this purpose -jglick
     * The only exception to this order is the treatment of
     * refid. A number of datatypes check if refid is set
     * when other attributes are set. This check will not
     * work if the build script has the other attribute before
     * the "refid" attribute, so now (ANT 1.7) the refid
     * attribute will be processed first.
     */
    private List/*<String>*/ attributeNames = null;

    /** Map of attribute names to values */
    private Map/*<String,String>*/ attributeMap = null;

    /** Text appearing within the element. */
    private StringBuffer characters = null;

    /** Indicates if the wrapped object has been configured */
    private boolean proxyConfigured = false;

    /** the polymorphic type */
    private String polyType = null;

    /** the "id" of this Element if it has one */
    private String id = null;

    /**
     * Sole constructor creating a wrapper for the specified object.
     *
     * @param proxy The element to configure. Must not be <code>null</code>.
     * @param elementTag The tag name generating this element.
     */
    public RuntimeConfigurable(Object proxy, String elementTag) {
        setProxy(proxy);
        setElementTag(elementTag);
        // Most likely an UnknownElement
        if (proxy instanceof Task) {
            ((Task) proxy).setRuntimeConfigurableWrapper(this);
        }
    }

    /**
     * Sets the element to configure.
     *
     * @param proxy The element to configure. Must not be <code>null</code>.
     */
    public synchronized void setProxy(Object proxy) {
        wrappedObject = proxy;
        proxyConfigured = false;
    }

    /**
     * Sets the creator of the element to be configured
     * used to store the element in the parent.
     *
     * @param creator the creator object.
     */
    synchronized void setCreator(IntrospectionHelper.Creator creator) {
        this.creator = creator;
    }

    /**
     * Get the object for which this RuntimeConfigurable holds the configuration
     * information.
     *
     * @return the object whose configure is held by this instance.
     */
    public synchronized Object getProxy() {
        return wrappedObject;
    }

    /**
     * Returns the id for this element.
     * @return the id.
     */
    public synchronized String getId() {
        return id;
    }

    /**
     * Get the polymorphic type for this element.
     * @return the ant component type name, null if not set.
     */
    public synchronized String getPolyType() {
        return polyType;
    }

    /**
     * Set the polymorphic type for this element.
     * @param polyType the ant component type name, null if not set.
     */
    public synchronized void setPolyType(String polyType) {
        this.polyType = polyType;
    }

    /**
     * Sets the attributes for the wrapped element.
     *
     * @deprecated since 1.6.x.
     * @param attributes List of attributes defined in the XML for this
     *                   element. May be <code>null</code>.
     */
    public synchronized void setAttributes(AttributeList attributes) {
        this.attributes = new AttributeListImpl(attributes);
        for (int i = 0; i < attributes.getLength(); i++) {
            setAttribute(attributes.getName(i), attributes.getValue(i));
        }
    }

    /**
     * Set an attribute to a given value.
     *
     * @param name the name of the attribute.
     * @param value the attribute's value.
     */
    public synchronized void setAttribute(String name, String value) {
        if (name.equalsIgnoreCase(ProjectHelper.ANT_TYPE)) {
            this.polyType = value;
        } else {
            if (attributeNames == null) {
                attributeNames = new ArrayList();
                attributeMap = new HashMap();
            }
            if (name.toLowerCase(Locale.US).equals("refid")) {
                attributeNames.add(0, name);
            } else {
                attributeNames.add(name);
            }
            attributeMap.put(name, value);
            if (name.equals("id")) {
                this.id = value;
            }
        }
    }

    /**
     * Delete an attribute.  Not for the faint of heart.
     * @param name the name of the attribute to be removed.
     */
    public synchronized void removeAttribute(String name) {
        attributeNames.remove(name);
        attributeMap.remove(name);
    }

    /**
     * Return the attribute map.
     *
     * @return Attribute name to attribute value map.
     * @since Ant 1.6
     */
    public synchronized Hashtable getAttributeMap() {
        return (attributeMap == null)
            ? EMPTY_HASHTABLE : new Hashtable(attributeMap);
    }

    /**
     * Returns the list of attributes for the wrapped element.
     *
     * @deprecated Deprecated since Ant 1.6 in favor of {@link #getAttributeMap}.
     * @return An AttributeList representing the attributes defined in the
     *         XML for this element. May be <code>null</code>.
     */
    public synchronized AttributeList getAttributes() {
        return attributes;
    }

    /**
     * Adds a child element to the wrapped element.
     *
     * @param child The child element wrapper to add to this one.
     *              Must not be <code>null</code>.
     */
    public synchronized void addChild(RuntimeConfigurable child) {
        children = (children == null) ? new ArrayList() : children;
        children.add(child);
    }

    /**
     * Returns the child wrapper at the specified position within the list.
     *
     * @param index The index of the child to return.
     *
     * @return The child wrapper at position <code>index</code> within the
     *         list.
     */
    synchronized RuntimeConfigurable getChild(int index) {
        return (RuntimeConfigurable) children.get(index);
    }

    /**
     * Returns an enumeration of all child wrappers.
     * @return an enumeration of the child wrappers.
     * @since Ant 1.6
     */
    public synchronized Enumeration getChildren() {
        return (children == null) ? new CollectionUtils.EmptyEnumeration()
            : Collections.enumeration(children);
    }

    /**
     * Adds characters from #PCDATA areas to the wrapped element.
     *
     * @param data Text to add to the wrapped element.
     *        Should not be <code>null</code>.
     */
    public synchronized void addText(String data) {
        if (data.length() == 0) {
            return;
        }
        characters = (characters == null)
            ? new StringBuffer(data) : characters.append(data);
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
    public synchronized void addText(char[] buf, int start, int count) {
        if (count == 0) {
            return;
        }
        characters = ((characters == null)
            ? new StringBuffer(count) : characters).append(buf, start, count);
    }

    /**
     * Get the text content of this element. Various text chunks are
     * concatenated, there is no way ( currently ) of keeping track of
     * multiple fragments.
     *
     * @return the text content of this element.
     * @since Ant 1.6
     */
    public synchronized StringBuffer getText() {
        return (characters == null) ? new StringBuffer(0) : characters;
    }

    /**
     * Set the element tag.
     * @param elementTag The tag name generating this element.
     */
    public synchronized void setElementTag(String elementTag) {
        this.elementTag = elementTag;
    }

    /**
     * Returns the tag name of the wrapped element.
     *
     * @return The tag name of the wrapped element. This is unlikely
     *         to be <code>null</code>, but may be.
     */
    public synchronized String getElementTag() {
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
     * @param configureChildren ignored.

     *
     * @exception BuildException if the configuration fails, for instance due
     *            to invalid attributes , or text being added to
     *            an element which doesn't accept it.
     */
    public synchronized void maybeConfigure(Project p, boolean configureChildren)
        throws BuildException {

        if (proxyConfigured) {
            return;
        }

        // Configure the object
        Object target = (wrappedObject instanceof TypeAdapter)
            ? ((TypeAdapter) wrappedObject).getProxy() : wrappedObject;

        IntrospectionHelper ih =
            IntrospectionHelper.getHelper(p, target.getClass());

        if (attributeNames != null) {
            for (int i = 0; i < attributeNames.size(); i++) {
                String name = (String) attributeNames.get(i);
                String value = (String) attributeMap.get(name);

                // reflect these into the target
                value = p.replaceProperties(value);
                try {
                    ih.setAttribute(p, target, name, value);
                } catch (UnsupportedAttributeException be) {
                    // id attribute must be set externally
                    if (name.equals("id")) {
                        // Do nothing
                    } else  if (getElementTag() == null) {
                        throw be;
                    } else {
                        throw new BuildException(
                            getElementTag() +  " doesn't support the \""
                            + be.getAttribute() + "\" attribute", be);
                    }
                } catch (BuildException be) {
                    if (name.equals("id")) {
                        // Assume that this is an not supported attribute type
                        // thrown for example by a dymanic attribute task
                        // Do nothing
                    } else {
                        throw be;
                    }
                }
            }
        }

        if (characters != null) {
            ProjectHelper.addText(p, wrappedObject, characters.substring(0));
        }

        if (id != null) {
            p.addReference(id, wrappedObject);
        }
        proxyConfigured = true;
    }

    /**
     * Reconfigure the element, even if it has already been configured.
     *
     * @param p the project instance for this configuration.
     */
    public void reconfigure(Project p) {
        proxyConfigured = false;
        maybeConfigure(p);
    }


    /**
     * Apply presets, attributes and text are set if not currently set.
     * Nested elements are prepended.
     *
     * @param r a <code>RuntimeConfigurable</code> value.
     */
    public void applyPreSet(RuntimeConfigurable r) {
        // Attributes
        if (r.attributeMap != null) {
            for (Iterator i = r.attributeMap.keySet().iterator(); i.hasNext();) {
                String name = (String) i.next();
                if (attributeMap == null || attributeMap.get(name) == null) {
                    setAttribute(name, (String) r.attributeMap.get(name));
                }
            }
        }
        // poly type

        polyType = (polyType == null) ? r.polyType : polyType;

        // Children (this is a shadow of UnknownElement#children)
        if (r.children != null) {
            List newChildren = new ArrayList();
            newChildren.addAll(r.children);
            if (children != null) {
                newChildren.addAll(children);
            }
            children = newChildren;
        }

        // Text
        if (r.characters != null) {
            if (characters == null
                || characters.toString().trim().length() == 0) {
                characters = new StringBuffer(r.characters.toString());
            }
        }
    }
}
