/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 1999, 2000 The Apache Software Foundation.  All rights
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
package org.apache.tools.ant.gui.customizer;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.BorderLayout;
import java.util.*;

/**
 * Custom property editor for the Properties class.
 * 
 * @version $Revision$ 
 * @author Simeon Fitch 
 */
public class PropertiesPropertyEditor extends AbstractPropertyEditor {
    /** Recommended size for widgets inside a JScrollPane, as communicated
     *  through the setPreferredScrollableViewportSize() method. */
    protected static final Dimension VIEWPORT_SIZE = new Dimension(200, 50);

    /** Container. */
    private JPanel _widget = null;
    /* The current properties being edited. */
    private Properties _properties = null;
    /** The table editor for the properties. */
    private JTable _table = null;

    /** 
     * Default ctor.
     * 
     */
    public PropertiesPropertyEditor() {
        _widget = new JPanel(new BorderLayout());
        _widget.addFocusListener(new FocusHandler(this));

        _table = new JTable();
        _table.setPreferredScrollableViewportSize(VIEWPORT_SIZE);
        JScrollPane scroller = new JScrollPane(_table);
        _widget.add(BorderLayout.CENTER, scroller);
    }

    /** 
     * Get the child editing component. Uses JComponent so we can have tool
     * tips, etc.
     * 
     * @return Child editing component.
     */
    protected Component getChild() {
        return _widget;
    }

    /**
     * This method is intended for use when generating Java code to set
     * the value of the property.  It should return a fragment of Java code
     * that can be used to initialize a variable with the current property
     * value.
     * <p>
     * Example results are "2", "new Color(127,127,34)", "Color.orange", etc.
     *
     * @return A fragment of Java code representing an initializer for the
     *      current value.
     */
    public String getJavaInitializationString() {
        return getAsText();
    }

    /**
     * Set (or change) the object that is to be edited.  Builtin types such
     * as "int" must be wrapped as the corresponding object type such as
     * "java.lang.Integer".
     *
     * @param value The new target object to be edited.  Note that this
     *     object should not be modified by the PropertyEditor, rather 
     *     the PropertyEditor should create a new object to hold any
     *     modified value.
     */
    public void setValue(Object value) {
        if(value != null && !(value instanceof Properties)) {
            throw new IllegalArgumentException(
                value.getClass().getName() + " is not of type Properties.");
        }

        Object old = _properties;
        _properties = (Properties) ((Properties) value).clone();

        PropertiesTableModel model = new PropertiesTableModel();
        _table.setModel(model);
        _table.clearSelection();
    }

    /**
     * @return The value of the property.  Builtin types
     * such as "int" will be wrapped as the corresponding
     * object type such as "java.lang.Integer".  
     */
    public Object getValue() {
        return _properties;
    }

    /**
     * Set the property value by parsing a given String.  May raise
     * java.lang.IllegalArgumentException if either the String is
     * badly formatted or if this kind of property can't be expressed
     * as text.
     * @param text  The string to be parsed.
     */
    public void setAsText(String text) throws IllegalArgumentException {
        throw new IllegalArgumentException("Cannot be expressed as a String");
    }

    /**
     * @return The property value as a human editable string.
     * <p>   Returns null if the value can't be expressed 
     *       as an editable string.
     * <p>   If a non-null value is returned, then the PropertyEditor should
     *       be prepared to parse that string back in setAsText().
     */
    public String getAsText() {
        return null;
    } 

    /** Table model view of the Properties object. */
    private class PropertiesTableModel extends AbstractTableModel {
        private static final int NAME = 0;
        private static final int VALUE = 1;

        private List _keys = null;

        public PropertiesTableModel() {
            // We need to store the property keys in an array
            // so that the ordering is preserved.
            _keys = new ArrayList(_properties.keySet());
            Collections.sort(_keys);
        }

        /** 
         * Get the number of rows. 
         * 
         * @return Number of rows.
         */
        public int getRowCount() {
            return _properties.size() + 1;
        }

        /** 
         * Get the number of columns.
         * 
         * @return 2
         */
        public int getColumnCount() {
            return 2;
        }

        /** 
         * Get the editing and display class of the given column.
         * 
         * @return String.class
         */
        public Class getColumnClass(int column) {
            return String.class;
        }

        /** 
         * Get the header name of the column.
         * 
         * @param column Column index.
         * @return Name of the column.
         */
        public String getColumnName(int column) {
            // XXX fix me.
            return column == NAME ? "Name" : "Value";
        }

        /** 
         * Determine if the given cell is editable.
         * 
         * @param row Cell row.
         * @param column Cell column.
         * @return true
         */
        public boolean isCellEditable(int row, int column) {
            return true;
        }

        /** 
         * Get the object at the given table coordinates.
         * 
         * @param row Table row.
         * @param column Table column.
         * @return Object at location, or null if none.
         */
        public Object getValueAt(int row, int column) {
            if(row < _properties.size()) {
                switch(column) {
                  case NAME: 
                      return _keys.get(row);
                  case VALUE: 
                      return _properties.getProperty((String)_keys.get(row));
                }
            }
            return null;
        }
        /** 
         * Set the table value at the given location.
         * 
         * @param value Value to set.
         * @param row Row.
         * @param column Column.
         */
        public void setValueAt(Object value, int row, int column) {
            String k = null;
            String v = null;

            String currKey = (String) getValueAt(row, NAME);
            switch(column) {
              case NAME: 
                  k = String.valueOf(value);
                  if(row < _keys.size()) {
                      _keys.set(row, k);
                  }
                  else {
                      _keys.add(k);
                  }
                  String currValue = null;
                  if(currKey != null) {
                      currValue = _properties.getProperty(currKey);
                      _properties.remove(currKey);
                  }
                  v = currValue == null ? "" : currValue;
                  break;
              case VALUE:
                  v = String.valueOf(value);
                  k = currKey;
                  if(k == null || k.length() == 0) {
                      k = "key-for-" + v;
                  }
                  break;
            }

            if(k.length() > 0) {
                _properties.setProperty(k, v);
            }

            fireTableRowsUpdated(row, row);
            // Fire change in outer class.
            firePropertyChange(null, _properties);
        }
    }
}


