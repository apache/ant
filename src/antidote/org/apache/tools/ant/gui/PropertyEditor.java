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
import org.apache.tools.ant.gui.acs.ACSTargetElement;
import org.apache.tools.ant.gui.event.*;
import javax.swing.*;
import java.util.*;
import java.io.StringReader;
import java.io.IOException;
import java.awt.BorderLayout;

/**
 * Stub for a property editor.
 *
 * @version $Revision$ 
 * @author Simeon H.K. Fitch 
 */
class PropertyEditor extends AntEditor {

    /** Text pane. */
    private JEditorPane _text = null;

	/** 
	 * Standard ctor.
	 * 
	 * @param context Application context. 
	 */
	public PropertyEditor(AppContext context) {
        super(context);
        context.getEventBus().addMember(EventBus.MONITORING, new Handler());
        setLayout(new BorderLayout());

        _text = new JEditorPane("text/html", getAppContext().getResources().
                                getString(getClass(), "noTargets"));
        _text.setEditable(false);
        _text.setOpaque(false);

        JScrollPane scroller = new JScrollPane(_text);

        add(BorderLayout.CENTER, scroller);
	}

	/** 
	 * Populate the display with the given target info.
	 * 
	 * @param targets Targets to display info for.
	 */
    private void displayTargetInfo(ACSTargetElement[] targets) {

        // The text to display.
        String text = null;

        int num = targets == null ? 0 : targets.length;
        Object[] args = null;
        switch(num) {
          case 0:
              text = getAppContext().getResources().
                  getString(getClass(), "noTargets");
              break;
          case 1:
              args = getTargetParams(targets[0]);
              text = getAppContext().getResources().
                  getMessage(getClass(), "oneTarget", args);
              break;
          default:
              args = getTargetParams(targets);
              text = getAppContext().getResources().
                  getMessage(getClass(), "manyTargets", args);
              break;
        }

        if(text != null) {
            _text.setText(text);
        }
    }

	/** 
	 * Get the parameters for the formatted message presented for a single
     * target.
	 * 
	 * @param target Target to generate params for.
     * @return Argument list for the formatted message.
	 */
    private Object[] getTargetParams(ACSTargetElement target) {
        List args = new LinkedList();
        args.add(target.getName());
        args.add(target.getDescription() == null ? 
                 "" : target.getDescription());
        StringBuffer buf = new StringBuffer();
        String[] depends = target.getDependencyNames();
        for(int i = 0; i < depends.length; i++) {
            buf.append(depends[i]);
            if(i < depends.length - 1) {
                buf.append(", ");
            }
        }
        args.add(buf.toString());

        return args.toArray();
    }

	/** 
	 * Get the parameters for the formatted message presented for multiple
     * targets.
	 * 
	 * @param target Targets to generate params for.
     * @return Argument list for the formatted message.
	 */
    private Object[] getTargetParams(ACSTargetElement[] targets) {
        List args = new LinkedList();

        StringBuffer buf = new StringBuffer();
        Set depends = new HashSet();
        for(int i = 0; i < targets.length; i++) {
            buf.append(targets[i].getName());
            if(i < targets.length - 1) {
                buf.append(", ");
            }

            String[] dependNames = targets[i].getDependencyNames();
            for(int j = 0; j < dependNames.length; j++) {
                depends.add(dependNames[j]);
            }
        }

        args.add(buf.toString());

        Iterator it = depends.iterator();
        buf = new StringBuffer();
        while(it.hasNext()) {
            buf.append(it.next());
            if(it.hasNext()) {
                buf.append(", ");
            }
        }

        args.add(buf.toString());

        return args.toArray();
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
         */
        public void eventPosted(EventObject event) {
            TargetSelectionEvent e = (TargetSelectionEvent) event;
            ACSTargetElement[] targets = e.getSelectedTargets();
            displayTargetInfo(targets);
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
            return event instanceof TargetSelectionEvent;
        }
    }
}
