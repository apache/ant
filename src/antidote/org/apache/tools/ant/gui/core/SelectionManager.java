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
import org.apache.tools.ant.gui.command.*;
import org.apache.tools.ant.gui.acs.*;
import java.util.EventObject;
import java.awt.event.ActionEvent;
import javax.swing.*;

/**
 * State management class for keeping track of what build file elements are
 * currently selected. It monitors the EventBus for selection events and
 * Records the current state. It should be registered with the EventBus
 * at the MONITORING level.
 *
 * @version $Revision$ 
 * @author Simeon Fitch 
 */
public class SelectionManager implements BusMember {
    /** The filter for getting the correct events.*/
    private final Filter _filter = new Filter();

    /** The currently selected project. */
    private ACSProjectElement _project = null;
    /** The current set of selected targets. */
    private ACSTargetElement[] _targets = null;
    /** The current set of selected elements. */
    private ACSElement[] _elements = null;

    public SelectionManager() {
    }

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
     * Get the currently selected project.
     * 
     * @return Current project.
     */
    public ACSProjectElement getSelectedProject() {
        return _project;
    }

    /** 
     * Get the selected elements that are targets.
     * 
     */
    public ACSTargetElement[] getSelectedTargets() {
        return _targets;
    }

    /** 
     * Get the selected elements.
     * 
     */
    public ACSElement[] getSelectedElements() {
        return _elements;
    }

    /** 
     * Called when an event is to be posed to the member.
     * 
     * @param event Event to post.
     * @return true if event should be propogated, false if
     * it should be cancelled.
     */
    public boolean eventPosted(EventObject event) {
        _elements = ((ElementSelectionEvent)event).getSelectedElements();

        if(event instanceof TargetSelectionEvent) {
            _targets = ((TargetSelectionEvent)event).getSelectedTargets();
        }
        else if(event instanceof ProjectSelectedEvent) {
            _project = ((ProjectSelectedEvent)event).getSelectedProject();
        }
        return true;
    }

    /** Filter for ElementSelectionEvent objects. */
    private static class Filter implements BusFilter {
        public boolean accept(EventObject event) {
            return event instanceof ElementSelectionEvent;
        }
    }
}
