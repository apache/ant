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

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.util.List;
import java.util.ArrayList;

import javax.swing.table.AbstractTableModel;

import org.apache.tools.ant.gui.acs.ACSProjectElement;
import org.apache.tools.ant.gui.acs.ACSTargetElement;

/**
 * This is a TableModel containing the co-targets of a distinct target.
 * 
 * @version $Revision$ 
 * @author Christoph Wilhelms 
 */
public class TargetsTableModel extends AbstractTableModel {
    // model data
    private List _delegate = new ArrayList();       
    private ACSTargetElement _mainTarget = null;        

    /**
     * Default constructor
     */
    public TargetsTableModel() {
        super();
    }
    
    /**
     * @param int columnIndex
     * @return String.class
     */
    public Class getColumnClass(int columnIndex) {
        return String.class;
    }
        
    /**
     * @return 2
     */
    public int getColumnCount() {
        return 2;
    }
       
    /**
     * @return number of containing element
     */
    public int getRowCount() {
        return _delegate.size();
    }
        
    /**
     * @param row and column in table
     * @return the requested object to be shown.
     */
    public Object getValueAt(int row, int col) {
        ACSTargetElement rowObj = (ACSTargetElement)_delegate.get(row);
        switch (col) {
            case 0: return rowObj.getName();
            case 1: return rowObj.getDescription();
            default: return "";
        }
    }
    
    /**
     * @return false for no cell is editable.
     */
    public boolean isCellEditable(int row, int col) {
        return false;
    }
    
    /**
     * Change the entire model.
     * @param List of ACSTargetElements
     */
    public void setTargets(List newTargets) {
        _delegate = newTargets;
        fireTableDataChanged();
    }

    /**
     * Access a single element.
     * @param rowIndex
     * @return ACSTargetElement 
     */
    public ACSTargetElement getTarget(int index) {
        return (ACSTargetElement)_delegate.get(index);
    }

    /**
     * Remove a single element.
     * @param int rowIndex
     */
    public void removeTarget(int index) {
        _delegate.remove(index);
        fireTableRowsDeleted(index -1, index);
    }

    /**
     * Add a new element to list.
     * @param ACSTargetElement newTarget to be added to the list.
     */
    public void addTarget(ACSTargetElement newTarget) {
        _delegate.add(newTarget);
        fireTableRowsInserted(_delegate.size()-1, _delegate.size());
    }
    
    /**
     * Moves a Target 
     * @param int rowindex in List
     * @param int delta to move (negative to move up)
     */
    public void moveTarget(int index, int delta) {
        if (index + delta < 0) return;
        else if (index + delta > _delegate.size()) return;
        Object backObj = _delegate.get(index + delta);
        _delegate.set(index + delta, _delegate.get(index) );
        _delegate.set(index, backObj);
        fireTableRowsUpdated(index + delta, index);
    }
    
    /**
     * @return a StringArray (String[]) containing the names of all targets.
     */
    public String[] getTargetsAsStringArray() {
        int length = _delegate.size();
        String[] retVal = new String[length];
        
        for (int i = 0; i < length; i++) {
            retVal[i] = ((ACSTargetElement)_delegate.get(i)).getName();
        }
        return retVal;
    }
}
