/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2002 The Apache Software Foundation.  All rights
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
 * 4. The names "Ant" and "Apache Software
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

package org.apache.tools.ant.types.selectors;

import java.io.File;
import java.util.Enumeration;

/**
 * This selector is here just to shake up your thinking a bit. Don't get
 * too caught up in boolean, there are other ways you can evaluate a
 * collection of selectors. This one takes a vote of the selectors it
 * contains, and majority wins. You could also have an "all-but-one"
 * selector, a "weighted-average" selector, and so on. These are left
 * as exercises for the reader (as are the usecases where this would
 * be necessary).
 *
 * @author <a href="mailto:bruce@callenish.com">Bruce Atherton</a>
 * @since 1.5
 */
public class MajoritySelector extends BaseSelectorContainer {

    private boolean allowtie = true;

    /**
     * Default constructor.
     */
    public MajoritySelector() {
    }

    public String toString() {
        StringBuffer buf = new StringBuffer();
        if (hasSelectors()) {
            buf.append("{majorityselect: ");
            buf.append(super.toString());
            buf.append("}");
        }
        return buf.toString();
    }

    public void setAllowtie(boolean tiebreaker) {
        allowtie = tiebreaker;
    }

    /**
     * Returns true (the file is selected) if most of the other selectors
     * agree. In case of a tie, go by the allowtie setting. That defaults
     * to true, meaning in case of a tie, the file is selected.
     *
     * @param basedir the base directory the scan is being done from
     * @param filename is the name of the file to check
     * @param file is a java.io.File object for the filename that the selector
     * can use
     * @return whether the file should be selected or not
     */
    public boolean isSelected(File basedir, String filename, File file) {
        validate();
        int yesvotes = 0;
        int novotes = 0;
        Enumeration e = selectorElements();
        boolean result;

        while(e.hasMoreElements()) {
            result = ((FileSelector)e.nextElement()).isSelected(basedir,
                    filename,file);
            if (result) {
                yesvotes = yesvotes + 1;
            }
            else {
                novotes = novotes + 1;
            }
        }
        if (yesvotes > novotes)
        {
            return true;
        }
        else if (novotes > yesvotes) {
            return false;
        }
        // At this point, we know we have a tie.
        return allowtie;
    }
}

