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
import java.awt.GridBagLayout;
import java.awt.GridBagConstraints;
import java.awt.Insets;
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
    private JTable _srcTable = null;
    private JTable _dstTable = null;
    private JButton _append = null;
    private JButton _remove = null;
    private JButton _moveUp = null;
    private JButton _moveDown = null;
    // Major Elements;
    private JPanel _commandButtonPanel = null;
    private JPanel _selectionPanel = null;
    // CommandButtons
    private JButton _ok = null;
    private JButton _cancel = null;

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
        
        // Populate container
        container.add(getCommandButtonPanel(), BorderLayout.SOUTH);
        container.add(getSelectionPanel(), BorderLayout.CENTER);

        // Apply model - must be done this late, because it relies
        // on an instntiated GUI
        setTarget(target);
        
        // Set an initial size and pack it
        container.setPreferredSize(new Dimension(500,200));
        pack();
    }

    /**
     * Lazily get the selectionPanel with 2 tables and 4 buttons
     * @return the created JPanel
     */
    private JPanel getSelectionPanel() {
        if (_selectionPanel == null) {
            _selectionPanel = new JPanel();
            _selectionPanel.setLayout(new GridBagLayout());
            
            // LEFT Table
            JScrollPane srcSP = new JScrollPane();
            srcSP.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
            srcSP.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
            _srcTable = createTargetsTable();
            srcSP.setViewportView(_srcTable);
            
            GridBagConstraints srcSPConstr = new GridBagConstraints();
            srcSPConstr.fill = GridBagConstraints.BOTH;
            srcSPConstr.anchor = GridBagConstraints.CENTER;
            srcSPConstr.gridx = 0; srcSPConstr.gridy = 0; 
            srcSPConstr.weightx = 1.0; srcSPConstr.weighty = 2.0; 
            srcSPConstr.gridwidth = 1; srcSPConstr.gridheight = 2;
            srcSPConstr.insets = new Insets(5,5,0,5);
            _selectionPanel.add(srcSP, srcSPConstr);
            
            // Append Button
            _append = new JButton();
            _append.setIcon(new ImageIcon(getClass().getResource("/org/apache/tools/ant/gui/resources/enter.gif")));
            _append.setPreferredSize(new Dimension(28, 28));
            _append.setMaximumSize(new Dimension(28, 28));
            _append.setMinimumSize(new Dimension(28, 28));
            _append.addActionListener(_handler);
            
            GridBagConstraints appendConstr = new GridBagConstraints();
            appendConstr.fill = GridBagConstraints.NONE;
            appendConstr.anchor = GridBagConstraints.SOUTH;
            appendConstr.gridx = 1; appendConstr.gridy = 0; 
            appendConstr.weightx = 0.0; appendConstr.weighty = 1.0; 
            appendConstr.gridwidth = 1; appendConstr.gridheight = 1;
            appendConstr.insets = new Insets(0,0,2,0);
            _selectionPanel.add(_append, appendConstr);

            // Remove Button
            _remove = new JButton();
            _remove.setIcon(new ImageIcon(getClass().getResource("/org/apache/tools/ant/gui/resources/exit.gif")));
            _remove.setPreferredSize(new Dimension(28, 28));
            _remove.setMaximumSize(new Dimension(28, 28));
            _remove.setMinimumSize(new Dimension(28, 28));
            _remove.addActionListener(_handler);
            GridBagConstraints removeConstr = new GridBagConstraints();
            removeConstr.fill = GridBagConstraints.NONE;
            removeConstr.anchor = GridBagConstraints.NORTH;
            removeConstr.gridx = 1; removeConstr.gridy = 1; 
            removeConstr.weightx = 0.0; removeConstr.weighty = 1.0; 
            removeConstr.gridwidth = 1; removeConstr.gridheight = 1;
            removeConstr.insets = new Insets(3,0,0,0);
            _selectionPanel.add(_remove, removeConstr);
            
            // RIGHT Table
            JScrollPane dstSP = new JScrollPane();
            dstSP.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
            dstSP.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
            _dstTable = createTargetsTable();
            dstSP.setViewportView(_dstTable);

            GridBagConstraints dstSPConstr = new GridBagConstraints();
            dstSPConstr.fill = GridBagConstraints.BOTH;
            dstSPConstr.anchor = GridBagConstraints.CENTER;
            dstSPConstr.gridx = 2; dstSPConstr.gridy = 0; 
            dstSPConstr.weightx = 1.0; dstSPConstr.weighty = 2.0; 
            dstSPConstr.gridwidth = 1; dstSPConstr.gridheight = 2;
            dstSPConstr.insets = new Insets(5,5,0,5);
            _selectionPanel.add(dstSP, dstSPConstr);

            // Move Up Button
            _moveUp = new JButton();
            _moveUp.setIcon(new ImageIcon(getClass().getResource("/org/apache/tools/ant/gui/resources/up.gif")));
            _moveUp.setPreferredSize(new Dimension(28, 28));
            _moveUp.setMaximumSize(new Dimension(28, 28));
            _moveUp.setMinimumSize(new Dimension(28, 28));
            _moveUp.addActionListener(_handler);
            GridBagConstraints moveUpConstr = new GridBagConstraints();
            moveUpConstr.fill = GridBagConstraints.NONE;
            moveUpConstr.anchor = GridBagConstraints.CENTER;
            moveUpConstr.gridx = 3; moveUpConstr.gridy = 0; 
            moveUpConstr.weightx = 0.0; moveUpConstr.weighty = 1.0; 
            moveUpConstr.gridwidth = 1; moveUpConstr.gridheight = 1;
            moveUpConstr.insets = new Insets(0,0,0,5);
            _selectionPanel.add(_moveUp, moveUpConstr);

            // Move Up Button
            _moveDown = new JButton();
            _moveDown.setIcon(new ImageIcon(getClass().getResource("/org/apache/tools/ant/gui/resources/down.gif")));
            _moveDown.setPreferredSize(new Dimension(28, 28));
            _moveDown.setMaximumSize(new Dimension(28, 28));
            _moveDown.setMinimumSize(new Dimension(28, 28));
            _moveDown.addActionListener(_handler);
            GridBagConstraints moveDownConstr = new GridBagConstraints();
            moveDownConstr.fill = GridBagConstraints.NONE;
            moveDownConstr.anchor = GridBagConstraints.CENTER;
            moveDownConstr.gridx = 3; moveDownConstr.gridy = 1; 
            moveDownConstr.weightx = 0.0; moveDownConstr.weighty = 1.0; 
            moveDownConstr.gridwidth = 1; moveDownConstr.gridheight = 1;
            moveDownConstr.insets = new Insets(0,0,0,5);
            _selectionPanel.add(_moveDown, moveDownConstr);
        }
        return _selectionPanel;
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
        
            _ok = new JButton("OK");
            _ok.addActionListener(_handler);
            getRootPane().setDefaultButton(_ok);
            _cancel = new JButton("Cancel");
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
        TargetsTableModel srcModel = (TargetsTableModel)_srcTable.getModel();
        srcModel.setTargets(getCoTargets(newTarget));
        srcModel.fireTableDataChanged();

        // fill dest-TableModel with selected depends-targets
        TargetsTableModel dstModel = (TargetsTableModel)_dstTable.getModel();
        dstModel.setTargets(fillDependsList(newTarget));
        dstModel.fireTableDataChanged();
    }
    
    /**
     * Fills a List with all sister- or depending-targets of a single target
     * @return filled or empty List 
     */
    private List fillDependsList(ACSTargetElement aTarget) {
        List retVal = new ArrayList();
            
        String[] dependNames = aTarget.getDepends();
        int length = dependNames.length;
        ArrayList allTargets = getCoTargets (aTarget);
        int allLen = allTargets.size();
        
        for (int i = 0; i < length; i++)
        {
            for (int j = 0; j < allLen; j++) {
                ACSTargetElement currentElement = (ACSTargetElement)allTargets.get(j);
                if (currentElement.getName().equalsIgnoreCase(dependNames[i].trim())) retVal.add(currentElement);
            }
        }
        return retVal;
    }

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
     * Checks if a String is part of an existing dependent task list
     * @return true, if it is a dependent target
     */
    private boolean checkDepends(String name, ACSTargetElement aTarget) {
        String[] depend = aTarget.getDepends();
        for( int i= 0; i < depend.length; i++) {
            if (name.equalsIgnoreCase(depend[i])) return true;
        }
        return false;
    }
    
    /**
     * Creates a target-table with two columns - we need two of them!
     * @return created JTable 
     */
    private JTable createTargetsTable() {
        JTable table = new JTable();
        table.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);
        table.setAutoCreateColumnsFromModel(false);
        table.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        // Coulumn showing the Target
        TableColumn targetCol = new TableColumn();
        targetCol.setHeaderValue("Target");
        targetCol.setModelIndex(0);
        targetCol.setPreferredWidth(150);
        targetCol.setMaxWidth(150);
        targetCol.setResizable(true);
        // Coulumn showing the description of targets
        TableColumn descrCol = new TableColumn();
        descrCol.setHeaderValue("Description");
        descrCol.setModelIndex(1);
        descrCol.setPreferredWidth(250);
        descrCol.setResizable(false);
        table.addColumn(targetCol);
        table.addColumn(descrCol);
        table.setModel(new TargetsTableModel());
        
        return table;
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
            TargetsTableModel srcModel = (TargetsTableModel)_srcTable.getModel();
            TargetsTableModel dstModel = (TargetsTableModel)_dstTable.getModel();
            int srcRow = _srcTable.getSelectedRow();
            int dstRow = _dstTable.getSelectedRow();
            // Evaluate EventSource
            if (e.getSource()==_ok) {
                // OK: take the selected targets and leave
                _target.setDepends( dstModel.getTargetsAsStringArray() );
                dispose();
            } else if (e.getSource()==_cancel) {
                // just close dialog
                dispose();
            } else if (e.getSource()==_moveUp) {
                // Move dependent target up (one row)
                dstModel.moveTarget(dstRow, -1);
                _dstTable.getSelectionModel().setSelectionInterval(dstRow - 1, dstRow - 1);
            } else if (e.getSource()==_moveDown) {
                // Move dependent target down (one row)
                dstModel.moveTarget(dstRow, 1);
                _dstTable.getSelectionModel().setSelectionInterval(dstRow + 1, dstRow + 1);
            } else if (e.getSource()==_append) {
                // Append selected target to depends
                if (srcRow >= 0) dstModel.addTarget(srcModel.getTarget(srcRow));
            } else if (e.getSource()==_remove) {
                // Remove dependent target
                if (dstRow >= 0) dstModel.removeTarget(dstRow);
            }
        }
    }
}