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
import javax.swing.border.TitledBorder;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Insets;
import java.util.*;

/**
 * Top level container and controller for wizard-type GUI.
 * 
 * @version $Revision$ 
 * @author Simeon Fitch 
 */
public class Wizard extends JComponent {
    /** The data model to pass on to each step. */
    private WizardData _data = null;
    /** Container for the step editors. */
    private JPanel _stepContainer = null;
    /** Layout manager for all the step panels. */
    private CardLayout _layout = null;
    /** Set initialized steps. */
    private Map _steps = new HashMap();
    /** Steps saved in a list to preserve ordering. */
    private List _stepOrdering = new ArrayList();
    /** Description text. XXX should probably change to some other widget. */
    private JTextArea _description = null;
    /** Progress meter. */
    private JProgressBar _progress = null;
    /** Widget for navigating through steps. */
    private WizardNavigator _nav = null;
    /** The current Wizard step. */
    private WizardStep _curr = null;
    /** The set of wizard listeners. */
    private List _listeners = new ArrayList(1);

    /** 
     * Standard ctor.
     * 
     * @param data Data for the wizard.
     */
    public Wizard(WizardData data) {
        setLayout(new BorderLayout());
        _data = data;

        _progress = new JProgressBar();
        _progress.setBorder(BorderFactory.createTitledBorder(
            _data.getResources().getString("progress")));
        _progress.setStringPainted(true);
        add(_progress, BorderLayout.NORTH);

        _description = new JTextArea();
        _description.setMargin(new Insets(5, 5, 5, 5));
        _description.setPreferredSize(new Dimension(100, 100));
        _description.setOpaque(true);
        _description.setFont(new Font("Serif", Font.PLAIN, 12));
        _description.setEditable(false);
        _description.setLineWrap(true);
        _description.setWrapStyleWord(true);

        JScrollPane scroller = new JScrollPane(
            _description, 
            JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, 
            JScrollPane.HORIZONTAL_SCROLLBAR_NEVER); 

        scroller.setBorder(BorderFactory.createTitledBorder(
            _data.getResources().getString("help")));
        add(scroller, BorderLayout.WEST);

        _stepContainer = new JPanel(_layout = new CardLayout());
        _stepContainer.setBorder(BorderFactory.createEtchedBorder());
        _stepContainer.setPreferredSize(new Dimension(400, 400));


        add(_stepContainer, BorderLayout.CENTER);

        _nav = new ButtonNavigator(_data.getResources());
        _nav.addNavigatorListener(new NavHandler());
        ((ButtonNavigator)_nav).setBorder(BorderFactory.createEtchedBorder());
        add((ButtonNavigator)_nav, BorderLayout.SOUTH);

        String[] steps = _data.getResources().getStringArray("steps");
        _progress.setMaximum(steps.length - 1);
        try {
            for(int i = 0; i < steps.length; i++) {
                Class type = _data.getResources().getClass(steps[i] + ".editor");
                WizardStep step = (WizardStep) type.newInstance();
                step.setResources(_data.getResources());
                step.setID(steps[i]);
                step.setTitle(
                    _data.getResources().getString(steps[i]+ ".title"));
                step.setDescription(
                    _data.getResources().getString(steps[i]+ ".description"));

                _steps.put(steps[i], step);
                _stepOrdering.add(step);
                _stepContainer.add(step.getEditorComponent(), steps[i]);
            }
            // Initialize the first screen with the data model.
            if(steps.length > 0) {
                WizardStep first = (WizardStep)_steps.get(steps[0]);
                first.setDataModel(_data);
                _curr = first;
                showStep(first);
            }
        }
        catch(Exception ex) {
            // If we get here then the wizard didn't initialize properly.
            // XXX log me.
            ex.printStackTrace();
        }

    }

    /** 
     * Add a wizard listener.
     * 
     * @param l Listener to add.
     */
    public void addWizardListener(WizardListener l) {
        _listeners.add(l);
    }

    /** 
     * Remove a wizard listener.
     * 
     * @param l Listener to remove.
     */
    public void removeWizardListener(WizardListener l) {
        _listeners.remove(l);
    }

    /** 
     * Go to the given step.
     * 
     * @param step Step to go to.
     */
    private void showStep(WizardStep step) {
        if(step == null) return;

        step.setDataModel(_curr.getDataModel());
        
        // Update the title and description.
        _stepContainer.setBorder(
            BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder(step.getTitle()),
                BorderFactory.createEmptyBorder(5, 15, 5, 15))); 
        _description.setText(step.getDescription());

        _nav.setBackEnabled(step.getPrevious() != null);
        _nav.setNextEnabled(step.getNext() != null);
        _nav.setFinishEnabled(step.getNext() == null);
        _progress.setValue(_stepOrdering.indexOf(step));

        // Tell the step to refresh its display based on the data model.
        step.updateDisplay();

        // Display the step.
        _layout.show(_stepContainer, step.getID());

        _curr = step;
    }

    /** Handler for actions invoked by wizard. */
    private class NavHandler implements NavigatorListener {
        public void nextStep() {
            // Called to give data model chance to make changes to what is next.
            _curr.updateDataModel();
            String nextID = _curr.getNext();
            if(nextID != null) {
                showStep((WizardStep)_steps.get(nextID));
            }
        }
        public void backStep() {
            // Called to give data model chance to make changes to what is 
            // before.
            _curr.updateDataModel();
            String prevID = _curr.getPrevious();
            if(prevID != null) {
                showStep((WizardStep)_steps.get(prevID));
            }
        }
        public void gotoStep(String stepID){
            _curr.updateDataModel();
            showStep((WizardStep) _steps.get(stepID));
        }
        public void cancel() {
            _curr.updateDataModel();
            Iterator it = _listeners.iterator();
            while(it.hasNext()) {
                WizardListener l = (WizardListener) it.next();
                l.canceled();
            }
        }
        public void finish() {
            _curr.updateDataModel();
            Iterator it = _listeners.iterator();
            while(it.hasNext()) {
                WizardListener l = (WizardListener) it.next();
                l.finished(_curr.getDataModel());
            }
        }
    }
}
