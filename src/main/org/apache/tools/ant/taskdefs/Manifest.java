/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2001 The Apache Software Foundation.  All rights 
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

package org.apache.tools.ant.taskdefs;

import java.util.*;
import java.io.*;

import org.apache.tools.ant.BuildException;

/**
 * Class to manage Manifest information
 * 
 * @author <a href="mailto:conor@apache.org">Conor MacNeill</a>
 */
public class Manifest {
    /** The standard manifest version header */
    static public final String ATTRIBUTE_MANIFEST_VERSION = "Manifest-Version";
    
    /** The standard Signature Version header */
    static public final String ATTRIBUTE_SIGNATURE_VERSION = "Signature-Version";
    
    /** The Name Attribute is the first in a named section */
    static public final String ATTRIBUTE_NAME = "Name";
    
    /** THe From Header is disallowed in a Manifest */    
    static public final String ATTRIBUTE_FROM = "From";
    
    /** Default Manifest version if one is not specified */
    static public final String DEFAULT_MANIFEST_VERSION = "1.0";
    
    /** The max length of a line in a Manifest */
    static public final int MAX_LINE_LENGTH = 70;
    
    /**
     * Class to hold manifest attributes
     */
    static public class Attribute {
        /** The attribute's name */
        private String name = null;
        
        /** The attribute's value */
        private String value = null;

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
            this.value = value;
        }
        
        public boolean equals(Object rhs) {
            if (!(rhs instanceof Attribute)) {
                return false;
            }
            
            Attribute rhsAttribute = (Attribute)rhs;
            return (name != null && rhsAttribute.name != null &&
                    name.toLowerCase().equals(rhsAttribute.name.toLowerCase()) &&
                    value != null && value.equals(rhsAttribute.value));
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
                throw new ManifestException("Manifest line \"" + line + "\" is not valid");
            }
            name = line.substring(0, index);
            value = line.substring(index + 2);
        } 
        
        /**
         * Set the Attribute's name
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
         * Set the Attribute's value
         * 
         * @param value the attribute's value
         */
        public void setValue(String value) {
            this.value = value;
        }
        
        /**
         * Get the Attribute's value
         *
         * @return the attribute's value.
         */
        public String getValue() {
            return value;
        }
        
        /**
         * Add a continuation line from the Manifest file
         *
         * When lines are too long in a manifest, they are continued on the 
         * next line by starting with a space. This method adds the continuation
         * data to the attribute value by skipping the first character.
         */
        public void addContinuation(String line) {
            value += line.substring(1);
        }
        
        /**
         * Write the attribute out to a print writer.
         *
         * @param writer the Writer to which the attribute is written
         *
         * @throws IOException if the attribte value cannot be written
         */
        public void write(PrintWriter writer) throws IOException {
            String line = name + ": " + value;
            while (line.getBytes().length > MAX_LINE_LENGTH) {
                // try to find a MAX_LINE_LENGTH byte section
                int breakIndex = MAX_LINE_LENGTH;
                String section = line.substring(0, breakIndex);
                while (section.getBytes().length > MAX_LINE_LENGTH && breakIndex > 0) {
                    breakIndex--;
                    section = line.substring(0, breakIndex);
                }
                if (breakIndex == 0) {
                    throw new IOException("Unable to write manifest line " + name + ": " + value);
                }
                writer.println(section);
                line = " " + line.substring(breakIndex);
            }
            writer.println(line);
        }    
    }

    /** 
     * Class to represent an individual section in the 
     * Manifest. A section consists of a set of attribute values,
     * separated from other sections by a blank line.
     */
    static public class Section {
        private Vector warnings = new Vector();
        
        /** The section's name if any. The main section in a manifest is unnamed.*/
        private String name = null;
        
        /** The section's attributes.*/
        private Hashtable attributes = new Hashtable();
        
        /**
         * Set the Section's name
         * 
         * @param name the section's name
         */
        public void setName(String name) {
            this.name = name;
        }
        
        /**
         * Get the Section's name
         *
         * @return the section's name.
         */
        public String getName() {
            return name;
        }
        
        /**
         * Read a section through a reader 
         * 
         * @param reader the reader from which the section is read
         *
         * @return the name of the next section if it has been read as part of this 
         *         section - This only happens if the Manifest is malformed.
         * 
         * @throws ManifestException if the section is not valid according to the JAR spec
         * @throws IOException if the section cannot be read from the reader.
         */
        public String read(BufferedReader reader) throws ManifestException, IOException {
            Attribute attribute = null;
            while (true) { 
                String line = reader.readLine();
                if (line == null || line.length() == 0) {
                    return null;
                }
                if (line.charAt(0) == ' ') {
                    // continuation line
                    if (attribute == null) {
                        throw new ManifestException("Can't start an attribute with a continuation line " + line);
                    }
                    attribute.addContinuation(line);
                }
                else {
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
            if (name == null && section.getName() != null ||
                    name != null && !(name.equalsIgnoreCase(section.getName()))) {
                throw new ManifestException("Unable to merge sections with different names");
            }
            
            for (Enumeration e = section.attributes.keys(); e.hasMoreElements();) {
                String attributeName = (String)e.nextElement();
                // the merge file always wins
                attributes.put(attributeName, section.attributes.get(attributeName));
            }
            
            // add in the warnings
            for (Enumeration e = section.warnings.elements(); e.hasMoreElements();) {
                warnings.addElement(e.nextElement());
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
            for (Enumeration e = attributes.elements(); e.hasMoreElements();) {
                Attribute attribute = (Attribute)e.nextElement();
                attribute.write(writer);
            }
            writer.println();
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
            Attribute attribute = (Attribute)attributes.get(attributeName.toLowerCase());
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
            attributes.remove(attributeName.toLowerCase());
        }

        public void addConfiguredAttribute(Attribute attribute) throws ManifestException {
            String check = addAttributeAndCheck(attribute);
            if (check != null) {
                throw new BuildException("Use the \"name\" attribute of the <section> element rather than using " +
                                         "the \"Name\" attribute");
            }
        }
        
        /**
         * Add an attribute to the section
         *
         * @param attribute the attribute to be added.
         *
         * @return the value of the attribute if it is a name attribute - null other wise
         *
         * @throws ManifestException if the attribute already exists in this section.
         */
        public String addAttributeAndCheck(Attribute attribute) throws ManifestException {
            if (attribute.getName() == null || attribute.getValue() == null) {
                throw new BuildException("Attributes must have name and value");
            }
            if (attribute.getName().equalsIgnoreCase(ATTRIBUTE_NAME)) {
                warnings.addElement("\"" + ATTRIBUTE_NAME + "\" attributes should not occur in the " +
                                    "main section and must be the first element in all " + 
                                    "other sections: \"" +attribute.getName() + ": " + attribute.getValue() + "\"");  
                return attribute.getValue();
            }
            
            if (attribute.getName().toLowerCase().startsWith(ATTRIBUTE_FROM.toLowerCase())) {
                warnings.addElement("Manifest attributes should not start with \"" +
                                    ATTRIBUTE_FROM + "\" in \"" +attribute.getName() + ": " + attribute.getValue() + "\"");  
            }
            else if (attributes.containsKey(attribute.getName().toLowerCase())) {
                throw new ManifestException("The attribute \"" + attribute.getName() + "\" may not " + 
                                            "occur more than once in the same section");
            }
            else {
                attributes.put(attribute.getName().toLowerCase(), attribute);
            }
            return null;
        }

        public Enumeration getWarnings() {
            return warnings.elements();
        }
        
        public boolean equals(Object rhs) {
            if (!(rhs instanceof Section)) {
                return false;
            }
            
            Section rhsSection = (Section)rhs;
            if (attributes.size() != rhsSection.attributes.size()) {
                return false;
            }
        
            for (Enumeration e = attributes.elements(); e.hasMoreElements();) {
                Attribute attribute  = (Attribute)e.nextElement();
                Attribute rshAttribute = (Attribute)rhsSection.attributes.get(attribute.getName().toLowerCase());
                if (!attribute.equals(rshAttribute)) {
                    return false;
                }
            }
            
            return true;
        }
    }        


    /** The version of this manifest */
    private String manifestVersion = DEFAULT_MANIFEST_VERSION;
    
    /** The main section of this manifest */ 
    private Section mainSection = new Section();
    
    /** The named sections of this manifest */
    private Hashtable sections = new Hashtable();

    /** Construct an empty manifest */
    public Manifest() {
    }
    
    /**
     * Read a manifest file from the given input stream
     *
     * @param is the input stream from which the Manifest is read 
     * 
     * @throws ManifestException if the manifest is not valid according to the JAR spec
     * @throws IOException if the manifest cannot be read from the reader.
     */
    public Manifest(InputStream is) throws ManifestException, IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        // This should be the manifest version
        String nextSectionName = mainSection.read(reader);
        String readManifestVersion = mainSection.getAttributeValue(ATTRIBUTE_MANIFEST_VERSION);
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
                    throw new ManifestException("Manifest sections should start with a \"" + ATTRIBUTE_NAME + 
                                                "\" attribute and not \"" + sectionName.getName() + "\"");
                }
                nextSectionName = sectionName.getValue();
            }
            else {
                // we have already started reading this section
                // this line is the first attribute. set it and then let the normal
                // read handle the rest
                Attribute firstAttribute = new Attribute(line);
                section.addAttributeAndCheck(firstAttribute);
            }
                    
            section.setName(nextSectionName);
            nextSectionName = section.read(reader);
            addConfiguredSection(section);
        }
    }
    
    public void addConfiguredSection(Section section) throws ManifestException {
        if (section.getName() == null) {
            throw new BuildException("Sections must have a name");
        }
        sections.put(section.getName().toLowerCase(), section);
    }
    
    public void addConfiguredAttribute(Attribute attribute) throws ManifestException {
        mainSection.addConfiguredAttribute(attribute);
    }
    
    /**
     * Merge the contents of the given manifest into this manifest
     *
     * @param other the Manifest to be merged with this one.
     *
     * @throws ManifestException if there is a problem merging the manfest according
     *         to the Manifest spec.
     */
    public void merge(Manifest other) throws ManifestException {
        manifestVersion = other.manifestVersion;
        mainSection.merge(other.mainSection);
        for (Enumeration e = other.sections.keys(); e.hasMoreElements();) {
            String sectionName = (String)e.nextElement();
            Section ourSection = (Section)sections.get(sectionName);
            Section otherSection = (Section)other.sections.get(sectionName);
            if (ourSection == null) {
                sections.put(sectionName.toLowerCase(), otherSection);
            }
            else {
                ourSection.merge(otherSection);
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
        writer.println(ATTRIBUTE_MANIFEST_VERSION + ": " + manifestVersion);
        String signatureVersion = mainSection.getAttributeValue(ATTRIBUTE_SIGNATURE_VERSION);
        if (signatureVersion != null) {
            writer.println(ATTRIBUTE_SIGNATURE_VERSION + ": " + signatureVersion);
            mainSection.removeAttribute(ATTRIBUTE_SIGNATURE_VERSION);
        }
        mainSection.write(writer);
        if (signatureVersion != null) {
            try {
                mainSection.addConfiguredAttribute(new Attribute(ATTRIBUTE_SIGNATURE_VERSION, signatureVersion));
            }
            catch (ManifestException e) {
                // shouldn't happen - ignore
            }
        }
        
        for (Enumeration e = sections.elements(); e.hasMoreElements();) {
            Section section = (Section)e.nextElement();
            section.write(writer);
        }
    }
    
    /**
     * Convert the manifest to its string representation
     *
     * @return a multiline string with the Manifest as it appears in a Manifest file.
     */
    public String toString() {
        StringWriter sw = new StringWriter();
        try {
            write(new PrintWriter(sw));
        }
        catch (IOException e) {
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
        
        for (Enumeration e2 = mainSection.getWarnings(); e2.hasMoreElements();) {
            warnings.addElement(e2.nextElement());
        }
        
        // create a vector and add in the warnings for all the sections
        for (Enumeration e = sections.elements(); e.hasMoreElements();) {
            Section section = (Section)e.nextElement();
            for (Enumeration e2 = section.getWarnings(); e2.hasMoreElements();) {
                warnings.addElement(e2.nextElement());
            }
        }
        
        return warnings.elements();
    }
    
    public boolean equals(Object rhs) {
        if (!(rhs instanceof Manifest)) {
            return false;
        }
        
        Manifest rhsManifest = (Manifest)rhs;
        if (!manifestVersion.equals(rhsManifest.manifestVersion)) {
            return false;
        }
        if (sections.size() != rhsManifest.sections.size()) {
            return false;
        }
        
        if (!mainSection.equals(rhsManifest.mainSection)) {
            return false;
        }
        
        for (Enumeration e = sections.elements(); e.hasMoreElements();) {
            Section section = (Section)e.nextElement();
            Section rhsSection = (Section)rhsManifest.sections.get(section.getName().toLowerCase());
            if (!section.equals(rhsSection)) {
                return false;
            }
        }
        
        return true;
    }
}
