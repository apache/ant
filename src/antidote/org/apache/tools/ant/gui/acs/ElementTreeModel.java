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
package org.apache.tools.ant.gui.acs;


import javax.swing.tree.*;
import javax.swing.event.TreeModelListener;
import javax.swing.event.TreeModelEvent;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.apache.tools.ant.gui.acs.*;
import java.util.*;

/**
 * Provides a tree model view of the Project class. XXX This
 * is a major hack right now that needs to be cleaned up.
 *
 * @version $Revision$ 
 * @author Simeon H.K. Fitch */
public class ElementTreeModel implements TreeModel {
    /** Root of the tree. */
    private ACSProjectElement _root = null;
    /** List of listeners. */
    private List _listeners = new ArrayList();

    public ElementTreeModel(ACSProjectElement root) {
        _root = root;
    }

    /**
     * Returns the root of the tree.  Returns null only if the tree has
     * no nodes.
     *
     * @return  the root of the tree
     */
    public Object getRoot() {
        return _root;
    }

    /** 
     * Gets the set of children that this tree model is interested in.
     * NB: This is <b>really</b> inefficient, but may not be an issue given
     * the number of times it is ultimately called. A profiler definately needs
     * to be applied here.
     * 
     * @param parent Parent to extract children from.
     */
    private List getChildren(Node parent) {
        NodeList children = parent.getChildNodes();
        int length = children.getLength();

        List retval = new ArrayList(length);
        for(int i = 0; i < length; i++) {
            // XXX This is where we will eventually add dynamic filtering
            // capabilities.
            Node n = children.item(i);
            if(n instanceof ACSTreeNodeElement) {
                retval.add(n);
            }
        }

        return retval;
    }

    /**
     * Returns the child of <I>parent</I> at index <I>index</I> in the parent's
     * child array.  <I>parent</I> must be a node previously obtained from
     * this data source. This should not return null if <i>index</i>
     * is a valid index for <i>parent</i> (that is <i>index</i> >= 0 &&
     * <i>index</i> < getChildCount(<i>parent</i>)).
     *
     * @param   parent  a node in the tree, obtained from this data source
     * @return  the child of <I>parent</I> at index <I>index</I>
     */
    public Object getChild(Object parent, int index) {
        if(parent instanceof Node) {
            Node n = (Node) parent;
            List children = getChildren(n);
            return children.get(index);
        }
        else {
            return null;
        }
    }


    /**
     * Returns the number of children of <I>parent</I>.  Returns 0 if the node
     * is a leaf or if it has no children.  <I>parent</I> must be a node
     * previously obtained from this data source.
     *
     * @param   parent  a node in the tree, obtained from this data source
     * @return  the number of children of the node <I>parent</I>
     */
    public int getChildCount(Object parent) {
        if(parent instanceof Node) {
            Node n = (Node) parent;
            return getChildren(n).size();
        }
        else {
            return 0;
        }
    }

    /**
     * Returns true if <I>node</I> is a leaf.  It is possible for this method
     * to return false even if <I>node</I> has no children.  A directory in a
     * filesystem, for example, may contain no files; the node representing
     * the directory is not a leaf, but it also has no children.
     *
     * @param   node    a node in the tree, obtained from this data source
     * @return  true if <I>node</I> is a leaf
     */
    public boolean isLeaf(Object node) {
        if(node instanceof Node) {
            Node n = (Node) node;
            return getChildren(n).size() == 0;
        }
        else {
            return true;
        }

    }

    /**
     * Returns the index of child in parent.
     */
    public int getIndexOfChild(Object parent, Object child) {
        if(parent instanceof Node && child instanceof Node) {
            Node n = (Node) parent;
            List children = getChildren(n);
            int count = children.size();
            for(int i = 0; i < count; i++) {
                if(children.get(i) == child) return i;
            }
        }
        return -1;
    }

    /**
      * Messaged when the user has altered the value for the item identified
      * by <I>path</I> to <I>newValue</I>.  If <I>newValue</I> signifies
      * a truly new value the model should post a treeNodesChanged
      * event.
      *
      * @param path path to the node that the user has altered.
      * @param newValue the new value from the TreeCellEditor.
      */
    public void valueForPathChanged(TreePath path, Object newValue) {
        // XXX What should the implementation be here?
        fireNodeChanged((Node) path.getLastPathComponent());
    }


    /**
     * Adds a listener for the TreeModelEvent posted after the tree changes.
     *
     * @see     #removeTreeModelListener
     * @param   l       the listener to add
     */
    public void addTreeModelListener(TreeModelListener l) {
        _listeners.add(l);
    }

    /**
     * Removes a listener previously added with <B>addTreeModelListener()</B>.
     *
     * @see     #addTreeModelListener
     * @param   l       the listener to remove
     */  
    public void removeTreeModelListener(TreeModelListener l) {
        _listeners.remove(l);
    }

    /** 
     * Get the list of nodes from the root to the
     * given node.
     * 
     * @param startNode Node to get path for.
     */
    public Node[] getPathToRoot(Node startNode) {
        return getPathToRoot(startNode, 0);
    }

    /** 
     * A recursive method for generating a list of nodes defining
     * the path from the given node to the root.
     * 
     * @param node Node to get path for.
     * @param depth The number of calls taken towards the root.
     */
    private Node[] getPathToRoot(Node node, int depth) {
        Node[] retval = null;

        depth++;
        if(node == _root || node.getParentNode() == null) {
            retval = new Node[depth];
        }
        else {
            retval = getPathToRoot(node.getParentNode(), depth);
        }

        retval[retval.length - depth] = node;
        return retval;
    }


    /** 
     * Fire a node change event.
     * 
     * @param node Node that changed.
     */
    public void fireNodeChanged(Node node) {
        TreeModelEvent event = new TreeModelEvent(this, getPathToRoot(node));

        // XXX This doen't support modifying the list during dispatch...
        Iterator it = _listeners.iterator();
        while(it.hasNext()) {
            TreeModelListener l = (TreeModelListener) it.next();
            l.treeNodesChanged(event);
        }
    }

    /** 
     * Fire a node change event.
     * 
     * @param node Node that changed.
     */
    public void fireNodeAdded(Node node) {
        Node parent = node.getParentNode();
        TreeModelEvent event = null;
        if(parent == null) {
            event = new TreeModelEvent(this, getPathToRoot(node));
        }
        else {
            Node[] path = getPathToRoot(parent);
            int[] indicies = null;
            Node[] children = new Node[] { node };

            // XXX Right now we assume that the node was added at the end.
            // This may not be the case in the future.
            if(parent.getLastChild() == node) {
                List filteredChildren = getChildren(parent);
                indicies = new int[] { filteredChildren.indexOf(node) };
            }
            else {
                throw new UnsupportedOperationException(
                    "Haven't implemented non-append notification yet.");
            }

            event = new TreeModelEvent(this, path, indicies, children);
        }

        // XXX This doen't support modifying the list during dispatch...
        Iterator it = _listeners.iterator();
        while(it.hasNext()) {
            TreeModelListener l = (TreeModelListener) it.next();
            l.treeNodesInserted(event);
        }
    }
}
