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
import javax.swing.JComponent;


/**
 * Interface for classes defining a step in a wizard.
 * 
 * @version $Revision$ 
 * @author Simeon Fitch 
 */
public interface WizardStep {
    /** 
     * Set the step's resources.
     * 
     */
    void setResources(ResourceManager resources);

    /** 
     * Set the step id. The id must be unique among steps within the wizard.
     * 
     * @param id Wizard id.
     */
    void setID(String id);

    /** 
     * Get the step id.
     * 
     * @return Step id.
     */
    String getID();

    /** 
     * Set the step title.
     * 
     * @param title Step title.
     */
    void setTitle(String title);
    /** 
     * Get the step title.
     * 
     * @return Step title.
     */
    String getTitle();

    /** 
     * Set the step description.
     * 
     * @param desc Step description.
     */
    void setDescription(String desc);
    /** 
     * Get the step description.
     * 
     * @return Step description.
     */
    String getDescription();

    /** 
     * Set the default id of the next step.
     * 
     * @param nextID ID of next step.
     */
    void setNext(String nextID);
    /** 
     * Get the id of the next step.
     * 
     * @return ID of next step.
     */
    String getNext();

    /** 
     * Set the default id of the previous step.
     * 
     * @param prevID ID of previous step.
     */
    void setPrevious(String prevID);

    /** 
     * Get the id of the previous step.
     * 
     * @return Previous step.
     */
    String getPrevious();

    /** 
     * Set the data model object that the step will edit. It is assumed 
     * that all steps initialized within a single wizard agree on the
     * data model type.
     * 
     * @param model Data model to edit.
     */
    void setDataModel(Object model);

    /** 
     * Get the data model that should be passeed on to the next step.
     * 
     * @return Current data model.
     */
    Object getDataModel();

    /** 
     * Get the component that should be displayed to the user for
     * editing the model. This component should <b>not</b> include the
     * title and text display, which is handled by the wizard container.
     * 
     * @return Editing component.
     */
    JComponent getEditorComponent();

    /** 
     * Called when the step should refresh its display based on the 
     * current model setting.
     * 
     */
    void updateDisplay();

    /** 
     * Called when the step should update the data model based on the
     * settings of its widgets.
     * 
     */
    void updateDataModel();

}
