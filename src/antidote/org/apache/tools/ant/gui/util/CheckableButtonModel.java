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
package org.apache.tools.ant.gui.util;

import java.awt.event.ActionEvent;
import javax.swing.DefaultButtonModel;

/**
 * Provides a button which appears "pressed" when it is in
 * a selected state.
 * <p>
 * Call <code>setSelected</code> to select the button. When the
 * button is selected, both the PRESSED and ARMED properties are
 * set which gives the button a pressed appearance.
 *
 * @version $Revision$
 * @author Nick Davis<a href="mailto:nick_home_account@yahoo.com">nick_home_account@yahoo.com</a>
 */
public class CheckableButtonModel extends DefaultButtonModel {

    boolean _pressed = false;
    boolean _armed = false;

    /**
     * Constructs a CheckableButtonModel
     *
     */
    public CheckableButtonModel() {
    }

    /**
     * Sets the button to pressed or unpressed.
     *
     * @param b true to set the button to "pressed"
     * @see #isPressed
     */
    public void setPressed(boolean b) {

        if((_pressed == b) || !isEnabled()) {
            return;
        }

        _pressed = b;

        if(!_pressed && _armed) {
            fireActionPerformed(
                new ActionEvent(this, ActionEvent.ACTION_PERFORMED,
                                getActionCommand())
                );
        }

        fireStateChanged();

        stateMask |= PRESSED;
    }

    /**
     * Marks the button as "armed". If the mouse button is
     * released while it is over this item, the button's action event
     * fires. If the mouse button is released elsewhere, the
     * event does not fire and the button is disarmed.
     *
     * @param b true to arm the button so it can be selected
     */
    public void setArmed(boolean b) {

        if((_armed == b) || !isEnabled()) {
            return;
        }

        _armed = b;
        fireStateChanged();
        stateMask |= ARMED;
    }

    /**
     * Returns true if the button is selected.
     *
     * @return true if the button is "selected"
     */
    public boolean isArmed() {
        return isSelected();
    }

    /**
     * Returns true if the button is selected.
     *
     * @return true if the button is "selected"
     */
    public boolean isPressed() {
        return isSelected();
    }
}
