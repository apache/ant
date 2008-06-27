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

package org.apache.tools.ant.types;

import java.util.Enumeration;
import java.util.Iterator;
import java.util.Map;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeMap;
import java.util.Hashtable;
import java.util.Properties;
import java.util.Vector;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.types.resources.PropertyResource;
import org.apache.tools.ant.util.FileNameMapper;
import org.apache.tools.ant.util.regexp.RegexpMatcher;
import org.apache.tools.ant.util.regexp.RegexpMatcherFactory;

/**
 * A set of properties.
 *
 * @since Ant 1.6
 */
public class PropertySet extends DataType implements ResourceCollection {

    private boolean dynamic = true;
    private boolean negate = false;
    private Set cachedNames;
    private Vector ptyRefs = new Vector();
    private Vector setRefs = new Vector();
    private Mapper mapper;

    /**
     * This is a nested class containing a reference to some properties
     * and optionally a source of properties.
     */
    public static class PropertyRef {

        private int count;
        private String name;
        private String regex;
        private String prefix;
        private String builtin;

        /**
         * Set the name.
         * @param name a <code>String</code> value.
         */
        public void setName(String name) {
            assertValid("name", name);
            this.name = name;
        }

        /**
         * Set the regular expression to use to filter the properties.
         * @param regex a regular expression.
         */
        public void setRegex(String regex) {
            assertValid("regex", regex);
            this.regex = regex;
        }

        /**
         * Set the prefix to use.
         * @param prefix a <code>String</code> value.
         */
        public void setPrefix(String prefix) {
            assertValid("prefix", prefix);
            this.prefix = prefix;
        }

        /**
         * Builtin property names - all, system or commandline.
         * @param b an enumerated <code>BuildinPropertySetName</code> value.
         */
        public void setBuiltin(BuiltinPropertySetName b) {
            String pBuiltIn = b.getValue();
            assertValid("builtin", pBuiltIn);
            this.builtin = pBuiltIn;
        }

        private void assertValid(String attr, String value) {
            if (value == null || value.length() < 1) {
                throw new BuildException("Invalid attribute: " + attr);
            }

            if (++count != 1) {
                throw new BuildException("Attributes name, regex, and "
                    + "prefix are mutually exclusive");
            }
        }

        /**
         * A debug toString().
         * @return a string version of this object.
         */
        public String toString() {
            return "name=" + name + ", regex=" + regex + ", prefix=" + prefix
                + ", builtin=" + builtin;
        }

    } //end nested class

    /**
     * Allow properties of a particular name in the set.
     * @param name the property name to allow.
     */
    public void appendName(String name) {
        PropertyRef r = new PropertyRef();
        r.setName(name);
        addPropertyref(r);
    }

    /**
     * Allow properties whose names match a regex in the set.
     * @param regex the regular expression to use.
     */
    public void appendRegex(String regex) {
        PropertyRef r = new PropertyRef();
        r.setRegex(regex);
        addPropertyref(r);
    }

    /**
     * Allow properties whose names start with a prefix in the set.
     * @param prefix the prefix to use.
     */
    public void appendPrefix(String prefix) {
        PropertyRef r = new PropertyRef();
        r.setPrefix(prefix);
        addPropertyref(r);
    }

    /**
     * Allow builtin (all, system or commandline) properties in the set.
     * @param b the type of builtin properties.
     */
    public void appendBuiltin(BuiltinPropertySetName b) {
        PropertyRef r = new PropertyRef();
        r.setBuiltin(b);
        addPropertyref(r);
    }

    /**
     * Set a mapper to change property names.
     * @param type mapper type.
     * @param from source pattern.
     * @param to output pattern.
     */
    public void setMapper(String type, String from, String to) {
        Mapper m = createMapper();
        Mapper.MapperType mapperType = new Mapper.MapperType();
        mapperType.setValue(type);
        m.setType(mapperType);
        m.setFrom(from);
        m.setTo(to);
    }

    /**
     * Add a property reference (nested element) to the references to be used.
     * @param ref a property reference.
     */
    public void addPropertyref(PropertyRef ref) {
        assertNotReference();
        ptyRefs.addElement(ref);
    }

    /**
     * Add another property set to this set.
     * @param ref another property set.
     */
    public void addPropertyset(PropertySet ref) {
        assertNotReference();
        setRefs.addElement(ref);
    }

    /**
     * Create a mapper to map the property names.
     * @return a mapper to be configured.
     */
    public Mapper createMapper() {
        assertNotReference();
        if (mapper != null) {
            throw new BuildException("Too many <mapper>s!");
        }
        mapper = new Mapper(getProject());
        return mapper;
    }

    /**
     * Add a nested FileNameMapper.
     * @param fileNameMapper the mapper to add.
     * @since Ant 1.6.3
     */
    public void add(FileNameMapper fileNameMapper) {
        createMapper().add(fileNameMapper);
    }

    /**
     * Set whether to reevaluate the set everytime the set is used.
     * Default is true.
     *
     * @param dynamic if true, reevaluate the property set each time
     *                the set is used. if false cache the property set
     *                the first time and use the cached set on subsequent
     *                occasions.
     */
    public void setDynamic(boolean dynamic) {
        assertNotReference();
        this.dynamic = dynamic;
    }

    /**
     * Set whether to negate results.
     * If "true", all properties not selected by nested elements will be returned.
     *  Default is "false".
     * @param negate if true, negate the selection criteria.
     */
    public void setNegate(boolean negate) {
        assertNotReference();
        this.negate = negate;
    }

    /**
     * Get the dynamic attribute.
     * @return true if the property set is to be evalulated each time it is used.
     */
    public boolean getDynamic() {
        return isReference() ? getRef().dynamic : dynamic;
    }

    /**
     * Get the mapper attribute.
     * @return the mapper attribute.
     */
    public Mapper getMapper() {
        return isReference() ? getRef().mapper : mapper;
    }

    /**
     * Convert the system properties to a hashtable.
     * Use propertynames to get the list of properties (including
     * default ones).
     */
    private Hashtable getAllSystemProperties() {
        Hashtable ret = new Hashtable();
        for (Enumeration e = System.getProperties().propertyNames();
             e.hasMoreElements();) {
            String name = (String) e.nextElement();
            ret.put(name, System.getProperties().getProperty(name));
        }
        return ret;
    }

    /**
     * This is the operation to get the existing or recalculated properties.
     * @return the properties for this propertyset.
     */
    public Properties getProperties() {
        if (isReference()) {
            return getRef().getProperties();
        }
        Set names = null;
        Project prj = getProject();
        Hashtable props =
            prj == null ? getAllSystemProperties() : prj.getProperties();

        //quick & dirty, to make nested mapped p-sets work:
        for (Enumeration e = setRefs.elements(); e.hasMoreElements();) {
            PropertySet set = (PropertySet) e.nextElement();
            props.putAll(set.getProperties());
        }

        if (getDynamic() || cachedNames == null) {
            names = new HashSet();
            addPropertyNames(names, props);
            // Add this PropertySet's nested PropertySets' property names.
            for (Enumeration e = setRefs.elements(); e.hasMoreElements();) {
                PropertySet set = (PropertySet) e.nextElement();
                names.addAll(set.getProperties().keySet());
            }
            if (negate) {
                //make a copy...
                HashSet complement = new HashSet(props.keySet());
                complement.removeAll(names);
                names = complement;
            }
            if (!getDynamic()) {
                cachedNames = names;
            }
        } else {
            names = cachedNames;
        }
        FileNameMapper m = null;
        Mapper myMapper = getMapper();
        if (myMapper != null) {
            m = myMapper.getImplementation();
        }
        Properties properties = new Properties();
        //iterate through the names, get the matching values
        for (Iterator iter = names.iterator(); iter.hasNext();) {
            String name = (String) iter.next();
            String value = (String) props.get(name);
            if (value != null) {
                // may be null if a system property has been added
                // after the project instance has been initialized
                if (m != null) {
                    //map the names
                    String[] newname = m.mapFileName(name);
                    if (newname != null) {
                        name = newname[0];
                    }
                }
                properties.setProperty(name, value);
            }
        }
        return properties;
    }

    /**
     * @param  names the output Set to fill with the property names
     *         matching this PropertySet selection criteria.
     * @param  properties the current Project properties, passed in to
     *         avoid needless duplication of the Hashtable during recursion.
     */
    private void addPropertyNames(Set names, Hashtable properties) {
        // Add this PropertySet's property names.
        for (Enumeration e = ptyRefs.elements(); e.hasMoreElements();) {
            PropertyRef r = (PropertyRef) e.nextElement();
            if (r.name != null) {
                if (properties.get(r.name) != null) {
                    names.add(r.name);
                }
            } else if (r.prefix != null) {
                for (Enumeration p = properties.keys(); p.hasMoreElements();) {
                    String name = (String) p.nextElement();
                    if (name.startsWith(r.prefix)) {
                        names.add(name);
                    }
                }
            } else if (r.regex != null) {
                RegexpMatcherFactory matchMaker = new RegexpMatcherFactory();
                RegexpMatcher matcher = matchMaker.newRegexpMatcher();
                matcher.setPattern(r.regex);
                for (Enumeration p = properties.keys(); p.hasMoreElements();) {
                    String name = (String) p.nextElement();
                    if (matcher.matches(name)) {
                        names.add(name);
                    }
                }
            } else if (r.builtin != null) {

                if (r.builtin.equals(BuiltinPropertySetName.ALL)) {
                    names.addAll(properties.keySet());
                } else if (r.builtin.equals(BuiltinPropertySetName.SYSTEM)) {
                    names.addAll(System.getProperties().keySet());
                } else if (r.builtin.equals(BuiltinPropertySetName
                                              .COMMANDLINE)) {
                    names.addAll(getProject().getUserProperties().keySet());
                } else {
                    throw new BuildException("Impossible: Invalid builtin "
                                             + "attribute!");
                }
            } else {
                throw new BuildException("Impossible: Invalid PropertyRef!");
            }
        }
    }

    /**
     * Performs the check for circular references and returns the
     * referenced PropertySet.
     * @return the referenced PropertySet.
     */
    protected PropertySet getRef() {
        return (PropertySet) getCheckedRef(PropertySet.class, "propertyset");
    }

    /**
     * Sets the value of the refid attribute.
     *
     * @param  r the reference this datatype should point to.
     * @throws BuildException if another attribute was set, since
     *         refid and all other attributes are mutually exclusive.
     */
    public final void setRefid(Reference r) {
        if (!noAttributeSet) {
            throw tooManyAttributes();
        }
        super.setRefid(r);
    }

    /**
     * Ensures this data type is not a reference.
     *
     * <p>Calling this method as the first line of every bean method of
     * this data type (setXyz, addXyz, createXyz) ensure proper handling
     * of the refid attribute.</p>
     *
     * @throws BuildException if the refid attribute was already set, since
     *         refid and all other attributes are mutually exclusive.
     */
    protected final void assertNotReference() {
        if (isReference()) {
            throw tooManyAttributes();
        }
        noAttributeSet = false;
    }

    /**
     * Flag which tracks whether any attribute has been set; used by
     * {@link #assertNotReference()} and {@link #setRefid(Reference)}.
     */
    private boolean noAttributeSet = true;

    /**
     * Used for propertyref's builtin attribute.
     */
    public static class BuiltinPropertySetName extends EnumeratedAttribute {
        static final String ALL = "all";
        static final String SYSTEM = "system";
        static final String COMMANDLINE = "commandline";
        /** {@inheritDoc}. */
        public String[] getValues() {
            return new String[] {ALL, SYSTEM, COMMANDLINE};
        }
    }

    /**
     * A debug toString.
     * This gets a comma separated list of key=value pairs for
     * the properties in the set.
     * The output order is sorted according to the keys' <i>natural order</i>.
     * @return a string rep of this object.
     */
    public String toString() {
        StringBuffer b = new StringBuffer();
        TreeMap sorted = new TreeMap(getProperties());
        for (Iterator i = sorted.entrySet().iterator(); i.hasNext();) {
            Map.Entry e = (Map.Entry) i.next();
            if (b.length() != 0) {
                b.append(", ");
            }
            b.append(e.getKey().toString());
            b.append("=");
            b.append(e.getValue().toString());
        }
        return b.toString();
    }

    /**
     * Fulfill the ResourceCollection interface.
     * @return an Iterator of Resources.
     * @since Ant 1.7
     */
    public Iterator iterator() {
        final Enumeration e = getProperties().propertyNames();
        return new Iterator() {
            public boolean hasNext() {
                return e.hasMoreElements();
            }
            public Object next() {
                return new PropertyResource(getProject(), (String) e.nextElement());
            }
            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }

    /**
     * Fulfill the ResourceCollection contract.
     * @return the size of this ResourceCollection.
     */
    public int size() {
        return isReference() ? getRef().size() : getProperties().size();
    }

    /**
     * Fulfill the ResourceCollection contract.
     * @return whether this is a filesystem-only resource collection.
     */
    public boolean isFilesystemOnly() {
        return isReference() && getRef().isFilesystemOnly();
    }

}
