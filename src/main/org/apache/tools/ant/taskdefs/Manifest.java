/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2001-2003 The Apache Software Foundation.  All rights
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

package org.apache.tools.ant.taskdefs;

import java.util.Vector;
import java.util.Hashtable;
import java.util.Enumeration;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.util.CollectionUtils;

/**
 * Holds the data of a jar manifest.
 *
 * Manifests are processed according to the
 * {@link <a href="http://java.sun.com/j2se/1.4/docs/guide/jar/jar.html">Jar
 * file specification.</a>}.
 * Specifically, a manifest element consists of
 * a set of attributes and sections. These sections in turn may contain
 * attributes. Note in particular that this may result in manifest lines
 * greater than 72 bytes being wrapped and continued on the next
 * line. If an application can not handle the continuation mechanism, it
 * is a defect in the application, not this task.

 * @author Conor MacNeill
 * @author <a href="mailto:stefan.bodewig@epost.de">Stefan Bodewig</a>
 * @author <a href="mailto:j_a_fernandez@yahoo.com">Jose Alberto Fernandez</a>
 *
 * @since Ant 1.4
 */
public class Manifest {
    /** The standard manifest version header */
    public static final String ATTRIBUTE_MANIFEST_VERSION
        = "Manifest-Version";

    /** The standard Signature Version header */
    public static final String ATTRIBUTE_SIGNATURE_VERSION
        = "Signature-Version";

    /** The Name Attribute is the first in a named section */
    public static final String ATTRIBUTE_NAME = "Name";

    /** The From Header is disallowed in a Manifest */
    public static final String ATTRIBUTE_FROM = "From";

    /** The Class-Path Header is special - it can be duplicated */
    public static final String ATTRIBUTE_CLASSPATH = "class-path";

    /** Default Manifest version if one is not specified */
    public static final  String DEFAULT_MANIFEST_VERSION = "1.0";

    /** The max length of a line in a Manifest */
    public static final int MAX_LINE_LENGTH = 72;

    /**
     * Max length of a line section which is continued. Need to allow
     * for the CRLF.
     */
    public static final int MAX_SECTION_LENGTH = MAX_LINE_LENGTH - 2;

    /** The End-Of-Line marker in manifests */
    public static final String EOL = "\r\n";

    /**
     * An attribute for the manifest.
     * Those attributes that are not nested into a section will be added to the "Main" section.
     */
    public static class Attribute {
        /** The attribute's name */
        private String name = null;

        /** The attribute's value */
        private Vector values = new Vector();

        /**
         * For multivalued attributes, this is the index of the attribute
         * currently being defined.
         */
        private int currentIndex = 0;

        /**
         * Construct an empty attribute */
        public Attribute() {
        }

        /**
         * Construct an attribute by parsing a line from the Manifest
         *
         * @param line the line containing the attribute name and value
         *
         * @throws ManifestException if the line is not valid
         */
        public Attribute(String line) throws ManifestException {
            parse(line);
        }

        /**
         * Construct a manifest by specifying its name and value
         *
         * @param name the attribute's name
         * @param value the Attribute's value
         */
        public Attribute(String name, String value) {
            this.name = name;
            setValue(value);
        }

        /**
         * @see java.lang.Object#hashCode
         */
        public int hashCode() {
            int hashCode = 0;

            if (name != null) {
                hashCode += name.hashCode();
            }

            hashCode += values.hashCode();
            return hashCode;
        }

        /**
         * @see java.lang.Object#equals
         */
        public boolean equals(Object rhs) {
            if (rhs == null || rhs.getClass() != getClass()) {
                return false;
            }

            if (rhs == this) {
                return true;
            }

            Attribute rhsAttribute = (Attribute) rhs;
            String lhsKey = getKey();
            String rhsKey = rhsAttribute.getKey();
            if ((lhsKey == null && rhsKey != null)
                 || (lhsKey != null && rhsKey == null)
                 || !lhsKey.equals(rhsKey)) {
                return false;
            }

            return CollectionUtils.equals(values, rhsAttribute.values);
        }

        /**
         * Parse a line into name and value pairs
         *
         * @param line the line to be parsed
         *
         * @throws ManifestException if the line does not contain a colon
         * separating the name and value
         */
        public void parse(String line) throws ManifestException {
            int index = line.indexOf(": ");
            if (index == -1) {
                throw new ManifestException("Manifest line \"" + line
                    + "\" is not valid as it does not "
                    + "contain a name and a value separated by ': ' ");
            }
            name = line.substring(0, index);
            setValue(line.substring(index + 2));
        }

        /**
         * Set the Attribute's name; required
         *
         * @param name the attribute's name
         */
        public void setName(String name) {
            this.name = name;
        }

        /**
         * Get the Attribute's name
         *
         * @return the attribute's name.
         */
        public String getName() {
            return name;
        }

        /**
         * Get the attribute's Key - its name in lower case.
         *
         * @return the attribute's key.
         */
        public String getKey() {
            if (name == null) {
                return null;
            }
            return name.toLowerCase();
        }

        /**
         * Set the Attribute's value; required
         *
         * @param value the attribute's value
         */
        public void setValue(String value) {
            if (currentIndex >= values.size()) {
                values.addElement(value);
                currentIndex = values.size() - 1;
            } else {
                values.setElementAt(value, currentIndex);
            }
        }

        /**
         * Get the Attribute's value.
         *
         * @return the attribute's value.
         */
        public String getValue() {
            if (values.size() == 0) {
                return null;
            }

            String fullValue = "";
            for (Enumeration e = getValues(); e.hasMoreElements();) {
                String value = (String) e.nextElement();
                fullValue += value + " ";
            }
            return fullValue.trim();
        }

        /**
         * Add a new value to this attribute - making it multivalued.
         *
         * @param value the attribute's additional value
         */
        public void addValue(String value) {
            currentIndex++;
            setValue(value);
        }

        /**
         * Get all the attribute's values.
         *
         * @return an enumeration of the attributes values
         */
        public Enumeration getValues() {
            return values.elements();
        }

        /**
         * Add a continuation line from the Manifest file.
         *
         * When lines are too long in a manifest, they are continued on the
         * next line by starting with a space. This method adds the continuation
         * data to the attribute value by skipping the first character.
         *
         * @param line the continuation line.
         */
        public void addContinuation(String line) {
            String currentValue = (String) values.elementAt(currentIndex);
            setValue(currentValue + line.substring(1));
        }

        /**
         * Write the attribute out to a print writer.
         *
         * @param writer the Writer to which the attribute is written
         *
         * @throws IOException if the attribte value cannot be written
         */
        public void write(PrintWriter writer) throws IOException {
            for (Enumeration e = getValues(); e.hasMoreElements();) {
                writeValue(writer, (String) e.nextElement());
            }
        }

        /**
         * Write a single attribute value out
         *
         * @param writer the Writer to which the attribute is written
         * @param value the attribute value
         *
         * @throws IOException if the attribte value cannot be written
         */
        private void writeValue(PrintWriter writer, String value)
             throws IOException {
            String line = name + ": " + value;
            while (line.getBytes().length > MAX_LINE_LENGTH) {
                // try to find a MAX_LINE_LENGTH byte section
                int breakIndex = MAX_SECTION_LENGTH;
                String section = line.substring(0, breakIndex);
                while (section.getBytes().length > MAX_SECTION_LENGTH
                     && breakIndex > 0) {
                    breakIndex--;
                    section = line.substring(0, breakIndex);
                }
                if (breakIndex == 0) {
                    throw new IOException("Unable to write manifest line "
                        + name + ": " + value);
                }
                writer.print(section + EOL);
                line = " " + line.substring(breakIndex);
            }
            writer.print(line + EOL);
        }
    }

    /**
     * A manifest section - you can nest attribute elements into sections.
     * A section consists of a set of attribute values,
     * separated from other sections by a blank line.
     */
    public static class Section {
        /** Warnings for this section */
        private Vector warnings = new Vector();

        /**
         * The section's name if any. The main section in a
         * manifest is unnamed.
         */
        private String name = null;

        /** The section's attributes.*/
        private Hashtable attributes = new Hashtable();

        /** Index used to retain the attribute ordering */
        private Vector attributeIndex = new Vector();

        /**
         * The name of the section; optional -default is the main section.
         * @param name the section's name
         */
        public void setName(String name) {
            this.name = name;
        }

        /**
         * Get the Section's name.
         *
         * @return the section's name.
         */
        public String getName() {
            return name;
        }

        /**
         * Read a section through a reader.
         *
         * @param reader the reader from which the section is read
         *
         * @return the name of the next section if it has been read as
         *         part of this section - This only happens if the
         *         Manifest is malformed.
         *
         * @throws ManifestException if the section is not valid according
         *         to the JAR spec
         * @throws IOException if the section cannot be read from the reader.
         */
        public String read(BufferedReader reader)
             throws ManifestException, IOException {
            Attribute attribute = null;
            while (true) {
                String line = reader.readLine();
                if (line == null || line.length() == 0) {
                    return null;
                }
                if (line.charAt(0) == ' ') {
                    // continuation line
                    if (attribute == null) {
                        if (name != null) {
                            // a continuation on the first line is a
                            // continuation of the name - concatenate this
                            // line and the name
                            name += line.substring(1);
                        } else {
                            throw new ManifestException("Can't start an "
                                + "attribute with a continuation line " + line);
                        }
                    } else {
                        attribute.addContinuation(line);
                    }
                } else {
                    attribute = new Attribute(line);
                    String nameReadAhead = addAttributeAndCheck(attribute);
                    if (nameReadAhead != null) {
                        return nameReadAhead;
                    }
                }
            }
        }

        /**
         * Merge in another section
         *
         * @param section the section to be merged with this one.
         *
         * @throws ManifestException if the sections cannot be merged.
         */
        public void merge(Section section) throws ManifestException {
            if (name == null && section.getName() != null
                || name != null
                && !(name.equalsIgnoreCase(section.getName()))) {
                throw new ManifestException("Unable to merge sections "
                    + "with different names");
            }

            Enumeration e = section.getAttributeKeys();
            while (e.hasMoreElements()) {
                String attributeName = (String) e.nextElement();
                Attribute attribute = section.getAttribute(attributeName);
                if (attributeName.equals(ATTRIBUTE_CLASSPATH) &&
                        attributes.containsKey(attributeName)) {
                    Attribute ourClassPath = getAttribute(attributeName);
                    Enumeration cpe = attribute.getValues();
                    while (cpe.hasMoreElements()) {
                        String value = (String) cpe.nextElement();
                        ourClassPath.addValue(value);
                    }
                } else {
                    // the merge file always wins
                    storeAttribute(attribute);
                }
            }

            // add in the warnings
            Enumeration warnEnum = section.warnings.elements();
            while (warnEnum.hasMoreElements()) {
                warnings.addElement(warnEnum.nextElement());
            }
        }

        /**
         * Write the section out to a print writer.
         *
         * @param writer the Writer to which the section is written
         *
         * @throws IOException if the section cannot be written
         */
        public void write(PrintWriter writer) throws IOException {
            if (name != null) {
                Attribute nameAttr = new Attribute(ATTRIBUTE_NAME, name);
                nameAttr.write(writer);
            }
            Enumeration e = getAttributeKeys();
            while (e.hasMoreElements()) {
                String key = (String) e.nextElement();
                Attribute attribute = getAttribute(key);
                attribute.write(writer);
            }
            writer.print(EOL);
        }

        /**
         * Get a attribute of the section
         *
         * @param attributeName the name of the attribute
         * @return a Manifest.Attribute instance if the attribute is
         *         single-valued, otherwise a Vector of Manifest.Attribute
         *         instances.
         */
        public Attribute getAttribute(String attributeName) {
            return (Attribute) attributes.get(attributeName.toLowerCase());
        }

        /**
         * Get the attribute keys.
         *
         * @return an Enumeration of Strings, each string being the lower case
         *         key of an attribute of the section.
         */
        public Enumeration getAttributeKeys() {
            return attributeIndex.elements();
        }

        /**
         * Get the value of the attribute with the name given.
         *
         * @param attributeName the name of the attribute to be returned.
         *
         * @return the attribute's value or null if the attribute does not exist
         *         in the section
         */
        public String getAttributeValue(String attributeName) {
            Attribute attribute = getAttribute(attributeName.toLowerCase());
            if (attribute == null) {
                return null;
            }
            return attribute.getValue();
        }

        /**
         * Remove tge given attribute from the section
         *
         * @param attributeName the name of the attribute to be removed.
         */
        public void removeAttribute(String attributeName) {
            String key = attributeName.toLowerCase();
            attributes.remove(key);
            attributeIndex.removeElement(key);
        }

        /**
         * Add an attribute to the section.
         *
         * @param attribute the attribute to be added to the section
         *
         * @exception ManifestException if the attribute is not valid.
         */
        public void addConfiguredAttribute(Attribute attribute)
             throws ManifestException {
            String check = addAttributeAndCheck(attribute);
            if (check != null) {
                throw new BuildException("Specify the section name using "
                    + "the \"name\" attribute of the <section> element rather "
                    + "than using a \"Name\" manifest attribute");
            }
        }

        /**
         * Add an attribute to the section
         *
         * @param attribute the attribute to be added.
         *
         * @return the value of the attribute if it is a name
         *         attribute - null other wise
         *
         * @exception ManifestException if the attribute already
         *            exists in this section.
         */
        public String addAttributeAndCheck(Attribute attribute)
             throws ManifestException {
            if (attribute.getName() == null || attribute.getValue() == null) {
                throw new BuildException("Attributes must have name and value");
            }
            if (attribute.getKey().equalsIgnoreCase(ATTRIBUTE_NAME)) {
                warnings.addElement("\"" + ATTRIBUTE_NAME + "\" attributes "
                    + "should not occur in the main section and must be the "
                    + "first element in all other sections: \""
                    + attribute.getName() + ": " + attribute.getValue() + "\"");
                return attribute.getValue();
            }

            if (attribute.getKey().startsWith(ATTRIBUTE_FROM.toLowerCase())) {
                warnings.addElement("Manifest attributes should not start "
                    + "with \"" + ATTRIBUTE_FROM + "\" in \""
                    + attribute.getName() + ": " + attribute.getValue() + "\"");
            } else {
                // classpath attributes go into a vector
                String attributeKey = attribute.getKey();
                if (attributeKey.equals(ATTRIBUTE_CLASSPATH)) {
                    Attribute classpathAttribute =
                        (Attribute) attributes.get(attributeKey);

                    if (classpathAttribute == null) {
                        storeAttribute(attribute);
                    } else {
                        Enumeration e = attribute.getValues();
                        while (e.hasMoreElements()) {
                            String value = (String) e.nextElement();
                            classpathAttribute.addValue(value);
                        }
                    }
                } else if (attributes.containsKey(attributeKey)) {
                    throw new ManifestException("The attribute \""
                        + attribute.getName() + "\" may not occur more "
                        + "than once in the same section");
                } else {
                    storeAttribute(attribute);
                }
            }
            return null;
        }

        /**
         * Clone this section
         *
         * @since Ant 1.5.2
         */
        public Object clone() {
            Section cloned = new Section();
            cloned.setName(name);
            Enumeration e = getAttributeKeys();
            while (e.hasMoreElements()) {
                String key = (String) e.nextElement();
                Attribute attribute = getAttribute(key);
                cloned.storeAttribute(new Attribute(attribute.getName(), 
                                                    attribute.getValue()));
            }
            return cloned;
        }

        /**
         * Store an attribute and update the index.
         *
         * @param attribute the attribute to be stored
         */
        private void storeAttribute(Attribute attribute) {
            if (attribute == null) {
                return;
            }
            String attributeKey = attribute.getKey();
            attributes.put(attributeKey, attribute);
            if (!attributeIndex.contains(attributeKey)) {
                attributeIndex.addElement(attributeKey);
            }
        }

        /**
         * Get the warnings for this section.
         *
         * @return an Enumeration of warning strings.
         */
        public Enumeration getWarnings() {
            return warnings.elements();
        }

        /**
         * @see java.lang.Object#hashCode
         */
        public int hashCode() {
            int hashCode = 0;

            if (name != null) {
                hashCode += name.hashCode();
            }

            hashCode += attributes.hashCode();
            return hashCode;
        }

        /**
         * @see java.lang.Object#equals
         */
        public boolean equals(Object rhs) {
            if (rhs == null || rhs.getClass() != getClass()) {
                return false;
            }

            if (rhs == this) {
                return true;
            }

            Section rhsSection = (Section) rhs;

            return CollectionUtils.equals(attributes, rhsSection.attributes);
        }
    }


    /** The version of this manifest */
    private String manifestVersion = DEFAULT_MANIFEST_VERSION;

    /** The main section of this manifest */
    private Section mainSection = new Section();

    /** The named sections of this manifest */
    private Hashtable sections = new Hashtable();

    /** Index of sections - used to retain order of sections in manifest */
    private Vector sectionIndex = new Vector();

    /**
     * Construct a manifest from Ant's default manifest file.
     *
     * @return the default manifest.
     * @exception BuildException if there is a problem loading the
     *            default manifest
     */
    public static Manifest getDefaultManifest() throws BuildException {
        try {
            String defManifest = "/org/apache/tools/ant/defaultManifest.mf";
            InputStream in = Manifest.class.getResourceAsStream(defManifest);
            if (in == null) {
                throw new BuildException("Could not find default manifest: "
                    + defManifest);
            }
            try {
                Manifest defaultManifest 
                    = new Manifest(new InputStreamReader(in, "UTF-8"));
                Attribute createdBy = new Attribute("Created-By", 
                    System.getProperty("java.vm.version") + " ("
                    + System.getProperty("java.vm.vendor") + ")" );
                defaultManifest.getMainSection().storeAttribute(createdBy);
                return defaultManifest;
            } catch (UnsupportedEncodingException e) {
                return new Manifest(new InputStreamReader(in));
            }
        } catch (ManifestException e) {
            throw new BuildException("Default manifest is invalid !!", e);
        } catch (IOException e) {
            throw new BuildException("Unable to read default manifest", e);
        }
    }

    /** Construct an empty manifest */
    public Manifest() {
        manifestVersion = null;
    }

    /**
     * Read a manifest file from the given reader
     *
     * @param r is the reader from which the Manifest is read
     *
     * @throws ManifestException if the manifest is not valid according
     *         to the JAR spec
     * @throws IOException if the manifest cannot be read from the reader.
     */
    public Manifest(Reader r) throws ManifestException, IOException {
        BufferedReader reader = new BufferedReader(r);
        // This should be the manifest version
        String nextSectionName = mainSection.read(reader);
        String readManifestVersion
            = mainSection.getAttributeValue(ATTRIBUTE_MANIFEST_VERSION);
        if (readManifestVersion != null) {
            manifestVersion = readManifestVersion;
            mainSection.removeAttribute(ATTRIBUTE_MANIFEST_VERSION);
        }

        String line = null;
        while ((line = reader.readLine()) != null) {
            if (line.length() == 0) {
                continue;
            }

            Section section = new Section();
            if (nextSectionName == null) {
                Attribute sectionName = new Attribute(line);
                if (!sectionName.getName().equalsIgnoreCase(ATTRIBUTE_NAME)) {
                    throw new ManifestException("Manifest sections should "
                        + "start with a \"" + ATTRIBUTE_NAME
                        + "\" attribute and not \""
                        + sectionName.getName() + "\"");
                }
                nextSectionName = sectionName.getValue();
            } else {
                // we have already started reading this section
                // this line is the first attribute. set it and then
                // let the normal read handle the rest
                Attribute firstAttribute = new Attribute(line);
                section.addAttributeAndCheck(firstAttribute);
            }

            section.setName(nextSectionName);
            nextSectionName = section.read(reader);
            addConfiguredSection(section);
        }
    }

    /**
     * Add a section to the manifest
     *
     * @param section the manifest section to be added
     *
     * @exception ManifestException if the secti0on is not valid.
     */
    public void addConfiguredSection(Section section)
         throws ManifestException {
        String sectionName = section.getName();
        if (sectionName == null) {
            throw new BuildException("Sections must have a name");
        }
        sections.put(sectionName, section);
        if (!sectionIndex.contains(sectionName)) {
            sectionIndex.addElement(sectionName);
        }
    }

    /**
     * Add an attribute to the manifest - it is added to the main section.
     *
     * @param attribute the attribute to be added.
     *
     * @exception ManifestException if the attribute is not valid.
     */
    public void addConfiguredAttribute(Attribute attribute)
         throws ManifestException {
        if (attribute.getKey() == null || attribute.getValue() == null) {
            throw new BuildException("Attributes must have name and value");
        }
        if (attribute.getKey().equalsIgnoreCase(ATTRIBUTE_MANIFEST_VERSION)) {
            manifestVersion = attribute.getValue();
        } else {
            mainSection.addConfiguredAttribute(attribute);
        }
    }

    /**
     * Merge the contents of the given manifest into this manifest
     *
     * @param other the Manifest to be merged with this one.
     *
     * @throws ManifestException if there is a problem merging the
     *         manfest according to the Manifest spec.
     */
    public void merge(Manifest other) throws ManifestException {
        merge(other, false);
    }

    /**
     * Merge the contents of the given manifest into this manifest
     *
     * @param other the Manifest to be merged with this one.
     * @param overwriteMain whether to overwrite the main section
     *        of the current manifest
     *
     * @throws ManifestException if there is a problem merging the
     *         manfest according to the Manifest spec.
     */
    public void merge(Manifest other, boolean overwriteMain)
         throws ManifestException {
        if (other != null) {
             if (overwriteMain) {
                 mainSection = (Section) other.mainSection.clone();
             } else {
                 mainSection.merge(other.mainSection);
             }

             if (other.manifestVersion != null) {
                 manifestVersion = other.manifestVersion;
             }

             Enumeration e = other.getSectionNames();
             while (e.hasMoreElements()) {
                 String sectionName = (String) e.nextElement();
                 Section ourSection = (Section) sections.get(sectionName);
                 Section otherSection
                    = (Section) other.sections.get(sectionName);
                 if (ourSection == null) {
                     if (otherSection != null) {
                         addConfiguredSection((Section) otherSection.clone());
                     }
                 } else {
                     ourSection.merge(otherSection);
                 }
             }
         }
    }

    /**
    * Write the manifest out to a print writer.
    *
    * @param writer the Writer to which the manifest is written
    *
    * @throws IOException if the manifest cannot be written
    */
    public void write(PrintWriter writer) throws IOException {
        writer.print(ATTRIBUTE_MANIFEST_VERSION + ": " + manifestVersion + EOL);
        String signatureVersion
            = mainSection.getAttributeValue(ATTRIBUTE_SIGNATURE_VERSION);
        if (signatureVersion != null) {
            writer.print(ATTRIBUTE_SIGNATURE_VERSION + ": "
                + signatureVersion + EOL);
            mainSection.removeAttribute(ATTRIBUTE_SIGNATURE_VERSION);
        }
        mainSection.write(writer);

        // add it back
        if (signatureVersion != null) {
            try {
                Attribute svAttr = new Attribute(ATTRIBUTE_SIGNATURE_VERSION,
                    signatureVersion);
                mainSection.addConfiguredAttribute(svAttr);
            } catch (ManifestException e) {
                // shouldn't happen - ignore
            }
        }

        Enumeration e = sectionIndex.elements();
        while (e.hasMoreElements()) {
            String sectionName = (String) e.nextElement();
            Section section = getSection(sectionName);
            section.write(writer);
        }
    }

    /**
     * Convert the manifest to its string representation
     *
     * @return a multiline string with the Manifest as it
     *         appears in a Manifest file.
     */
    public String toString() {
        StringWriter sw = new StringWriter();
        try {
            write(new PrintWriter(sw));
        } catch (IOException e) {
            return null;
        }
        return sw.toString();
    }

    /**
     * Get the warnings for this manifest.
     *
     * @return an enumeration of warning strings
     */
    public Enumeration getWarnings() {
        Vector warnings = new Vector();

        Enumeration warnEnum = mainSection.getWarnings();
        while (warnEnum.hasMoreElements()) {
            warnings.addElement(warnEnum.nextElement());
        }

        // create a vector and add in the warnings for all the sections
        Enumeration e = sections.elements();
        while (e.hasMoreElements()) {
            Section section = (Section) e.nextElement();
            Enumeration e2 = section.getWarnings();
            while (e2.hasMoreElements()) {
                warnings.addElement(e2.nextElement());
            }
        }

        return warnings.elements();
    }

    /**
     * @see java.lang.Object#hashCode
     */
    public int hashCode() {
        int hashCode = 0;

        if (manifestVersion != null) {
            hashCode += manifestVersion.hashCode();
        }
        hashCode += mainSection.hashCode();
        hashCode += sections.hashCode();

        return hashCode;
    }

    /**
     * @see java.lang.Object#equals
     */
    public boolean equals(Object rhs) {
        if (rhs == null || rhs.getClass() != getClass()) {
            return false;
        }

        if (rhs == this) {
            return true;
        }

        Manifest rhsManifest = (Manifest) rhs;
        if (manifestVersion == null) {
            if (rhsManifest.manifestVersion != null) {
                return false;
            }
        } else if (!manifestVersion.equals(rhsManifest.manifestVersion)) {
            return false;
        }

        if (!mainSection.equals(rhsManifest.mainSection)) {
            return false;
        }

        return CollectionUtils.equals(sections, rhsManifest.sections);
    }

    /**
     * Get the version of the manifest
     *
     * @return the manifest's version string
     */
    public String getManifestVersion() {
        return manifestVersion;
    }

    /**
     * Get the main section of the manifest
     *
     * @return the main section of the manifest
     */
    public Section getMainSection() {
        return mainSection;
    }

    /**
     * Get a particular section from the manifest
     *
     * @param name the name of the section desired.
     * @return the specified section or null if that section
     * does not exist in the manifest
     */
    public Section getSection(String name) {
        return (Section) sections.get(name);
    }

    /**
     * Get the section names in this manifest.
     *
     * @return an Enumeration of section names
     */
    public Enumeration getSectionNames() {
        return sectionIndex.elements();
    }

}
