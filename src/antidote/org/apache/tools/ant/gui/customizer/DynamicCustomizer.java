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
 * 4. The names "The Jakarta Project", "Tomcat", and "Apache Software
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

import org.apache.tools.ant.gui.LabelFieldGBC;
import java.lang.reflect.*;
import java.beans.*;
import javax.swing.*;
import java.util.Hashtable;
import java.util.Enumeration;
import java.awt.GridBagLayout;
import java.awt.GridBagConstraints;
import java.awt.Component;

/**
 * Widget for dynamically constructing a property editor based on the 
 * an Object's BeanInfo. Essentially a property sheet.
 * 
 * @version $Revision$ 
 * @author Simeon Fitch 
 */
public class DynamicCustomizer extends JPanel {
	static {
		PropertyEditorManager.registerEditor(
			String.class, StringPropertyEditor.class);
		PropertyEditorManager.registerEditor(
			int.class, IntegerPropertyEditor.class);
		PropertyEditorManager.registerEditor(
			Integer.class, IntegerPropertyEditor.class);
		PropertyEditorManager.registerEditor(
			double.class, DoublePropertyEditor.class);
		PropertyEditorManager.registerEditor(
			Double.class, DoublePropertyEditor.class);
	}

	/** The type that this editor instance can handle. */
	private Class _type = null;
	/** The value currently being edited. */
	private Object _value = null;
	/** Mapping from PropertyDescriptor to PropertyEditor. */
	private Hashtable _prop2Editor = new Hashtable();
	/** Mapping from PropertyEditor to field PropertyDescriptor. */
	private Hashtable _editor2Prop = new Hashtable();
	/** Listener for receiving change events from the editors. */
	private EditorChangeListener _eListener = new EditorChangeListener();
    /** Read-only flag. */
    private boolean _readOnly = false;


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
            setBorder(BorderFactory.createTitledBorder(
                info.getBeanDescriptor().getDisplayName()));
            PropertyDescriptor[] props = info.getPropertyDescriptors();
            for(int i = 0; i < props.length; i++) {
                if(props[i].getName().equals("class")) continue;
                JLabel label = new JLabel(props[i].getDisplayName() + ":");
                
                // Lookup the editor.
                PropertyEditor editor = getEditorForProperty(props[i]);
                if(editor == null) continue;
                // Add a listener to the editor so we know when to update
                // the bean's fields.
                editor.addPropertyChangeListener(_eListener);
                
                // XXX What we need to do right here is provide a component
                // that makes use of the "paintable" capability of the editor.
                Component comp = editor.getCustomEditor();
                if(comp == null) {
                    comp = new JLabel("<<null editor>>");
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
        
        // Iterate over each property, doing a lookup on the associated editor
        // and setting the editor's value to the value of the property.
        Enumeration enum = _prop2Editor.keys();
        while(enum.hasMoreElements()) {
            PropertyDescriptor desc = (PropertyDescriptor) enum.nextElement();
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
    }

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
            retval = PropertyEditorManager.findEditor(prop.getPropertyType());
        }

        return retval;
    }

    /** Class for receiving change events from teh PropertyEditor objects. */
    private class EditorChangeListener implements PropertyChangeListener {
        public void propertyChange(PropertyChangeEvent e) {
            PropertyEditor editor = (PropertyEditor) e.getSource();
            PropertyDescriptor prop =
                (PropertyDescriptor) _editor2Prop.get(editor);
            Method writer = prop.getWriteMethod();
            if(writer != null) {
                try {
                    Object[] params = { editor.getValue() };
                    writer.invoke(_value, params);
                    //firePropertyChange(
                    //prop.getName(), null, editor.getValue());
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


    /** 
     * Test code.
     * 
     * @param args First arg is the class name to create
     */
    public static void main(String[] args) {

        try {
            Class c = Class.forName(args[0]);
            JFrame f = new JFrame(c.getName());
            DynamicCustomizer custom = 
                new DynamicCustomizer(c);
            custom.setObject(c.newInstance());
            f.getContentPane().add(custom);
            f.pack();
            f.setVisible(true);
        }
        catch(Exception ex) {
            ex.printStackTrace();
        }
    }
}
