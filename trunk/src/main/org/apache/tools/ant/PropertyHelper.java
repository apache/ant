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

import java.util.Hashtable;
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

/** NOT FINAL. API MAY CHANGE
 *
 * Deals with properties - substitution, dynamic properties, etc.
 *
 * This is the same code as in Ant1.5. The main addition is the ability
 * to chain multiple PropertyHelpers and to replace the default.
 *
 * @since Ant 1.6
 */
public class PropertyHelper {

    private Project project;
    private PropertyHelper next;

    /** Project properties map (usually String to String). */
    private Hashtable properties = new Hashtable();

    /**
     * Map of "user" properties (as created in the Ant task, for example).
     * Note that these key/value pairs are also always put into the
     * project properties, so only the project properties need to be queried.
     * Mapping is String to String.
     */
    private Hashtable userProperties = new Hashtable();

    /**
     * Map of inherited "user" properties - that are those "user"
     * properties that have been created by tasks and not been set
     * from the command line or a GUI tool.
     * Mapping is String to String.
     */
    private Hashtable inheritedProperties = new Hashtable();

    /**
     * Default constructor.
     */
    protected PropertyHelper() {
    }

    //override facility for subclasses to put custom hashtables in


    // --------------------  Hook management  --------------------

    /**
     * Set the project for which this helper is performing property resolution
     *
     * @param p the project instance.
     */
    public void setProject(Project p) {
        this.project = p;
    }

    /** There are 2 ways to hook into property handling:
     *  - you can replace the main PropertyHelper. The replacement is required
     * to support the same semantics (of course :-)
     *
     *  - you can chain a property helper capable of storing some properties.
     *  Again, you are required to respect the immutability semantics (at
     *  least for non-dynamic properties)
     *
     * @param next the next property helper in the chain.
     */
    public void setNext(PropertyHelper next) {
        this.next = next;
    }

    /**
     * Get the next property helper in the chain.
     *
     * @return the next property helper.
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
     * @param project the project fro which the property helper is required.
     *
     * @return the project's property helper.
     */
    public static synchronized
        PropertyHelper getPropertyHelper(Project project) {
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
     */
    public boolean setPropertyHook(String ns, String name,
                                   Object value,
                                   boolean inherited, boolean user,
                                   boolean isNew) {
        if (getNext() != null) {
            boolean subst = getNext().setPropertyHook(ns, name, value,
                    inherited, user, isNew);
            // If next has handled the property
            if (subst) {
                return true;
            }
        }
        return false;
    }

    /** Get a property. If all hooks return null, the default
     * tables will be used.
     *
     * @param ns namespace of the sought property.
     * @param name name of the sought property.
     * @param user True if this is a user property.
     * @return The property, if returned by a hook, or null if none.
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
     */
    public void parsePropertyString(String value, Vector fragments,
                                    Vector propertyRefs)
        throws BuildException {
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
    public String replaceProperties(String ns, String value, Hashtable keys)
            throws BuildException {
        if (value == null || value.indexOf('$') == -1) {
            return value;
        }
        Vector fragments = new Vector();
        Vector propertyRefs = new Vector();
        parsePropertyString(value, fragments, propertyRefs);

        StringBuffer sb = new StringBuffer();
        Enumeration i = fragments.elements();
        Enumeration j = propertyRefs.elements();

        while (i.hasMoreElements()) {
            String fragment = (String) i.nextElement();
            if (fragment == null) {
                String propertyName = (String) j.nextElement();
                Object replacement = null;

                // try to get it from the project or keys
                // Backward compatibility
                if (keys != null) {
                    replacement = keys.get(propertyName);
                }
                if (replacement == null) {
                    replacement = getProperty(ns, propertyName);
                }

                if (replacement == null) {
                    project.log("Property \"" + propertyName
                            + "\" has not been set", Project.MSG_VERBOSE);
                }
                fragment = (replacement != null)
                        ? replacement.toString()
                        : "${" + propertyName + "}";
            }
            sb.append(fragment);
        }
        return sb.toString();
    }

    // -------------------- Default implementation  --------------------
    // Methods used to support the default behavior and provide backward
    // compatibility. Some will be deprecated, you should avoid calling them.


    /** Default implementation of setProperty. Will be called from Project.
     *  This is the original 1.5 implementation, with calls to the hook
     *  added.
     *  @param ns      The namespace for the property (currently not used).
     *  @param name    The name of the property.
     *  @param value   The value to set the property to.
     *  @param verbose If this is true output extra log messages.
     *  @return true if the property is set.
     */
    public synchronized boolean setProperty(String ns, String name,
                                            Object value, boolean verbose) {
        // user (CLI) properties take precedence
        if (null != userProperties.get(name)) {
            if (verbose) {
                project.log("Override ignored for user property \"" + name
                    + "\"", Project.MSG_VERBOSE);
            }
            return false;
        }

        boolean done = setPropertyHook(ns, name, value, false, false, false);
        if (done) {
            return true;
        }

        if (null != properties.get(name) && verbose) {
            project.log("Overriding previous definition of property \"" + name
                + "\"", Project.MSG_VERBOSE);
        }

        if (verbose) {
            project.log("Setting project property: " + name + " -> "
                + value, Project.MSG_DEBUG);
        }
        properties.put(name, value);
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
     */
    public synchronized void setNewProperty(String ns, String name,
                                            Object value) {
        if (null != properties.get(name)) {
            project.log("Override ignored for property \"" + name
                + "\"", Project.MSG_VERBOSE);
            return;
        }

        boolean done = setPropertyHook(ns, name, value, false, false, true);
        if (done) {
            return;
        }

        project.log("Setting project property: " + name + " -> "
            + value, Project.MSG_DEBUG);
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
     */
    public synchronized void setUserProperty(String ns, String name,
                                             Object value) {
        project.log("Setting ro project property: " + name + " -> "
            + value, Project.MSG_DEBUG);
        userProperties.put(name, value);

        boolean done = setPropertyHook(ns, name, value, false, true, false);
        if (done) {
            return;
        }
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
     */
    public synchronized void setInheritedProperty(String ns, String name,
                                                  Object value) {
        inheritedProperties.put(name, value);

        project.log("Setting ro project property: " + name + " -> "
            + value, Project.MSG_DEBUG);
        userProperties.put(name, value);

        boolean done = setPropertyHook(ns, name, value, true, false, false);
        if (done) {
            return;
        }
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
     */
    public synchronized Object getProperty(String ns, String name) {
        if (name == null) {
            return null;
        }

        Object o = getPropertyHook(ns, name, false);
        if (o != null) {
            return o;
        }

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
     */
    public synchronized Object getUserProperty(String ns, String name) {
        if (name == null) {
            return null;
        }
        Object o = getPropertyHook(ns, name, true);
        if (o != null) {
            return o;
        }
        return  userProperties.get(name);
    }


    // -------------------- Access to property tables  --------------------
    // This is used to support ant call and similar tasks. It should be
    // deprecated, it is possible to use a better (more efficient)
    // mechanism to preserve the context.

    /**
     * Returns a copy of the properties table.
     * @return a hashtable containing all properties
     *         (including user properties).
     */
    public Hashtable getProperties() {
        return new Hashtable(properties);
        // There is a better way to save the context. This shouldn't
        // delegate to next, it's for backward compatibility only.
    }

    /**
     * Returns a copy of the user property hashtable
     * @return a hashtable containing just the user properties
     */
    public Hashtable getUserProperties() {
        return new Hashtable(userProperties);
    }

    /**
     * special back door for subclasses, internal access to
     * the hashtables
     * @return the live hashtable of all properties
     */
    protected Hashtable getInternalProperties() {
        return properties;
    }

    /**
     * special back door for subclasses, internal access to
     * the hashtables
     *
     * @return the live hashtable of user properties
     */
    protected Hashtable getInternalUserProperties() {
        return userProperties;
    }

    /**
     * special back door for subclasses, internal access to
     * the hashtables
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

    // -------------------- Property parsing  --------------------
    // Moved from ProjectHelper. You can override the static method -
    // this is used for backward compatibility (for code that calls
    // the parse method in ProjectHelper).

    /** Default parsing method. It is here only to support backward compatibility
     * for the static ProjectHelper.parsePropertyString().
     */
    static void parsePropertyStringDefault(String value, Vector fragments,
                                    Vector propertyRefs)
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
                    throw new BuildException("Syntax error in property: "
                                                 + value);
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

}
