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
import org.apache.tools.ant.gui.command.*;
import java.util.EventObject;
import java.awt.event.ActionEvent;
import javax.swing.*;

/**
 * The purpose of this class is to watch for events that require some sort
 * of action, like opening a file.
 * 
 * @version $Revision$ 
 * @author Simeon Fitch 
 */
class EventResponder {

    /** The application context. */
    private AppContext _context = null;

	/** 
	 * Standard constructor. 
	 * 
	 * @param context Application context.
	 */
    public EventResponder(AppContext context) {
        _context = context;

        // XXX This needs to be changed, along with the event bus,
        // to allow the EventResponder to be the last listener
        // to receive the event. This will allow the addition
        // of event filters to yank an event out of the bus, sort of 
        // like an interrupt level.
        _context.getEventBus().addMember(
			EventBus.RESPONDING, new ActionResponder());
        _context.getEventBus().addMember(
			EventBus.RESPONDING, new AntResponder());
    }

    /** Handler for bus events. */
    private class ActionResponder implements BusMember {
        private final ActionFilter _filter = new ActionFilter();

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
            String command = ((ActionEvent)event).getActionCommand();

            Command cmd = 
                _context.getActions().getActionCommand(command, _context);
            if(cmd != null) {
                cmd.run();
                return false;
            }
            else {
				// XXX log me.
                System.err.println("Unhandled action: " + command);
                // XXX temporary.
                new DisplayErrorCmd(
                    _context, 
                    "Sorry. \"" + command + 
                    "\" not implemented yet. Care to help out?").run();
                return true;
            }
        }
    }

    /** Filter for action events. */
    private static class ActionFilter implements BusFilter {
        public boolean accept(EventObject event) {
            return event instanceof ActionEvent;
        }
    }

    /** Handler for bus events. */
    private class AntResponder implements BusMember {
        private final AntFilter _filter = new AntFilter();

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
            AntEvent e = (AntEvent) event;
            Command cmd = e.createDefaultCmd();
            cmd.run();
            return cmd instanceof NoOpCmd;
        }
    }

    /** Filter for ant events. */
    private static class AntFilter implements BusFilter {
        public boolean accept(EventObject event) {
            return event instanceof AntEvent;
        }
    }

}
