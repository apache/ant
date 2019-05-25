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

package org.apache.tools.ant.taskdefs;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.StringWriter;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Vector;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.util.StreamUtils;

/**
 * Holds the data of a jar manifest.
 *
 * Manifests are processed according to the
 * <a href="https://docs.oracle.com/javase/8/docs/technotes/guides/jar/jar.html">Jar
 * file specification</a>.
 * Specifically, a manifest element consists of
 * a set of attributes and sections. These sections in turn may contain
 * attributes. Note in particular that this may result in manifest lines
 * greater than 72 bytes being wrapped and continued on the next
 * line. If an application can not handle the continuation mechanism, it
 * is a defect in the application, not this task.
 *
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
    public static final String ATTRIBUTE_CLASSPATH = "Class-Path";

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
    /** Error for attributes */
    public static final String ERROR_FROM_FORBIDDEN = "Manifest attributes should not start "
                        + "with \"" + ATTRIBUTE_FROM + "\" in \"";

    /** Charset to be used for JAR files. */
    public static final Charset JAR_CHARSET = StandardCharsets.UTF_8;
    /** Encoding to be used for JAR files. */
    @Deprecated
    public static final String JAR_ENCODING = JAR_CHARSET.name();

    private static final String ATTRIBUTE_MANIFEST_VERSION_LC =
        ATTRIBUTE_MANIFEST_VERSION.toLowerCase(Locale.ENGLISH);
    private static final String ATTRIBUTE_NAME_LC =
        ATTRIBUTE_NAME.toLowerCase(Locale.ENGLISH);
    private static final String ATTRIBUTE_FROM_LC =
        ATTRIBUTE_FROM.toLowerCase(Locale.ENGLISH);
    private static final String ATTRIBUTE_CLASSPATH_LC =
        ATTRIBUTE_CLASSPATH.toLowerCase(Locale.ENGLISH);

    /**
     * An attribute for the manifest.
     * Those attributes that are not nested into a section will be added to the "Main" section.
     */
    public static class Attribute {

        /**
         * Maximum length of the name to have the value starting on the same
         * line as the name. This to stay under 72 bytes total line length
         * (including CRLF).
         */
        private static final int MAX_NAME_VALUE_LENGTH = 68;

        /**
         * Maximum length of the name according to the jar specification.
         * In this case the first line will have 74 bytes total line length
         * (including CRLF). This conflicts with the 72 bytes total line length
         * max, but is the only possible conclusion from the manifest specification, if
         * names with 70 bytes length are allowed, have to be on the first line, and
         * have to be followed by ": ".
         */
        private static final int MAX_NAME_LENGTH = 70;

        /** The attribute's name */
        private String name = null;

        /** The attribute's value */
        private Vector<String> values = new Vector<>();

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
         * @return a hashcode based on the key and values.
         */
        @Override
        public int hashCode() {
            return Objects.hash(getKey(), values);
        }

        /**
         * @param rhs the object to check for equality.
         * @see java.lang.Object#equals
         * @return true if the key and values are the same.
         */
        @Override
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
            return (lhsKey != null || rhsKey == null)
                    && (lhsKey == null || lhsKey.equals(rhsKey)) && values.equals(rhsAttribute.values);
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
                    + "\" is not valid as it does not contain a name and a value separated by ': '");
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
            return name == null ? null : name.toLowerCase(Locale.ENGLISH);
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
            return values.isEmpty() ? null
                : String.join(" ", values);
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
        public Enumeration<String> getValues() {
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
            setValue(values.elementAt(currentIndex) + line.substring(1));
        }

        /**
         * Write the attribute out to a print writer without
         * flattening multi-values attributes (i.e. Class-Path).
         *
         * @param writer the Writer to which the attribute is written
         *
         * @throws IOException if the attribute value cannot be written
         */
        public void write(PrintWriter writer) throws IOException {
            write(writer, false);
        }

        /**
         * Write the attribute out to a print writer.
         *
         * @param writer the Writer to which the attribute is written
         * @param flatten whether to collapse multi-valued attributes
         *        (i.e. potentially Class-Path) Class-Path into a
         *        single attribute.
         *
         * @throws IOException if the attribute value cannot be written
         * @since Ant 1.8.0
         */
        public void write(PrintWriter writer, boolean flatten)
            throws IOException {
            if (flatten) {
                writeValue(writer, getValue());
            } else {
                for (String value : values) {
                    writeValue(writer, value);
                }
            }
        }

        /**
         * Write a single attribute value out
         *
         * @param writer the Writer to which the attribute is written
         * @param value the attribute value
         *
         * @throws IOException if the attribute value cannot be written
         */
        private void writeValue(PrintWriter writer, String value)
             throws IOException {
            String line;
            int nameLength = name.getBytes(JAR_CHARSET).length;
            if (nameLength > MAX_NAME_VALUE_LENGTH) {
                if (nameLength > MAX_NAME_LENGTH) {
                    throw new IOException("Unable to write manifest line "
                            + name + ": " + value);
                }
                writer.print(name + ": " + EOL);
                line = " " + value;
            } else {
                line = name + ": " + value;
            }
            while (line.getBytes(JAR_CHARSET).length > MAX_SECTION_LENGTH) {
                // try to find a MAX_LINE_LENGTH byte section
                int breakIndex = MAX_SECTION_LENGTH;
                if (breakIndex >= line.length()) {
                    breakIndex = line.length() - 1;
                }
                String section = line.substring(0, breakIndex);
                while (section.getBytes(JAR_CHARSET).length > MAX_SECTION_LENGTH
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
        private List<String> warnings = new Vector<>();

        /**
         * The section's name if any. The main section in a
         * manifest is unnamed.
         */
        private String name = null;

        /** The section's attributes.*/
        private Map<String, Attribute> attributes = new LinkedHashMap<>();

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
                if (line == null || line.isEmpty()) {
                    return null;
                }
                if (line.charAt(0) == ' ') {
                    // continuation line
                    if (attribute == null) {
                        if (name == null) {
                            throw new ManifestException("Can't start an "
                                + "attribute with a continuation line " + line);
                        }
                        // a continuation on the first line is a
                        // continuation of the name - concatenate this
                        // line and the name
                        name += line.substring(1);
                    } else {
                        attribute.addContinuation(line);
                    }
                } else {
                    attribute = new Attribute(line);
                    String nameReadAhead = addAttributeAndCheck(attribute);
                    // refresh attribute in case of multivalued attributes.
                    attribute = getAttribute(attribute.getKey());
                    if (nameReadAhead != null) {
                        return nameReadAhead;
                    }
                }
            }
        }

        /**
         * Merge in another section without merging Class-Path attributes.
         *
         * @param section the section to be merged with this one.
         *
         * @throws ManifestException if the sections cannot be merged.
         */
        public void merge(Section section) throws ManifestException {
            merge(section, false);
        }

        /**
         * Merge in another section
         *
         * @param section the section to be merged with this one.
         * @param mergeClassPaths whether Class-Path attributes should
         *        be merged.
         *
         * @throws ManifestException if the sections cannot be merged.
         */
        public void merge(Section section, boolean mergeClassPaths)
            throws ManifestException {
            if (name == null && section.getName() != null
                || (name != null && section.getName() != null
                    && !(name.toLowerCase(Locale.ENGLISH)
                         .equals(section.getName().toLowerCase(Locale.ENGLISH))))
                ) {
                throw new ManifestException(
                    "Unable to merge sections with different names");
            }

            Attribute classpathAttribute = null;
            for (String attributeName : Collections.list(section.getAttributeKeys())) {
                Attribute attribute = section.getAttribute(attributeName);
                if (ATTRIBUTE_CLASSPATH.equalsIgnoreCase(attributeName)) {
                    if (classpathAttribute == null) {
                        classpathAttribute = new Attribute();
                        classpathAttribute.setName(ATTRIBUTE_CLASSPATH);
                    }
                    Collections.list(attribute.getValues()).forEach(classpathAttribute::addValue);
                } else {
                    // the merge file always wins
                    storeAttribute(attribute);
                }
            }

            if (classpathAttribute != null) {
                if (mergeClassPaths) {
                    Attribute currentCp = getAttribute(ATTRIBUTE_CLASSPATH);
                    if (currentCp != null) {
                        Collections.list(currentCp.getValues()).forEach(classpathAttribute::addValue);
                    }
                }
                storeAttribute(classpathAttribute);
            }

            // add in the warnings
            warnings.addAll(section.warnings);
        }

        /**
         * Write the section out to a print writer without flattening
         * multi-values attributes (i.e. Class-Path).
         *
         * @param writer the Writer to which the section is written
         *
         * @throws IOException if the section cannot be written
         */
        public void write(PrintWriter writer) throws IOException {
            write(writer, false);
        }

        /**
         * Write the section out to a print writer.
         *
         * @param writer the Writer to which the section is written
         * @param flatten whether to collapse multi-valued attributes
         *        (i.e. potentially Class-Path) Class-Path into a
         *        single attribute.
         *
         * @throws IOException if the section cannot be written
         * @since Ant 1.8.0
         */
        public void write(PrintWriter writer, boolean flatten)
            throws IOException {
            if (name != null) {
                Attribute nameAttr = new Attribute(ATTRIBUTE_NAME, name);
                nameAttr.write(writer);
            }
            for (String key : Collections.list(getAttributeKeys())) {
                getAttribute(key).write(writer, flatten);
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
            return attributes.get(attributeName.toLowerCase(Locale.ENGLISH));
        }

        /**
         * Get the attribute keys.
         *
         * @return an Enumeration of Strings, each string being the lower case
         *         key of an attribute of the section.
         */
        public Enumeration<String> getAttributeKeys() {
            return Collections.enumeration(attributes.keySet());
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
            Attribute attribute = getAttribute(attributeName.toLowerCase(Locale.ENGLISH));
            return attribute == null ? null : attribute.getValue();
        }

        /**
         * Remove the given attribute from the section
         *
         * @param attributeName the name of the attribute to be removed.
         */
        public void removeAttribute(String attributeName) {
            String key = attributeName.toLowerCase(Locale.ENGLISH);
            attributes.remove(key);
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
                throw new BuildException(
                    "Specify the section name using the \"name\" attribute of the <section> element rather than using a \"Name\" manifest attribute");
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
            String attributeKey = attribute.getKey();
            if (attributeKey.equals(ATTRIBUTE_NAME_LC)) {
                warnings.add("\"" + ATTRIBUTE_NAME
                    + "\" attributes should not occur in the main section and must be the first element in all other sections: \""
                    + attribute.getName() + ": " + attribute.getValue() + "\"");
                return attribute.getValue();
            }

            if (attributeKey.startsWith(ATTRIBUTE_FROM_LC)) {
                warnings.add(ERROR_FROM_FORBIDDEN
                    + attribute.getName() + ": " + attribute.getValue() + "\"");
            } else {
                // classpath attributes go into a vector
                if (attributeKey.equals(ATTRIBUTE_CLASSPATH_LC)) {
                    Attribute classpathAttribute =
                        attributes.get(attributeKey);

                    if (classpathAttribute == null) {
                        storeAttribute(attribute);
                    } else {
                        warnings.add(
                            "Multiple Class-Path attributes are supported but violate the Jar specification and may not be correctly processed in all environments");
                        Collections.list(attribute.getValues()).forEach(classpathAttribute::addValue);
                    }
                } else if (attributes.containsKey(attributeKey)) {
                    throw new ManifestException("The attribute \""
                        + attribute.getName()
                        + "\" may not occur more than once in the same section");
                } else {
                    storeAttribute(attribute);
                }
            }
            return null;
        }

        /**
         * Clone this section
         *
         * @return the cloned Section
         * @since Ant 1.5.2
         */
        @Override
        public Object clone() {
            Section cloned = new Section();
            cloned.setName(name);
            StreamUtils.enumerationAsStream(getAttributeKeys())
                    .map(key -> new Attribute(getAttribute(key).getName(),
                    getAttribute(key).getValue())).forEach(cloned::storeAttribute);
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
            attributes.put(attribute.getKey(), attribute);
        }

        /**
         * Get the warnings for this section.
         *
         * @return an Enumeration of warning strings.
         */
        public Enumeration<String> getWarnings() {
            return Collections.enumeration(warnings);
        }

        /**
         * @see java.lang.Object#hashCode
         * @return a hash value based on the attributes.
         */
        @Override
        public int hashCode() {
            return attributes.hashCode();
        }

        /**
         * @see java.lang.Object#equals
         * @param rhs the object to check for equality.
         * @return true if the attributes are the same.
         */
        @Override
        public boolean equals(Object rhs) {
            if (rhs == null || rhs.getClass() != getClass()) {
                return false;
            }

            if (rhs == this) {
                return true;
            }

            Section rhsSection = (Section) rhs;

            return attributes.equals(rhsSection.attributes);
        }
    }


    /** The version of this manifest */
    private String manifestVersion = DEFAULT_MANIFEST_VERSION;

    /** The main section of this manifest */
    private Section mainSection = new Section();

    /** The named sections of this manifest */
    private Map<String, Section> sections = new LinkedHashMap<>();

    /**
     * Construct a manifest from Ant's default manifest file.
     *
     * @return the default manifest.
     * @exception BuildException if there is a problem loading the
     *            default manifest
     */
    public static Manifest getDefaultManifest() throws BuildException {
        String defManifest = "/org/apache/tools/ant/defaultManifest.mf";
        try (InputStream in = Manifest.class.getResourceAsStream(defManifest)) {
            if (in == null) {
                throw new BuildException("Could not find default manifest: %s",
                    defManifest);
            }
            Manifest defaultManifest = new Manifest(new InputStreamReader(in, JAR_CHARSET));
            String version = System.getProperty("java.runtime.version");
            if (version == null) {
                version = System.getProperty("java.vm.version");
            }
            Attribute createdBy = new Attribute("Created-By", version
                    + " (" + System.getProperty("java.vm.vendor") + ")");
            defaultManifest.getMainSection().storeAttribute(createdBy);
            return defaultManifest;
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

        String line;
        while ((line = reader.readLine()) != null) {
            if (line.isEmpty()) {
                continue;
            }

            Section section = new Section();
            if (nextSectionName == null) {
                Attribute sectionName = new Attribute(line);
                if (!ATTRIBUTE_NAME.equalsIgnoreCase(sectionName.getName())) {
                    throw new ManifestException(
                        "Manifest sections should start with a \""
                            + ATTRIBUTE_NAME + "\" attribute and not \""
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
        if (ATTRIBUTE_MANIFEST_VERSION_LC.equals(attribute.getKey())) {
            manifestVersion = attribute.getValue();
        } else {
            mainSection.addConfiguredAttribute(attribute);
        }
    }

    /**
     * Merge the contents of the given manifest into this manifest
     * without merging Class-Path attributes.
     *
     * @param other the Manifest to be merged with this one.
     *
     * @throws ManifestException if there is a problem merging the
     *         manifest according to the Manifest spec.
     */
    public void merge(Manifest other) throws ManifestException {
        merge(other, false);
    }

    /**
     * Merge the contents of the given manifest into this manifest
     * without merging Class-Path attributes.
     *
     * @param other the Manifest to be merged with this one.
     * @param overwriteMain whether to overwrite the main section
     *        of the current manifest
     *
     * @throws ManifestException if there is a problem merging the
     *         manifest according to the Manifest spec.
     */
    public void merge(Manifest other, boolean overwriteMain)
         throws ManifestException {
        merge(other, overwriteMain, false);
    }

    /**
     * Merge the contents of the given manifest into this manifest
     *
     * @param other the Manifest to be merged with this one.
     * @param overwriteMain whether to overwrite the main section
     *        of the current manifest
     * @param mergeClassPaths whether Class-Path attributes should be
     *        merged.
     *
     * @throws ManifestException if there is a problem merging the
     *         manifest according to the Manifest spec.
     *
     * @since Ant 1.8.0
     */
    public void merge(Manifest other, boolean overwriteMain,
                      boolean mergeClassPaths)
         throws ManifestException {
        if (other != null) {
             if (overwriteMain) {
                 mainSection = (Section) other.mainSection.clone();
             } else {
                 mainSection.merge(other.mainSection, mergeClassPaths);
             }

             if (other.manifestVersion != null) {
                 manifestVersion = other.manifestVersion;
             }

             for (String sectionName : Collections.list(other.getSectionNames())) {
                 Section ourSection = sections.get(sectionName);
                 Section otherSection = other.sections.get(sectionName);
                 if (ourSection == null) {
                     if (otherSection != null) {
                         addConfiguredSection((Section) otherSection.clone());
                     }
                 } else {
                     ourSection.merge(otherSection, mergeClassPaths);
                 }
             }
         }
    }

    /**
     * Write the manifest out to a print writer without flattening
     * multi-values attributes (i.e. Class-Path).
     *
     * @param writer the Writer to which the manifest is written
     *
     * @throws IOException if the manifest cannot be written
     */
    public void write(PrintWriter writer) throws IOException {
        write(writer, false);
    }

    /**
    * Write the manifest out to a print writer.
    *
    * @param writer the Writer to which the manifest is written
    * @param flatten whether to collapse multi-valued attributes
    *        (i.e. potentially Class-Path) Class-Path into a single
    *        attribute.
    *
    * @throws IOException if the manifest cannot be written
    * @since Ant 1.8.0
    */
    public void write(PrintWriter writer, boolean flatten) throws IOException {
        writer.print(ATTRIBUTE_MANIFEST_VERSION + ": " + manifestVersion + EOL);
        String signatureVersion
            = mainSection.getAttributeValue(ATTRIBUTE_SIGNATURE_VERSION);
        if (signatureVersion != null) {
            writer.print(ATTRIBUTE_SIGNATURE_VERSION + ": "
                + signatureVersion + EOL);
            mainSection.removeAttribute(ATTRIBUTE_SIGNATURE_VERSION);
        }
        mainSection.write(writer, flatten);

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

        for (String sectionName : sections.keySet()) {
            Section section = getSection(sectionName);
            section.write(writer, flatten);
        }
    }

    /**
     * Convert the manifest to its string representation
     *
     * @return a multiline string with the Manifest as it
     *         appears in a Manifest file.
     */
    @Override
    public String toString() {
        StringWriter sw = new StringWriter();
        try {
            write(new PrintWriter(sw));
        } catch (IOException e) {
            return "";
        }
        return sw.toString();
    }

    /**
     * Get the warnings for this manifest.
     *
     * @return an enumeration of warning strings
     */
    public Enumeration<String> getWarnings() {
        // create a vector and add in the warnings for the main section
        List<String> warnings = Collections.list(mainSection.getWarnings());

        // add in the warnings for all the sections
        sections.values().stream().map(section -> Collections.list(section.getWarnings()))
                .forEach(warnings::addAll);

        return Collections.enumeration(warnings);
    }

    /**
     * @see java.lang.Object#hashCode
     * @return a hashcode based on the version, main and sections.
     */
    @Override
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
     * @param rhs the object to check for equality.
     * @return true if the version, main and sections are the same.
     */
    @Override
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

        return mainSection.equals(rhsManifest.mainSection) && sections.equals(rhsManifest.sections);

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
        return sections.get(name);
    }

    /**
     * Get the section names in this manifest.
     *
     * @return an Enumeration of section names
     */
    public Enumeration<String> getSectionNames() {
        return Collections.enumeration(sections.keySet());
    }
}
