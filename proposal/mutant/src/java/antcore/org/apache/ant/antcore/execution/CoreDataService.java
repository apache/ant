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
package org.apache.ant.antcore.execution;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.apache.ant.common.event.MessageLevel;
import org.apache.ant.common.model.Project;
import org.apache.ant.common.service.DataService;
import org.apache.ant.common.util.AntException;
import org.apache.ant.common.util.DataValue;
import org.apache.ant.common.util.PropertyUtils;

/**
 * This is the core's implementation of the DataService service interface. It
 * gives Ant libraries access to property values maintained in the Frame.
 *
 * @author Conor MacNeill
 * @created 31 January 2002
 */
public class CoreDataService implements DataService {

    /** The Frame this service instance is working for */
    private Frame frame;

    /** all properties to be unset without throwing an exception */
    private boolean allowUnsetProperties;

    /**
     * The context of this execution. This contains all data object's created
     * by tasks that have been executed
     */
    private Map dataValues = new HashMap();

    /**
     * The property overrides for the referenced frames. This map is indexed
     * by the reference names of the frame. Each entry is another Map of
     * property values indexed by their relative name.
     */
    private Map overrides = new HashMap();

    /**
     * Constructor
     *
     * @param frame the frame containing this context
     * @param allowUnsetProperties true if the reference to an unset property
     *      should not throw an exception
     */
    protected CoreDataService(Frame frame,
                              boolean allowUnsetProperties) {
        this.frame = frame;
        this.allowUnsetProperties = allowUnsetProperties;
    }

    /**
     * Get the Data service of a frame
     *
     * @param frame the frame whose data service is required.
     * @return the frame's data service.
     * @exception ExecutionException if the frame's data service cannot be
     *            retrieved.
     */
    private DataService getFrameDataService(Frame frame)
         throws ExecutionException {
        return (DataService) frame.getCoreService(DataService.class);
    }

    /**
     * Update a DataValue in a repository.
     *
     * The value is only updated if it is a higher priority than any existing
     * values or the same priority and the mutable flag is set.
     *
     * @param repository the repository containing the values.
     * @param key the key under which the value is to be stored.
     * @param value the data vale instance
     * @param mutable true if a value of equal priority can be overwritten.
     */
    private void updateValue(Map repository, String key,
                             DataValue value, boolean mutable) {
        int priority = value.getPriority();
        DataValue currentDataValue = (DataValue) repository.get(key);
        if (currentDataValue != null) {
            int currentPriority = currentDataValue.getPriority();
            if (currentPriority > priority
                 || (currentPriority == priority && !mutable)) {
                frame.log("Ignoring override for data value " + key,
                    MessageLevel.VERBOSE);
                return;
            }
        }
        repository.put(key, value);
    }

    /**
     * Set a value in this frame or any of its imported frames.
     *
     * @param name the name of the value
     * @param value the actual value
     * @param mutable if true, existing values can be changed
     * @exception AntException if the value cannot be set.
     */
    public void setDataValue(String name, DataValue value, boolean mutable)
         throws AntException {
        Frame containingFrame = frame.getContainingFrame(name);

        if (containingFrame == null) {
            setOverrideProperty(name, value, mutable);
            return;
        }

        String localName = frame.getNameInFrame(name);
        if (containingFrame == frame) {
            updateValue(dataValues, localName, value, mutable);
        } else {
            DataService actualDataService = getFrameDataService(containingFrame);
            actualDataService.setDataValue(localName, value, mutable);
        }
    }

    /**
     * When a frame has not yet been referenced, this method is used to set
     * the initial properties for the frame when it is introduced.
     *
     * @param name the name of the value
     * @param value the actual value
     * @param mutable if true, existing values can be changed
     * @exception ExecutionException if attempting to override a property in
     *      the current frame.
     */
    private void setOverrideProperty(String name, DataValue value,
                                     boolean mutable)
         throws ExecutionException {
        int refIndex = name.indexOf(Project.REF_DELIMITER);
        if (refIndex == -1) {
            throw new ExecutionException("Property overrides can only be set"
                 + " for properties in referenced projects - not "
                 + name);
        }

        String firstFrameName = name.substring(0, refIndex);

        String relativeName
             = name.substring(refIndex + Project.REF_DELIMITER.length());

        Map frameOverrides = (Map) overrides.get(firstFrameName);
        if (frameOverrides == null) {
            frameOverrides = new HashMap();
            overrides.put(firstFrameName, frameOverrides);
        }

        updateValue(frameOverrides, relativeName, value, mutable);
    }


    /**
     * Get a data value
     *
     * @param name the name of the data value
     * @return the current object associated with the name or null if no value
     *      is currently associated with the name
     * @exception AntException if the value cannot be retrieved.
     */
    public Object getDataValue(String name) throws AntException {
        Frame containingFrame = frame.getContainingFrame(name);

        if (containingFrame == null) {
            return getOverrideProperty(name);
        }
        if (containingFrame == frame) {
            DataValue dataValue = (DataValue) dataValues.get(name);
            if (dataValue == null) {
                return null;
            }
            return dataValue.getValue();
        } else {
            String localName = frame.getNameInFrame(name);
            DataService actualDataService = getFrameDataService(containingFrame);
            return actualDataService.getDataValue(localName);
        }
    }

    /**
     * Indicate if a data value has been set
     *
     * @param name the name of the data value - may contain reference
     *      delimiters
     * @return true if the value exists
     * @exception AntException if the data value cannot be accessed.
     */
    public boolean isDataValueSet(String name) throws AntException {
        Frame containingFrame = frame.getContainingFrame(name);

        if (containingFrame == null) {
            return isOverrideSet(name);
        }
        if (containingFrame == frame) {
            return dataValues.containsKey(name);
        } else {
            String localName = frame.getNameInFrame(name);
            DataService actualDataService = getFrameDataService(containingFrame);
            return actualDataService.isDataValueSet(localName);
        }
    }

    /**
     * Get all the data values from the frame and any referenced frames. This
     * is an expensive operation since it must clone all of the property
     * stores in all frames
     *
     * @return a Map containing the frames data values indexed by their full
     *      name.
     * @exception AntException if the values cannot be retrieved.
     */
    public Map getAllDataValues() throws AntException {
        Map allValues = new HashMap();
        mergeDataValues(allValues, dataValues);

        // add in values from sub frames
        for (Iterator i = frame.getRefNames(); i.hasNext();) {
            String refName = (String) i.next();
            Frame refFrame = frame.getReferencedFrame(refName);

            DataService refDataService = getFrameDataService(refFrame);
            Map refValues = refDataService.getAllDataValues();
            Iterator j = refValues.keySet().iterator();

            while (j.hasNext()) {
                String name = (String) j.next();
                DataValue value = (DataValue) refValues.get(name);
                updateValue(allValues, refName + Project.REF_DELIMITER + name,
                    value, false);
            }
        }

        // add in values from overrides which have not yet been activated
        for (Iterator i = overrides.keySet().iterator(); i.hasNext();) {
            String refName = (String) i.next();
            Map refOverrides = (Map) overrides.get(refName);
            for (Iterator j = refOverrides.keySet().iterator(); j.hasNext();) {
                String name = (String) j.next();
                DataValue value = (DataValue) refOverrides.get(name);
                updateValue(allValues, refName + Project.REF_DELIMITER + name,
                    value, false);
            }
        }

        return allValues;
    }


    /**
     * Replace ${} style constructions in the given value with the string
     * value of the corresponding data values in the frame
     *
     * @param value the string to be scanned for property references.
     * @return the string with all property references replaced
     * @exception AntException if any of the properties do not exist
     */
    public String replacePropertyRefs(String value)
         throws AntException {
        if (value == null) {
            return null;
        }

        List fragments = new ArrayList();
        List propertyRefs = new ArrayList();
        PropertyUtils.parsePropertyString(value, fragments, propertyRefs);

        StringBuffer sb = new StringBuffer();
        Iterator i = fragments.iterator();
        Iterator j = propertyRefs.iterator();
        while (i.hasNext()) {
            String fragment = (String) i.next();
            if (fragment == null) {
                String propertyName = (String) j.next();
                if (!isDataValueSet(propertyName)) {
                    if (!allowUnsetProperties) {
                        throw new ExecutionException("Property \""
                             + propertyName + "\" has not been set");
                    }
                    fragment = "${" + propertyName + "}";
                } else {
                    fragment = getDataValue(propertyName).toString();
                }
            }
            sb.append(fragment);
        }

        return sb.toString();
    }

    /**
     * Replace ${} style constructions in the given value with the string
     * value of the objects in the given map. Any values which are not found
     * are left unchanged.
     *
     * @param value the string to be scanned for property references.
     * @param replacementValues the collection of replacement values
     * @return the string with all property references replaced
     * @exception AntException if any of the properties do not exist
     */
    public String replacePropertyRefs(String value, Map replacementValues)
         throws AntException {
        if (value == null) {
            return null;
        }

        List fragments = new ArrayList();
        List propertyRefs = new ArrayList();
        PropertyUtils.parsePropertyString(value, fragments, propertyRefs);

        StringBuffer sb = new StringBuffer();
        Iterator i = fragments.iterator();
        Iterator j = propertyRefs.iterator();
        while (i.hasNext()) {
            String fragment = (String) i.next();
            if (fragment == null) {
                String propertyName = (String) j.next();
                if (!replacementValues.containsKey(propertyName)) {
                    fragment = "${" + propertyName + "}";
                } else {
                    fragment
                         = replacementValues.get(propertyName).toString();
                }
            }
            sb.append(fragment);
        }

        return sb.toString();
    }

    /**
     * Get the data value overrides associated with a given reference name.
     *
     * @param refName The name of the reference for which overriding datavalues
     *                are required.
     * @return The map of named DataValue instances.
     */
    protected Map getOverrides(String refName) {
        return (Map) overrides.get(refName);
    }

    /**
     * Remove the override values associated with a given reference name.
     *
     * @param refName The name of the reference for which overriding datavalues
     *                are required.
     */
    protected void removeOverrides(String refName) {
        overrides.remove(refName);
    }

    /**
     * Get a value which exists in the frame property overrides awaiting the
     * frame to be introduced.
     *
     * @param name the name of the value
     * @return the value of the property or null if the property does not
     *      exist.
     * @exception ExecutionException if attempting to get an override in the
     *      current frame.
     */
    private Object getOverrideProperty(String name) throws ExecutionException {
        int refIndex = name.indexOf(Project.REF_DELIMITER);
        if (refIndex == -1) {
            throw new ExecutionException("Property overrides can only be"
                 + " returned for properties in referenced projects - not "
                 + name);
        }

        String firstFrameName = name.substring(0, refIndex);

        String relativeName
             = name.substring(refIndex + Project.REF_DELIMITER.length());

        Map frameOverrides = (Map) overrides.get(firstFrameName);
        if (frameOverrides == null) {
            return null;
        }

        return frameOverrides.get(relativeName);
    }

    /**
     * Get a value which exists in the frame property overrides awaiting the
     * frame to be introduced.
     *
     * @param name the name of the value
     * @return the value of the property or null if the property does not
     *      exist.
     * @exception ExecutionException if attempting to check an override in the
     *      current frame.
     */
    private boolean isOverrideSet(String name) throws ExecutionException {
        int refIndex = name.indexOf(Project.REF_DELIMITER);
        if (refIndex == -1) {
            throw new ExecutionException("Property overrides can only be"
                 + " returned for properties in referenced projects - not "
                 + name);
        }

        String firstFrameName = name.substring(0, refIndex);

        String relativeName
             = name.substring(refIndex + Project.REF_DELIMITER.length());

        Map frameOverrides = (Map) overrides.get(firstFrameName);
        if (frameOverrides == null) {
            return false;
        }

        return frameOverrides.containsKey(relativeName);
    }


    /**
     * Add a collection of properties to this frame with a given priority.
     *
     * @param properties the collection of property values, indexed by their
     *      names
     * @param priority the priority at which the values are added.
     * @exception AntException if the values cannot be added.
     */
    protected void addProperties(Map properties, int priority)
         throws AntException {
        addDataValues(DataValue.makeDataValues(properties, priority));
    }

    /**
     * Add a set of data values.
     *
     * @param dataValues a collection of DataValue instances named
     * @exception AntException if the values cannot be added.
     */
    protected void addDataValues(Map values) throws AntException {
        mergeDataValues(dataValues, values);
    }

    /**
     * Merge one set of values into another
     *
     * @param values the values to which the new values are added
     * @param newValues the values to be added in.
     */
    public void mergeDataValues(Map values, Map newValues) {
        mergeDataValues(values, newValues, DataValue.PRIORITY_BASE);
    }

    /**
     * Merge in values which are of a given priority or higher.
     *
     * @param values the values to which the new values are added
     * @param newValues the values to be added in.
     * @param threshold The require data value priority for a value to be
     *        merged.
     */
    public void mergeDataValues(Map values, Map newValues, int threshold) {
        if (newValues == null) {
            return;
        }

        for (Iterator i = newValues.keySet().iterator(); i.hasNext();) {
            String name = (String) i.next();
            DataValue value = (DataValue) newValues.get(name);
            if (value.getPriority() >= threshold) {
                updateValue(values, name, value, false);
            }
        }
    }
}

