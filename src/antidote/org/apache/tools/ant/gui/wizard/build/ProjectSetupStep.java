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
package org.apache.tools.ant.gui.wizard.build;

import org.apache.tools.ant.gui.wizard.AbstractWizardStep;
import org.apache.tools.ant.gui.util.LabelFieldGBC;
import org.apache.tools.ant.gui.customizer.FilePropertyEditor;
import javax.swing.*;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridBagLayout;
import java.util.*;
import java.io.File;

/**
 * Build file wizard step for naming the project and 
 * selecting what features are desired.
 * 
 * @version $Revision$ 
 * @author Simeon Fitch 
 */
public class ProjectSetupStep extends AbstractWizardStep {

    /** ID for compile option. */
    public static final String COMPILE_OPTION = "compile"; 
    /** ID for JAR option. */
    public static final String JAR_OPTION = "jar"; 
    /** ID for JavaDoc option. */
    public static final String JAVADOC_OPTION = "javadoc"; 

    /** Available options as an array. */
    public static final String[] OPTIONS = { 
        COMPILE_OPTION,
        JAR_OPTION,
        JAVADOC_OPTION
    };

    /** Array of the option selections. */
    private JCheckBox[] _selections = null;


    /** Name of the project. */
    private JTextField _name = null;
    /** Control for selecting a file. */
    private FilePropertyEditor _fileEditor = null;


    /** 
     * Initialize the screen widgets.
     * 
     */
    protected void init() {
        setLayout(new BorderLayout());

        LabelFieldGBC gbc = new LabelFieldGBC();
        JPanel p = new JPanel(new GridBagLayout());
        add(p, BorderLayout.NORTH);

        _fileEditor = new FilePropertyEditor();
        p.add(new JLabel(
            getResources().getString(getID() + ".fileLabel")), gbc.forLabel());
        p.add(_fileEditor.getCustomEditor(), gbc.forField());

        _name = new JTextField(10);
        p.add(new JLabel(
            getResources().getString(getID() + ".nameLabel")), gbc.forLabel());
        p.add(_name, gbc.forField());

        p = new JPanel(null);
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createTitledBorder(
                getResources().getString(getID() + ".optionsLabel")),
            BorderFactory.createEmptyBorder(5, 10, 5, 10)));
        add(p, BorderLayout.CENTER);

        _selections = new JCheckBox[OPTIONS.length];
        for(int i = 0; i < OPTIONS.length; i++) {
            _selections[i] = new JCheckBox(
                getResources().getString(getID() + "." + OPTIONS[i] + ".label"));
            _selections[i].setSelected(true);
            p.add(_selections[i]);
        }
    }

    /** 
     * Called when the step should refresh its display based on the 
     * current model setting.
     * 
     */
    public void updateDisplay() {
        // Name.
        BuildData data = (BuildData) getDataModel();
        _name.setText(data.getProjectName());

        _fileEditor.setValue(data.getOutputFile());

        // Steps.
        List steps = data.getOptionalSteps();
        if(steps != null) {
            for(int i = 0; i < _selections.length; i++) {
                _selections[i].setSelected(steps.contains(OPTIONS[i]));
            }
        }
    }

    /** 
     * Called when the step should update the data model based on the
     * settings of its widgets.
     * 
     */
    public void updateDataModel() {
        // Name.
        BuildData data = (BuildData) getDataModel();
        data.setProjectName(_name.getText());

        data.setOutputFile((File)_fileEditor.getValue());

        // Steps.
        List steps = new ArrayList();
        for(int i = 0; i < _selections.length; i++) {
            if(_selections[i].isSelected()) {
                steps.add(OPTIONS[i]);
            }
        }

        data.setOptionalSteps(steps);
    }

    /** 
     * Get the id of the next step.
     * 
     * @return ID of next step.
     */
    public String getNext() {
        return ((BuildStateMachine)getDataModel().getStateMachine()).
            getNext(this, getDataModel());
    }

    /** 
     * Get the id of the previous step.
     * 
     * @return Previous step.
     */
    public String getPrevious() {
        return ((BuildStateMachine)getDataModel().getStateMachine()).
            getPrevious(this, getDataModel());
    }
}

