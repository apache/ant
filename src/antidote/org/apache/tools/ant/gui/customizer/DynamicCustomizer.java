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

import org.apache.tools.ant.gui.util.LabelFieldGBC;
import java.lang.reflect.*;
import java.beans.*;
import javax.swing.*;
import java.util.*;
import java.io.File;
import java.awt.*;

/**
 * Widget for dynamically constructing a property editor based on the 
 * an Object's BeanInfo. Essentially a property sheet.
 * 
 * @version $Revision$ 
 * @author Simeon Fitch 
 */
public class DynamicCustomizer extends JPanel implements Customizer, Scrollable {
	static {
		PropertyEditorManager.registerEditor(
			String.class, StringPropertyEditor.class);
		PropertyEditorManager.registerEditor(
			String[].class, StringArrayPropertyEditor.class);
		PropertyEditorManager.registerEditor(
			int.class, IntegerPropertyEditor.class);
		PropertyEditorManager.registerEditor(
			Integer.class, IntegerPropertyEditor.class);
		PropertyEditorManager.registerEditor(
			double.class, DoublePropertyEditor.class);
		PropertyEditorManager.registerEditor(
			Double.class, DoublePropertyEditor.class);
		PropertyEditorManager.registerEditor(
			Properties.class, PropertiesPropertyEditor.class);
		PropertyEditorManager.registerEditor(
			File.class, FilePropertyEditor.class);
		PropertyEditorManager.registerEditor(
			Object.class, ObjectPropertyEditor.class);
	}

    /** Property name that PropertyDescriptors can save in their property
     *  dictionaries for for specifiying a display sorting order. The value
     *  sould be of type Integer. */
    public static final String SORT_ORDER = "sortOrder";

	/** The type that this editor instance can handle. */
	private Class _type = null;
	/** The value currently being edited. */
	private Object _value = null;
	/** Mapping from PropertyDescriptor to PropertyEditor. */
	private Map _prop2Editor = new HashMap();
	/** Mapping from PropertyEditor to field PropertyDescriptor. */
	private Map _editor2Prop = new HashMap();
	/** Listener for receiving change events from the editors. */
	private EditorChangeListener _eListener = new EditorChangeListener();
    /** Read-only flag. */
    private boolean _readOnly = false;
    /** List of property change listeners interested when the bean
     *  being edited has been changed. */
    private java.util.List _changeListeners = new LinkedList();
    /** Flag to trun off event propogation. */
    private boolean _squelchChangeEvents = false;

	/** 
     * Standard constructor.
     *
     * @param type Type that you are going to be creating and editor for.
     */
    public DynamicCustomizer(Class type) {
        this(type, false);
    }

	/** 
     * Standard constructor.
     *
     * @param type Type that you are going to be creating and editor for.
     * @param readOnly Set to true to create a read-only customizer.
     */
    public DynamicCustomizer(Class type, boolean readOnly) {
        super(new GridBagLayout());
        _readOnly = readOnly;
        _type = type;
        
        LabelFieldGBC gbc = new LabelFieldGBC();
        try {
            BeanInfo info = Introspector.getBeanInfo(type);
            // Set up pretty display stuff.
            setBorder(BorderFactory.createTitledBorder(
                info.getBeanDescriptor().getDisplayName()));
            setToolTipText(info.getBeanDescriptor().getShortDescription());

            // Get the properties and sort them.
            PropertyDescriptor[] props = info.getPropertyDescriptors();
            Arrays.sort(props, new PropertyComparator());
            for(int i = 0; i < props.length; i++) {
                // Ignore the "class" property, if it is provided.
                if(props[i].getName().equals("class")) continue;
                // Create a label for the field.
                JLabel label = new JLabel(props[i].getDisplayName() + ":");
                
                // Lookup the editor.
                PropertyEditor editor = getEditorForProperty(props[i]);

                // Add a listener to the editor so we know when to update
                // the bean's fields.
                editor.addPropertyChangeListener(_eListener);
                
                // XXX What we need to do right here is provide a component
                // that makes use of the "paintable" capability of the editor.
                Component comp = editor.getCustomEditor();
                if(comp == null) {
                    comp = new JLabel("<No editor available.>");
                    ((JLabel)comp).setBorder(BorderFactory.createEtchedBorder());
                }
                
                // See if it is a read-only property. If so, then just
                // display it.
                if(_readOnly || props[i].getWriteMethod() == null) {
                    comp.setEnabled(false);
                }

                // Setup the accellerator key.
                label.setLabelFor(comp);
                label.setDisplayedMnemonic(label.getText().charAt(0));

                // Set the tool tip text, if any.
                String tip = props[i].getShortDescription();
                if(tip != null) {
                    label.setToolTipText(tip);
                    if(comp instanceof JComponent) {
                        ((JComponent)comp).setToolTipText(tip);
                    }
                }

                // Add the label and fields.
                add(label, gbc.forLabel());
                add(comp, gbc.forField());

                // Set the mappings between editor and property, etc. for
                // quick lookup later.
                _prop2Editor.put(props[i], editor);
                _editor2Prop.put(editor, props[i]);
            }
            // Filler...
            add(new JLabel(), gbc.forLastLabel());

        }
        catch(Exception ex) {
            ex.printStackTrace();
        }
    }


    /** 
     * Set the object to be edited.
     * 
     * @param value The object to be edited.
     */
    public void setObject(Object value) {
        if(!(_type.isInstance(value))) {
            throw new IllegalArgumentException(
                value.getClass() + " is not of type " + _type);
        } 
        _value = value;
        
        // Disable event generation.
        _squelchChangeEvents = true;

        // Iterate over each property, doing a lookup on the associated editor
        // and setting the editor's value to the value of the property.
        Iterator it = _prop2Editor.keySet().iterator();
        while(it.hasNext()) {
            PropertyDescriptor desc = (PropertyDescriptor) it.next();
            PropertyEditor editor = (PropertyEditor) _prop2Editor.get(desc);
            Method reader = desc.getReadMethod();
            if(reader != null) {
                try {
                    Object val = reader.invoke(_value, null);
                    editor.setValue(val);
                }
                catch(IllegalAccessException ex) {
                    ex.printStackTrace();
                }
                catch(InvocationTargetException ex) {
                    ex.getTargetException().printStackTrace();
                }
            }
        }

        // Enable event generation.
        _squelchChangeEvents = false;

    }

	/** 
	 * Get the appropriate editor for the given property.
	 * 
	 * @param prop Property to get editor for.
	 * @return Editor to use, or null if none found.
	 */
    private PropertyEditor getEditorForProperty(PropertyDescriptor prop) {
        PropertyEditor retval = null;
        Class type = prop.getPropertyEditorClass();
        if(type != null) {
            try {
                retval = (PropertyEditor) type.newInstance();
            }
            catch(Exception ex) {
                ex.printStackTrace();
            }
        }
        // Handle case where there is no special editor
        // associated with the property. In that case we ask the 
        // PropertyEditor manager for the editor registered for the
        // given property type.
        if(retval == null) {
            Class t = prop.getPropertyType();
            if(t != null) {
                retval = PropertyEditorManager.findEditor(t);
            }
        }

        // In the worse case we resort to the generic editor for Object types.
        if(retval == null) {
            retval = PropertyEditorManager.findEditor(Object.class);
        }

        return retval;
    }

	/** 
	 * Add the given listener. Will receive a change event for 
     * changes to the bean being edited.
	 * 
	 * @param l Listner to add.
	 */
    public void addPropertyChangeListener(PropertyChangeListener l) {
        _changeListeners.add(l);
    }


	/** 
	 * Remove the given property change listener.
	 * 
	 * @param l Listener to remove.
	 */
    public void removePropertyChangeListener(PropertyChangeListener l) {
        _changeListeners.remove(l);
    }

	/** 
	 * Fire a property change event to each listener.
	 * 
	 * @param bean Bean being edited. 
	 * @param propName Name of the property.
	 * @param oldValue Old value.
	 * @param newValue New value.
	 */
    protected void firePropertyChange(Object bean, String propName, 
                                      Object oldValue, Object newValue) {

        PropertyChangeEvent e = new PropertyChangeEvent(
            bean, propName, oldValue, newValue);

        Iterator it = _changeListeners.iterator();
        while(it.hasNext()) {
            PropertyChangeListener l = (PropertyChangeListener) it.next();
            l.propertyChange(e);
        }
    }

    /**
     * Returns the preferred size of the viewport for a view component.
     * For example the preferredSize of a JList component is the size
     * required to acommodate all of the cells in its list however the
     * value of preferredScrollableViewportSize is the size required for
     * JList.getVisibleRowCount() rows.   A component without any properties
     * that would effect the viewport size should just return 
     * getPreferredSize() here.
     * 
     * @return The preferredSize of a JViewport whose view is this Scrollable.
     * @see JViewport#getPreferredSize
     */
    public Dimension getPreferredScrollableViewportSize() {
        Dimension size = getPreferredSize();
        Dimension retval = new Dimension();
        retval.width = size.width > 600 ? 600 : size.width;
        retval.height = size.height > 400 ? 400 : size.height;
        return retval;
    }


    /**
     * Components that display logical rows or columns should compute
     * the scroll increment that will completely expose one new row
     * or column, depending on the value of orientation.  Ideally, 
     * components should handle a partially exposed row or column by 
     * returning the distance required to completely expose the item.
     * <p>
     * Scrolling containers, like JScrollPane, will use this method
     * each time the user requests a unit scroll.
     * 
     * @param visibleRect The view area visible within the viewport
     * @param orientation Either SwingConstants.VERTICAL or 
     *                    SwingConstants.HORIZONTAL.
     * @param direction Less than zero to scroll up/left, 
     *                  greater than zero for down/right.
     * @return The "unit" increment for scrolling in the specified direction
     * @see JScrollBar#setUnitIncrement
     */
    public int getScrollableUnitIncrement(Rectangle visibleRect, 
                                          int orientation, int direction) {
        return 1;
    }


    /**
     * Components that display logical rows or columns should compute
     * the scroll increment that will completely expose one block
     * of rows or columns, depending on the value of orientation. 
     * <p>
     * Scrolling containers, like JScrollPane, will use this method
     * each time the user requests a block scroll.
     * 
     * @param visibleRect The view area visible within the viewport
     * @param orientation Either SwingConstants.VERTICAL or 
     *                    SwingConstants.HORIZONTAL.
     * @param direction Less than zero to scroll up/left, 
     *                  greater than zero for down/right.
     * @return The "block" increment for scrolling in the specified direction.
     * @see JScrollBar#setBlockIncrement
     */
    public int getScrollableBlockIncrement(Rectangle visibleRect, 
                                           int orientation, int direction) {
        return orientation == SwingConstants.VERTICAL ? 
            visibleRect.height / 2 : visibleRect.width / 2;
    }
    

    /**
     * Return true if a viewport should always force the width of this 
     * Scrollable to match the width of the viewport.  For example a noraml 
     * text view that supported line wrapping would return true here, since it
     * would be undesirable for wrapped lines to disappear beyond the right
     * edge of the viewport.  Note that returning true for a Scrollable
     * whose ancestor is a JScrollPane effectively disables horizontal
     * scrolling.
     * <p>
     * Scrolling containers, like JViewport, will use this method each 
     * time they are validated.  
     * 
     * @return True if a viewport should force the Scrollables 
     *         width to match its own.
     */
    public boolean getScrollableTracksViewportWidth() {
        return true;
    }

    /**
     * Return true if a viewport should always force the height of this 
     * Scrollable to match the height of the viewport.  For example a 
     * columnar text view that flowed text in left to right columns 
     * could effectively disable vertical scrolling by returning
     * true here.
     * <p>
     * Scrolling containers, like JViewport, will use this method each 
     * time they are validated.  
     * 
     * @return True if a viewport should force the Scrollables 
     *         height to match its own.
     */
    public boolean getScrollableTracksViewportHeight() {
        return false;
    }


    /** Class for receiving change events from the PropertyEditor objects. */
    private class EditorChangeListener implements PropertyChangeListener {
        public void propertyChange(PropertyChangeEvent e) {
            if(_squelchChangeEvents) return;

            PropertyEditor editor = (PropertyEditor) e.getSource();
            PropertyDescriptor prop =
                (PropertyDescriptor) _editor2Prop.get(editor);
            Method writer = prop.getWriteMethod();
            if(writer != null) {
                try {
                    Object[] params = { editor.getValue() };
                    writer.invoke(_value, params);
                    setObject(_value);
                    firePropertyChange(
                        _value, prop.getName(), null, editor.getValue());
                }
                catch(IllegalAccessException ex) {
                    ex.printStackTrace();
                }
                catch(InvocationTargetException ex) {
                    ex.getTargetException().printStackTrace();
                }
            }
        }
    }


    /** Comparator for sorting PropertyDescriptor values. */
    private static class PropertyComparator implements Comparator {
        public int compare(Object o1, Object o2) {
            PropertyDescriptor p1 = (PropertyDescriptor)o1;
            PropertyDescriptor p2 = (PropertyDescriptor)o2;

            Integer i1 = (Integer) p1.getValue(SORT_ORDER);
            Integer i2 = (Integer) p2.getValue(SORT_ORDER);
            
            if(i1 == null && i2 == null) {
                return p1.getName().compareTo(p2.getName());
            }
            else if(i1 != null) {
                return i1.compareTo(i2);
            }
            else {
                return i2.compareTo(i1) * -1;
            }
        }
    }


    /*----------------------------------------------------------------------*/

    /** Class for testing this. */
    private static class TestClass implements Cloneable {
        private String _String = "This string is my name.";
        private String[] _StringArray = { "one", "two", "three" };
        private int _int = Integer.MIN_VALUE;
        private Integer _Integer = new Integer(Integer.MAX_VALUE);
        private double _double = Double.MIN_VALUE;
        private Double _Double = new Double(Double.MAX_VALUE);
        private Properties _Properties = System.getProperties();
        private File _File = new File("/");
        private Object _Object = new Font("Monospaced", Font.PLAIN, 12);
        private JButton _button = new JButton("I'm a button!");

        public void setName(String string) {
            _String = string;
        }
        public String getName() {
            return _String;
        }

        public void setStringArray(String[] array) {
            _StringArray = array;
        }
        public String[] getStringArray() {
            return _StringArray;
        }

        public void setInt(int val) {
            _int = val;
        }
        public int getInt() {
            return _int;
        }

        public void setInteger(Integer val) {
            _Integer = val;
        }
        public Integer getInteger() {
            return _Integer;
        }

        public void setDoub(double val) {
            _double = val;
        }
        public double getDoub() {
            return _double;
        }

        public void setDouble(Double val) {
            _Double = val;
        }
        public Double getDouble() {
            return _Double;
        }

        public void setProperties(Properties props) {
            _Properties = props;
        }
        public Properties getProperties() {
            return _Properties;
        }

        public void setFile(File f) {
            _File = f;
        }
        public File getFile() {
            return _File;
        }
        
        public void setButton(JButton button) {
            _button = button;
        }

        public JButton getButton() {
            return _button;
        }

        public void setObject(Object o) {
            _Object = o;
        }

        public Object getObject() {
            return _Object;
        }

        public Object clone() {
            try {
                return super.clone();
            }
            catch(CloneNotSupportedException ex) {
                return null;
            }
        }
    }


    /** 
     * Test code.
     * 
     * @param args First arg is the class name to create
     */
    public static void main(String[] args) {

        try {
            Class type = null;
            Object instance = null;
            if(args.length > 0) {
                type = Class.forName(args[0]);
                instance = type.newInstance();
            }
            else {
                type = TestClass.class;
                instance = new TestClass();
                ((TestClass)instance).setObject(new TestClass());
            }

            JFrame f = new JFrame(type.getName());
            DynamicCustomizer custom = new DynamicCustomizer(type);
            custom.setObject(instance);
            f.getContentPane().add(new JScrollPane(custom));
            f.pack();
            f.setVisible(true);
        }
        catch(Exception ex) {
            ex.printStackTrace();
        }
    }
}
