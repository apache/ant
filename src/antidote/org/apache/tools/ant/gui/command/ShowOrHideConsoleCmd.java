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
package org.apache.tools.ant.gui.command;
import org.apache.tools.ant.gui.core.AppContext;
import org.apache.tools.ant.gui.event.*;
import java.awt.*;
import javax.swing.*;

/**
 * Toggles the display of the console window
 *
 * @version $Revision$
 * @author Nick Davis<a href="mailto:nick_home_account@yahoo.com">nick_home_account@yahoo.com</a>
 */
public class ShowOrHideConsoleCmd extends AbstractCommand {
    /** Always show the console */
    boolean _alwaysShow = false;

    /**
    * Standard ctor.
    *
    * @param context Application context.
     */
    public ShowOrHideConsoleCmd(AppContext context) {
        super(context);
        _alwaysShow = false;
    }

    /**
    * Standard ctor.
    *
    * @param context Application context.
     */
    public ShowOrHideConsoleCmd(AppContext context, boolean alwaysShow) {
        super(context);
        _alwaysShow = alwaysShow;
    }


    /**
     * If the console pane is visible, hide it.
     * If the console pane is not visible, show it.
     */
    public void run() {
        JComponent component = (JComponent) findComponent("Console");
        JSplitPane pane = (JSplitPane) component.getParent();
        if (_alwaysShow) {
            if (component.getHeight() == 0) {
                pane.setDividerLocation(pane.getLastDividerLocation());
            }
        } else {
            if (component.getHeight() == 0) {
                pane.setDividerLocation(pane.getLastDividerLocation());
            } else {
                pane.setDividerLocation(1.0);
            }
        }
    }

    /**
     * Starting from the top Frame, find the
     * first child window with the input name.
     *
     * @param name The name of the <code>Component</code>
     */
    private Component findComponent(String name) {
        JFrame frame = (JFrame) getContext().getParentFrame();
        JRootPane root = frame.getRootPane();
        return findChild(root.getContentPane(), name);
    }

    /**
     * Search the <code>Container</code> for a <code>Component</code>
     * with the input name. The search is recursive.
     *
     * @param container The <code>Container</code> to search
     * @param name The name of the <code>Component</code>
     */
    private Component findChild(Container container, String name) {
        Component[] components = container.getComponents();
        for (int i = 0; i < components.length; i++) {
            Component component = components[i];
            if ( name.equals(component.getName()) ) {
                return component;
            }
            if (component instanceof java.awt.Container) {
                Component test = findChild(
                    (java.awt.Container) component, name);
                if (test != null) {
                    return test;
                }
            }
        }
        return null;
    }
}
