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
package org.apache.tools.ant.gui.event;

import java.util.*;
import javax.swing.SwingUtilities;
/**
 * An event "bus" providing a centralized place for posting
 * and recieving generic application events. To receive events a class must 
 * implement the "BusMember" interface. When registering as a member, an
 * "interrupt level" is provided, which specifies a relative ordering level
 * that the member wishes to receive events for. By convention, a member
 * can be registered at the MONITORING, VETOING, or RESPONDING levels, which
 * correspond to recieving events first to receiving events last. If a member
 * receives an event, the event is of type AntEvent, and the member calls the 
 * AntEvent.cancel() method, the event is not then delivered
 * to subsequent members. Members also indicate interest in an event
 * by providing an instance of the BusFilter interface.<BR>
 *
 * NB: This class is overly simple right now, but will eventually
 * be expanded to do better event filtering, interrupt levels, etc. 
 * 
 * @version $Revision$ 
 * @author Simeon Fitch 
*/
public class EventBus {
	/** The default "monitoring" interrupt level, used by members who
	 * are only listeners/monitors of events. */
	public static final int MONITORING = 1;
	/** The default "vetoing" interrupt level, used by bus members
	 *  whose role is to veto request events or otherwise handle an
	 *  event before it is processed by the default handler. */
	public static final int VETOING = 5;
	/** The default "responding" interrupt level, for members who service
	 *  events in a default manner. */
	public static final int RESPONDING = 10;

	/** The maximum valid interrupt value. */
	public static final int MAX_INTERRUPT = 15;

    /** Set of bus members, with a list for each interrupt level. */
    private List[] _memberSet = new List[MAX_INTERRUPT];

	/** 
	 * Default ctor.
	 * 
	 */
    public EventBus() {
    }

	/** 
	 * Add a member to the bus. 
	 * 
	 * @param intLevel Interrupt level.
	 * @param member Member to add.
	 */
    public void addMember(int intLevel, BusMember member) {
		if(intLevel < 1 || intLevel > MAX_INTERRUPT) {
			throw new IllegalArgumentException(
				"Invalid interrupt level: " + intLevel);
		}
        synchronized(_memberSet) {
			List list = _memberSet[intLevel - 1];
			if(list == null) {
				list = new LinkedList();
				_memberSet[intLevel - 1] = list;
			}
            list.add(member);
        }
    }


	/** 
	 * Remove a member from the bus.
	 * 
	 * @param member Member to remove.
	 */
    public void removeMember(BusMember member) {
        synchronized(_memberSet) {
			// XXX lets hope we don't do too much removing. Yuck...
			for(int i = 0; i < _memberSet.length; i++) {
				if(_memberSet[i] == null) continue;
				_memberSet[i].remove(member);
			}
        }
    }

	/** 
	 * Method used for sending an event to the bus.
	 * 
	 * @param event Event to post.
	 */
    public void postEvent(EventObject event) {
        EventDispatcher disp = new EventDispatcher(event);

        // Events need to be dispatched on the AWTEvent thread, as the UI
        // components assume that.
        if(SwingUtilities.isEventDispatchThread()) {
            disp.run();
        }
        else {
            SwingUtilities.invokeLater(disp);
        }
    }

    /** Class that performs the duty of dispatching events to the members. */
    private class EventDispatcher implements Runnable {
        /** Event to dispatch. */
        private EventObject _event = null;
        
        /** 
         * Standard ctor.
         * 
         * @param event Event to dispatch.
         */
        public EventDispatcher(EventObject event) {
            _event = event;
        }

        /** 
         * Perform dispatching.
         * 
         */
        public void run() {
            synchronized(_memberSet) {
                for(int i = 0; i < _memberSet.length; i++) {
                    if(_memberSet[i] == null) continue;
                    
                    Iterator it = _memberSet[i].iterator();
                    while(it.hasNext()) {
                        BusMember next = (BusMember) it.next();
                        BusFilter filter = next.getBusFilter();
                        if(filter == null || filter.accept(_event)) {
                            next.eventPosted(_event);
                        }
                        // Check to see if the member cancelled the event. If so
                        // then don't send it on to the other members.
                        if(_event instanceof AntEvent &&
                           ((AntEvent)_event).isCancelled()) break;
                    }
                }
            }
        }
    }
}
