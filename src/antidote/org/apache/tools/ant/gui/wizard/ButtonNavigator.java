/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2001 The Apache Software Foundation.  All rights
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
package org.apache.tools.ant.gui.wizard;
import org.apache.tools.ant.gui.core.ResourceManager;
import javax.swing.*;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.FlowLayout;
import java.util.*;

class ButtonNavigator extends JComponent implements WizardNavigator {
    public static final String NEXT = "next";
    public static final String BACK = "back";
    public static final String CANCEL = "cancel";
    public static final String FINISH = "finish";

    /** Resources. */
    private ResourceManager _resources = null;
    /** Event listeners. */
    private List _listeners = new ArrayList();

    /* Buttons. */
    private JButton _next = null;
    private JButton _back = null;
    private JButton _cancel = null;
    private JButton _finish = null;

    /** Action handler. */
    private ActionHandler _handler = new ActionHandler();

    public ButtonNavigator(ResourceManager resources) {
        _resources = resources;
        setLayout(new FlowLayout(FlowLayout.RIGHT));
        
        _back = new JButton(_resources.getString(BACK));
        _next = new JButton(_resources.getString(NEXT));
        _finish = new JButton(_resources.getString(FINISH));
        _cancel = new JButton(_resources.getString(CANCEL));

        _back.setActionCommand(BACK);
        _next.setActionCommand(NEXT);
        _finish.setActionCommand(FINISH);
        _cancel.setActionCommand(CANCEL);

        _back.addActionListener(_handler);
        _next.addActionListener(_handler);
        _finish.addActionListener(_handler);
        _cancel.addActionListener(_handler);

        _back.setEnabled(false);
        _next.setEnabled(false);
        _finish.setEnabled(false);
        _cancel.setEnabled(true);

        add(_back);
        add(_next);
        add(_finish);
        add(_cancel);
    }

    /** 
     * Add a navigator listener. 
     * 
     * @param l Listener to add.
     */
    public void addNavigatorListener(NavigatorListener l) {
        _listeners.add(l);
    }

    /** 
     * Remove a navigator listener.
     * 
     * @param l Listener to remove.
     */
    public void removeNavigatorListener(NavigatorListener l) {
        _listeners.remove(l);
    }

    /** 
     * Set the enabled state of the back button.
     * 
     * @param state True for enabled, false for disabled.
     */
    public void setBackEnabled(boolean state) {
        _back.setEnabled(state);
    }
    /** 
     * Set the enabled state of the next button.
     * 
     * @param state True for enabled, false for disabled.
     */
    public void setNextEnabled(boolean state) {
        _next.setEnabled(state);
    }
    /** 
     * Set the enabled state of the finished button.
     * 
     * @param state True for enabled, false for disabled.
     */
    public void setFinishEnabled(boolean state) {
        _finish.setEnabled(state);
    }

    /** Handler of the button presses. */
    private class ActionHandler implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            Object source = e.getSource();

            // Predetermine which method to call so that 
            // we don't traverse if statements for each iteration.
            int idx = -1;
            if(source == _next) {
                idx = 0;
            }
            else if(source == _back) {
                idx = 1;
            }
            else if(source == _cancel) {
                idx = 2;
            }
            else if(source == _finish) {
                idx = 3;
            }

            Iterator it = _listeners.iterator();
            while(it.hasNext()) {
                NavigatorListener l = (NavigatorListener) it.next();
                switch(idx) {
                  case 0:
                      l.nextStep();
                      break;
                  case 1:
                      l.backStep();
                      break;
                  case 2:
                      l.cancel();
                      break;
                  case 3:
                      l.finish();
                      break;
                }

            }
        }
    }
}
