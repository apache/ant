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

import org.apache.tools.ant.gui.customizer.DynamicCustomizer;
import org.apache.tools.ant.gui.core.*;
import org.apache.tools.ant.gui.acs.*;
import org.apache.tools.ant.gui.event.*;
import javax.swing.*;
import java.util.*;
import java.beans.*;
import java.io.StringReader;
import java.io.IOException;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Point;

/**
 * Stub for a property editor.
 *
 * @version $Revision$ 
 * @author Simeon H.K. Fitch 
 */
public class PropertyEditor extends AntModule {

    /** The editor for current bean.*/
    private Customizer _customizer = null;
    /** Container for the customizer. */
    private JPanel _container = null;
    /** Scroll area containing contents. */
    private JScrollPane _scroller = null;
    /** Property change forwarder. */
    private PropertyChangeForwarder _forwarder = new PropertyChangeForwarder();

    /** 
     * Default ctor.
     * 
     */
    public PropertyEditor() {
    }

    /** 
     * Using the given AppContext, initialize the display.
     * 
     * @param context Application context.
     */
    public void contextualize(AppContext context) {
        setContext(context);
        context.getEventBus().addMember(EventBus.MONITORING, new Handler());
        setLayout(new BorderLayout());
        _container = new JPanel(new BorderLayout());
        add(_scroller = new JScrollPane(_container));
    }

    /** 
     * Update the display for the current items. 
     * 
     * @param items Current items to display.
     */
    private void updateDisplay(ACSElement[] items) {
        if(_customizer != null) {
            _customizer.removePropertyChangeListener(_forwarder);
            _container.remove((Component)_customizer);
            _customizer = null;
        }

        if(items != null && items.length > 0) {
            // The last selection element is the the one the
            // user most recently selected.
            ACSElement item = items[items.length - 1];

            try {
                BeanInfo info = Introspector.getBeanInfo(item.getClass());
                _customizer = (Customizer) info.getBeanDescriptor().
                    getCustomizerClass().newInstance();
                _customizer.setObject(item);
                _container.add(BorderLayout.CENTER, (Component) _customizer);
                _customizer.addPropertyChangeListener(_forwarder);
            }
            catch(Exception ex) {
                // XXX log me.
                ex.printStackTrace();
            }
        }

        _container.validate();
        _container.repaint();
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
         * Called when an event is to be posted to the member.
         * 
         * @param event Event to post.
         * @return true if event should be propogated, false if
         * it should be cancelled.
         */
        public boolean  eventPosted(EventObject event) {
            ElementSelectionEvent e = (ElementSelectionEvent) event;
            ACSElement[] elements = e.getSelectedElements();
            updateDisplay(elements);
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
            return event instanceof ElementSelectionEvent;
        }
    }

    /** Class for forwarding property change events to the event bus. */
    private class PropertyChangeForwarder implements PropertyChangeListener {
        public void propertyChange(PropertyChangeEvent e) {
            getContext().getEventBus().postEvent(e);
        }
    }

}
