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
import org.apache.tools.ant.gui.acs.*;
import org.apache.tools.ant.gui.command.Command;
import org.apache.tools.ant.gui.command.DisplayErrorCmd;
import org.apache.tools.ant.gui.core.AppContext;

/**
 * Event indicating that the current set of selected targets has changed.
 * 
 * @version $Revision$ 
 * @author Simeon Fitch 
 */
public class ElementSelectionEvent extends AntEvent {

    /** New set of selected elements. */
    private ACSElement[] _selected = null;

	/** 
	 * Standard ctor.
	 * 
	 * @param context application context.
     * @param selected the selected Elements.
	 */
    protected ElementSelectionEvent(AppContext context, 
                                    ACSElement[] selected) {
        super(context);
        _selected = selected;
    }

	/** 
	 * Current set of selected elements.
	 * 
     * @return selected element set.
	 */
    public ACSElement[] getSelectedElements() {
        return _selected;
    }


	/** 
	 * Factory method for creating the appropriate specialization of this
     * for communicating an element selection.
	 * 
	 * @param context App context.
	 * @param selected The set of selected events. The last elemetn in the 
     *                 array is used to determine the specialization of this
     *                 that should be returned.
	 * @return Event to communicate selection to.
	 */
    public static ElementSelectionEvent createEvent(AppContext context, 
                                                    ACSElement[] selected) {
        ElementSelectionEvent retval = null;

        if(selected != null && selected.length > 0) {
            Class type = selected[selected.length - 1].getClass();
            if(type.isAssignableFrom(ACSTargetElement.class)) {
                retval = new TargetSelectionEvent(context, selected);
            }
            else if(type.isAssignableFrom(ACSTaskElement.class)) {
                retval = new TaskSelectionEvent(context, selected);
            }
            else if(type.isAssignableFrom(ACSPropertyElement.class)) {
                retval = new PropertySelectionEvent(context, selected);
            }
            else if(type.isAssignableFrom(ACSProjectElement.class)) {
                retval = new ProjectSelectionEvent(context, selected);
            }
            else {
                // For elements without a specific event
                // type just send and instance of this.
                retval = new ElementSelectionEvent(context, selected);
            }
        }
        else {
            retval = new NullSelectionEvent(context);
        }

        return retval;
    }
}
