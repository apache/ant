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

import java.text.ParsePosition;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.Vector;
import java.util.Enumeration;

/* ISSUES:
 - ns param. It could be used to provide "namespaces" for properties, which
 may be more flexible.
 - Object value. In ant1.5 String is used for Properties - but it would be nice
 to support generic Objects (the property remains immutable - you can't change
 the associated object). This will also allow JSP-EL style setting using the
 Object if an attribute contains only the property (name="${property}" could
 avoid Object->String->Object conversion)
 - Currently we "chain" only for get and set property (probably most users
 will only need that - if they need more they can replace the top helper).
 Need to discuss this and find if we need more.
 */

/* update for impending Ant 1.8:

   - I can't see any reason for ns and would like to deprecate it.
   - Replacing chaining with delegates for certain behavioral aspects.
   - Object value seems valuable as outlined.

 */

/** NOT FINAL. API MAY CHANGE
 *
 * Deals with properties - substitution, dynamic properties, etc.
 *
 * This is the same code as in Ant1.5. The main addition is the ability
 * to chain multiple PropertyHelpers and to replace the default.
 *
 * @since Ant 1.6
 */
public class PropertyHelper implements Cloneable {

    /**
     * Marker interface for a PropertyHelper delegate.
     * @since Ant 1.8
     */
    public interface Delegate {
    }

    /**
     * Describes an entity capable of evaluating a property name for value.
     * @since Ant 1.8
     */
    public interface PropertyEvaluator extends Delegate {
        /**
         * Evaluate a property.
         * @param property the property's String "identifier".
         * @param propertyHelper the invoking PropertyHelper.
         * @return Object value.
         */
        Object evaluate(String property, PropertyHelper propertyHelper);
    }

    /**
     * Describes an entity capable of expanding properties embedded in a string.
     * @since Ant 1.8
     */
    public interface PropertyExpander extends Delegate {
        /**
         * Parse the next property name.
         * @param s the String to parse.
         * @param pos the ParsePosition in use.
         * @param propertyHelper the invoking PropertyHelper.
         * @return parsed String if any, else <code>null</code>.
         */
        String parsePropertyName(String s, ParsePosition pos, PropertyHelper propertyHelper);
    }

    private static final PropertyEvaluator TO_STRING = new PropertyEvaluator() {
        private String prefix = "toString:";
        public Object evaluate(String property, PropertyHelper propertyHelper) {
            Object o = null;
            if (property.startsWith(prefix) && propertyHelper.getProject() != null) {
                o = propertyHelper.getProject().getReference(property.substring(prefix.length()));
            }
            return o == null ? null : o.toString();
        }
    };

    private static final PropertyExpander DEFAULT_EXPANDER = new PropertyExpander() {
        public String parsePropertyName(String s, ParsePosition pos, PropertyHelper propertyHelper) {
            int index = pos.getIndex();
            if (s.indexOf("${", index) == index) {
                int end = s.indexOf('}', index);
                if (end < 0) {
                    throw new BuildException("Syntax error in property: " + s);
                }
                int start = index + 2;
                pos.setIndex(end + 1);
                return s.substring(start, end);
            }
            return null;
        }
    };

    /** dummy */
    private static final PropertyExpander SKIP_$$ = new PropertyExpander() {
        /**
         * {@inheritDoc}
         * @see org.apache.tools.ant.PropertyHelper.PropertyExpander#parsePropertyName(java.lang.String, java.text.ParsePosition, org.apache.tools.ant.PropertyHelper)
         */
        public String parsePropertyName(String s, ParsePosition pos, PropertyHelper propertyHelper) {
            int index = pos.getIndex();
            if (s.indexOf("$$", index) == index) {
                pos.setIndex(++index);
            }
            return null;
        }
    };

    private Project project;
    private PropertyHelper next;
    private Hashtable delegates = new Hashtable();

    /** Project properties map (usually String to String). */
    private Hashtable properties = new Hashtable();

    /**
     * Map of "user" properties (as created in the Ant task, for example).
     * Note that these key/value pairs are also always put into the
     * project properties, so only the project properties need to be queried.
     */
    private Hashtable userProperties = new Hashtable();

    /**
     * Map of inherited "user" properties - that are those "user"
     * properties that have been created by tasks and not been set
     * from the command line or a GUI tool.
     */
    private Hashtable inheritedProperties = new Hashtable();

    /**
     * Default constructor.
     */
    protected PropertyHelper() {
        add(TO_STRING);
        add(SKIP_$$);
        add(DEFAULT_EXPANDER);
    }

    //override facility for subclasses to put custom hashtables in

    // --------------------  Hook management  --------------------

    /**
     * Set the project for which this helper is performing property resolution.
     *
     * @param p the project instance.
     */
    public void setProject(Project p) {
        this.project = p;
    }

    /**
     * Get this PropertyHelper's Project.
     * @return Project
     */
    public Project getProject() {
        return project;
    }

    /**
     *  There are 2 ways to hook into property handling:
     *  - you can replace the main PropertyHelper. The replacement is required
     * to support the same semantics (of course :-)
     *
     *  - you can chain a property helper capable of storing some properties.
     *  Again, you are required to respect the immutability semantics (at
     *  least for non-dynamic properties)
     *
     * @param next the next property helper in the chain.
     * @deprecated
     */
    public void setNext(PropertyHelper next) {
        this.next = next;
    }

    /**
     * Get the next property helper in the chain.
     *
     * @return the next property helper.
     * @deprecated
     */
    public PropertyHelper getNext() {
        return next;
    }

    /**
     * Factory method to create a property processor.
     * Users can provide their own or replace it using "ant.PropertyHelper"
     * reference. User tasks can also add themselves to the chain, and provide
     * dynamic properties.
     *
     * @param project the project for which the property helper is required.
     *
     * @return the project's property helper.
     */
    public static synchronized PropertyHelper getPropertyHelper(Project project) {
        PropertyHelper helper
                = (PropertyHelper) project.getReference(MagicNames.REFID_PROPERTY_HELPER);
        if (helper != null) {
            return helper;
        }
        helper = new PropertyHelper();
        helper.setProject(project);

        project.addReference(MagicNames.REFID_PROPERTY_HELPER, helper);
        return helper;
    }

    // --------------------  Methods to override  --------------------

    /**
     * Sets a property. Any existing property of the same name
     * is overwritten, unless it is a user property. Will be called
     * from setProperty().
     *
     * If all helpers return false, the property will be saved in
     * the default properties table by setProperty.
     *
     * @param ns   The namespace that the property is in (currently
     *             not used.
     * @param name The name of property to set.
     *             Must not be <code>null</code>.
     * @param value The new value of the property.
     *              Must not be <code>null</code>.
     * @param inherited True if this property is inherited (an [sub]ant[call] property).
     * @param user      True if this property is a user property.
     * @param isNew     True is this is a new property.
     * @return true if this helper has stored the property, false if it
     *    couldn't. Each helper should delegate to the next one (unless it
     *    has a good reason not to).
     * @deprecated PropertyHelper chaining is deprecated.
     */
    public boolean setPropertyHook(String ns, String name,
                                   Object value,
                                   boolean inherited, boolean user,
                                   boolean isNew) {
        if (getNext() != null) {
            boolean subst = getNext().setPropertyHook(ns, name, value, inherited, user, isNew);
            // If next has handled the property
            if (subst) {
                return true;
            }
        }
        return false;
    }

    /**
     * Get a property. If all hooks return null, the default
     * tables will be used.
     *
     * @param ns namespace of the sought property.
     * @param name name of the sought property.
     * @param user True if this is a user property.
     * @return The property, if returned by a hook, or null if none.
     * @deprecated PropertyHelper chaining is deprecated.
     */
    public Object getPropertyHook(String ns, String name, boolean user) {
        if (getNext() != null) {
            Object o = getNext().getPropertyHook(ns, name, user);
            if (o != null) {
                return o;
            }
        }
        // Experimental/Testing, will be removed
        if (name.startsWith("toString:")) {
            name = name.substring("toString:".length());
            Object v = project.getReference(name);
            return (v == null) ? null : v.toString();
        }
        return null;
    }

    // -------------------- Optional methods   --------------------
    // You can override those methods if you want to optimize or
    // do advanced things (like support a special syntax).
    // The methods do not chain - you should use them when embedding ant
    // (by replacing the main helper)

    /**
     * Parses a string containing <code>${xxx}</code> style property
     * references into two lists. The first list is a collection
     * of text fragments, while the other is a set of string property names.
     * <code>null</code> entries in the first list indicate a property
     * reference from the second list.
     *
     * It can be overridden with a more efficient or customized version.
     *
     * @param value     Text to parse. Must not be <code>null</code>.
     * @param fragments List to add text fragments to.
     *                  Must not be <code>null</code>.
     * @param propertyRefs List to add property names to.
     *                     Must not be <code>null</code>.
     *
     * @exception BuildException if the string contains an opening
     *                           <code>${</code> without a closing
     *                           <code>}</code>
     * @deprecated We can do better than this.
     */
    public void parsePropertyString(String value, Vector fragments,
                                    Vector propertyRefs) throws BuildException {
        parsePropertyStringDefault(value, fragments, propertyRefs);
    }

    /**
     * Replaces <code>${xxx}</code> style constructions in the given value
     * with the string value of the corresponding data types.
     *
     * @param ns    The namespace for the property.
     * @param value The string to be scanned for property references.
     *              May be <code>null</code>, in which case this
     *              method returns immediately with no effect.
     * @param keys  Mapping (String to String) of property names to their
     *              values. If <code>null</code>, only project properties will
     *              be used.
     *
     * @exception BuildException if the string contains an opening
     *                           <code>${</code> without a closing
     *                           <code>}</code>
     * @return the original string with the properties replaced, or
     *         <code>null</code> if the original string is <code>null</code>.
     */
    public String replaceProperties(String ns, String value, Hashtable keys) throws BuildException {
        return replaceProperties(value);
    }

    /**
     * Replaces <code>${xxx}</code> style constructions in the given value
     * with the string value of the corresponding data types.
     *
     * @param value The string to be scanned for property references.
     *              May be <code>null</code>, in which case this
     *              method returns immediately with no effect.
     *
     * @exception BuildException if the string contains an opening
     *                           <code>${</code> without a closing
     *                           <code>}</code>
     * @return the original string with the properties replaced, or
     *         <code>null</code> if the original string is <code>null</code>.
     */
    public String replaceProperties(String value) throws BuildException {
        Object o = parseProperties(value);
        return o == null || o instanceof String ? (String) o : o.toString();
    }

    /**
     * Decode properties from a String representation.  If the entire
     * contents of the String resolve to a single property, that value
     * is returned.  Otherwise a String is returned.
     *
     * @param value The string to be scanned for property references.
     *              May be <code>null</code>, in which case this
     *              method returns immediately with no effect.
     *
     * @exception BuildException if the string contains an opening
     *                           <code>${</code> without a closing
     *                           <code>}</code>
     * @return the original string with the properties replaced, or
     *         <code>null</code> if the original string is <code>null</code>.
     */
    public Object parseProperties(String value) throws BuildException {
        if (value == null || "".equals(value)) {
            return value;
        }
        ParsePosition pos = new ParsePosition(0);
        Object o = parseNextProperty(value, pos);
        if (o != null && pos.getIndex() == value.length()) {
            return o;
        }
        StringBuffer sb = new StringBuffer(value.length() * 2);
        if (o == null) {
            sb.append(value.charAt(pos.getIndex()));
            pos.setIndex(pos.getIndex() + 1);
        } else {
            sb.append(o);
        }
        while (pos.getIndex() < value.length()) {
            o = parseNextProperty(value, pos);
            if (o == null) {
                sb.append(value.charAt(pos.getIndex()));
                pos.setIndex(pos.getIndex() + 1);
            } else {
                sb.append(o);
            }
        }
        return sb.toString();
    }

    /**
     * Learn whether a String contains replaceable properties.
     * @param value the String to check.
     * @return <code>true</code> if <code>value</code> contains property notation.
     */
    public boolean containsProperties(String value) {
        if (value == null) {
            return false;
        }
        for (ParsePosition pos = new ParsePosition(0); pos.getIndex() < value.length();) {
            if (parsePropertyName(value, pos) != null) {
                return true;
            }
            pos.setIndex(pos.getIndex() + 1);
        }
        return false;
    }

    /**
     * Return any property that can be parsed from the specified position in the specified String.
     * @param value String to parse
     * @param pos ParsePosition
     * @return Object or null if no property is at the current location.
     */
    public Object parseNextProperty(String value, ParsePosition pos) {
        int start = pos.getIndex();
        String propertyName = parsePropertyName(value, pos);
        if (propertyName != null) {
            Object result = getProperty(propertyName);
            if (result != null) {
                return result;
            }
            getProject().log("Property \"" + propertyName
                    + "\" has not been set", Project.MSG_VERBOSE);
            return value.substring(start, pos.getIndex());
        }
        return null;
    }

    private String parsePropertyName(String value, ParsePosition pos) {
        for (Iterator iter = getDelegates(PropertyExpander.class).iterator(); iter.hasNext();) {
            String propertyName = ((PropertyExpander) iter.next()).parsePropertyName(value, pos, this);
            if (propertyName == null) {
                continue;
            }
            return propertyName;
        }
        return null;
    }

    // -------------------- Default implementation  --------------------
    // Methods used to support the default behavior and provide backward
    // compatibility. Some will be deprecated, you should avoid calling them.

    /**
     * Default implementation of setProperty. Will be called from Project.
     * This is the original 1.5 implementation, with calls to the hook
     * added.
     * @param ns      The namespace for the property (currently not used).
     * @param name    The name of the property.
     * @param value   The value to set the property to.
     * @param verbose If this is true output extra log messages.
     * @return true if the property is set.
     * @deprecated namespaces are unnecessary.
     */
    public boolean setProperty(String ns, String name, Object value, boolean verbose) {
        return setProperty(name, value, verbose);
    }

    /**
     * Default implementation of setProperty. Will be called from Project.
     *  This is the original 1.5 implementation, with calls to the hook
     *  added.
     *  @param name    The name of the property.
     *  @param value   The value to set the property to.
     *  @param verbose If this is true output extra log messages.
     *  @return true if the property is set.
     */
    public synchronized boolean setProperty(String name, Object value, boolean verbose) {
        // user (CLI) properties take precedence
        if (null != userProperties.get(name)) {
            if (verbose) {
                project.log("Override ignored for user property \"" + name
                        + "\"", Project.MSG_VERBOSE);
            }
            return false;
        }

//        boolean done = setPropertyHook(ns, name, value, false, false, false);
//        if (done) {
//            return true;
//        }

        if (null != properties.get(name) && verbose) {
            project.log("Overriding previous definition of property \"" + name
                    + "\"", Project.MSG_VERBOSE);
        }

        if (verbose) {
            project.log("Setting project property: " + name + " -> "
                    + value, Project.MSG_DEBUG);
        }
        if (name != null && value != null) {
            properties.put(name, value);
        }
        return true;
    }

    /**
     * Sets a property if no value currently exists. If the property
     * exists already, a message is logged and the method returns with
     * no other effect.
     *
     * @param ns   The namespace for the property (currently not used).
     * @param name The name of property to set.
     *             Must not be <code>null</code>.
     * @param value The new value of the property.
     *              Must not be <code>null</code>.
     * @since Ant 1.6
     * @deprecated namespaces are unnecessary.
     */
    public void setNewProperty(String ns, String name, Object value) {
        setNewProperty(name, value);
    }

    /**
     * Sets a property if no value currently exists. If the property
     * exists already, a message is logged and the method returns with
     * no other effect.
     *
     * @param name The name of property to set.
     *             Must not be <code>null</code>.
     * @param value The new value of the property.
     *              Must not be <code>null</code>.
     * @since Ant 1.8
     */
    public synchronized void setNewProperty(String name, Object value) {
        if (null != properties.get(name)) {
            project.log("Override ignored for property \"" + name + "\"", Project.MSG_VERBOSE);
            return;
        }
//        boolean done = setPropertyHook(ns, name, value, false, false, true);
//        if (done) {
//            return;
//        }
        project.log("Setting project property: " + name + " -> " + value, Project.MSG_DEBUG);
        if (name != null && value != null) {
            properties.put(name, value);
        }
    }

    /**
     * Sets a user property, which cannot be overwritten by
     * set/unset property calls. Any previous value is overwritten.
     * @param ns   The namespace for the property (currently not used).
     * @param name The name of property to set.
     *             Must not be <code>null</code>.
     * @param value The new value of the property.
     *              Must not be <code>null</code>.
     * @deprecated namespaces are unnecessary.
     */
    public void setUserProperty(String ns, String name, Object value) {
        setUserProperty(name, value);
    }

    /**
     * Sets a user property, which cannot be overwritten by
     * set/unset property calls. Any previous value is overwritten.
     * @param name The name of property to set.
     *             Must not be <code>null</code>.
     * @param value The new value of the property.
     *              Must not be <code>null</code>.
     */
    public synchronized void setUserProperty(String name, Object value) {
        project.log("Setting ro project property: " + name + " -> " + value, Project.MSG_DEBUG);
        userProperties.put(name, value);

//        boolean done = setPropertyHook(ns, name, value, false, true, false);
//        if (done) {
//            return;
//        }
        properties.put(name, value);
    }

    /**
     * Sets an inherited user property, which cannot be overwritten by set/unset
     * property calls. Any previous value is overwritten. Also marks
     * these properties as properties that have not come from the
     * command line.
     *
     * @param ns   The namespace for the property (currently not used).
     * @param name The name of property to set.
     *             Must not be <code>null</code>.
     * @param value The new value of the property.
     *              Must not be <code>null</code>.
     * @deprecated namespaces are unnecessary.
     */
    public void setInheritedProperty(String ns, String name, Object value) {
        setInheritedProperty(name, value);
    }

    /**
     * Sets an inherited user property, which cannot be overwritten by set/unset
     * property calls. Any previous value is overwritten. Also marks
     * these properties as properties that have not come from the
     * command line.
     *
     * @param name The name of property to set.
     *             Must not be <code>null</code>.
     * @param value The new value of the property.
     *              Must not be <code>null</code>.
     */
    public synchronized void setInheritedProperty(String name, Object value) {
        inheritedProperties.put(name, value);

        project.log("Setting ro project property: " + name + " -> " + value, Project.MSG_DEBUG);
        userProperties.put(name, value);

//        boolean done = setPropertyHook(ns, name, value, true, false, false);
//        if (done) {
//            return;
//        }
        properties.put(name, value);
    }

    // -------------------- Getting properties  --------------------

    /**
     * Returns the value of a property, if it is set.  You can override
     * this method in order to plug your own storage.
     *
     * @param ns   The namespace for the property (currently not used).
     * @param name The name of the property.
     *             May be <code>null</code>, in which case
     *             the return value is also <code>null</code>.
     * @return the property value, or <code>null</code> for no match
     *         or if a <code>null</code> name is provided.
     * @deprecated namespaces are unnecessary.
     */
    public synchronized Object getProperty(String ns, String name) {
        return getProperty(name);
    }

    /**
     * Returns the value of a property, if it is set.  You can override
     * this method in order to plug your own storage.
     *
     * @param name The name of the property.
     *             May be <code>null</code>, in which case
     *             the return value is also <code>null</code>.
     * @return the property value, or <code>null</code> for no match
     *         or if a <code>null</code> name is provided.
     */
    public synchronized Object getProperty(String name) {
        if (name == null) {
            return null;
        }
        for (Iterator iter = getDelegates(PropertyEvaluator.class).iterator(); iter.hasNext();) {
            Object o = ((PropertyEvaluator) iter.next()).evaluate(name, this);
            if (o != null) {
                return o;
            }
        }
//        Object o = getPropertyHook(ns, name, false);
//        if (o != null) {
//            return o;
//        }
        return properties.get(name);
    }

    /**
     * Returns the value of a user property, if it is set.
     *
     * @param ns   The namespace for the property (currently not used).
     * @param name The name of the property.
     *             May be <code>null</code>, in which case
     *             the return value is also <code>null</code>.
     * @return the property value, or <code>null</code> for no match
     *         or if a <code>null</code> name is provided.
     * @deprecated namespaces are unnecessary.
     */
    public Object getUserProperty(String ns, String name) {
        return getUserProperty(name);
    }

    /**
     * Returns the value of a user property, if it is set.
     *
     * @param name The name of the property.
     *             May be <code>null</code>, in which case
     *             the return value is also <code>null</code>.
     * @return the property value, or <code>null</code> for no match
     *         or if a <code>null</code> name is provided.
     */
    public synchronized Object getUserProperty(String name) {
        if (name == null) {
            return null;
        }
/*
        Object o = getPropertyHook(ns, name, true);
        if (o != null) {
            return o;
        }
*/
        return userProperties.get(name);
    }

    // -------------------- Access to property tables  --------------------
    // This is used to support ant call and similar tasks. It should be
    // deprecated, it is possible to use a better (more efficient)
    // mechanism to preserve the context.

    /**
     * Returns a copy of the properties table.
     * @return a hashtable containing all properties (including user properties).
     */
    public Hashtable getProperties() {
        //avoid concurrent modification:
        synchronized (properties) {
            return new Hashtable(properties);
        }
        // There is a better way to save the context. This shouldn't
        // delegate to next, it's for backward compatibility only.
    }

    /**
     * Returns a copy of the user property hashtable
     * @return a hashtable containing just the user properties
     */
    public Hashtable getUserProperties() {
        //avoid concurrent modification:
        synchronized (userProperties) {
            return new Hashtable(userProperties);
        }
    }

    /**
     * special back door for subclasses, internal access to the hashtables
     * @return the live hashtable of all properties
     */
    protected Hashtable getInternalProperties() {
        return properties;
    }

    /**
     * special back door for subclasses, internal access to the hashtables
     *
     * @return the live hashtable of user properties
     */
    protected Hashtable getInternalUserProperties() {
        return userProperties;
    }

    /**
     * special back door for subclasses, internal access to the hashtables
     *
     * @return the live hashtable inherited properties
     */
    protected Hashtable getInternalInheritedProperties() {
        return inheritedProperties;
    }

    /**
     * Copies all user properties that have not been set on the
     * command line or a GUI tool from this instance to the Project
     * instance given as the argument.
     *
     * <p>To copy all "user" properties, you will also have to call
     * {@link #copyUserProperties copyUserProperties}.</p>
     *
     * @param other the project to copy the properties to.  Must not be null.
     *
     * @since Ant 1.6
     */
    public void copyInheritedProperties(Project other) {
        //avoid concurrent modification:
        synchronized (inheritedProperties) {
            Enumeration e = inheritedProperties.keys();
            while (e.hasMoreElements()) {
                String arg = e.nextElement().toString();
                if (other.getUserProperty(arg) != null) {
                    continue;
                }
                Object value = inheritedProperties.get(arg);
                other.setInheritedProperty(arg, value.toString());
            }
        }
    }

    /**
     * Copies all user properties that have been set on the command
     * line or a GUI tool from this instance to the Project instance
     * given as the argument.
     *
     * <p>To copy all "user" properties, you will also have to call
     * {@link #copyInheritedProperties copyInheritedProperties}.</p>
     *
     * @param other the project to copy the properties to.  Must not be null.
     *
     * @since Ant 1.6
     */
    public void copyUserProperties(Project other) {
        //avoid concurrent modification:
        synchronized (userProperties) {
            Enumeration e = userProperties.keys();
            while (e.hasMoreElements()) {
                Object arg = e.nextElement();
                if (inheritedProperties.containsKey(arg)) {
                    continue;
                }
                Object value = userProperties.get(arg);
                other.setUserProperty(arg.toString(), value.toString());
            }
        }
    }

    // -------------------- Property parsing  --------------------
    // Moved from ProjectHelper. You can override the static method -
    // this is used for backward compatibility (for code that calls
    // the parse method in ProjectHelper).

    /**
     * Default parsing method. It is here only to support backward compatibility
     * for the static ProjectHelper.parsePropertyString().
     */
    static void parsePropertyStringDefault(String value, Vector fragments, Vector propertyRefs)
            throws BuildException {
        int prev = 0;
        int pos;
        //search for the next instance of $ from the 'prev' position
        while ((pos = value.indexOf("$", prev)) >= 0) {

            //if there was any text before this, add it as a fragment
            //TODO, this check could be modified to go if pos>prev;
            //seems like this current version could stick empty strings
            //into the list
            if (pos > 0) {
                fragments.addElement(value.substring(prev, pos));
            }
            //if we are at the end of the string, we tack on a $
            //then move past it
            if (pos == (value.length() - 1)) {
                fragments.addElement("$");
                prev = pos + 1;
            } else if (value.charAt(pos + 1) != '{') {
                //peek ahead to see if the next char is a property or not
                //not a property: insert the char as a literal
                /*
                fragments.addElement(value.substring(pos + 1, pos + 2));
                prev = pos + 2;
                */
                if (value.charAt(pos + 1) == '$') {
                    //backwards compatibility two $ map to one mode
                    fragments.addElement("$");
                    prev = pos + 2;
                } else {
                    //new behaviour: $X maps to $X for all values of X!='$'
                    fragments.addElement(value.substring(pos, pos + 2));
                    prev = pos + 2;
                }
            } else {
                //property found, extract its name or bail on a typo
                int endName = value.indexOf('}', pos);
                if (endName < 0) {
                    throw new BuildException("Syntax error in property: " + value);
                }
                String propertyName = value.substring(pos + 2, endName);
                fragments.addElement(null);
                propertyRefs.addElement(propertyName);
                prev = endName + 1;
            }
        }
        //no more $ signs found
        //if there is any tail to the file, append it
        if (prev < value.length()) {
            fragments.addElement(value.substring(prev));
        }
    }

    /**
     * Add the specified delegate object to this PropertyHelper.
     * Delegates are processed in LIFO order.
     * @param delegate the delegate to add.
     * @since Ant 1.8
     */
    public synchronized void add(Delegate delegate) {
        for (Iterator iter = getDelegateInterfaces(delegate).iterator(); iter.hasNext();) {
            Object key = iter.next();
            List list = (List) delegates.get(key);
            if (list == null) {
                list = new ArrayList();
                delegates.put(key, list);
            }
            if (list.contains(delegate)) {
                list.remove(delegate);
            }
            list.add(0, delegate);
        }
    }

    /**
     * Get the Collection of delegates of the specified type.
     * @param type delegate type.
     * @return Collection.
     * @since Ant 1.8
     */
    protected synchronized List getDelegates(Class type) {
        return delegates.containsKey(type)
                ? (List) new ArrayList((List) delegates.get(type)) : Collections.EMPTY_LIST;
    }

    /**
     * Get all Delegate interfaces (excluding Delegate itself) from the specified Delegate.
     * @param d the Delegate to inspect.
     * @return Set<Class>
     * @since Ant 1.8
     */
    protected Set getDelegateInterfaces(Delegate d) {
        HashSet result = new HashSet();
        Class c = d.getClass();
        while (c != null) {
            Class[] ifs = c.getInterfaces();
            for (int i = 0; i < ifs.length; i++) {
                if (Delegate.class.isAssignableFrom(ifs[i])) {
                    result.add(ifs[i]);
                }
            }
            c = c.getSuperclass();
        }
        result.remove(Delegate.class);
        return result;
    }

    /**
     * Make a clone of this PropertyHelper.
     * @return the cloned PropertyHelper.
     * @since Ant 1.8
     */
    public synchronized Object clone() {
        PropertyHelper result;
        try {
            result = (PropertyHelper) super.clone();
            result.delegates = (Hashtable) delegates.clone();
            result.properties = (Hashtable) properties.clone();
            result.userProperties = (Hashtable) userProperties.clone();
            result.inheritedProperties = (Hashtable) inheritedProperties.clone();
        } catch (CloneNotSupportedException e) {
            throw new BuildException(e);
        }
        return result;
    }
}
