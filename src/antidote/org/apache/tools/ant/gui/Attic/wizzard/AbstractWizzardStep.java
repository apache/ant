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
import javax.swing.JComponent;


/**
 * Abstract class implementing the basic support for the WizzardStep interface.
 * 
 * @version $Revision$ 
 * @author Simeon Fitch 
 */
public abstract class  AbstractWizzardStep extends JComponent 
    implements WizzardStep {
    
    /** Step id. */
    private String _id = null;
    /** Step display title. */
    private String _title = null;
    /** Description of the step. */
    private String _description = null;
    /** Data model. */
    private Object _model = null;
    /** ID of next step. */
    private String _nextID = null;
    /** ID of previous step. */
    private String _prevID = null;

    /** 
     * Set the step id. The id must be unique among steps within the wizzard.
     * 
     * @param id Wizzard id.
     */
    public void setID(String id) {
        _id = id;
    }

    /** 
     * Get the step id.
     * 
     * @return Step id.
     */
    public String getID() {
        return _id;
    }

    /** 
     * Set the step title.
     * 
     * @param title Step title.
     */
    public void setTitle(String title) {
        _title = title;
    }

    /** 
     * Get the step title.
     * 
     * @return Step title.
     */
    public String getTitle() {
        return _title;
    }

    /** 
     * Set the step description.
     * 
     * @param desc Step description.
     */
    public void setDescription(String desc) {
        _description = desc;
    }

    /** 
     * Get the step description.
     * 
     * @return Step description.
     */
    public String getDescription() {
        return _description;
    }

    /** 
     * Set the default id of the next step.
     * 
     * @param nextID ID of next step.
     */
    public void setNext(String nextID) {
        _nextID = nextID;
    }

    /** 
     * Get the id of the next step.
     * 
     * @return ID of next step.
     */
    public String getNext() {
        return _nextID;
    }

    /** 
     * Set the default id of the previous step.
     * 
     * @param prevID ID of previous step.
     */
    public void setPrevious(String prevID) {
        _prevID = prevID;
    }

    /** 
     * Get the id of the previous step.
     * 
     * @return Previous step.
     */
    public String getPrevious() {
        return _prevID;
    }

    /** 
     * Set the data model object that the step will edit. It is assumed 
     * that all steps initialized within a single wizzard agree on the
     * data model type.
     * 
     * @param model Data model to edit.
     */
    public void setDataModel(Object model) {
        _model = model;
    }

    /** 
     * Get the data model that should be passeed on to the next step.
     * 
     * @return Current data model.
     */
    public Object getDataModel() {
        return _model;
    }

    /** 
     * Get the component that should be displayed to the user for
     * editing the model. This component should <b>not</b> include the
     * title and text display, which is handled by the wizzard container.
     * 
     * @return Editing component.
     */
    public JComponent getEditorComponent() {
        return this;
    }

    /** 
     * Get a string representation of this.
     * 
     * @return String representation.
     */
    public String toString() {
        StringBuffer buf = new StringBuffer(getClass().getName());
        buf.append("[id=");
        buf.append(getID());
        buf.append(",prev=");
        buf.append(getPrevious());
        buf.append(",next=");
        buf.append(getNext());
        buf.append("]");
        return buf.toString();
    }

}
