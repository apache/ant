package org.apache.ant.engine;

import java.util.*;

public class HierarchicalHashtable extends Hashtable {
    
    private HierarchicalHashtable parent;
    
    public HierarchicalHashtable() {
        this(null);
    }
    
    public HierarchicalHashtable(HierarchicalHashtable parent) {
        super();
        this.parent = parent;
    }
    
    public HierarchicalHashtable getParent() {
        return parent;
    }
    
    public void setParent(HierarchicalHashtable parent) {
        this.parent = parent;
    }
    
    public List getPropertyNames() {
        ArrayList list = new ArrayList();
        
        Enumeration e = keys();
        while (e.hasMoreElements()) {
            list.add(e.nextElement());
        }
        
        if (getParent() != null) {
            list.addAll(getParent().getPropertyNames());
        }
        
        return list;
    }
    
    public Object getPropertyValue(String name) {
        Object value = get(name);
        if (value == null && getParent() != null) {
            return getParent().getPropertyValue(name);
        }
        return value;
    }
    
    public void setPropertyValue(String name, Object value) {
        put(name, value);
    }
    
    public void removePropertyValue(String name) {
        Object value = get(name);
        if (value == null && getParent() != null) {
            getParent().removePropertyValue(name);
        }
        if (value != null) {
            remove(name);
        }
    }
}
