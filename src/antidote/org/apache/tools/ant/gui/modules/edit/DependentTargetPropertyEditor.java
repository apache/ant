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

import java.util.StringTokenizer;

import java.awt.Component;
import java.awt.BorderLayout;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.event.FocusListener;
import java.awt.event.FocusEvent;
import java.awt.event.KeyListener;
import java.awt.event.KeyEvent;
import java.awt.Point;
import java.awt.Dimension;
import java.awt.Toolkit;

import javax.swing.JPanel;
import javax.swing.JButton;
import javax.swing.JTextField;
import javax.swing.JOptionPane;
import javax.swing.BorderFactory;
import javax.swing.border.BevelBorder;

import org.apache.tools.ant.gui.customizer.AbstractPropertyEditor;
import org.apache.tools.ant.gui.acs.ACSTargetElement;

/**
 * PropertyEditor for DependentTargets
 * 
 * @version $Revision$ 
 * @author Christoph Wilhelms 
 */
public class DependentTargetPropertyEditor extends AbstractPropertyEditor {
    private JTextField _textField = null;
    private JButton _button = null;
    private JPanel _widget = null;
    private ACSTargetElement _value = null;
    private EventHandler _handler = new EventHandler();
    private static DependentTargetChooser _dialog = null;

    /**
     * Gets the editor component: A panel containing a textfield an a button.
     * @return the property editor component
     */
    public Component getChild() {
        if (_widget == null) // Lazy get
        {
            _widget = new JPanel();
            _widget.setLayout(new BorderLayout());
            _widget.add(getTextField(), BorderLayout.CENTER);
            _widget.add(getButton(), BorderLayout.EAST);
        }
        return _widget;
    }

    /**
     * Lazily create the textfield, to make sure its only instantiated once.
     * @return the button
     */
    private JTextField getTextField() {
        if (_textField == null) {
            _textField = new JTextField();
            _textField.setText(getAsText());
            _textField.setBorder(BorderFactory.createBevelBorder(BevelBorder.LOWERED));
            _textField.addFocusListener(_handler);
            _textField.addKeyListener(_handler);
            // We dont really need the next line in Antidote context, just for testpurpose!
            _textField.setPreferredSize(new Dimension(150, _textField.getPreferredSize().height));
        }
        // Transfer Tooltip from Panel to TextField - this hat to be done at every get!
        _textField.setToolTipText( _widget.getToolTipText());

        return _textField;
    }
    
    /**
     * Lazily create the button, to make sure its only instantiated once.
     * @return the button
     */
    private JButton getButton() {
        if (_button == null) {
            _button = new JButton();
            _button.setText("...");     // Probably an Image is more nice, but ... is standart, I think.
            _button.setPreferredSize(new Dimension(getTextField().getPreferredSize().height, getTextField().getPreferredSize().height));
            _button.addActionListener(_handler);
        }
        return _button;
    }
    
    /**
     * Sets the model for the component. Used to comunicate externally.
     * @param The target element the editior works with.
     */
    public void setValue(Object newValue) {
        if(!(newValue instanceof ACSTargetElement)) {
            throw new IllegalArgumentException(
                "Value must be of type ACSTargetElement.");
        }
        _value = (ACSTargetElement)newValue;
        
        // Directly show the targets.
        getTextField().setText(getAsText());
    }
    
    /**
     * Sets the model for the component. Used to comunicate externally.
     * @return The target element the editior works with.
     */
    public Object getValue() {
        return _value;
    }
    
    /**
     * Sets the depends for the target as text.
     * @param A String containing all targetnames, separated by ","
     */
    public void setAsText(String newValue) {
        String vals = getTextField().getText();
        StringTokenizer tok = new StringTokenizer(vals,",");
        String[] depends = new String[tok.countTokens()];
        for(int i = 0; i < depends.length; i++) {
            depends[i] = tok.nextToken().trim();
        }
        // Directly supply the target with the new depends.
        ((ACSTargetElement)getValue()).setDepends(depends);;
    }
    
    /**
     * Gets the depends for the target as text.
     * @return A String containing all targetnames, separated by ","
     */
    public String getAsText() {
        if (_value == null) return "";
        String[] dep = _value.getDepends();
        String retVal = "";
        for (int i = 0; i < dep.length; i++) {
            retVal += dep[i];
            if (i < dep.length - 1) retVal += ", ";
        }
        return retVal;
    }
    
    /**
     * Creates and shows the dialog for selecting dependent targets.
     */
    private void showSelectionDialog() {
        // Create the dialog lazyly - it is static, for we want only ONE instance!
        if (_dialog == null) {
            _dialog = new DependentTargetChooser(JOptionPane.getFrameForComponent(getChild()), (ACSTargetElement)getValue());
        }
        else {
            // Supply dialog with target - it needs nothing else ;-) - but the 
            // target will be modified by the dialog.
            _dialog.setTarget((ACSTargetElement)getValue());
        }
        String oldValue = getTextField().getText();
        // Set the position of the dialog right under the Editor, if possible
        DependentTargetPropertyEditor.setWindowRelativeToComponent (_dialog, getChild());
        _dialog.show();
        // after the modal dialog is disposed make shure that the propertyChangeEvent
        // will be thrown and the textfield becomes updated!
        Object newValue = (Object)getAsText();

        getTextField().setText(getAsText());
        firePropertyChange(newValue, oldValue);
    }
    
    /** 
     * Handler for ButtonAction, Focus- and KeyListening. I can't use the FocusHandler
     * in superclass, for I have a different Object in Property Change!
     */
    private class EventHandler implements ActionListener, FocusListener, KeyListener {
        /* ActionListener methods */
        public void actionPerformed(ActionEvent e) {
            if (e.getSource() == _button) showSelectionDialog();
	}
        /* FocusListener methods */
        public void focusLost(FocusEvent e) {
            Object oldValue = (Object)getAsText();
            String newValue = getTextField().getText();
            setAsText(newValue);
            firePropertyChange(newValue, oldValue);
        }
        public void focusGained(FocusEvent e) {
        }
        /* KeyListener methods */
        public void keyPressed(KeyEvent e) {
        }
        public void keyTyped(KeyEvent e) {
        }
        public void keyReleased(KeyEvent e) {
            if (e.getKeyCode() == KeyEvent.VK_F4) {
                showSelectionDialog();
            }
        }
    }
    
    /**
     * Places a window, depending of it's size, relative to a component and it's size
     * TODO: move to WindowUtilities
     */
    public static void setWindowRelativeToComponent (java.awt.Window window, Component theComp) {
        int compX = theComp.getLocationOnScreen().x; 
        int compY = theComp.getLocationOnScreen().y;
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        Dimension dialogSize = window.getSize();
        int x = 0, y = 0;

        // Window should be aligned LEFT with component
        x = compX;
        // If there is not enough space to align LEFT, align RIGTH
        if (x + dialogSize.width > screenSize.getWidth()) {
            x = compX - (dialogSize.width - theComp.getWidth());
        }
        // If there is not enough space to align LEFT, make sure that it
        // will be display completely.
        if (x < 0) x = 0;
        
        // Window should be located BELOW component
        y = compY + theComp.getHeight();
        // If there is not enough space Window BELOW component, place ABOVE component
        if (y + dialogSize.height > screenSize.getHeight() ) {
            y = compY - dialogSize.height;
        }
        // If there is not enough space Window ABOVE component make sure that it 
        // will be display completely.
        if (y < 0) y = 0;        
        
        window.setLocation(x, y);
    }

    /** main just for Test reasons */
    public static void main(String[] args) {
        javax.swing.JFrame f = new javax.swing.JFrame();
        f.setDefaultCloseOperation(3 /*JFrame.EXIT_ON_CLOSE*/);
        
        org.apache.tools.ant.gui.core.AppContext context = new org.apache.tools.ant.gui.core.AppContext(f);
        try {
            context.getProjectManager().open(new java.io.File("G:\\build.xml"));
        } catch (Exception e) {
            System.out.println("No buildfile found");
        }

        JPanel c = new JPanel();
        c.setLayout(new java.awt.FlowLayout());
        f.setContentPane(c);
        
        org.w3c.dom.NodeList all2ndLevelNodes = context.getProjectManager().getOpen()[0].getChildNodes();

        int i= 0;
        while (i < all2ndLevelNodes.getLength()) {
           org.w3c.dom.Node node = all2ndLevelNodes.item(i);
           if (node instanceof ACSTargetElement) {
               ACSTargetElement newTarget = ((ACSTargetElement)node);
               DependentTargetPropertyEditor a = new DependentTargetPropertyEditor();
               c.add(a.getChild());
               a.setValue(newTarget);
           }
           i++;
        }
        f.pack();
                
        f.addWindowListener(new java.awt.event.WindowListener() {
            public void windowOpened(java.awt.event.WindowEvent e) {}
            public void windowIconified(java.awt.event.WindowEvent e) {}
            public void windowDeiconified(java.awt.event.WindowEvent e) {}
            public void windowActivated(java.awt.event.WindowEvent e) {}
            public void windowDeactivated(java.awt.event.WindowEvent e) {}
            public void windowClosing(java.awt.event.WindowEvent e) {}
            public void windowClosed(java.awt.event.WindowEvent e) {System.exit(0);}
        });
        f.show();
    }
}