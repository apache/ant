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

/**
 * Class to manage Manifest information
 * 
 * @author <a href="mailto:conor@apache.org">Conor MacNeill</a>
 */
public class Manifest {
    static public final String ATTR_MANIFEST_VERSION = "Manifest-Version";
    static public final String ATTR_SIGNATURE_VERSION = "Signature-Version";
    static public final String ATTR_NAME = "Name";
    static public final String ATTR_FROM = "From";
    static public final String DEFAULT_MANIFEST_VERSION = "1.0";
    static public final int MAX_LINE_LENGTH = 70;
    
    /**
     * Class to hold manifest attributes
     */
    static private class Attribute {
        /** The attribute's name */
        private String name = null;
        
        /** The attribute's value */
        private String value = null;

        public Attribute() {
        }
        
        public Attribute(String line) throws IOException {
            parse(line);
        }
        
        public Attribute(String name, String value) {
            this.name = name;
            this.value = value;
        }
        
        public void parse(String line) throws IOException {
            int index = line.indexOf(": ");
            if (index == -1) {
                throw new IOException("Manifest line \"" + line + "\" is not valid");
            }
            name = line.substring(0, index);
            value = line.substring(index + 2);
        } 
        
        public void setName(String name) {
            this.name = name;
        }
        
        public String getName() {
            return name;
        }
        
        public void setValue(String value) {
            this.value = value;
        }
        
        public String getValue() {
            return value;
        }
        
        public void addContinuation(String line) {
            value += line.substring(1);
        }
        
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
     * Manifest 
     */
    static private class Section {
        private String name = null;
        
        private Hashtable attributes = new Hashtable();
        
        public void setName(String name) {
            this.name = name;
        }
        
        public String getName() {
            return name;
        }
        
        public void read(BufferedReader reader) throws IOException {
            Attribute attribute = null;
            while (true) { 
                String line = reader.readLine();
                if (line == null || line.length() == 0) {
                    return;
                }
                if (line.charAt(0) == ' ') {
                    // continuation line
                    if (attribute == null) {
                        throw new IOException("Can't start an attribute with a continuation line " + line);
                    }
                    attribute.addContinuation(line);
                }
                else {
                    attribute = new Attribute(line);
                    if (name == null && attribute.getName().equalsIgnoreCase(ATTR_NAME)) {
                        throw new IOException("The " + ATTR_NAME + " header may not occur in the main section ");
                    }
                    
                    if (attribute.getName().toLowerCase().startsWith(ATTR_FROM.toLowerCase())) {
                        throw new IOException("Attribute names may not start with " + ATTR_FROM);
                    }
                    
                    addAttribute(attribute);
                }
            }
        }
        
        public void merge(Section section) throws IOException {
            if (name == null && section.getName() != null ||
                    name != null && !(name.equalsIgnoreCase(section.getName()))) {
                throw new IOException("Unable to merge sections with different names");
            }
            
            for (Enumeration e = section.attributes.keys(); e.hasMoreElements();) {
                String attributeName = (String)e.nextElement();
                // the merge file always wins
                attributes.put(attributeName, section.attributes.get(attributeName));
            }
        }
        
        public void write(PrintWriter writer) throws IOException {
            if (name != null) {
                Attribute nameAttr = new Attribute(ATTR_NAME, name);
                nameAttr.write(writer);
            }
            for (Enumeration e = attributes.elements(); e.hasMoreElements();) {
                Attribute attribute = (Attribute)e.nextElement();
                attribute.write(writer);
            }
            writer.println();
        }
    
        public String getAttributeValue(String attributeName) {
            Attribute attribute = (Attribute)attributes.get(attributeName.toLowerCase());
            if (attribute == null) {
                return null;
            }
            return attribute.getValue();
        }
        
        public void removeAttribute(String attributeName) {
            attributes.remove(attributeName.toLowerCase());
        }
        
        public void addAttribute(Attribute attribute) throws IOException {
            if (attributes.containsKey(attribute.getName().toLowerCase())) {
                throw new IOException("The attribute \"" + attribute.getName() + "\" may not occur more than" +
                                      " once in the same section");
            }
            attributes.put(attribute.getName().toLowerCase(), attribute);
        }
    }

    
    private String manifestVersion = DEFAULT_MANIFEST_VERSION;
    private Section mainSection = new Section();
    private Hashtable sections = new Hashtable();

    public Manifest() {
    }
    
    /**
     * Read a manifest file from the given input stream
     *
     * @param is the input stream from which the Manifest is read 
     */
    public Manifest(InputStream is) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        String line = reader.readLine();
        if (line == null) {
            return;
        }
        
        // This should be the manifest version
        Attribute version = new Attribute(line);
        if (!version.getName().equalsIgnoreCase(ATTR_MANIFEST_VERSION)) {
            throw new IOException("Manifest must start with \"" + ATTR_MANIFEST_VERSION + 
                                  "\" and not \"" + line + "\"");
        }
        manifestVersion = version.getValue();
        mainSection.read(reader);
        
        while ((line = reader.readLine()) != null) {
            if (line.length() == 0) {
                continue;
            }
            Attribute sectionName = new Attribute(line);
            if (!sectionName.getName().equalsIgnoreCase(ATTR_NAME)) {
                throw new IOException("Manifest sections should start with a \"" + ATTR_NAME + 
                                      "\" attribute and not \"" + sectionName.getName() + "\"");
            }
                
            Section section = new Section();
            section.setName(sectionName.getValue());
            section.read(reader);
            sections.put(section.getName().toLowerCase(), section);
        }
    }
    
    /**
     * Merge the contents of the given manifest into this manifest
     */
    public void merge(Manifest other) throws IOException {
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
    
    public void write(PrintWriter writer) throws IOException {
        writer.println(ATTR_MANIFEST_VERSION + ": " + manifestVersion);
        String signatureVersion = mainSection.getAttributeValue(ATTR_SIGNATURE_VERSION);
        if (signatureVersion != null) {
            writer.println(ATTR_SIGNATURE_VERSION + ": " + signatureVersion);
            mainSection.removeAttribute(ATTR_SIGNATURE_VERSION);
        }
        mainSection.write(writer);
        if (signatureVersion != null) {
            mainSection.addAttribute(new Attribute(ATTR_SIGNATURE_VERSION, signatureVersion));
        }
        
        for (Enumeration e = sections.elements(); e.hasMoreElements();) {
            Section section = (Section)e.nextElement();
            section.write(writer);
        }
    }
    
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
}
