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
 * 4. The names "The Jakarta Project", "Tomcat", and "Apache Software
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
package org.apache.tools.ant.gui;
import org.apache.tools.ant.gui.event.*;
import javax.swing.*;
import java.awt.GridLayout;
import java.awt.Dimension;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.EventObject;

/**
 * AntEditor for displaying the project target in a 
 * 
 * @version $Revision$ 
 * @author Simeon Fitch 
 */
class ProjectNavigator extends AntModule {

    /** Navigation via a tree widget. */
    private JTree _tree = null;

	/** 
	 * Default ctor.
	 * 
	 */
	public ProjectNavigator() {
    }

	/** 
	 * Using the given AppContext, initialize the display.
	 * 
	 * @param context Application context.
	 */
    public void contextualize(AppContext context) {
        setContext(context);
        context.getEventBus().addMember(EventBus.MONITORING, new Handler());

        setLayout(new GridLayout(1,1));

        _tree = new JTree();
        _tree.setModel(null);
        _tree.setCellRenderer(new AntTreeCellRenderer());
        _tree.addMouseListener(new PopupHandler());
        JScrollPane scroller = new JScrollPane(_tree);
        add(scroller);

        setPreferredSize(new Dimension(200, 100));
        setMinimumSize(new Dimension(200, 100));
	}

    /** Class for handling project events. */
    private class Handler implements BusMember {
        private final Filter _filter = new Filter();

        /** 
         * Get the filter to that is used to determine if an event should
         * to to the member.
         * 
         * @return Filter to use.
         */
        public BusFilter getBusFilter() {
            return _filter;
        }
        
        /** 
         * Called when an event is to be posed to the member.
         * 
         * @param event Event to post.
         * @return true if event should be propogated, false if
         * it should be cancelled.
         */
        public boolean eventPosted(EventObject event) {
            ProjectProxy project = getContext().getProject();

            if(project == null) {
                // The project has been closed.
                // XXX this needs to be tested against 
                // different version of Swing...
                _tree.setModel(null);
                _tree.setSelectionModel(null);
            }
            else {
                _tree.setModel(project.getTreeModel());
                _tree.setSelectionModel(project.getTreeSelectionModel());
            }
            return true;
        }
    }

    /** Class providing filtering for project events. */
    private static class Filter implements BusFilter {
        /** 
         * Determines if the given event should be accepted.
         * 
         * @param event Event to test.
         * @return True if event should be given to BusMember, false otherwise.
         */
        public boolean accept(EventObject event) {
            return event instanceof NewProjectEvent;
        }
    }

    /** Mouse listener for showing popup menu. */
    private class PopupHandler extends MouseAdapter {
        private void handle(MouseEvent e) {
            if(e.isPopupTrigger()) {
                ActionManager mgr = getContext().getActions();
                JPopupMenu menu = mgr.createPopup(
                    getContext().getResources().getStringArray(
                        ProjectNavigator.class, "popupActions"));
                menu.show((JComponent)e.getSource(), e.getX(), e.getY());
            }
        }

        public void mousePressed(MouseEvent e) {
            handle(e);
        }
        public void mouseReleased(MouseEvent e) {
            handle(e);
        }
        public void mouseClicked(MouseEvent e) {
            handle(e);
        }
    }
}
