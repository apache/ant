/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2002 The Apache Software Foundation.  All rights
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
package org.apache.ant.antlib.system;
import java.util.HashMap;
import java.util.Map;
import org.apache.ant.common.antlib.AbstractComponent;
import org.apache.ant.common.antlib.AbstractTask;
import org.apache.ant.common.antlib.AntContext;
import org.apache.ant.common.service.DataService;
import org.apache.ant.common.service.ExecService;
import org.apache.ant.common.util.ExecutionException;

/**
 * Common Base class all tasks that can pass references and property overrides
 * to a subbuild
 *
 * @author Conor MacNeill
 * @created 22 April 2002
 */
public abstract class SubBuild extends AbstractTask {

    /**
     * Simple Property value storing class
     *
     * @author Conor MacNeill
     * @created 5 February 2002
     */
    public static class Property extends AbstractComponent {
        /** The property name */
        private String name;

        /** The property value */
        private String value;


        /**
         * Gets the name of the Property
         *
         * @return the name value
         */
        public String getName() {
            return name;
        }


        /**
         * Gets the value of the Property
         *
         * @return the value value
         */
        public String getValue() {
            return value;
        }


        /**
         * Sets the name of the Property
         *
         * @param name the new name value
         */
        public void setName(String name) {
            this.name = name;
        }


        /**
         * Sets the value of the Property
         *
         * @param value the new value value
         */
        public void setValue(String value) {
            this.value = value;
        }


        /**
         * Validate this data type instance
         *
         * @exception ExecutionException if either attribute has not been set
         */
        public void validateComponent() throws ExecutionException {
            if (name == null) {
                throw new ExecutionException("\"name\" attribute of "
                     + "<property> must be supplied");
            }
            if (value == null) {
                throw new ExecutionException("\"value\" attribute of "
                     + "<property> must be supplied");
            }
        }
    }


    /**
     * A simple class to store information about references being passed
     *
     * @author Conor MacNeill
     * @created 5 February 2002
     */
    public static class Reference extends AbstractComponent {
        /** The id of the reference to be passed */
        private String refId;
        /** The id to be used in the sub-build for this reference */
        private String toId;


        /**
         * Gets the refId of the Reference
         *
         * @return the refId value
         */
        public String getRefId() {
            return refId;
        }


        /**
         * Gets the toId of the Reference
         *
         * @return the toId value
         */
        public String getToId() {
            return toId;
        }


        /**
         * Sets the refId of the Reference
         *
         * @param refId the new refId value
         */
        public void setRefId(String refId) {
            this.refId = refId;
        }


        /**
         * Sets the toId of the Reference
         *
         * @param toId the new toId value
         */
        public void setToId(String toId) {
            this.toId = toId;
        }


        /**
         * Validate this data type instance
         *
         * @exception ExecutionException if the refid attribute has not been
         *      set
         */
        public void validateComponent() throws ExecutionException {
            if (refId == null) {
                throw new ExecutionException("\"refid\" attribute of "
                     + "<reference> must be supplied");
            }
        }
    }


    /** The core's data service for manipulating the properties */
    private DataService dataService;

    /** The core's ExecutionService for running builds and external programs */
    private ExecService execService;

    /** The properties which will be passed to the sub-build */
    private Map properties = new HashMap();


    /**
     * Add a property to be passed to the subbuild
     *
     * @param property descriptor for the property to be passed
     */
    public void addProperty(Property property) {
        properties.put(property.getName(), property.getValue());
    }


    /**
     * Add a reference to be passed
     *
     * @param reference the descriptor of the reference to be passed
     * @exception ExecutionException if the reference does not reference a
     *      valid object
     */
    public void addReference(Reference reference) throws ExecutionException {
        String refId = reference.getRefId();

        if (!dataService.isDataValueSet(refId)) {
            throw new ExecutionException("RefId \"" + refId + "\" is not set");
        }
        Object value = dataService.getDataValue(refId);
        String toId = reference.getToId();

        if (toId == null) {
            toId = refId;
        }

        properties.put(toId, value);
    }


    /**
     * Get the core's execution service
     *
     * @return the core's execution service.
     */
    protected ExecService getExecService() {
        return execService;
    }


    /**
     * Get the core's date service
     *
     * @return the core's data service.
     */
    protected DataService getDataService() {
        return dataService;
    }


    /**
     * Get the properties to be used with the sub-build
     *
     * @return the properties the sub-build will start with
     */
    protected Map getProperties() {
        return properties;
    }


    /**
     * Initialise this task
     *
     * @param context core's context
     * @param componentType the component type of this component (i.e its
     *      defined name in the build file)
     * @exception ExecutionException if we can't access the data service
     */
    public void init(AntContext context, String componentType)
         throws ExecutionException {
        super.init(context, componentType);
        dataService = (DataService) getCoreService(DataService.class);
        execService = (ExecService) getCoreService(ExecService.class);
    }


    /**
     * Set a property for the subbuild
     *
     * @param propertyName the property name
     * @param propertyValue the value of the property
     */
    protected void setProperty(String propertyName, Object propertyValue) {
        properties.put(propertyName, propertyValue);
    }
}

