/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2003 The Apache Software Foundation.  All rights
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
 * 4. The names "Ant" and "Apache Software
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

package org.apache.tools.ant.types;

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Properties;
import java.util.Stack;
import java.util.Vector;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.util.FileNameMapper;
import org.apache.tools.ant.util.regexp.RegexpMatcher;
import org.apache.tools.ant.util.regexp.RegexpMatcherFactory;

/**
 * A set of properties.
 *
 * @author <a href="mailto:ddevienne@lgc.com">Dominique Devienne</a>
 * @since Ant 1.6
 */
public class PropertySet extends DataType {

    private boolean dynamic = true;
    private Vector cachedNames;
    private Vector ptyRefs = new Vector();
    private Vector setRefs = new Vector();
    private Mapper _mapper;

    public static class PropertyRef {

        private int count;
        private String name;
        private String regex;
        private String prefix;
        private String builtin;

        public void setName(String name) {
            assertValid("name", name);
            this.name = name;
        }

        public void setRegex(String regex) {
            assertValid("regex", regex);
            this.regex = regex;
        }

        public void setPrefix(String prefix) {
            assertValid("prefix", prefix);
            this.prefix = prefix;
        }

        public void setBuiltin(BuiltinPropertySetName b) {
            String builtin = b.getValue();
            assertValid("builtin", builtin);
            this.builtin = builtin;
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

        public String toString() {
            return "name=" + name + ", regex=" + regex + ", prefix=" + prefix
                + ", builtin=" + builtin;
        }

    }

    public void appendName(String name) {
        PropertyRef ref = new PropertyRef();
        ref.setName(name);
        addPropertyref(ref);
    }

    public void appendRegex(String regex) {
        PropertyRef ref = new PropertyRef();
        ref.setRegex(regex);
        addPropertyref(ref);
    }

    public void appendPrefix(String prefix) {
        PropertyRef ref = new PropertyRef();
        ref.setPrefix(prefix);
        addPropertyref(ref);
    }

    public void appendBuiltin(BuiltinPropertySetName b) {
        PropertyRef ref = new PropertyRef();
        ref.setBuiltin(b);
        addPropertyref(ref);
    }

    public void setMapper(String type, String from, String to) {
        Mapper mapper = createMapper();
        Mapper.MapperType mapperType = new Mapper.MapperType();
        mapperType.setValue(type);
        mapper.setFrom(from);
        mapper.setTo(to);
    }

    public void addPropertyref(PropertyRef ref) {
        assertNotReference();
        ptyRefs.addElement(ref);
    }

    public void addPropertyset(PropertySet ref) {
        assertNotReference();
        setRefs.addElement(ref);
    }

    public Mapper createMapper() {
        assertNotReference();
        if (_mapper != null) {
            throw new BuildException("Too many <mapper>s!");
        }
        _mapper = new Mapper(getProject());
        return _mapper;
    }

    public void setDynamic(boolean dynamic) {
        assertNotReference();
        this.dynamic = dynamic;
    }

    public boolean getDynamic() {
        return isReference() ? getRef().dynamic : dynamic;
    }

    public Mapper getMapper() {
        return isReference() ? getRef()._mapper : _mapper;
    }

    public Properties getProperties() {
        Vector names = null;
        Project prj = getProject();

        if (getDynamic() || cachedNames == null) {
            names = new Vector(); // :TODO: should be a Set!
            if (isReference()) {
                getRef().addPropertyNames(names, prj.getProperties());
            } else {
                addPropertyNames(names, prj.getProperties());
            }

            if (!getDynamic()) {
                cachedNames = names;
            }
        } else {
            names = cachedNames;
        }

        FileNameMapper mapper = null;
        Mapper myMapper = getMapper();
        if (myMapper != null) {
            mapper = myMapper.getImplementation();
        }
        Properties properties = new Properties();
        for (Enumeration e = names.elements(); e.hasMoreElements();) {
            String name = (String) e.nextElement();
            String value = prj.getProperty(name);
            if (mapper != null) {
                String[] newname = mapper.mapFileName(name);
                if (newname != null) {
                    name = newname[0];
                }
            }
            properties.setProperty(name, value);
        }
        return properties;
    }

    /**
     * @param  names the output vector to fill with the property names
     *         matching this PropertySet selection criteria.
     * @param  properties the current Project properties, passed in to
     *         avoid needless duplication of the Hashtable during recursion.
     */
    private void addPropertyNames(Vector names, Hashtable properties) {
        Project prj = getProject();

        // Add this PropertySet's property names.
        for (Enumeration e = ptyRefs.elements(); e.hasMoreElements();) {
            PropertyRef ref = (PropertyRef) e.nextElement();
            if (ref.name != null) {
                if (prj.getProperty(ref.name) != null) {
                    names.addElement(ref.name);
                }
            } else if (ref.prefix != null) {
                for (Enumeration p = properties.keys(); p.hasMoreElements();) {
                    String name = (String) p.nextElement();
                    if (name.startsWith(ref.prefix)) {
                        names.addElement(name);
                    }
                }
            } else if (ref.regex != null) {
                RegexpMatcherFactory matchMaker = new RegexpMatcherFactory();
                RegexpMatcher matcher = matchMaker.newRegexpMatcher();
                matcher.setPattern(ref.regex);
                for (Enumeration p = properties.keys(); p.hasMoreElements();) {
                    String name = (String) p.nextElement();
                    if (matcher.matches(name)) {
                        names.addElement(name);
                    }
                }
            } else if (ref.builtin != null) {

                Enumeration enum = null;
                if (ref.builtin.equals(BuiltinPropertySetName.ALL)) {
                    enum = properties.keys();
                } else if (ref.builtin.equals(BuiltinPropertySetName.SYSTEM)) {
                    enum = System.getProperties().keys();
                } else if (ref.builtin.equals(BuiltinPropertySetName
                                              .COMMANDLINE)) {
                    enum = getProject().getUserProperties().keys();
                } else {
                    throw new BuildException("Impossible: Invalid builtin "
                                             + "attribute!");
                }

                while (enum.hasMoreElements()) {
                    names.addElement(enum.nextElement());
                }

            } else {
                throw new BuildException("Impossible: Invalid PropertyRef!");
            }
        }

        // Add this PropertySet's nested PropertySets' property names.
        for (Enumeration e = setRefs.elements(); e.hasMoreElements();) {
            PropertySet set = (PropertySet) e.nextElement();
            set.addPropertyNames(names, properties);
        }
    }

    /**
     * Performs the check for circular references and returns the
     * referenced FileList.
     */
    protected PropertySet getRef() {
        if (!isChecked()) {
            Stack stk = new Stack();
            stk.push(this);
            dieOnCircularReference(stk, getProject());
        }

        Object o = getRefid().getReferencedObject(getProject());
        if (!(o instanceof PropertySet)) {
            String msg = getRefid().getRefId()
                + " doesn\'t denote a propertyset";
            throw new BuildException(msg);
        } else {
            return (PropertySet) o;
        }
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
    private boolean noAttributeSet = true;

    /**
     * Used for propertyref's builtin attribute.
     */
    public static class BuiltinPropertySetName extends EnumeratedAttribute {
        static final String ALL = "all";
        static final String SYSTEM = "system";
        static final String COMMANDLINE = "commandline";
        public String[] getValues() {
            return new String[] {ALL, SYSTEM, COMMANDLINE};
        }
    }
} // END class PropertySet

