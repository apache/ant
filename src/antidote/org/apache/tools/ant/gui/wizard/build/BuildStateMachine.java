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
package org.apache.tools.ant.gui.wizard.build;
import org.apache.tools.ant.gui.wizard.*;
import org.apache.tools.ant.gui.core.ResourceManager;
import java.util.*;

/**
 * State machine defining the step ordering for the build wizard.
 * 
 * @version $Revision$ 
 * @author Simeon Fitch 
 */
public class BuildStateMachine extends DefaultStateMachine {
    private List _optionals = Arrays.asList(ProjectSetupStep.OPTIONS);


    /** 
     * Get the next step.
     * 
     * @param curr The current step.
     * @param data The current state of the wizard.
     * @return The ID of next step, or null if there currently isn't one.
     */
    public String getNext(ProjectSetupStep curr, WizardData data) {
        return getFollowingID(super.getNext(curr, data), data, +1);
    }

    /** 
     * Get the next step.
     * 
     * @param curr The current step.
     * @param data The current state of the wizard.
     * @return The ID of next step, or null if there currently isn't one.
     */
    public String getNext(OptionalStep curr, WizardData data) {
        return getFollowingID(super.getNext(curr, data), data, +1);
    }

    /** 
     * Get the previous step.
     * 
     * @param curr The current step.
     * @param data The current state of the wizard.
     * @return The ID of previous step, or null if there currently isn't one.
     */
    public String getPrevious(OptionalStep curr, WizardData data) {
        return getFollowingID(super.getPrevious(curr, data), data, -1);
    }

    /** 
     * Get the previous step.
     * 
     * @param curr The current step.
     * @param data The current state of the wizard.
     * @return The ID of previous step, or null if there currently isn't one.
     */
    public String getPrevious(FinishStep curr, WizardData data) {
        return getFollowingID(super.getPrevious(curr, data), data, -1);
    }

    /** 
     * Figure out which ID should follow the given one based on the current
     * state setting of the optional steps.
     * 
     * @param curr ID of the current step.
     * @param data State data.
     * @param direction +1 for next, -1 for previous.
     * @return The ID to follow, or null if none.
     */
    private String getFollowingID(String curr, WizardData data, int direction) {
        String follow = curr;
        List steps = getStepList(data);
        List setting = ((BuildData)data).getOptionalSteps();

        while(follow != null && _optionals.contains(follow) && 
              !setting.contains(follow)) {

            int index = steps.indexOf(follow) + direction;
            if(index >= 0 && index < steps.size()) {
                follow = (String) steps.get(index);
            }
            else {
                follow = null;
            }
        }

        return follow;
    }
}
