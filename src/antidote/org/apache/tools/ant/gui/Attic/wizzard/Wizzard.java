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
package org.apache.tools.ant.gui.wizzard;
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
 * Top level container and controller for wizzard-type GUI.
 * 
 * @version $Revision$ 
 * @author Simeon Fitch 
 */
public class Wizzard extends JComponent {
    /** Resources defining the wizzard contents. Separate from the
     *  application context resources. */
    private ResourceManager _resources = null;
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
    private WizzardNavigator _nav = null;
    /** The data model to pass on to each step. */
    private Object _model = null;
    /** The current Wizzard step. */
    private WizzardStep _curr = null;
    /** The set of wizzard listeners. */
    private List _listeners = new ArrayList(1);

    /** 
     * Standard ctor.
     * 
     * @param resources Wizzard definition resources
     * @param dataModel Initial data model.
     */
    public Wizzard(ResourceManager resources, Object dataModel) {
        setLayout(new BorderLayout());
        _resources = resources;
        _model = dataModel;

        _progress = new JProgressBar();
        _progress.setBorder(BorderFactory.createTitledBorder(
            _resources.getString("progress")));
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
            _resources.getString("help")));
        add(scroller, BorderLayout.WEST);

        _stepContainer = new JPanel(_layout = new CardLayout());
        _stepContainer.setBorder(BorderFactory.createEtchedBorder());
        _stepContainer.setPreferredSize(new Dimension(400, 400));


        add(_stepContainer, BorderLayout.CENTER);

        _nav = new ButtonNavigator(_resources);
        _nav.addNavigatorListener(new NavHandler());
        ((ButtonNavigator)_nav).setBorder(BorderFactory.createEtchedBorder());
        add((ButtonNavigator)_nav, BorderLayout.SOUTH);

        String[] steps = _resources.getStringArray("steps");
        _progress.setMaximum(steps.length - 1);
        try {
            for(int i = 0; i < steps.length; i++) {
                Class type = _resources.getClass(steps[i] + ".editor");
                WizzardStep step = (WizzardStep) type.newInstance();
                step.setResources(_resources);
                step.setID(steps[i]);
                step.setTitle(_resources.getString(steps[i]+ ".title"));
                step.setDescription(
                    _resources.getString(steps[i]+ ".description"));

                String id = _resources.getString(steps[i] + ".next");
                id = (id == null && i < steps.length - 1) ? steps[i + 1] : id;
                step.setNext(id);

                id = _resources.getString(steps[i] + ".prev");
                id = (id == null && i > 0) ? steps[i - 1] : id;
                step.setPrevious(id);

                _steps.put(steps[i], step);
                _stepOrdering.add(step);
                _stepContainer.add(step.getEditorComponent(), steps[i]);
            }
            // Initialize the first screen with the data model.
            if(steps.length > 0) {
                WizzardStep first = (WizzardStep)_steps.get(steps[0]);
                first.setDataModel(_model);
                _curr = first;
                showStep(first);
            }
        }
        catch(Exception ex) {
            // If we get here then the wizzard didn't initialize properly.
            // XXX log me.
            ex.printStackTrace();
        }

    }

    /** 
     * Add a wizzard listener.
     * 
     * @param l Listener to add.
     */
    public void addWizzardListener(WizzardListener l) {
        _listeners.add(l);
    }

    /** 
     * Remove a wizzard listener.
     * 
     * @param l Listener to remove.
     */
    public void removeWizzardListener(WizzardListener l) {
        _listeners.remove(l);
    }

    /** 
     * Go to the given step.
     * 
     * @param step Step to go to.
     */
    private void showStep(WizzardStep step) {
        if(step == null) return;

        // Transfer data model (in case step wants to create a new one.
        _curr.updateDataModel();
        step.setDataModel(_curr.getDataModel());
        
        // Update the title and description.
        _stepContainer.setBorder(
            BorderFactory.createTitledBorder(step.getTitle()));
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

    /** Handler for actions invoked by wizzard. */
    private class NavHandler implements NavigatorListener {
        public void nextStep() {
            String nextID = _curr.getNext();
            if(nextID != null) {
                showStep((WizzardStep)_steps.get(nextID));
            }
        }
        public void backStep() {
            String prevID = _curr.getPrevious();
            if(prevID != null) {
                showStep((WizzardStep)_steps.get(prevID));
            }
        }
        public void gotoStep(String stepID){
            showStep((WizzardStep) _steps.get(stepID));
        }
        public void cancel() {
            Iterator it = _listeners.iterator();
            while(it.hasNext()) {
                WizzardListener l = (WizzardListener) it.next();
                l.canceled();
            }
        }
        public void finish() {
            Iterator it = _listeners.iterator();
            while(it.hasNext()) {
                WizzardListener l = (WizzardListener) it.next();
                l.finished(_curr.getDataModel());
            }
        }
    }
}
