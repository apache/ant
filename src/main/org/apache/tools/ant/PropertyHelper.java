/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package org.apache.tools.ant;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import org.apache.tools.ant.property.GetProperty;
import org.apache.tools.ant.property.NullReturn;
import org.apache.tools.ant.property.ParseProperties;
import org.apache.tools.ant.property.PropertyExpander;

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

/* update for impending Ant 1.8.0:

   - I can't see any reason for ns and would like to deprecate it.
   - Replacing chaining with delegates for certain behavioral aspects.
   - Object value seems valuable as outlined.

 */

/**
 * Deals with properties - substitution, dynamic properties, etc.
 *
 * <p>This code has been heavily restructured for Ant 1.8.0.  It is
 * expected that custom PropertyHelper implementation that used the
 * older chaining mechanism of Ant 1.6 won't work in all cases, and
 * its usage is deprecated.  The preferred way to customize Ant's
 * property handling is by {@link #add adding} {@link
 * PropertyHelper.Delegate delegates} of the appropriate subinterface
 * and have this implementation use them.</p>
 *
 * <p>When {@link #parseProperties expanding a string that may contain
 * properties} this class will delegate the actual parsing to {@link
 * org.apache.tools.ant.property.ParseProperties#parseProperties
 * parseProperties} inside the ParseProperties class which in turn
 * uses the {@link org.apache.tools.ant.property.PropertyExpander
 * PropertyExpander delegates} to find properties inside the string
 * and this class to expand the property names found into the
 * corresponding values.</p>
 *
 * <p>When {@link #getProperty looking up a property value} this class
 * will first consult all {@link PropertyHelper.PropertyEvaluator
 * PropertyEvaluator} delegates and fall back to an internal map of
 * "project properties" if no evaluator matched the property name.</p>
 *
 * <p>When {@link #setProperty setting a property value} this class
 * will first consult all {@link PropertyHelper.PropertySetter
 * PropertySetter} delegates and fall back to an internal map of
 * "project properties" if no setter matched the property name.</p>
 *
 * @since Ant 1.6
 */
public class PropertyHelper implements GetProperty {

    //  --------------------------------------------------------
    //
    //    The property delegate interfaces
    //
    //  --------------------------------------------------------

    /**
     * Marker interface for a PropertyHelper delegate.
     * @since Ant 1.8.0
     */
    public interface Delegate {
    }

    /**
     * Looks up a property's value based on its name.
     *
     * <p>Can be used to look up properties in a different storage
     * than the project instance (like local properties for example)
     * or to implement custom "protocols" like Ant's
     * <code>${toString:refid}</code> syntax.</p>
     *
     * @since Ant 1.8.0
     */
    public interface PropertyEvaluator extends Delegate {
        /**
         * Evaluate a property.
         *
         * @param property the property's String "identifier".
         * @param propertyHelper the invoking PropertyHelper.
         * @return null if the property name could not be found, an
         * instance of {@link org.apache.tools.ant.property.NullReturn
         * NullReturn} to indicate a property with a name that can be
         * matched but a value of <code>null</code> and the property's
         * value otherwise.
         */
        Object evaluate(String property, PropertyHelper propertyHelper);
    }

    /**
     * Sets or overrides a property.
     *
     * <p>Can be used to store properties in a different storage than
     * the project instance (like local properties for example).</p>
     *
     * @since Ant 1.8.0
     */
    public interface PropertySetter extends Delegate {
        /**
         * Set a *new" property.
         *
         * <p>Should not replace the value of an existing property.</p>
         *
         * @param property the property's String "identifier".
         * @param value    the value to set.
         * @param propertyHelper the invoking PropertyHelper.
         * @return true if this entity 'owns' the property.
         */
        boolean setNew(
            String property, Object value, PropertyHelper propertyHelper);

        /**
         * Set a property.
         *
         * <p>May replace the value of an existing property.</p>
         *
         * @param property the property's String "identifier".
         * @param value    the value to set.
         * @param propertyHelper the invoking PropertyHelper.
         * @return true if this entity 'owns' the property.
         */
        boolean set(
            String property, Object value, PropertyHelper propertyHelper);
    }

    /**
     * Obtains the names of all known properties.
     *
     * @since 1.10.9
     */
    public interface PropertyEnumerator extends Delegate {
        /**
         * Returns the names of all properties known to this delegate.
         *
         * @return the names of all properties known to this delegate.
         */
        Set<String> getPropertyNames();
    }

    //  --------------------------------------------------------
    //
    //    The predefined property delegates
    //
    //  --------------------------------------------------------

    private static final PropertyEvaluator TO_STRING = new PropertyEvaluator() {
        private final String PREFIX = "toString:";
        private final int PREFIX_LEN = PREFIX.length();

        public Object evaluate(String property, PropertyHelper propertyHelper) {
            Object o = null;
            if (property.startsWith(PREFIX) && propertyHelper.getProject() != null) {
                o = propertyHelper.getProject().getReference(property.substring(PREFIX_LEN));
            }
            return o == null ? null : o.toString();
        }
    };

    private static final PropertyExpander DEFAULT_EXPANDER =
        (s, pos, notUsed) -> {
            int index = pos.getIndex();
            //directly check near, triggering characters:
            if (s.length() - index >= 3 && '$' == s.charAt(index)
                && '{' == s.charAt(index + 1)) {
                int start = index + 2;
                //defer to String.indexOf() for protracted check:
                int end = s.indexOf('}', start);
                if (end < 0) {
                    throw new BuildException(
                        "Syntax error in property: " + s.substring(index));
                }
                pos.setIndex(end + 1);
                return start == end ? "" : s.substring(start, end);
            }
            return null;
        };

    /** dummy */
    private static final PropertyExpander SKIP_DOUBLE_DOLLAR =
        (s, pos, notUsed) -> {
            int index = pos.getIndex();
            if (s.length() - index >= 2) {
                /* check for $$; if found, advance by one--
                 * this expander is at the bottom of the stack
                 * and will thus be the last consulted,
                 * so the next thing that ParseProperties will do
                 * is advance the parse position beyond the second $
                 */
                if ('$' == s.charAt(index) && '$' == s.charAt(++index)) {
                    pos.setIndex(index);
                }
            }
            return null;
        };

    /**
     * @since Ant 1.8.0
     */
    private static final PropertyEvaluator FROM_REF = new PropertyEvaluator() {
        private final String PREFIX = "ant.refid:";
        private final int PREFIX_LEN = PREFIX.length();

        public Object evaluate(String prop, PropertyHelper helper) {
            return prop.startsWith(PREFIX) && helper.getProject() != null
                ? helper.getProject().getReference(prop.substring(PREFIX_LEN))
                : null;
        }
    };

    private Project project;
    private PropertyHelper next;
    private final Hashtable<Class<? extends Delegate>, List<Delegate>> delegates = new Hashtable<>();

    /** Project properties map (usually String to String). */
    private final Hashtable<String, Object> properties = new Hashtable<>();

    /**
     * Map of "user" properties (as created in the Ant task, for example).
     * Note that these key/value pairs are also always put into the
     * project properties, so only the project properties need to be queried.
     */
    private final Hashtable<String, Object> userProperties = new Hashtable<>();

    /**
     * Map of inherited "user" properties - that are those "user"
     * properties that have been created by tasks and not been set
     * from the command line or a GUI tool.
     */
    private final Hashtable<String, Object> inheritedProperties = new Hashtable<>();

    /**
     * Default constructor.
     */
    protected PropertyHelper() {
        add(FROM_REF);
        add(TO_STRING);
        add(SKIP_DOUBLE_DOLLAR);
        add(DEFAULT_EXPANDER);
    }

    //  --------------------------------------------------------
    //
    //    Some helper static methods to get and set properties
    //
    //  --------------------------------------------------------

    /**
     * A helper static method to get a property
     * from a particular project.
     * @param project the project in question.
     * @param name the property name
     * @return the value of the property if present, null otherwise.
     * @since Ant 1.8.0
     */
    public static Object getProperty(Project project, String name) {
        return PropertyHelper.getPropertyHelper(project)
            .getProperty(name);
    }

    /**
     * A helper static method to set a property
     * from a particular project.
     * @param project the project in question.
     * @param name the property name
     * @param value the value to use.
     * @since Ant 1.8.0
     */
    public static void setProperty(Project project, String name, Object value) {
        PropertyHelper.getPropertyHelper(project)
            .setProperty(name, value, true);
    }

    /**
     * A helper static method to set a new property
     * from a particular project.
     * @param project the project in question.
     * @param name the property name
     * @param value the value to use.
     * @since Ant 1.8.0
     */
    public static void setNewProperty(
        Project project, String name, Object value) {
        PropertyHelper.getPropertyHelper(project)
            .setNewProperty(name, value);
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
     * Prior to Ant 1.8.0 there have been 2 ways to hook into property handling:
     *
     *  - you can replace the main PropertyHelper. The replacement is required
     * to support the same semantics (of course :-)
     *
     *  - you can chain a property helper capable of storing some properties.
     *  Again, you are required to respect the immutability semantics (at
     *  least for non-dynamic properties)
     *
     * <p>As of Ant 1.8.0 this method is never invoked by any code
     * inside of Ant itself.</p>
     *
     * @param next the next property helper in the chain.
     * @deprecated use the delegate mechanism instead
     */
    @Deprecated
    public void setNext(PropertyHelper next) {
        this.next = next;
    }

    /**
     * Get the next property helper in the chain.
     *
     * <p>As of Ant 1.8.0 this method is never invoked by any code
     * inside of Ant itself except the {@link #setPropertyHook
     * setPropertyHook} and {@link #getPropertyHook getPropertyHook}
     * methods in this class.</p>
     *
     * @return the next property helper.
     * @deprecated use the delegate mechanism instead
     */
    @Deprecated
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
        PropertyHelper helper = null;
        if (project != null) {
            helper = project.getReference(MagicNames.REFID_PROPERTY_HELPER);
        }
        if (helper != null) {
            return helper;
        }

        helper = new PropertyHelper();
        helper.setProject(project);

        if (project != null) {
            project.addReference(MagicNames.REFID_PROPERTY_HELPER, helper);
        }

        return helper;
    }

    /**
     * Get the {@link PropertyExpander expanders}.
     * @since Ant 1.8.0
     * @return the expanders.
     */
    public Collection<PropertyExpander> getExpanders() {
        return getDelegates(PropertyExpander.class);
    }


    // --------------------  Methods to override  --------------------

    /**
     * Sets a property. Any existing property of the same name
     * is overwritten, unless it is a user property.
     *
     * If all helpers return false, the property will be saved in
     * the default properties table by setProperty.
     *
     * <p>As of Ant 1.8.0 this method is never invoked by any code
     * inside of Ant itself.</p>
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
    @Deprecated
    public boolean setPropertyHook(String ns, String name,
                                   Object value,
                                   boolean inherited, boolean user,
                                   boolean isNew) {
        if (getNext() != null) {
            // If next has handled the property
            return getNext().setPropertyHook(ns, name, value, inherited, user, isNew);
        }
        return false;
    }

    /**
     * Get a property. If all hooks return null, the default
     * tables will be used.
     *
     * <p>As of Ant 1.8.0 this method is never invoked by any code
     * inside of Ant itself.</p>
     *
     * @param ns namespace of the sought property.
     * @param name name of the sought property.
     * @param user True if this is a user property.
     * @return The property, if returned by a hook, or null if none.
     * @deprecated PropertyHelper chaining is deprecated.
     */
    @Deprecated
    public Object getPropertyHook(String ns, String name, boolean user) {
        if (getNext() != null) {
            Object o = getNext().getPropertyHook(ns, name, user);
            if (o != null) {
                return o;
            }
        }
        // Experimental/Testing, will be removed
        if (project != null && name.startsWith("toString:")) {
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
     * <p>Delegates to {@link #parsePropertyStringDefault
     * parsePropertyStringDefault}.</p>
     *
     * <p>As of Ant 1.8.0 this method is never invoked by any code
     * inside of Ant itself except {ProjectHelper#parsePropertyString
     * ProjectHelper.parsePropertyString}.</p>
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
     * @deprecated use the other mechanisms of this class instead
     */
    @Deprecated
    public void parsePropertyString(String value, Vector<String> fragments,
                                    Vector<String> propertyRefs) throws BuildException {
        parsePropertyStringDefault(value, fragments, propertyRefs);
    }

    /**
     * Replaces <code>${xxx}</code> style constructions in the given value
     * with the string value of the corresponding data types.
     *
     * <p>Delegates to the one-arg version, completely ignoring the ns
     * and keys parameters.</p>
     *
     * @param ns    The namespace for the property.
     * @param value The string to be scanned for property references.
     *              May be <code>null</code>, in which case this
     *              method returns immediately with no effect.
     * @param keys  Mapping (String to Object) of property names to their
     *              values. If <code>null</code>, only project properties will
     *              be used.
     *
     * @exception BuildException if the string contains an opening
     *                           <code>${</code> without a closing
     *                           <code>}</code>
     * @return the original string with the properties replaced, or
     *         <code>null</code> if the original string is <code>null</code>.
     */
    //TODO deprecate?  Recall why no longer using ns/keys params
    public String replaceProperties(String ns, String value, Hashtable<String, Object> keys) throws BuildException {
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
        return new ParseProperties(getProject(), getExpanders(), this)
            .parseProperties(value);
    }

    /**
     * Learn whether a String contains replaceable properties.
     * @param value the String to check.
     * @return <code>true</code> if <code>value</code> contains property notation.
     */
    public boolean containsProperties(String value) {
        return new ParseProperties(getProject(), getExpanders(), this)
            .containsProperties(value);
    }

    // -------------------- Default implementation  --------------------
    // Methods used to support the default behavior and provide backward
    // compatibility. Some will be deprecated, you should avoid calling them.

    /**
     * Default implementation of setProperty. Will be called from Project.
     * This is the original 1.5 implementation, with calls to the hook
     * added.
     *
     * <p>Delegates to the three-arg version, completely ignoring the
     * ns parameter.</p>
     *
     * @param ns      The namespace for the property (currently not used).
     * @param name    The name of the property.
     * @param value   The value to set the property to.
     * @param verbose If this is true output extra log messages.
     * @return true if the property is set.
     * @deprecated namespaces are unnecessary.
     */
    @Deprecated
    public boolean setProperty(String ns, String name, Object value, boolean verbose) {
        return setProperty(name, value, verbose);
    }

    /**
     * Default implementation of setProperty. Will be called from Project.
     *  @param name    The name of the property.
     *  @param value   The value to set the property to.
     *  @param verbose If this is true output extra log messages.
     *  @return true if the property is set.
     */
    public boolean setProperty(String name, Object value, boolean verbose) {
        for (PropertySetter setter : getDelegates(PropertySetter.class)) {
            if (setter.set(name, value, this)) {
                return true;
            }
        }
        synchronized (this) {
            // user (CLI) properties take precedence
            if (userProperties.containsKey(name)) {
                if (project != null && verbose) {
                    project.log("Override ignored for user property \""
                                + name + "\"", Project.MSG_VERBOSE);
                }
                return false;
            }
            if (project != null && verbose) {
                if (properties.containsKey(name)) {
                    project.log("Overriding previous definition of property \""
                                + name + "\"", Project.MSG_VERBOSE);
                }
                project.log("Setting project property: " + name + " -> "
                            + value, Project.MSG_DEBUG);
            }
            if (name != null && value != null) {
                properties.put(name, value);
            }
            return true;
        }
    }

    /**
     * Sets a property if no value currently exists. If the property
     * exists already, a message is logged and the method returns with
     * no other effect.
     *
     * <p>Delegates to the two-arg version, completely ignoring the
     * ns parameter.</p>
     *
     * @param ns   The namespace for the property (currently not used).
     * @param name The name of property to set.
     *             Must not be <code>null</code>.
     * @param value The new value of the property.
     *              Must not be <code>null</code>.
     * @since Ant 1.6
     * @deprecated namespaces are unnecessary.
     */
    @Deprecated
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
     * @since Ant 1.8.0
     */
    public void setNewProperty(String name, Object value) {
        for (PropertySetter setter : getDelegates(PropertySetter.class)) {
            if (setter.setNew(name, value, this)) {
                return;
            }
        }
        synchronized (this) {
            if (project != null && properties.containsKey(name)) {
                project.log("Override ignored for property \"" + name
                            + "\"", Project.MSG_VERBOSE);
                return;
            }
            if (project != null) {
                project.log("Setting project property: " + name
                            + " -> " + value, Project.MSG_DEBUG);
            }
            if (name != null && value != null) {
                properties.put(name, value);
            }
        }
    }

    /**
     * Sets a user property, which cannot be overwritten by
     * set/unset property calls. Any previous value is overwritten.
     *
     * <p>Delegates to the two-arg version, completely ignoring the
     * ns parameter.</p>
     *
     * @param ns   The namespace for the property (currently not used).
     * @param name The name of property to set.
     *             Must not be <code>null</code>.
     * @param value The new value of the property.
     *              Must not be <code>null</code>.
     * @deprecated namespaces are unnecessary.
     */
    @Deprecated
    public void setUserProperty(String ns, String name, Object value) {
        setUserProperty(name, value);
    }

    /**
     * Sets a user property, which cannot be overwritten by
     * set/unset property calls. Any previous value is overwritten.
     *
     * <p>Does <code>not</code> consult any delegates.</p>
     *
     * @param name The name of property to set.
     *             Must not be <code>null</code>.
     * @param value The new value of the property.
     *              Must not be <code>null</code>.
     */
    public void setUserProperty(String name, Object value) {
        if (project != null) {
            project.log("Setting ro project property: "
                        + name + " -> " + value, Project.MSG_DEBUG);
        }
        synchronized (this) {
            userProperties.put(name, value);
            properties.put(name, value);
        }
    }

    /**
     * Sets an inherited user property, which cannot be overwritten by set/unset
     * property calls. Any previous value is overwritten. Also marks
     * these properties as properties that have not come from the
     * command line.
     *
     * <p>Delegates to the two-arg version, completely ignoring the
     * ns parameter.</p>
     *
     * @param ns   The namespace for the property (currently not used).
     * @param name The name of property to set.
     *             Must not be <code>null</code>.
     * @param value The new value of the property.
     *              Must not be <code>null</code>.
     * @deprecated namespaces are unnecessary.
     */
    @Deprecated
    public void setInheritedProperty(String ns, String name, Object value) {
        setInheritedProperty(name, value);
    }

    /**
     * Sets an inherited user property, which cannot be overwritten by set/unset
     * property calls. Any previous value is overwritten. Also marks
     * these properties as properties that have not come from the
     * command line.
     *
     * <p>Does <code>not</code> consult any delegates.</p>
     *
     * @param name The name of property to set.
     *             Must not be <code>null</code>.
     * @param value The new value of the property.
     *              Must not be <code>null</code>.
     */
    public void setInheritedProperty(String name, Object value) {
        if (project != null) {
            project.log("Setting ro project property: " + name + " -> "
                        + value, Project.MSG_DEBUG);
        }

        synchronized (this) {
            inheritedProperties.put(name, value);
            userProperties.put(name, value);
            properties.put(name, value);
        }
    }

    // -------------------- Getting properties  --------------------

    /**
     * Returns the value of a property, if it is set.  You can override
     * this method in order to plug your own storage.
     *
     * <p>Delegates to the one-arg version ignoring the ns parameter.</p>
     *
     * @param ns   The namespace for the property (currently not used).
     * @param name The name of the property.
     *             May be <code>null</code>, in which case
     *             the return value is also <code>null</code>.
     * @return the property value, or <code>null</code> for no match
     *         or if a <code>null</code> name is provided.
     * @deprecated namespaces are unnecessary.
     */
    @Deprecated
    public Object getProperty(String ns, String name) {
        return getProperty(name);
    }

    /**
     * Returns the value of a property, if it is set.
     *
     * <p>This is the method that is invoked by {Project#getProperty
     * Project.getProperty}.</p>
     *
     * <p>You can override this method in order to plug your own
     * storage but the recommended approach is to add your own
     * implementation of {@link PropertyEvaluator PropertyEvaluator}
     * instead.</p>
     *
     * @param name The name of the property.
     *             May be <code>null</code>, in which case
     *             the return value is also <code>null</code>.
     * @return the property value, or <code>null</code> for no match
     *         or if a <code>null</code> name is provided.
     */
    public Object getProperty(String name) {
        if (name == null) {
            return null;
        }
        for (PropertyEvaluator evaluator : getDelegates(PropertyEvaluator.class)) {
            final Object o = evaluator.evaluate(name, this);
            if (o == null) {
                continue;
            }
            return o instanceof NullReturn ? null : o;
        }
        return properties.get(name);
    }

    /**
     * Returns the names of all known properties.
     * @since 1.10.9
     * @return the names of all known properties.
     */
    public Set<String> getPropertyNames() {
        final Set<String> names = new HashSet<>(properties.keySet());
        getDelegates(PropertyEnumerator.class)
            .forEach(e -> names.addAll(e.getPropertyNames()));
        return Collections.unmodifiableSet(names);
    }

    /**
     * Returns the value of a user property, if it is set.
     *
     * <p>Delegates to the one-arg version ignoring the ns parameter.</p>
     *
     * @param ns   The namespace for the property (currently not used).
     * @param name The name of the property.
     *             May be <code>null</code>, in which case
     *             the return value is also <code>null</code>.
     * @return the property value, or <code>null</code> for no match
     *         or if a <code>null</code> name is provided.
     * @deprecated namespaces are unnecessary.
     */
    @Deprecated
    public Object getUserProperty(String ns, String name) {
        return getUserProperty(name);
    }

    /**
     * Returns the value of a user property, if it is set.
     *
     * <p>Does <code>not</code> consult any delegates.</p>
     *
     * @param name The name of the property.
     *             May be <code>null</code>, in which case
     *             the return value is also <code>null</code>.
     * @return the property value, or <code>null</code> for no match
     *         or if a <code>null</code> name is provided.
     */
    public Object getUserProperty(String name) {
        if (name == null) {
            return null;
        }
        return userProperties.get(name);
    }

    // -------------------- Access to property tables  --------------------
    // This is used to support ant call and similar tasks. It should be
    // deprecated, it is possible to use a better (more efficient)
    // mechanism to preserve the context.

    /**
     * Returns a copy of the properties table.
     *
     * <p>Does not contain properties held by implementations of
     * delegates (like local properties).</p>
     *
     * @return a hashtable containing all properties (including user properties).
     */
    public Hashtable<String, Object> getProperties() {
        //avoid concurrent modification:
        synchronized (properties) {
            return new Hashtable<>(properties);
        }
        // There is a better way to save the context. This shouldn't
        // delegate to next, it's for backward compatibility only.
    }

    /**
     * Returns a copy of the user property hashtable
     *
     * <p>Does not contain properties held by implementations of
     * delegates (like local properties).</p>
     *
     * @return a hashtable containing just the user properties
     */
    public Hashtable<String, Object> getUserProperties() {
        //avoid concurrent modification:
        synchronized (userProperties) {
            return new Hashtable<>(userProperties);
        }
    }

    /**
     * Returns a copy of the inherited property hashtable
     *
     * <p>Does not contain properties held by implementations of
     * delegates (like local properties).</p>
     *
     * @return a hashtable containing just the inherited properties
     */
    public Hashtable<String, Object> getInheritedProperties() {
        //avoid concurrent modification:
        synchronized (inheritedProperties) {
            return new Hashtable<>(inheritedProperties);
        }
    }

    /**
     * special back door for subclasses, internal access to the hashtables
     * @return the live hashtable of all properties
     */
    protected Hashtable<String, Object> getInternalProperties() {
        return properties;
    }

    /**
     * special back door for subclasses, internal access to the hashtables
     *
     * @return the live hashtable of user properties
     */
    protected Hashtable<String, Object> getInternalUserProperties() {
        return userProperties;
    }

    /**
     * special back door for subclasses, internal access to the hashtables
     *
     * @return the live hashtable inherited properties
     */
    protected Hashtable<String, Object> getInternalInheritedProperties() {
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
     * <p>Does not copy properties held by implementations of
     * delegates (like local properties).</p>
     *
     * @param other the project to copy the properties to.  Must not be null.
     *
     * @since Ant 1.6
     */
    public void copyInheritedProperties(Project other) {
        //avoid concurrent modification:
        synchronized (inheritedProperties) {
            for (Map.Entry<String, Object> entry : inheritedProperties.entrySet()) {
                String arg = entry.getKey();
                if (other.getUserProperty(arg) == null) {
                    other.setInheritedProperty(arg, entry.getValue().toString());
                }
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
     * <p>Does not copy properties held by implementations of
     * delegates (like local properties).</p>
     *
     * @param other the project to copy the properties to.  Must not be null.
     *
     * @since Ant 1.6
     */
    public void copyUserProperties(Project other) {
        //avoid concurrent modification:
        synchronized (userProperties) {
            for (Map.Entry<String, Object> entry : userProperties.entrySet()) {
                String arg = entry.getKey();
                if (!inheritedProperties.containsKey(arg)) {
                    other.setUserProperty(arg, entry.getValue().toString());
                }
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
    static void parsePropertyStringDefault(String value, Vector<String> fragments, Vector<String> propertyRefs)
            throws BuildException {
        int prev = 0;
        int pos;
        //search for the next instance of $ from the 'prev' position
        while ((pos = value.indexOf('$', prev)) >= 0) {

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
                } else {
                    //new behaviour: $X maps to $X for all values of X!='$'
                    fragments.addElement(value.substring(pos, pos + 2));
                }
                prev = pos + 2;
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
     * @since Ant 1.8.0
     */
    public void add(Delegate delegate) {
        synchronized (delegates) {
            for (Class<? extends Delegate> key : getDelegateInterfaces(delegate)) {
                List<Delegate> list = delegates.get(key);
                if (list == null) {
                    list = new ArrayList<>();
                } else {
                    //copy on write, top priority
                    list = new ArrayList<>(list);
                    list.remove(delegate);
                }
                list.add(0, delegate);
                delegates.put(key, Collections.unmodifiableList(list));
            }
        }
    }

    /**
     * Get the Collection of delegates of the specified type.
     *
     * @param <D> desired type.
     * @param type
     *            delegate type.
     * @return Collection.
     * @since Ant 1.8.0
     */
    protected <D extends Delegate> List<D> getDelegates(Class<D> type) {
        @SuppressWarnings("unchecked")
        final List<D> result = (List<D>) delegates.get(type);
        return result == null ? Collections.emptyList() : result;
    }

    /**
     * Get all Delegate interfaces (excluding Delegate itself) from the specified Delegate.
     * @param d the Delegate to inspect.
     * @return Set&lt;Class&gt;
     * @since Ant 1.8.0
     */
    @SuppressWarnings("unchecked")
    protected static Set<Class<? extends Delegate>> getDelegateInterfaces(Delegate d) {
        final HashSet<Class<? extends Delegate>> result = new HashSet<>();
        Class<?> c = d.getClass();
        while (c != null) {
            for (Class<?> ifc : c.getInterfaces()) {
                if (Delegate.class.isAssignableFrom(ifc)) {
                    result.add((Class<? extends Delegate>) ifc);
                }
            }
            c = c.getSuperclass();
        }
        result.remove(Delegate.class);
        return result;
    }

    /**
     * If the given object can be interpreted as a true/false value,
     * turn it into a matching Boolean - otherwise return null.
     * @param value Object
     * @return Boolean
     * @since Ant 1.8.0
     */
    public static Boolean toBoolean(Object value) {
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        if (value instanceof String) {
            String s = (String) value;
            if (Project.toBoolean(s)) {
                return Boolean.TRUE;
            }
            if ("off".equalsIgnoreCase(s)
                || "false".equalsIgnoreCase(s)
                || "no".equalsIgnoreCase(s)) {
                return Boolean.FALSE;
            }
        }
        return null;
    }

    /**
     * Returns true if the object is null or an empty string.
     *
     * @param value Object
     * @return boolean
     * @since Ant 1.8.0
     */
    private static boolean nullOrEmpty(Object value) {
        return value == null || "".equals(value);
    }

    /**
     * Returns true if the value can be interpreted as a true value or
     * cannot be interpreted as a false value and a property of the
     * value's name exists.
     * @param value Object
     * @return boolean
     * @since Ant 1.8.0
     */
    private boolean evalAsBooleanOrPropertyName(Object value) {
        Boolean b = toBoolean(value);
        if (b != null) {
            return b;
        }
        return getProperty(String.valueOf(value)) != null;
    }

    /**
     * Returns true if the value is null or an empty string, can be
     * interpreted as a true value or cannot be interpreted as a false
     * value and a property of the value's name exists.
     * @param value Object
     * @return boolean
     * @since Ant 1.8.0
     */
    public boolean testIfCondition(Object value) {
        return nullOrEmpty(value) || evalAsBooleanOrPropertyName(value);
    }

    /**
     * Returns true if the value is null or an empty string, can be
     * interpreted as a false value or cannot be interpreted as a true
     * value and a property of the value's name doesn't exist.
     * @param value Object
     * @return boolean
     * @since Ant 1.8.0
     */
    public boolean testUnlessCondition(Object value) {
        return nullOrEmpty(value) || !evalAsBooleanOrPropertyName(value);
    }
}
