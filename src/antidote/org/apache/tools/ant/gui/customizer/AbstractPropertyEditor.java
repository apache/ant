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

import java.beans.*;
import java.awt.Graphics;
import java.awt.Component;
import java.awt.Rectangle;
import java.awt.Dimension;
import javax.swing.JComponent;
import java.awt.event.FocusEvent;
import java.awt.event.FocusAdapter;

/**
 * Abstract base class for the custom type property editors.
 * 
 * @version $Revision$ 
 * @author Simeon Fitch 
 */
public abstract class AbstractPropertyEditor implements PropertyEditor {

    /** Bean property change property name. */
    public static final String BEAN_PROP = "BeanEditorProperty";
    /** Event listener support. */
    private PropertyChangeSupport _listeners = new PropertyChangeSupport(this);

    /** 
     * Default constructor.
     * 
     */
    protected AbstractPropertyEditor() {
    }

    /**
     * Paint a representation of the value into a given area of screen
     * real estate.  Note that the propertyEditor is responsible for doing
     * its own clipping so that it fits into the given rectangle.
     * <p>
     * If the PropertyEditor doesn't honor paint requests (see isPaintable)
     * this method should be a silent noop.
     * <p>
     * The given Graphics object will have the default font, color, etc of
     * the parent container.  The PropertyEditor may change graphics attributes
     * such as font and color and doesn't need to restore the old values.
     *
     * @param gfx  Graphics object to paint into.
     * @param box  Rectangle within graphics object into which we should paint.
     */
    public void paintValue(Graphics gfx, Rectangle box) {
        Object o = getValue();
        String s = o == null ? "<null>" : o.toString();
        gfx.drawString(s, box.x, box.y);
    }


    /** 
     * Fire a property change event to listeners.
     * 
     * @param oldValue Old value.
     * @param newValue New value.
     */
    public void firePropertyChange(Object oldValue, Object newValue) {
        _listeners.firePropertyChange(BEAN_PROP, oldValue, newValue);
    }

    /** 
     * Add a property change listener. XXX This may cause undesired
     * side affects with merging property changes with JPanel class.
     * Need to test for a while.
     * 
     * @param l Change listener.
     */
    public void addPropertyChangeListener(PropertyChangeListener l) {
        _listeners.addPropertyChangeListener(l);
    }

    /** 
     * Remove a property change listener. 
     * 
     * @param l Change listener.
     */
    public void removePropertyChangeListener(PropertyChangeListener l) {
        _listeners.removePropertyChangeListener(l);
    }

    /**
     * @return  True if the class will honor the paintValue method.
     */
    public boolean isPaintable() {
        return true;
    }

    /**
     * If the property value must be one of a set of known tagged values, 
     * then this method should return an array of the tags.  This can
     * be used to represent (for example) enum values.  If a PropertyEditor
     * supports tags, then it should support the use of setAsText with
     * a tag value as a way of setting the value and the use of getAsText
     * to identify the current value.
     *
     * @return The tag values for this property.  May be null if this 
     *   property cannot be represented as a tagged value.
     *  
     */
    public String[] getTags() {
        return null;
    }

    /**
     * A PropertyEditor may choose to make available a full custom Component
     * that edits its property value.  It is the responsibility of the
     * PropertyEditor to hook itself up to its editor Component itself and
     * to report property value changes by firing a PropertyChange event.
     * <P>
     * The higher-level code that calls getCustomEditor may either embed
     * the Component in some larger property sheet, or it may put it in
     * its own individual dialog, or ...
     *
     * @return A java.awt.Component that will allow a human to directly
     *      edit the current property value.  May be null if this is
     *      not supported.
     */
    public Component getCustomEditor() {
        return getChild();
    }

    /**
     * @return  True if the propertyEditor can provide a custom editor.
     */
    public boolean supportsCustomEditor() {
        return true;
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
        return "";
    }

    /** 
     * Get the child editing component. 
     * 
     * @return Child editing component.
     */
    protected abstract Component getChild();

    /** Helper class for detecting changes and generating change events
     *  on a focus lost event. */
    protected static class FocusHandler extends FocusAdapter {
        /** Last value of the editor. */
        private Object _value = null;
        /** Editor of interest. */
        private AbstractPropertyEditor _editor = null;

        /** 
         * Standard constructor.
         * 
         * @param editor Editor of interest.
         */
        public FocusHandler(AbstractPropertyEditor editor) {
            _editor = editor;
        }

        /** 
         * Called when focus is gained.
         * 
         * @param e Focus event.
         */
        public void focusGained(FocusEvent e) {
            _value = _editor.getValue();
        }
        
        /** 
         * Called when focus is lost. Checks to see if value changed and
         * fires a change event if needed.
         * 
         * @param e Focus event.
         */
        public void focusLost(FocusEvent e) {
            if((_value != null && !_value.equals(_editor.getValue())) ||
               (_value == null && _editor.getValue() != null)) {
                _editor.firePropertyChange(_value, _editor.getValue());
            }
        }
    }
}
