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

package org.apache.tools.ant.gui.modules.edit;
import org.apache.tools.ant.gui.customizer.AbstractPropertyEditor;
import org.apache.tools.ant.gui.acs.ACSDtdDefinedAttributes;
import javax.swing.*;
import javax.swing.table.*;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.BorderLayout;
import java.awt.event.*;
import java.util.*;

/**
 * Custom property editor for the DtdAttributes.
 *
 * @version $Revision$
 * @author Nick Davis<a href="mailto:nick_home_account@yahoo.com">nick_home_account@yahoo.com</a>
 */
public class DtdAttributePropertyEditor extends AbstractPropertyEditor {

    /** Recommended size for widgets inside a JScrollPane, as communicated
     *  through the setPreferredScrollableViewportSize() method. */
    protected static final Dimension VIEWPORT_SIZE = new Dimension(200, 150);

    /** Container. */
    private JPanel _widget = null;
    /* The current properties being edited. */
    private ACSDtdDefinedAttributes _attributes = null;
    /** The table editor for the properties. */
    private JTable _table = null;
    /** Displays possible attribute values. */
    private JComboBox _combo = new JComboBox();

    /**
     * Default ctor.
     *
     */
    public DtdAttributePropertyEditor() {
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
        return "new ACSDtdDefinedAttributes()";
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
        if(value == null) {
            value = new ACSDtdDefinedAttributes();
        }

        if(!(value instanceof ACSDtdDefinedAttributes)) {
            throw new IllegalArgumentException(
                value.getClass().getName() +
                " is not of type ACSDtdDefinedAttributes.");
        }

        Object old = _attributes;
        _attributes = (ACSDtdDefinedAttributes)
            ((ACSDtdDefinedAttributes) value).clone();

        TableModel model = new TableModel();
        _table.setModel(model);

        // Setup the combo box
        updateComboBox();
        _combo.setEditable(true);
        
        // Set the first column to use the combo box
        TableColumn tableColumn = _table.getColumnModel().getColumn(0);
        tableColumn.setCellEditor(new DefaultCellEditor(_combo));

        // When the combo box is updated, update the table.
        _combo.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                JComboBox cb = (JComboBox)e.getSource();
                String newSelection = (String)cb.getSelectedItem();

                // Should we update the table?
                if (newSelection != null && _table.getEditingRow() > 0) {
                    _table.getModel().setValueAt(newSelection,
                        _table.getEditingRow(), _table.getEditingColumn() );
                }
            }
        });

        _table.clearSelection();
    }

    /**
     * Fills the combobox with possible values
     */
    private void updateComboBox() {
        _combo.removeAllItems();
        ArrayList array = new ArrayList();

        // Add the optional attributes
        String[] valueArray = _attributes.getOptionalAttributes();
        if (valueArray != null) {
            for(int i = 0; i < valueArray.length; i++) {
                if (_attributes.getProperty(valueArray[i]) == null) {
                    array.add(valueArray[i]);
                }
            }
        }

        // Add the required attributes
        valueArray = _attributes.getRequiredAttributes();
        if (valueArray != null) {
            for(int i = 0; i < valueArray.length; i++) {
                if (_attributes.getProperty(valueArray[i]) == null) {
                    array.add(valueArray[i]);
                }
            }
        }
        
        Collections.sort(array);
        for(int i = 0; i < array.size(); i++) {
            _combo.addItem(array.get(i));
        }
    }

    /**
     * @return The value of the property.  Builtin types
     * such as "int" will be wrapped as the corresponding
     * object type such as "java.lang.Integer".
     */
    public Object getValue() {
        return _attributes;
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
    private class TableModel extends AbstractTableModel {
        private static final int NAME = 0;
        private static final int VALUE = 1;

        private List _keys = null;

        public TableModel() {
            // We need to store the property keys in an array
            // so that the ordering is preserved.
            _keys = new ArrayList(_attributes.keySet());
            Collections.sort(_keys);
        }

        /**
         * Get the number of rows.
         *
         * @return Number of rows.
         */
        public int getRowCount() {
            return _attributes.size() + 1;
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
            if(row < _attributes.size()) {
                switch(column) {
                  case NAME:
                      return _keys.get(row);
                  case VALUE:
                      return _attributes.getProperty((String)_keys.get(row));
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

            // Get the current key and value.
            String currKey = (String) getValueAt(row, NAME);
            String currValue = null;
            if(currKey != null) {
                currValue = _attributes.getProperty(currKey);
            }
            
            switch(column) {
              case NAME:
                  k = (String) value;
                  
                  // Update or add the key value.
                  if(row < _keys.size()) {
                      _keys.set(row, k);
                  }
                  else {
                      _keys.add(k);
                  }
                  
                  // Remove the old key.
                  if(currKey != null) {
                      _attributes.remove(currKey);
                  }
                  v = currValue == null ? "" : currValue;
                  break;
              case VALUE:
                  v = String.valueOf(value);
                  k = currKey;
                  
                  // Should we create a temp key?
                  if( (k == null || k.length() == 0 ) && v.length() != 0 ) {
                      k = "key-for-" + v;
                  }
                  break;
            }

            // If there is a key, update the list.
            if(k != null && k.length() > 0) {
                _attributes.setProperty(k, v);
            }

            // Has something changed?
            if( (k != null && v != null) && 
                (!k.equals(currKey) || !v.equals(currValue) ) ) {

                fireTableRowsUpdated(row, row);
                // Fire change in outer class.
                firePropertyChange(null, _attributes);
                
                // Reset the combobox
                updateComboBox();
            }
        }
    }
}
