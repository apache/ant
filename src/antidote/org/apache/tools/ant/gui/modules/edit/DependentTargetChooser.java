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
 * ITS ConstrIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
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

import java.util.List;
import java.util.ArrayList;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.Dimension;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JButton;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.ImageIcon;
import javax.swing.JTable;
import javax.swing.table.TableColumn;

import org.apache.tools.ant.gui.util.WindowUtils;
import org.apache.tools.ant.gui.acs.ACSProjectElement;
import org.apache.tools.ant.gui.acs.ACSTargetElement;
import org.apache.tools.ant.gui.core.ResourceManager;

/**
 * Dialog for choosing dependent targes comfortable.
 * 
 * @version $Revision$ 
 * @author Christoph Wilhelms 
 */
public class DependentTargetChooser extends JDialog {
    // "Business"-Object
    private ACSTargetElement _target = null;
    // Tables
    private JTable _targetsTable = null;
    // Major Elements;
    private JPanel _commandButtonPanel = null;
    // CommandButtons
    private JButton _ok = null;
    private JButton _cancel = null;
    // common
    private ResourceManager _resources = new ResourceManager();

    private static ActionHandler _handler = null;

    /**
     * Constructor needs a parent Frame
     */
    public DependentTargetChooser (Frame parentFrame) {
        this (parentFrame, null);
    }
    
    /**
     * Constructor needs a parent Frame, target can be set later
     */
    public DependentTargetChooser (Frame parentFrame, ACSTargetElement target) {
        super(parentFrame, true);
        
        _handler = new ActionHandler(); // get the ActionHandler ready
        
        // Dialog settings
        setTitle("Select dependent targets");
        setDefaultCloseOperation(HIDE_ON_CLOSE);
        
        // Container
        JPanel container = new JPanel();
        this.setContentPane(container);
        container.setLayout(new BorderLayout());
        
        JScrollPane tableScrollPane = new JScrollPane();
        tableScrollPane.setViewportView(getTargetsTable());
        
        // Populate container
        container.add(getCommandButtonPanel(), BorderLayout.SOUTH);
        container.add(tableScrollPane, BorderLayout.CENTER);

        // Apply model - must be done this late, because it relies
        // on an instntiated GUI
        setTarget(target);
        
        // Set an initial size and pack it
        container.setPreferredSize(new Dimension(350,250));
        pack();
    }

    
    /**
     * Lazily get the commandButtonPanel
     * @return the created JPanel
     */
    private JPanel getCommandButtonPanel() {
        if (_commandButtonPanel == null) {
            _commandButtonPanel = new JPanel();
            FlowLayout btnLayout = new FlowLayout();
            btnLayout.setAlignment(FlowLayout.RIGHT);
            _commandButtonPanel.setLayout(btnLayout);
        
            _ok = new JButton(_resources.getString(getClass(), "ok"));
            _ok.setMnemonic('O');
            _ok.addActionListener(_handler);
            getRootPane().setDefaultButton(_ok);
            getRootPane().setDefaultButton(_ok);
            _cancel = new JButton(_resources.getString(getClass(), "cancel"));
            _cancel.setMnemonic('c');
            _cancel.addActionListener(_handler);
        
            _commandButtonPanel.add(_ok);
            _commandButtonPanel.add(_cancel);
        }
        return _commandButtonPanel;
    }
    
    /**
     * Writer method for the model-element
     * @param ACSTargetElement the new Target model element.
     */
    public void setTarget(ACSTargetElement newTarget) {
        _target = newTarget;

        // fill source-TableModel with "sister-targets"
        SelectableTargetsTableModel model = (SelectableTargetsTableModel) _targetsTable.getModel();
        model.setTargets(getCoTargets(newTarget));
        model.preselectTargets(newTarget.getDepends());
        model.fireTableDataChanged();
    }
    
    /**
     * Fills a List with all sister-targets of a single target
     * @return filled or empty List 
     */
    private ArrayList getCoTargets (ACSTargetElement aTarget) {
        ACSProjectElement parentProject = null;
        // Caution is the mother of wisdom ;-)
        if (aTarget.getParentNode() instanceof ACSProjectElement) 
            parentProject = (ACSProjectElement) aTarget.getParentNode();
        else throw new IllegalArgumentException("Target not part of Project");
        
        NodeList allNodes = parentProject.getChildNodes();
        ArrayList retVal = new ArrayList();
        int length = allNodes.getLength();
        for (int i = 0; i < length; i++)
        {
            Node node = allNodes.item(i);
            if (node instanceof ACSTargetElement) {
                ACSTargetElement currentElement = ((ACSTargetElement)node);
                // ... leave out the current target TODO: avoid cyclic relations!
                if (currentElement != aTarget) retVal.add(currentElement);
            }
        }
        return retVal;
    }
    
    /**
     * Lazily get a target-table
     * @return created JTable 
     */
    private JTable getTargetsTable() {
        if (_targetsTable == null) {
            _targetsTable = new JTable();
            _targetsTable.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);
            _targetsTable.setAutoCreateColumnsFromModel(false);
            _targetsTable.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            // Coulumn showing the selection of the Target
            TableColumn selectCol = new TableColumn();
            selectCol.setHeaderValue(_resources.getString(getClass(), "selection"));
            selectCol.setModelIndex(0);
            selectCol.setPreferredWidth(70);
            selectCol.setMaxWidth(70);
            selectCol.setResizable(false);
            // Coulumn showing the Target
            TableColumn targetCol = new TableColumn();
            targetCol.setHeaderValue(_resources.getString(getClass(), "target"));
            targetCol.setModelIndex(1);
            targetCol.setPreferredWidth(150);
            targetCol.setMaxWidth(150);
            targetCol.setResizable(true);
            // Coulumn showing the description of targets
            TableColumn descrCol = new TableColumn();
            descrCol.setHeaderValue(_resources.getString(getClass(), "description"));
            descrCol.setModelIndex(2);
            descrCol.setPreferredWidth(250);
            descrCol.setResizable(false);
            _targetsTable.addColumn(selectCol);
            _targetsTable.addColumn(targetCol);
            _targetsTable.addColumn(descrCol);
            _targetsTable.setModel(new SelectableTargetsTableModel());
        }
        return _targetsTable;
    }
    
    /**
     * A decorator for TargetsTableModel to allow selection of targets.
     *
     * @see org.apache.tools.ant.gui.modules.edit.TargetsTableModel
     */
    private class SelectableTargetsTableModel extends TargetsTableModel {
        private boolean[] _selected = new boolean[0];
        
        /**
         * Change the entire model.
         * @param List of ACSTargetElements
         */
        public void setTargets(List newTargets) {
            _selected = new boolean[newTargets.size()];
            super.setTargets(newTargets);
        }

        /**
         * @param Stringarray of target-names
         */
        public void preselectTargets(String[] targetNames) {
            int i = 0, j = 0;
            int iDim = getRowCount();
            int jDim = targetNames.length;
            for (i = 0; i < iDim; i++) {
                String name = getTarget(i).getName();
                for( j= 0; j < jDim; j++) {
                    if (name.equalsIgnoreCase(targetNames[j])) _selected[i] = true;
                }
            }
        }

        /**
         * @param int columnIndex
         * @return String.class
         */
        public Class getColumnClass(int columnIndex) {
            if (columnIndex == 0) return Boolean.class;
            else return String.class;
        }
        
        /**
         * @return 3
         */
        public int getColumnCount() {
            return 3;
        }

        /**
         * @return true in case of the first column.
         */
        public boolean isCellEditable(int row, int col) {
            return (col == 0);
        }
    
        /**
         * @param row and column in table
         * @return the requested object to be shown.
         */
        public Object getValueAt(int row, int col) {
            ACSTargetElement rowObj = getTarget(row);
            switch (col) {
                case 0: return new Boolean(_selected[row]);
                case 1: return rowObj.getName();
                case 2: return rowObj.getDescription();
                default: return "";
            }
        }

        /**
         * @param new value resulting from the editor
         * @param row and column in table
         */
        public void setValueAt(Object newValue, int row, int col) {
            if (col == 0) {
                _selected[row] = ((Boolean)newValue).booleanValue();
            }
        }

        public int getSelectedTargetCount() {
            int retVal = 0;
            int length = getRowCount();
            for (int i = 0; i < length; i++) {
                if (_selected[i]) retVal++;
            }
            return retVal;
        }
        
        /**
         * @return a StringArray (String[]) containing the names of all selected targets.
         */
        public String[] getSelectedTargetsAsStringArray() {
            int length = getRowCount();
            String[] retVal = new String[getSelectedTargetCount()];
            int i = 0, j = 0;
        
            for (i = 0; i < length; i++) {
                if (_selected[i]) {
                    retVal[j] = getTarget(i).getName();
                    j++;
                }
            }
            return retVal;
        }
    }

    /** 
     * Ihis handler is the ActionListener for each button. 
     */
    private class ActionHandler implements ActionListener {
        /**
         * ActionListener Interface ethod
         * @param ActionEvent
         */
        public void actionPerformed(ActionEvent e) {
            // Get some initial values needed later
            // Evaluate EventSource
            if (e.getSource()==_ok) {
                // OK: take the selected targets and leave
                _target.setDepends( ((SelectableTargetsTableModel)_targetsTable.getModel()).getSelectedTargetsAsStringArray() );
                dispose();
            } else if (e.getSource()==_cancel) {
                // just close dialog
                dispose();
            }
        }
    }
}
