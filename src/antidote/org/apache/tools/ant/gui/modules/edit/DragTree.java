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

import java.awt.datatransfer.*;
import java.awt.dnd.*;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Component;
import java.awt.Insets;
import java.awt.Rectangle;

import javax.swing.JComponent;
import javax.swing.JTree;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.TreePath;
import javax.swing.JScrollPane;

import java.io.IOException;
import java.util.TooManyListenersException;

/**
 * A tree which allows reorganization via drop and drag
 *
 * @version $Revision$
 * @author Nick Davis<a href="mailto:nick_home_account@yahoo.com">nick_home_account@yahoo.com</a>
 */
public class DragTree extends JTree implements DragSourceListener, 
    DragGestureListener, DropTargetListener {
        
    /**
     * The <code>DragTreeListener</code> 
     * associated with this <code>DragTree</code>.
     */
    private DragTreeListener _dragTreeListener;
    
    /**
     * Holds the position where the dropped item should be
     * placed.  Possible values are DROP_BEFORE,  DROP_ON
     * or DROP_AFTER.
     */
    private int _dropPosition;
    
    /**
     * The point where the drop line should be drawn.
     */
    private Point _point;
    
    /**
     * The object the drop occured on.
     */
    private Object _dropOn;
    
    /**
     * The path of where the drop occured.
     */
    private TreePath _dropOnPath;

    /**
     * The path of the item being dropped.
     */
    private TreePath _droppedPath;
    
    /**
     * The item being dragged should be placed before (or above)
     * the item it is dropped on.
     */
    final static protected int DROP_BEFORE = 0;
    
    /**
     * The item being dragged should be placed on (as a child)
     * the item it is dropped on.
     */
    final static protected int DROP_ON = 1;
    
    /**
     * The item being dragged should be placed on (or after)
     * the item it is dropped on.
     */
    final static protected int DROP_AFTER = 2;
    
    /**
     * Default Constuctor
     */
    public DragTree() {
        
        DragSource dragSource = DragSource.getDefaultDragSource();

        // Use the default gesture recognizer
        dragSource.createDefaultDragGestureRecognizer(
           this,
           DnDConstants.ACTION_COPY_OR_MOVE,
           this);
        
        // Setup to be a drop target
        new DropTarget(this,
            DnDConstants.ACTION_COPY_OR_MOVE,
            this);
    }
    
    /**
     * Starts the drag operation.
     * <P>
     * @param e the <code>DragGestureEvent</code> describing 
     * the gesture that has just occurred
     */
    public void dragGestureRecognized(DragGestureEvent e) {
        
        // Find the path for the cursor position.
        Point p = e.getDragOrigin();
        _droppedPath = getPathForLocation(p.x, p.y);
        
        if (_droppedPath == null) {
            return;
        }
        
        // Select the item.
        setSelectionPath(_droppedPath);
        
        // Wrap the object and start the drag.
        Object obj = _droppedPath.getLastPathComponent();
        Wrapper wrapper = new Wrapper(obj);
        e.startDrag(DragSource.DefaultMoveNoDrop, wrapper, this);
    }

    // 
    // DragSourceListener methods
    // 
    public void dragDropEnd(DragSourceDropEvent e) {}
    public void dragEnter(DragSourceDragEvent e) {}
    public void dragExit(DragSourceEvent e) {}
    public void dragOver(DragSourceDragEvent e) {}
    public void dropActionChanged(DragSourceDragEvent e) {}    
    
    // 
    // DropTargetListener methods
    // 
    public void dropActionChanged(java.awt.dnd.DropTargetDragEvent e) {}
    public void dragEnter(DropTargetDragEvent e) {
        dragOver(e);
    }
    
    /**
     * Called when a drag operation is ongoing 
     * on the <code>DropTarget</code>.
     * <P>
     * @param dtde the <code>DropTargetDragEvent</code> 
     */
    public void dragOver(DropTargetDragEvent e) {

        checkAutoScroll(e.getLocation());
        
        Point p = computeDropLocation(e.getLocation());

        // Don't allow a parent to be dropped on one of its children.
        if (_droppedPath.isDescendant(_dropOnPath)) {
            _point = null;
            _dropOn = null;
            p = null;
            e.rejectDrag();
        } else {
            e.acceptDrag(e.getDropAction());
        }
            
        // If the point has changed, repaint the display.
        if (_point == null || !p.equals(_point)) {
            _point = p;
            repaint();
        }
    }    
    
    /**
     * Determines where the item will be dropped.
     */
    private Point computeDropLocation(Point p) {
        
        int rowCount = getRowCount();
        int height = findCellHeight();
        int row = (p.y / height);
        int offset = (p.y % height);
        
        // Move the point to the top of the cell.
        p.y -= offset;

        int delta = 0;
        
        //  Is the point at or past the end of the list?
        if (row > (rowCount - 1) ) {
            p.y = (rowCount - 1) * height;
            row = rowCount - 1;
            delta = height;
            _dropPosition = DROP_AFTER;
        }
        //  Is the point at the begining of the list?
        else if (row <= 0) {
            p.y = 0;
            delta = height;
            if (rowCount > 1) {
                row = 1;
                _dropPosition = DROP_BEFORE;
            } else {
                row = 0;
                _dropPosition = DROP_ON;
            }
        }
        //  The point is in the middle of the tree.
        else {
            // Is the point on the top third of the cell?
            if (offset < height * 0.333) {
                
                // Set the line to the top of the cell.
                delta = 0;
                _dropPosition = DROP_BEFORE;
            }
            // Is the point on the bottom third of the cell?
            else if (offset > height * 0.666){
                
                // Set the line to the bottom of the cell.
                delta = height;
                _dropPosition = DROP_AFTER;
            }
            // The point is in the middle of the cell?
            else {
                
                // Set the line to the middle of the cell.
                delta = height / 2;
                _dropPosition = DROP_ON;
            }
        }

        // Find the object to use for the drop.
        _dropOnPath  = getPathForRow(row);
        if (_dropOnPath != null) {
            _dropOn = _dropOnPath.getLastPathComponent();
        } else {
            _dropOn = null;
        }

        // Adjust the point used to draw the drop line.
        p.y += delta;
        p.x = 0;

        return p;
    }    
    
    /**
     * Process the drop
     *
     * @param e the <code>DropTargetDropEvent</code> 
     * @see DropTargetListener.drop
     */
    public void drop(DropTargetDropEvent e) {
        
        if (_dropOn == null || _dropOnPath == null) {
            return;
        }

        // Get the object being transfered.
        Object obj = null;
        Transferable t = e.getTransferable();
        try {
            obj = t.getTransferData(_flavors[0]);
        } catch (Exception exp) {
            System.out.println(exp);
        }
        
        if (obj != null) {
            
            Object droppedObj = obj;
            Object droppedOnObj = _dropOn;
            Object parentObj = getParentOfDroppedOnObject();

            if (_dropPosition == DROP_ON) {
                
                fireAppendChild(droppedOnObj, droppedObj);
                setExpandedState(_dropOnPath, true);
            }
            else if (_dropPosition == DROP_BEFORE) {
                
                fireInsertBefore(parentObj, droppedOnObj, droppedObj);
            }
            else if (_dropPosition == DROP_AFTER) {
                
                // If the cell is exanded, add the new item before our 
                // first child.
                if (isExpanded(getRowForPath(_dropOnPath))) {
                    parentObj = droppedOnObj;
                }

                // Find the next sibling
                int index = getModel().getIndexOfChild(parentObj, droppedOnObj);
                int count = getModel().getChildCount(parentObj);
                
                if (index == count-1) {
                    fireAppendChild(parentObj, droppedObj);
                } else {
                    Object sibling = getModel().getChild(parentObj, index+1);
                    fireInsertBefore(parentObj, sibling, droppedObj);
                }
            }

            if (e.getDropAction() == DnDConstants.ACTION_MOVE) {
                fireRemoveChild(droppedObj);
            }

            this.updateUI();
        }
    }

    /**
     * Returns the parent of the dropped on object.
     */
    private Object getParentOfDroppedOnObject() {
        int count = _dropOnPath.getPathCount();
        return _dropOnPath.getPathComponent(count-2);
    }
            
    /**
     * Repaint the display to cleanup any lines.
     */
    public void dragExit(DropTargetEvent e) {
        _point = null;
        repaint();
    }
    
    /**
     * Draws the line which shows where the dropped
     * item will land.
     *
     * @see #JComponent.paintComponent
     */
    public void paintComponent(Graphics g){
        super.paintComponent(g);
        
        if (_point != null) {
            
            if (_dropPosition == DROP_ON) {
                // If the drop is on another item, draw
                // two short lines.
                g.drawLine(0, _point.y, 10, _point.y);
                g.drawLine(getWidth()-10, _point.y, getWidth(), _point.y);
            } else {
                // If the drop is above or below an item, draw
                // one long line.
                g.drawLine(0, _point.y, getWidth(), _point.y);
            }
        }
    }
    
    /**
     * Wrapper holds the object to transfer
     */
    protected class Wrapper implements Transferable {

        /** The object to transfer */
        private Object _obj;
        
        /**
         * Creates a wrapper for the input object.
         *
         * @param obj object to wrap
         */
        public Wrapper(Object obj) {
            _obj = obj;
        }
        
        /**
         * Return out object if the DataFlavor is correct.
         *
         * @param flavor only javaJVMLocalObjectMimeType is supported
         */
        public Object getTransferData(DataFlavor flavor)
            throws UnsupportedFlavorException, IOException {
            if (isDataFlavorSupported(flavor)) {
                return _obj;
            }
            throw new UnsupportedFlavorException(flavor);
        }
        
        /**
         * Return true if the input flavor is support.
         *
         * @param flavor DataFlavor to test
         */
        public boolean isDataFlavorSupported(DataFlavor flavor) {
	    return flavor.equals(_flavors[0]);
        }
        
        /**
         * Return true if the input flavor is support.
         *
         * @param flavor DataFlavor to test
         */
        public DataFlavor[] getTransferDataFlavors() {
            return _flavors;
        }
    }

    /**
     * DataFlavors which are support for transfer
     */
    private static final DataFlavor[] _flavors = {
        createConstant(DataFlavor.javaJVMLocalObjectMimeType)
    };

    /**
     * Returns a new DataFlavor or null
     *
     * @param flavor the flavor 
     */
    static private DataFlavor createConstant(String flavor) {
        try {
            return new DataFlavor(flavor);
        } catch (Exception e) {
            return null;
        }
    }
    
    /**
     * Returns the cell height for the tree.
     */
    protected int findCellHeight() {
        DefaultTreeCellRenderer renderer =
            (DefaultTreeCellRenderer) getCellRenderer();
        return renderer.getPreferredSize().height;
    }
    
    /**
     * Register a new <code>DragTreeListener</code>.
     * <P>
     * @param dtl the <code>DragTreeListener</code> to register 
     * with this <code>DragTree</code>.
     */
    public synchronized void addDragTreeListener(DragTreeListener dtl) {
        _dragTreeListener = dtl;
    }

    /**
     * unregister the current DragTreeListener
     * <P>
     * @param dtl the <code>DragTreeListener</code> to unregister 
     * <P>
     * @throws <code>IllegalArgumentException</code> if 
     * dtl is not (equal to) the currently registered
     * <code>DragTreeListener</code>.
     */
    public synchronized void removeDragTreeListener(DragTreeListener dtl) {
	if (_dragTreeListener == null || !_dragTreeListener.equals(dtl))
	    throw new IllegalArgumentException();
	else {
	    _dragTreeListener = null;
	}
    }

    /**
     * Notify the DragTreeListener that an <code>appendChild</code> has
     * been requested.
     */
    protected synchronized Object fireAppendChild(Object parent,
            Object newChild) {
                
	if (_dragTreeListener != null) {
	    return _dragTreeListener.appendChild(parent, newChild);
	}
        return null;
    }
    
    /**
     * Notify the DragTreeListener that an <code>insertBefore</code> has
     * been requested.
     */
    protected synchronized Object fireInsertBefore(Object parent, Object index,
            Object newChild) {
                
	if (_dragTreeListener != null) {
	    return _dragTreeListener.insertBefore(parent, index, newChild);
	}
        return null;
    }
    
    /**
     * Notify the DragTreeListener that an <code>removeChild</code> has
     * been requested.
     */
    protected synchronized void fireRemoveChild(Object child) {
                
	if (_dragTreeListener != null) {
	    _dragTreeListener.removeChild(child);
	}
    }

    /**
     * Scrolls tree if nessasary
     * <P>
     * @param p A <code>Point</code> indicating the 
     * location of the cursor that triggered this operation.
     */
    protected void checkAutoScroll(Point p) {
        
        Point locn = new Point(p);
        javax.swing.SwingUtilities.convertPointToScreen(locn, this);
        javax.swing.SwingUtilities.convertPointFromScreen(locn, getParent());
        
        Rectangle  outer = new Rectangle();
        Rectangle  inner = new Rectangle();

        Insets    i    = new java.awt.Insets(10, 10, 10, 10);
        Dimension size = getParent().getSize();

        if (size.width != outer.width || size.height != outer.height)
            outer.setBounds(0, 0, size.width, size.height);

        if (inner.x != i.left || inner.y != i.top)
            inner.setLocation(i.left, i.top);

        int newWidth  = size.width -  (i.left + i.right);
        int newHeight = size.height - (i.top  + i.bottom);

        if (newWidth != inner.width || newHeight != inner.height)
            inner.setSize(newWidth, newHeight);

        if (outer.contains(locn) && !inner.contains(locn)) {
            if (locn.y >= inner.height) { 
                scrollDown();
            } else {
                scrollUp();
            }
        }
    }
        
    /**
     * Scroll the tree up one cell
     */
    public void scrollUp() {
        Rectangle visibleRect = getVisibleRect();
        int height = findCellHeight();
        visibleRect.y -= height;
        visibleRect.height -= height;
        scrollRectToVisible(visibleRect);
    }
    
    /**
     * Scroll the tree down one cell
     */
    public void scrollDown() {
        Rectangle visibleRect = getVisibleRect();
        int height = findCellHeight();
        visibleRect.y += height;
        visibleRect.height += height;
        scrollRectToVisible(visibleRect);
    }
}
