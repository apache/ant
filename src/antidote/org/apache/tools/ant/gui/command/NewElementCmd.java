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
package org.apache.tools.ant.gui.command;
import java.util.EventObject;
import javax.swing.AbstractButton;
import javax.swing.Action;
import org.apache.tools.ant.gui.core.AppContext;
import org.apache.tools.ant.gui.event.NewBaseElementEvent;
import org.apache.tools.ant.gui.event.RefreshDisplayEvent;
import org.apache.tools.ant.gui.acs.*;
import org.apache.tools.ant.gui.util.WindowUtils;
import org.apache.tools.ant.gui.core.AntAction;

/**
 * Command for creating a new propertyh.
 * 
 * @version $Revision$ 
 * @author Simeon Fitch 
 */
public class NewElementCmd extends AbstractCommand {
    /** New count for this session. Used to create default names, 
     *  numbered as a convenience. */
    private static int _count = 1;
    private EventObject _event = null;
    
	/** 
	 * Standard ctor.
	 * 
	 * @param context Application context.
	 */ 
    public NewElementCmd(AppContext context, EventObject event) {
        super(context);
        _event = event;
    }

    /** 
     * Creates a new xml element based on the button which
     * was pressed.  The button text may contain the name
     * of the new element or a dialog box is presented which
     * asks the user for the element type.
     */
    public void run() {

        // Find which element is selected.
        ACSElement[] vals = getContext().getSelectionManager().
            getSelectedElements();
        if(vals == null || vals.length == 0) {
            return;
        }
            
        // Find the text of the button which was pressed
        // to determine the type of element to create.
        Object source = _event.getSource();
        if (!(source instanceof AbstractButton)) {
            return;
        }
        AbstractButton button = (AbstractButton) source;
        String name = button.getText();

        // Get the AntAction
        String cmdStr = button.getActionCommand();
        AntAction antAction = getContext().getActions().getAction(cmdStr);
        if (antAction == null) {
            return;
        }

        ACSElement e = vals[vals.length - 1];
        
        // Should we prompt the user use the element type?
        if (antAction.getName().equals(name)) {
            
            // Display the dialog box.
            ACSDtdDefinedElement dtde = (ACSDtdDefinedElement) e;
            NewElementDlg dlg = new NewElementDlg(
                getContext().getParentFrame(), true);
            dlg.setList(dtde.getPossibleChildren());
            dlg.pack();
            WindowUtils.centerWindow(dlg);
            dlg.setTitle("Select the new element type");
            dlg.setVisible(true);
        
            // Get the element type 
            if (dlg.getCancel()) {
                name = "";
            } else {
                name = dlg.getElementName();
            }
        }

        if (name.length() > 0) {
            // Create the new element
            ACSElement retval = 
                ACSFactory.getInstance().createElement(e, name);
            getContext().getEventBus().postEvent(
                new NewBaseElementEvent(getContext(),  retval));
        } else {
            // Request a refresh so the popup menu is removed 
            // from the display.
            getContext().getEventBus().postEvent(
                new RefreshDisplayEvent(getContext()));
        }
    }
}



