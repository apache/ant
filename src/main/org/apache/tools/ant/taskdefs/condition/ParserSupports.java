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
package org.apache.tools.ant.taskdefs.condition;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.ProjectComponent;
import org.apache.tools.ant.util.JAXPUtils;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXNotSupportedException;
import org.xml.sax.XMLReader;

/**
 * Test for the XML parser supporting a particular feature
 * @since Ant 1.7
 */
public class ParserSupports extends ProjectComponent implements Condition {

    // Error messages
    /** error - combined attributes not allowed */
    public static final String ERROR_BOTH_ATTRIBUTES =
            "Property and feature attributes are exclusive";
    /** feature */
    public static final String FEATURE = "feature";
    /** property */
    public static final String PROPERTY = "property";

    /** error - not recognized */
    public static final String NOT_RECOGNIZED =
            " not recognized: ";
    /** error - not supported */
    public static final String NOT_SUPPORTED =
            " not supported: ";
    /** error - missing attribute */
    public static final String ERROR_NO_ATTRIBUTES =
        "Neither feature or property are set";
    /** error - no value */
    public static final String ERROR_NO_VALUE =
        "A value is needed when testing for property support";

    private String feature;
    private String property;
    private String value;

    /**
     * Feature to probe for.
     * @param feature the feature to probe for.
     */
    public void setFeature(String feature) {
        this.feature = feature;
    }

    /**
     * Property to probe for
     * @param property the property to probe for.
     */
    public void setProperty(String property) {
        this.property = property;
    }

    /**
     * Optional value to set.
     * Converted to a boolean value when setting a property
     * @param value the value to set.
     */
    public void setValue(String value) {
        this.value = value;
    }

    /** {@inheritDoc}. */
    @Override
    public boolean eval() throws BuildException {
        if (feature != null && property != null) {
            throw new BuildException(ERROR_BOTH_ATTRIBUTES);
        }
        if (feature == null && property == null) {
            throw new BuildException(ERROR_NO_ATTRIBUTES);
        }
        //pick a value that is good for everything
        if (feature != null) {
            return evalFeature();
        }
        if (value == null) {
            throw new BuildException(ERROR_NO_VALUE);
        }
        return evalProperty();
    }

    /**
     * Get our reader
     * @return a reader
     */
    private XMLReader getReader() {
        JAXPUtils.getParser();
        return JAXPUtils.getXMLReader();
    }

    /**
     * Set a feature
     * @return true if the feature could be set
     */
    public boolean evalFeature() {
        XMLReader reader = getReader();
        if (value == null) {
            value = "true";
        }
        boolean v = Project.toBoolean(value);
        try {
            reader.setFeature(feature, v);
        } catch (SAXNotRecognizedException e) {
            log(FEATURE + NOT_RECOGNIZED + feature, Project.MSG_VERBOSE);
            return false;
        } catch (SAXNotSupportedException e) {
            log(FEATURE + NOT_SUPPORTED + feature, Project.MSG_VERBOSE);
            return false;
        }
        return true;
    }

    /**
     * Set a property
     * @return true if the feature could be set
     */
    public boolean evalProperty() {
        XMLReader reader = getReader();
        try {
            reader.setProperty(property, value);
        } catch (SAXNotRecognizedException e) {
            log(PROPERTY + NOT_RECOGNIZED + property, Project.MSG_VERBOSE);
            return false;
        } catch (SAXNotSupportedException e) {
            log(PROPERTY + NOT_SUPPORTED + property, Project.MSG_VERBOSE);
            return false;
        }
        return true;
    }
}
