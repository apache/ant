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
package org.apache.tools.ant.gui.modules;

import org.apache.tools.ant.gui.core.AntModule;
import org.apache.tools.ant.gui.core.AppContext;
import org.apache.tools.ant.gui.event.*;
import javax.swing.*;
import javax.swing.plaf.ComponentUI;
import javax.swing.text.*;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Dimension;
import java.awt.Color;
import java.util.EventObject;

/**
 * Logging console display.
 * 
 * @version $Revision$ 
 * @author Simeon Fitch 
 */
public class BuildConsole extends AntModule {
    /** Area where messages are printed. */
    private JTextPane _text = null;
    /** Selection of logging levels. */
    private JComboBox _logLevel = null;
    /** Display styles. */
    private ConsoleStyleContext _styles = null;
    
	/** 
	 * Default ctor.
	 */
    public BuildConsole() {
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

        _styles = new ConsoleStyleContext();
        _text = new JTextPane(_styles.getStyledDocument());
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
         * @return true if event should be propogated, false if
         * it should be cancelled.
         */
        public boolean eventPosted(EventObject event) {
            if(event instanceof NewProjectEvent) {
                clearDisplay();
                return true;
            }

            AntBuildEvent buildEvent = (AntBuildEvent) event;
            Style style = null;
            String text = null;

            switch(buildEvent.getType().getValue()) {
              case BuildEventType.BUILD_STARTED_VAL:
                  clearDisplay();
              case BuildEventType.BUILD_FINISHED_VAL:
                  text = buildEvent.getType().toString();
                  style = _styles.getHeadingStyle();
                  break;
              case BuildEventType.TARGET_STARTED_VAL:
                  text = buildEvent.getEvent().getTarget().getName() + ":";
                  style = _styles.getSubheadingStyle();
                  break;
              case BuildEventType.TARGET_FINISHED_VAL:
              case BuildEventType.TASK_STARTED_VAL:
              case BuildEventType.TASK_FINISHED_VAL:
                  break;
              case BuildEventType.MESSAGE_LOGGED_VAL:
                  // Filter out events that are below our
                  // selected filterint level.
                  LogLevelEnum level = 
                      (LogLevelEnum) _logLevel.getSelectedItem();
                  int priority = buildEvent.getEvent().getPriority();
                  if(priority <= level.getValue()) {
                      text = buildEvent.toString();
                      style = _styles.getStyle(LogLevelEnum.fromInt(priority));
                  }
                  break;
            }

            if(text != null) {
                try {
                    Document doc = _text.getDocument();
                    doc.insertString(doc.getLength(), text, style);
                    doc.insertString(doc.getLength(), "\n", null);
                }
                catch(Exception ex) {
                    // XXX log me.
                    ex.printStackTrace();
                }
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
            return event instanceof AntBuildEvent ||
                event instanceof NewProjectEvent;
        }
    }

    /** Style set for pretty display of the console messages. */
    private static class ConsoleStyleContext extends StyleContext {
        /** Name of the style used for headings. */
        public static final String HEADING_STYLE = "headingStyle";
        /** Name of the style used for subheadings. */
        public static final String SUBHEADING_STYLE = "subheadingStyle";

        /** XXX temporary list of style colors. To go away once a real set of
         *  properties is implemented... */
        private static final Color[] _colors = {
            Color.red,
            Color.magenta,
            Color.black,
            Color.darkGray,
            Color.blue
        };

        /** 
         * Default ctor.
         * 
         */
        public ConsoleStyleContext() {
            Style defaultStyle = getStyle(DEFAULT_STYLE);
            StyleConstants.setFontSize(defaultStyle, 12);

            Style msgBase = addStyle("msgBase", defaultStyle);
            StyleConstants.setFontFamily(msgBase, "Monospaced");

            LogLevelEnum[] levels = LogLevelEnum.getValues();
            for(int i = 0; i < levels.length; i++) {
                Style curr = addStyle(levels[i].toString(), msgBase);
                StyleConstants.setFontSize(curr, 10);
                StyleConstants.setForeground(curr, _colors[i]);
            }

            Style heading = addStyle(HEADING_STYLE, defaultStyle);
            StyleConstants.setFontFamily(heading, "SansSerif");
            StyleConstants.setBold(heading, true);
            StyleConstants.setUnderline(heading, true);

            Style subheading = addStyle(SUBHEADING_STYLE, heading);
            StyleConstants.setFontSize(subheading, 10);
            StyleConstants.setUnderline(subheading, false);
        }

        /** 
         * Get the style to use for the given logging level.
         * 
         * @param level Logging level.
         * @return Style to use for display.
         */
        Style getStyle(LogLevelEnum level) {
            Style retval = getStyle(level.toString());
            return retval == null ? getDefaultStyle() : retval;
        }

        /** 
         * Get the default style.
         * 
         * @return Default style.
         */
        Style getDefaultStyle() {
            return getStyle(DEFAULT_STYLE);
        }

        /** 
         * Get the style to use for headings.
         * 
         * @return Heading style.
         */
        Style getHeadingStyle() {
            return getStyle(HEADING_STYLE);
        }

        /** 
         * Get the style to use for subheadings.
         * 
         * @return Subheading style.
         */
        Style getSubheadingStyle() {
            return getStyle(SUBHEADING_STYLE);
        }

        /** 
         * Get a StyledDocument initialized with this.
         * 
         * @return SytledDocument.
         */
        StyledDocument getStyledDocument() {
            DefaultStyledDocument retval = new DefaultStyledDocument(this);
            retval.setLogicalStyle(0, getDefaultStyle());
            return retval;
        }
    }

}
