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

import org.apache.tools.ant.types.EnumeratedAttribute;
import org.apache.tools.ant.types.Parameter;
import org.apache.tools.ant.BuildException;

/**
 * Selector that filters files based on their size.
 *
 * @author <a href="mailto:bruce@callenish.com">Bruce Atherton</a>
 * @since 1.5
 */
public class SizeSelector extends BaseExtendSelector {

    private long size = -1;
    private long multiplier = 1;
    private long sizelimit = -1;
    private int cmp = 2;
    public final static String SIZE_KEY = "value";
    public final static String UNITS_KEY = "units";
    public final static String WHEN_KEY = "when";

    public SizeSelector() {
    }

    public String toString() {
        StringBuffer buf = new StringBuffer("{sizeselector value: ");
        buf.append(sizelimit);
        buf.append("compare: ");
        if (cmp == 0) {
            buf.append("less");
        }
        else if (cmp == 1) {
            buf.append("more");
        } else {
            buf.append("equal");
        }
        buf.append("}");
        return buf.toString();
    }

    /**
     * A size selector needs to know what size to base its selecting on.
     * This will be further modified by the multiplier to get an
     * actual size limit.
     *
     * @param size the size to select against expressed in units
     */
    public void setValue(long size) {
        this.size = size;
        if ((multiplier != 0) && (size > -1)) {
            sizelimit = size * multiplier;
        }
    }

    /**
     * Sets the units to use for the comparison. This is a little
     * complicated because common usage has created standards that
     * play havoc with capitalization rules. Thus, some people will
     * use "K" for indicating 1000's, when the SI standard calls for
     * "k". Others have tried to introduce "K" as a multiple of 1024,
     * but that falls down when you reach "M", since "m" is already
     * defined as 0.001.
     * <p>
     * To get around this complexity, a number of standards bodies
     * have proposed the 2^10 standard, and at least one has adopted
     * it. But we are still left with a populace that isn't clear on
     * how capitalization should work.
     * <p>
     * We therefore ignore capitalization as much as possible.
     * Completely mixed case is not possible, but all upper and lower
     * forms are accepted for all long and short forms. Since we have
     * no need to work with the 0.001 case, this practice works here.
     * <p>
     * This function translates all the long and short forms that a
     * unit prefix can occur in and translates them into a single
     * multiplier.
     *
     * @param units The units to compare the size to, using an
     *        EnumeratedAttribute
     */
    public void setUnits(ByteUnits units) {
        int i = units.getIndex();
        multiplier = 0;
        if ((i > -1) && (i < 4)) {
            multiplier = 1000;
        }
        else if ((i > 3) && (i < 9)) {
            multiplier = 1024;
        }
        else if ((i > 8) && (i < 13)) {
            multiplier = 1000000;
        }
        else if ((i > 12) && (i < 18)) {
            multiplier = 1048576;
        }
        else if ((i > 17) && (i < 22)) {
            multiplier = 1000000000L;
        }
        else if ((i > 21) && (i < 27)) {
            multiplier = 1073741824L;
        }
        else if ((i > 26) && (i < 31)) {
            multiplier = 1000000000000L;
        }
        else if ((i > 30) && (i < 36)) {
            multiplier = 1099511627776L;
        }
        if ((multiplier > 0) && (size > -1)) {
            sizelimit = size * multiplier;
        }
    }

    /**
     * This specifies when the file should be selected, whether it be
     * when the file matches a particular size, when it is smaller,
     * or whether it is larger.
     *
     * @param cmp The comparison to perform, an EnumeratedAttribute
     */
    public void setWhen(SizeComparisons cmp) {
        this.cmp = cmp.getIndex();
    }

    /**
     * When using this as a custom selector, this method will be called.
     * It translates each parameter into the appropriate setXXX() call.
     *
     * @param parameters the complete set of parameters for this selector
     */
    public void setParameters(Parameter[] parameters) {
        super.setParameters(parameters);
        if (parameters != null) {
            for (int i = 0; i < parameters.length; i++) {
                String paramname = parameters[i].getName();
                if (SIZE_KEY.equalsIgnoreCase(paramname)) {
                    try {
                        setValue(new Long(parameters[i].getValue()
                                ).longValue());
                    } catch (NumberFormatException nfe) {
                        setError("Invalid size setting "
                            + parameters[i].getValue());
                    }
                }
                else if (UNITS_KEY.equalsIgnoreCase(paramname)) {
                    ByteUnits units = new ByteUnits();
                    units.setValue(parameters[i].getValue());
                    setUnits(units);
                }
                else if (WHEN_KEY.equalsIgnoreCase(paramname)) {
                    SizeComparisons cmp = new SizeComparisons();
                    cmp.setValue(parameters[i].getValue());
                    setWhen(cmp);
                }
                else {
                    setError("Invalid parameter " + paramname);
                }
            }
        }
    }

    /**
     * <p>Checks to make sure all settings are kosher. In this case, it
     * means that the size attribute has been set (to a positive value),
     * that the multiplier has a valid setting, and that the size limit
     * is valid. Since the latter is a calculated value, this can only
     * fail due to a programming error.
     * </p>
     * <p>If a problem is detected, the setError() method is called.
     * </p>
     */
    public void verifySettings() {
        if (size < 0) {
            setError("The value attribute is required, and must be positive");
        }
        else if (multiplier < 1) {
            setError("Invalid Units supplied, must be K,Ki,M,Mi,G,Gi,T,or Ti");
        }
        else if (sizelimit < 0) {
            setError("Internal error: Code is not setting sizelimit correctly");
        }
    }

    /**
     * The heart of the matter. This is where the selector gets to decide
     * on the inclusion of a file in a particular fileset.
     *
     * @param basedir A java.io.File object for the base directory
     * @param filename The name of the file to check
     * @param file A File object for this filename
     * @return whether the file should be selected or not
     */
    public boolean isSelected(File basedir, String filename, File file) {

        // throw BuildException on error
        validate();

        // Directory size never selected for
        if (file.isDirectory()) {
            return true;
        }
        if (cmp == 0) {
            return (file.length() < sizelimit);
        }
        else if (cmp == 1) {
            return (file.length() > sizelimit);
        }
        else {
            return (file.length() == sizelimit);
        }
    }



    /**
     * Enumerated attribute with the values for units.
     * <p>
     * This treats the standard SI units as representing powers of ten,
     * as they should. If you want the powers of 2 that approximate
     * the SI units, use the first two characters followed by a
     * <code>bi</code>. So 1024 (2^10) becomes <code>kibi</code>,
     * 1048576 (2^20) becomes <code>mebi</code>, 1073741824 (2^30)
     * becomes <code>gibi</code>, and so on. The symbols are also
     * accepted, and these are the first letter capitalized followed
     * by an <code>i</code>. <code>Ki</code>, <code>Mi</code>,
     * <code>Gi</code>, and so on. Capitalization variations on these
     * are also accepted.
     * <p>
     * This binary prefix system is approved by the IEC and appears on
     * its way for approval by other agencies, but it is not an SI
     * standard. It disambiguates things for us, though.
     */
    public static class ByteUnits extends EnumeratedAttribute {
        public String[] getValues() {
            return new String[] {"K", "k", "kilo", "KILO",
                                 "Ki", "KI", "ki", "kibi", "KIBI",
                                 "M", "m", "mega", "MEGA",
                                 "Mi", "MI", "mi", "mebi", "MEBI",
                                 "G", "g", "giga", "GIGA",
                                 "Gi", "GI", "gi", "gibi", "GIBI",
                                 "T", "t", "tera", "TERA",
            /* You wish! */      "Ti", "TI", "ti", "tebi", "TEBI"
                                 };
        }
    }

    /**
     * Enumerated attribute with the values for size comparison.
     */
    public static class SizeComparisons extends EnumeratedAttribute {
        public String[] getValues() {
            return new String[] {"less", "more", "equal"};
        }
    }

}

