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
package org.apache.tools.ant.gui.core;

import javax.swing.*;
import java.net.URL;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.*;

import org.apache.tools.ant.gui.event.EventBus;

/**
 * Class representing an action in the Antidote application. 
 * 
 * @version $Revision$ 
 * @author Simeon Fitch 
 */
public class AntAction extends AbstractAction {
    /** Property name for the parent menu item. */
    public static final String PARENT_MENU_NAME = "parentMenuName";
    public static final String SEPARATOR = "separator";
    public static final String ACCELERATOR = "accelerator";
    public static final String ENABLED = "enabled";
    public static final String ENABLE_ON = "enableOn";
    public static final String DISABLE_ON = "disableOn";
    public static final String TOGGLE = "toggle";
    public static final String COMMAND = "command";

    /** Property resources. */
    private  ResourceBundle _resources =  null;
    /** Event bus. */
    private EventBus _bus = null;
    /** Unique id. */
    private String _id = null;

    /** Events that the action should cause transition to the 
     *  enabled(true) state. */
    private Class[] _enableOn = null;
    /** Events that the action should cause transition to the 
     *  enabled(false) state. */
    private Class[] _disableOn = null;
    /** Flag indicating toggle action. */
    private boolean _toggle = false;


    /** 
     * Standard ctor.
     * 
     * @param id Unique id for the action
     */
    public AntAction(ResourceBundle resources, EventBus bus, String id) {
        _resources = resources;
        _bus = bus;
        _id = id;
        putValue(NAME, getString("name"));
        putValue(SHORT_DESCRIPTION, getString("shortDescription"));
        putValue(PARENT_MENU_NAME, getString(PARENT_MENU_NAME));
        putValue(SEPARATOR, getString(SEPARATOR));


        // Set the default enabled state.
        String enabled = getString(ENABLED);
        if(enabled != null) {
            setEnabled(Boolean.valueOf(enabled).booleanValue());
        }

        // Set an accellerator if any.
        String accelerator = getString(ACCELERATOR);
        if(accelerator != null) {
            putValue(ACCELERATOR, KeyStroke.getKeyStroke(accelerator));
        }

        // Check to see if action is a toggle action.
        String toggle = getString(TOGGLE);
        if(toggle != null) {
            _toggle = Boolean.valueOf(toggle).booleanValue();
        }

        // See if there is a command associated with the action.
        String command = getString(COMMAND);
        if(command != null) {
            try {
                Class cmd = Class.forName(command);
                putValue(COMMAND, cmd);
            }
            catch(Exception ex) {
                // XXX log me.
                ex.printStackTrace();
            }
        }

        // Add an icon if any (which means it'll show up on the tool bar).
        String iconName = getString("icon");
        if(iconName != null) {
            try {
                URL imageLoc = 
                    AntAction.class.getResource("resources/" + iconName);
                if(imageLoc != null) {
                    putValue(SMALL_ICON, new ImageIcon(imageLoc));
                }
            }
            catch(Exception ex) {
                // XXX log me.
                ex.printStackTrace();
            }
        }

        _enableOn = resolveClasses(getString(ENABLE_ON));
        _disableOn = resolveClasses(getString(DISABLE_ON));

    }
        
	/** 
	 * Convenience method for looking put a resource with the name
     * "id.key". Will return null if the resource doesn't exist.
	 * 
	 * @param key Key name for the action.
	 * @return String resource for composite key, or null if not found.
	 */
    private String getString(String key) {
        String retval = null;
        try {
            retval = _resources.getString(_id + "." + key);
        }
        catch(MissingResourceException ex) {
            // Its ok to be missing a resource name...
            // Too bad the API throws an exception in this case. 
        }
        return retval;
    }


	/** 
	 * Parse out the list of classes from the given string and
     * resolve them into classes.
	 * 
	 * @param classNames Comma delimited list of class names.
	 */
    private Class[] resolveClasses(String classNames) {
        if(classNames == null) return null;

        StringTokenizer tok = new StringTokenizer(classNames, ", ");
        Vector vals = new Vector();
        while(tok.hasMoreTokens()) {
            String name = tok.nextToken();
            try {
                vals.addElement(Class.forName(name));
            }
            catch(ClassNotFoundException ex) {
                //XXX log me.
                System.err.println(
                    "Warning: the event class " + name + 
                    " was not found. Please check config file.");
            }
        }

        Class[] retval = new Class[vals.size()];
        vals.copyInto(retval);
        return retval;
    }

    /** 
     * Unique id for the action.
     * 
     * @return Action id.
     */
    public String getID() {
        return _id;
    }

    /** 
     * Get the name of the menu in the menu bar that this action shoul
     * appear under.
     * 
     * @return Menu to appear under, or null if not a menu action.
     */
    public String getParentMenuName() {
        return (String) getValue(PARENT_MENU_NAME);
    }
        
    /** 
     * Get the localized name for the action.
     * 
     * @return Name
     */
    public String getName() {
        return (String) getValue(NAME);
    }
        
    /** 
     * Get the short description. Used in tool tips.
     * 
     * @return Short description.
     */
    public String getShortDescription() {
        return (String) getValue(SHORT_DESCRIPTION);
    }
        
    /** 
     * Determine if a separator should appear before the action.
     * 
     * @return True if add separator, false otherwise.
     */
    public boolean isPreceededBySeparator() {
        return Boolean.valueOf(
            String.valueOf(getValue(SEPARATOR))).booleanValue();
    }

    /** 
     * Get the icon.
     * 
     * @return Icon for action, or null if none.
     */
    public Icon getIcon() {
        return (Icon) getValue(SMALL_ICON);
    }

	/** 
	 * Get the accelerator keystroke.
	 * 
	 * @return Accelerator
	 */
    public KeyStroke getAccelerator() {
        return (KeyStroke) getValue(ACCELERATOR);
    }


	/** 
	 * Get the event types which should cause this to go to the
     * enabled state.
	 * 
	 */
    public Class[] getEnableOnEvents() {
        return _enableOn;
    }

	/** 
	 * Get the event types which should cause this to go to 
     * this disabled state.
	 * 
	 */
    public Class[] getDisableOnEvents() {
        return _disableOn;
    }

	/** 
	 * True if this is a toggle action, false otherwise.
	 * 
	 * @return True if this is a toggle action, false otherwise.
	 */
    public boolean isToggle() {
        return _toggle;
    }


	/** 
	 * Get the assciated command class.
	 * 
	 * @return Command class.
	 */
    public Class getCommandClass() {
        return (Class) getValue(COMMAND);
    }

    /** 
     * Pass the action on to the EventBus.
     * 
     * @param e Event to forward.
     */
    public void actionPerformed(ActionEvent e) {
        _bus.postEvent(e);
    }
}

