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
package org.apache.tools.ant.gui.core;

import org.apache.tools.ant.gui.event.*;
import org.apache.tools.ant.gui.acs.ACSProjectElement;
import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.*;
import java.beans.PropertyChangeEvent;

/**
 * Specialization of JMenu providing selectability of the currently
 * open projects.
 * 
 * @version $Revision$ 
 * @author Simeon Fitch 
 */
public class ProjectSelectionMenu extends JMenu {
    /** Application context. */
    private AppContext _context = null;
    /** Current set of menus. */
    private Map _menus = new HashMap();
        

    /** 
     * Standard ctor.
     * 
     * @param context Application context.
     */
    public ProjectSelectionMenu(AppContext context) {
        super(context.getResources().getString(
            ProjectSelectionMenu.class, "name"), true);
        _context = context;
        _context.getEventBus().addMember(
            EventBus.MONITORING, new ProjectListener());
        setMnemonic(getText().charAt(0));
    }

    /** 
     * Replace or add the JMenu called "Projects" with this.
     * 
     * @param menuBar Menu bar to insert into.
     */
    public void insertInto(JMenuBar menuBar) {
        // Iterate of the menu items looking for the one with the same name
        // as ours.
        int count = menuBar.getComponentCount();
        for(int i = 0; i < count; i++) {
            JMenuItem menu = (JMenuItem) menuBar.getComponent(i);
            if(menu.getText().equals(getText())) {
                menuBar.remove(menu);
                menuBar.add(this, i);
                return;
            }
        }

        // Getting here we didn't find a menu with the same name.
        add(this);
    }


    /** Listener for updating the contents of the menu. */
    private class ProjectListener implements BusMember {
        /** Event filter. */
        private final Filter _filter = new Filter();
        /** Action handler. */
        private final ActionHandler _handler = new ActionHandler();
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
            // Clear out our existing members
            removeAll();
            _menus.clear();

            ACSProjectElement[] projects = 
                _context.getProjectManager().getOpen();
            for(int i = 0; i < projects.length; i++) {
                JMenuItem menu = new JMenuItem(projects[i].getName());
                menu.addActionListener(_handler);
                _menus.put(menu, projects[i]);
                add(menu);
            }
            return true;
        }
    }

    /** Filter for project related events. */
    private static class Filter implements BusFilter {
        public boolean accept(EventObject event) {
            // We want events related to projects.
            return event instanceof ProjectSelectedEvent ||
                event instanceof ProjectClosedEvent || 
                (event instanceof PropertyChangeEvent && 
                 ((PropertyChangeEvent)event).getSource() 
                 instanceof ACSProjectElement);
        }
    }

    /** Handler for selecting the project. */
    private class ActionHandler implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            ACSProjectElement project = 
                (ACSProjectElement) _menus.get(e.getSource());
            _context.getEventBus().postEvent(
                new ProjectSelectedEvent(_context, project));
        }
    }

}
