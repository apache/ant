/* Copyright (c) 2000 The Apache Software Foundation */

package org.apache.tools.ant;

import java.beans.*;
import java.lang.reflect.*;
import java.util.*;

/**
 *  This class stores info about a bean's properties so that
 *  the actual bean can be instantiated at a later time. This data
 *  is used to store info about a task, since the actual
 *  task class might not be loaded until after parsing is completed.
 *
 *  @see TaskProxy
 *
 *  @author <a href="mailto:mpfoemme@thoughtworks.com">Matthew Foemmel</a>
 */
public class TaskData {
    private TaskProxy proxy;
    private String location;
    private String text;
    private Map properties;

    /**
     *  Constructs a new TaskData under the specified task.
     */
    public TaskData(TaskProxy proxy) {
        this.proxy = proxy;
        this.location = null;
        this.properties = new HashMap();
    }

    /**
     *  Returns the task proxy that this data is associated with.
     */
    public TaskProxy getTaskProxy() {
        return proxy;
    }

    /**
     *  Returns the location in the build fiole where this data was defined.
     */
    public String getLocation() {
        return location;
    }

    /**
     *  Returns the location in the build fiole where this data was defined.
     */
    public void setLocation(String location) {
        this.location = location;
    }

    /**
     *  Sets the text for this bean data, for cases where the bean is a simple
     *  type like String or int.
     */
    public void setText(String text) {
        this.text = text;
    }


    /**
     *  Sets the value of a property on the bean. Multiple properties can be
     *  added with the same name only if the property on the bean is an array.
     */
    public TaskData addProperty(String name) {
        TaskData data = new TaskData(proxy);
        getProperties(name).add(data);
        return data;
    }

    /**
     *  Returns the list of property values for the specified name.
     */
    private List getProperties(String name) {
        List result = (List) properties.get(name);
        if (result == null) {
            result = new ArrayList();
            properties.put(name, result);
        }
        return result;
    }

    /**
     *  Creates a new bean instance and initializes its properties.
     */
    public Object createBean(Class type) throws BuildException {
        Object bean = null;

        // See if an editor exists for this type
        PropertyEditor editor = PropertyEditorManager.findEditor(type);

        if (editor == null) {
            // We don't know how to handle text for types without editors
            if (text != null) {
                throw new BuildException("Unexpected text \"" + text + "\"", location);
            }

            try {
                bean = type.newInstance();
            }
            catch(InstantiationException exc) {
                throw new AntException("Unable to instantiate " + type.getName(), exc);
            }
            catch(IllegalAccessException exc) {
                throw new AntException("Unable to access constructor for " + type.getName(), exc);
            }
        }
        else {
            try {
                // Let the editor parse the text
                editor.setAsText(parseVariables(text));
            }
            catch(NumberFormatException exc) {
                throw new BuildException("\"" + text + "\" is not a valid number", location);
            }

            bean = editor.getValue();
        }

        // Update the fields on the bean
        updateProperties(bean);

        return bean;
    }

    /**
     *  Sets all of the property values on the bean.
     */
    private void updateProperties(Object bean) throws BuildException {

        // Call setProperty for each property that's been defined
        Iterator itr = properties.entrySet().iterator();
        while (itr.hasNext()) {
            Map.Entry entry = (Map.Entry) itr.next();
            String name = (String) entry.getKey();
            List values = (List) entry.getValue();
            setProperty(bean, name, values);
        }
    }

    /**
     *  Finds the PropertyDescriptor for the specifed property and sets it.
     */
    private void setProperty(Object bean, String name, List value) throws BuildException {
        PropertyDescriptor[] descriptors = getPropertyDescriptors(bean.getClass());

        // Search for the property with the matching name
        for (int i = 0; i < descriptors.length; i++) {
            if (descriptors[i].getName().equals(name)) {
                setProperty(bean, descriptors[i], value);
                return;
            }
        }

        throw new BuildException("Unexpected attribute \"" + name + "\"", location);
    }

    /**
     *  Sets a single property on a bean.
     */
    private static void setProperty(Object obj, PropertyDescriptor descriptor, List values) throws BuildException {
        Object value = null;

        Class type = descriptor.getPropertyType();

        if (type.isArray()) {
            value = createBeans(type.getComponentType(), values);
        }
        else if (values.size() == 1) {
            TaskData data = (TaskData) values.get(0);
            value = data.createBean(type);

        }

        try {
            descriptor.getWriteMethod().invoke(obj, new Object[] { value });
        }
        catch(IllegalAccessException exc) {
            throw new AntException("Unable to access write method for \"" + descriptor.getName() + "\"", exc);
        }
        catch(InvocationTargetException exc) {
            throw new AntException("Unable to set property \"" + descriptor.getName() + "\"", exc.getTargetException());
        }
    }

    /**
     *  Creates a number of beans with the same type using the list of TaskData's
     */
    private static Object[] createBeans(Class type, List values) throws BuildException {
        Object[] beans = (Object[]) Array.newInstance(type, values.size());
        int i = 0;
        Iterator itr = values.iterator();
        while (itr.hasNext()) {
            TaskData data = (TaskData) itr.next();
            beans[i++] = data.createBean(type);
        }
        return beans;
    }

    /**
     *  Uses the Introspector class to lookup the property descriptors for the class.
     */
    private static PropertyDescriptor[] getPropertyDescriptors(Class type) {
        try {
            return Introspector.getBeanInfo(type, Object.class).getPropertyDescriptors();
        }
        catch(IntrospectionException exc) {
            throw new AntException("Unable to get bean info for " + type.getName());
        }
    }

    /**
     *  Replaces any variables in the input string with their values.
     */
    private String parseVariables(String input) throws BuildException {
        StringBuffer output = new StringBuffer();

        int start = 0;
        int end = 0;
        while ((start = input.indexOf('{', end)) != -1) {
            output.append(input.substring(end,start));
            end = input.indexOf('}', start);
            if (end != -1) {
                String name = input.substring(++start, end++);
                String value = proxy.getTarget().getProject().getVariable(name);
                if (value == null) {
                    throw new BuildException("The variable \"" + name + "\" has not been defined");
                }
                output.append(value);
            }
        }

        output.append(input.substring(end));

        return output.toString();
    }
}