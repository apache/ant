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
import java.util.*;

/**
 * Manager of antidote actions. Receives its configuration from the action
 * ResourceBundle.
 * 
 * @version $Revision$ 
 * @author Simeon Fitch 
 */
public class ActionManager {
    private  ResourceBundle _resources = 
        ResourceBundle.getBundle(
            "org.apache.tools.ant.gui.resources.action");

    /** Array of action identifiers. */
    private String[] _actionIDs = null;

    /** Look table of all defined actions. */
    private Map _actions = new HashMap();

    /** Event bus. */
    private EventBus _bus = null;
    /** Class for storing the event type to action type
     *  mapping for setting enabled state. */
    private EventToActionMapper _mapper = null;


	/** 
	 * Standard ctor.
	 * 
	 * @param bus Event bus to post events to.
	 */
    public ActionManager(EventBus bus) {
        _bus = bus;
        bus.addMember(EventBus.RESPONDING, new Enabler());

        _mapper = new EventToActionMapper();

        // Configure the set of actions.
        String toTok = _resources.getString("actions");
        StringTokenizer tok = new StringTokenizer(toTok, ", ");
        _actionIDs = new String[tok.countTokens()];
        for(int i = 0; i < _actionIDs.length; i++) {
            _actionIDs[i] = tok.nextToken();
            AntAction action = new AntAction(_resources, _bus, _actionIDs[i]);
            _actions.put(_actionIDs[i], action);
                         

            // For each action we need to add the reverse event trigger
            // lookup.
            _mapper.addAction(action);

        }

    }

	/** 
	 * Create a menubar for the application based on the configuration file.
	 * 
	 * @return Menubar.
	 */
    public JMenuBar createMenuBar() {
        JMenuBar retval = new JMenuBar();
        Map menus = new HashMap();

        String toTok = _resources.getString("menus");
        StringTokenizer tok = new StringTokenizer(toTok, ", ");
        while(tok.hasMoreTokens()) {
            String name = tok.nextToken();
            JMenu menu = new JMenu(name);

            // XXX should be in config file
            menu.setMnemonic(name.charAt(0));

            // XXX need to i18n here...
            if(name.equalsIgnoreCase("help")) {
                try {
                    retval.setHelpMenu(menu);
                }
                catch(Error err) {
                    // Catch the "not implemented" error in
                    // some (all?) Swing implementations
                    retval.add(menu);
                }
            }
            else {
                retval.add(menu);
            }
            menus.put(name, menu);
        }

        for(int i = 0; i < _actionIDs.length; i++) {
            AntAction action = (AntAction) _actions.get(_actionIDs[i]);
            String parent = action.getParentMenuName();
            if(parent != null) {
                JMenu menu = (JMenu) menus.get(parent);
                // A well configured file shouldn't cause this,
                // but be safe anyway.
                if(menu == null) {
                    menu = new JMenu(parent);
                    retval.add(menu);
                    menus.put(parent, menu);
                }

                // See if we should add a separator.
                if(action.isPreceededBySeparator() && 
                   menu.getMenuComponentCount() > 0) {
                    menu.addSeparator();
                }
                JMenuItem item = menu.add(action);
                item.setAccelerator(action.getAccelerator());
                addNiceStuff(item, action);
            }
        }

        return retval;
    }

	/** 
	 * Create a tool bar based on the current configuration.
	 * 
	 * @return Toolbar ready for action.
	 */
    public JToolBar createToolBar() {
        JToolBar retval = new JToolBar();
        
        for(int i = 0; i < _actionIDs.length; i++) {
            AntAction action = (AntAction) _actions.get(_actionIDs[i]);
            // If it has an icon, then we add it to the toolbar.
            if(action.getIcon() != null) {
                if(action.isPreceededBySeparator()) {
                    retval.addSeparator();
                }

                JButton button = retval.add(action);
                button.setText(null);

                addNiceStuff(button, action);
            }
        }

        return retval;
    }

    private void addNiceStuff(AbstractButton button, AntAction action) {
        // Set the action command so that it is consitent
        // no matter what language the display is in.
        button.setActionCommand(action.getID());

        // XXX this should be moved to the config file.
        String label = button.getText();
        if(label != null) {
            button.setMnemonic(label.charAt(0));
        }

        String tip = action.getShortDescription();
        if(tip != null) {
            button.setToolTipText(tip);
        }
    }


    /** Class for updating the enabled status of icons based
     *  on the events seen. */
    private class Enabler implements BusMember {
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
         * Receives all events.
         * 
         * @param event Event to post.
         * @return true if event should be propogated, false if
         * it should be cancelled.
         */
        public boolean eventPosted(EventObject event) {
            _mapper.applyEvent(event);
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
            return true;
        }
    }


}
