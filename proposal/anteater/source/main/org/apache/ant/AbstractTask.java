package org.apache.ant;

import java.io.*;
import java.util.*;
import java.lang.reflect.*;
import java.beans.*;

/**
 * Superclass of all Tasks. All tasks extend from this.
 *
 * @author James Duncan Davidson (duncan@apache.org)
 */
public abstract class AbstractTask {
    
    // -----------------------------------------------------------------
    // PROTECTED DATA MEMBERS
    // -----------------------------------------------------------------
    
    /**
     *
     */
    protected Project project;
    
    // -----------------------------------------------------------------
    // ABSTRACT PUBLIC METHODS
    // -----------------------------------------------------------------     
    
    /**
     *
     */
    public abstract boolean execute() throws AntException;
    
    // -----------------------------------------------------------------
    // PUBLIC METHODS
    // -----------------------------------------------------------------  
    
    /**
     * Used by the system to set the attributes which then get reflected
     * into the particular implementation class
     */
    public void setAttributes(Hashtable attributes) {
        Class clazz = this.getClass();
        BeanInfo bi;
        try {
            bi = Introspector.getBeanInfo(clazz);
        } catch (IntrospectionException ie) {
            System.out.println("Can't reflect on: " + clazz);
            // XXX exception out
            return;
        }
        PropertyDescriptor[] pda = bi.getPropertyDescriptors();
        for (int i = 0; i < pda.length; i++) {
            PropertyDescriptor pd = pda[i];
            String property = pd.getName();
            Object o = attributes.get(property);
            if (o != null) {
                String value = (String)o;
                Method setMethod = pd.getWriteMethod();
                if (setMethod != null) {
                    Class[] ma = setMethod.getParameterTypes();
                    if (ma.length == 1) {
                        Class c = ma[0];
                        if (c.getName().equals("java.lang.String")) {
                            try {
                                setMethod.invoke(this, new String[] {value});
                            } catch (Exception e) {
                                // XXX bad bad bad -- narrow to exact exceptions
                                System.out.println("OUCH: " + e);
                                // XXX exception out.
                            }
                        }
                    }
                }
            }
        }
    }
    
    /**
     * Used by system to set the project.
     */  
    public void setProject(Project project) {
        this.project = project;
    }
  
}