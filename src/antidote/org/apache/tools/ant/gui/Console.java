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
import javax.swing.plaf.ComponentUI;
import javax.swing.text.Document;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Dimension;
import java.util.EventObject;

/**
 * Logging console display.
 * 
 * @version $Revision$ 
 * @author Simeon Fitch 
 */
public class Console extends AntEditor {
    /** Area where messages are printed. */
    private JTextPane _text = null;
    /** Selection of logging levels. */
    private JComboBox _logLevel = null;
        
	/** 
	 * Standard ctor.
	 * 
	 * @param context Application context;
	 */
    public Console(AppContext context) {
        super(context);
        context.getEventBus().addMember(EventBus.MONITORING, new Handler());
        setLayout(new BorderLayout());

        _text = new NoWrapTextPane();
        _text.setEditable(false);
        JScrollPane scroller = new JScrollPane(_text);
        scroller.setVerticalScrollBarPolicy(
            JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        add(BorderLayout.CENTER, scroller);

        JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JLabel label = new JLabel(
            context.getResources().getString(getClass(), "logLevel"));
        controls.add(label);
        _logLevel = new JComboBox(LogLevelEnum.getValues());
        _logLevel.setSelectedItem(LogLevelEnum.INFO);
        controls.add(_logLevel);
        
        add(BorderLayout.NORTH, controls);

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
         * Clear the contents of the console.
         * 
         */
        private void clearDisplay() {
            Document doc = _text.getDocument();
            try {
                doc.remove(0, doc.getLength());
                
            }
            catch(Exception ex) {
                // Intentionally ignored.
            }
        }

        /** 
         * Called when an event is to be posed to the member.
         * 
         * @param event Event to post.
         */
        public void eventPosted(EventObject event) {
            if(event instanceof NewProjectEvent) {
                clearDisplay();
                return;
            }

            AntBuildEvent buildEvent = (AntBuildEvent) event;
            String text = null;

            switch(buildEvent.getType().getValue()) {
              case BuildEventType.BUILD_STARTED_VAL:
                  clearDisplay();
                  break;
              case BuildEventType.TARGET_STARTED_VAL:
                  text = buildEvent.getEvent().getTarget().getName() + ":";
                  break;
              case BuildEventType.TARGET_FINISHED_VAL:
              case BuildEventType.TASK_STARTED_VAL:
              case BuildEventType.TASK_FINISHED_VAL:
                  break;
              case BuildEventType.MESSAGE_LOGGED_VAL:
                  text = buildEvent.toString();
                  break;
            }

            // Filter out events that are below our selected filterint level.
            LogLevelEnum level = (LogLevelEnum) _logLevel.getSelectedItem();
            if(buildEvent.getEvent().getPriority() > level.getValue()) return;

            if(text != null) {
                try {
                    Document doc = _text.getDocument();
                    doc.insertString(doc.getLength(), text, null);
                    doc.insertString(doc.getLength(), "\n", null);
                }
                catch(Exception ex) {
                    // XXX log me.
                    ex.printStackTrace();
                }
            }
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
            return event instanceof AntBuildEvent ||
                event instanceof NewProjectEvent;
        }
    }

    /** Specialization of JTextPane to provide proper wrapping behavior. */
    private static class NoWrapTextPane extends JTextPane {
        /** 
         * Overridden to ensure that the JTextPane is only
         * forced to match the viewport if it is smaller than
         * the viewport.
         * 
         * @return True if smaller than viewport, false otherwise.
         */
        public boolean getScrollableTracksViewportWidth() {
            ComponentUI ui = getUI();
            return getParent() != null ? ui.getPreferredSize(this).width <=
                getParent().getSize().width : true;
        }
    }

}
