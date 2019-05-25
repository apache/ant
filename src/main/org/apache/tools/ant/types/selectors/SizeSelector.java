/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package org.apache.tools.ant.types.selectors;

import java.io.File;

import org.apache.tools.ant.types.Comparison;
import org.apache.tools.ant.types.EnumeratedAttribute;
import org.apache.tools.ant.types.Parameter;

/**
 * Selector that filters files based on their size.
 *
 * @since 1.5
 */
public class SizeSelector extends BaseExtendSelector {

    /** Constants for kilo, kibi etc */
    private static final int  KILO = 1000;
    private static final int  KIBI = 1024;
    private static final int  KIBI_POS = 4;
    private static final int  MEGA = 1000000;
    private static final int  MEGA_POS = 9;
    private static final int  MEBI = 1048576;
    private static final int  MEBI_POS = 13;
    private static final long GIGA = 1000000000L;
    private static final int  GIGA_POS = 18;
    private static final long GIBI = 1073741824L;
    private static final int  GIBI_POS = 22;
    private static final long TERA = 1000000000000L;
    private static final int  TERA_POS = 27;
    private static final long TEBI = 1099511627776L;
    private static final int  TEBI_POS = 31;
    private static final int  END_POS = 36;

    /** Used for parameterized custom selector */
    public static final String SIZE_KEY = "value";
    /** Used for parameterized custom selector */
    public static final String UNITS_KEY = "units";
    /** Used for parameterized custom selector */
    public static final String WHEN_KEY = "when";

    private long size = -1;
    private long multiplier = 1;
    private long sizelimit = -1;
    private Comparison when = Comparison.EQUAL;

    /**
     * Returns a <code>String</code> object representing the specified
     * SizeSelector. This is "{sizeselector value: " + &lt;"compare",
     * "less", "more", "equal"&gt; + "}".
     * @return a string describing this object
     */
    public String toString() {
        return String.format("{sizeselector value: %d compare: %s}",
                sizelimit, when.getValue());
    }

    /**
     * A size selector needs to know what size to base its selecting on.
     * This will be further modified by the multiplier to get an
     * actual size limit.
     *
     * @param size the size to select against expressed in units.
     */
    public void setValue(long size) {
        this.size = size;
        if (multiplier != 0 && size > -1) {
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
     *        EnumeratedAttribute.
     */
    public void setUnits(ByteUnits units) {
        int i = units.getIndex();
        multiplier = 0;
        if (i > -1 && i < KIBI_POS) {
            multiplier = KILO;
        } else if (i < MEGA_POS) {
            multiplier = KIBI;
        } else if (i < MEBI_POS) {
            multiplier = MEGA;
        } else if (i < GIGA_POS) {
            multiplier = MEBI;
        } else if (i < GIBI_POS) {
            multiplier = GIGA;
        } else if (i < TERA_POS) {
            multiplier = GIBI;
        } else if (i < TEBI_POS) {
            multiplier = TERA;
        } else if (i < END_POS) {
            multiplier = TEBI;
        }
        if (multiplier > 0 && size > -1) {
            sizelimit = size * multiplier;
        }
    }

    /**
     * This specifies when the file should be selected, whether it be
     * when the file matches a particular size, when it is smaller,
     * or whether it is larger.
     *
     * @param when The comparison to perform, an EnumeratedAttribute.
     */
    public void setWhen(SizeComparisons when) {
        this.when = when;
    }

    /**
     * When using this as a custom selector, this method will be called.
     * It translates each parameter into the appropriate setXXX() call.
     *
     * @param parameters the complete set of parameters for this selector.
     */
    @Override
    public void setParameters(Parameter... parameters) {
        super.setParameters(parameters);
        if (parameters != null) {
            for (Parameter parameter : parameters) {
                String paramname = parameter.getName();
                if (SIZE_KEY.equalsIgnoreCase(paramname)) {
                    try {
                        setValue(Long.parseLong(parameter.getValue()));
                    } catch (NumberFormatException nfe) {
                        setError("Invalid size setting "
                                + parameter.getValue());
                    }
                } else if (UNITS_KEY.equalsIgnoreCase(paramname)) {
                    ByteUnits units = new ByteUnits();
                    units.setValue(parameter.getValue());
                    setUnits(units);
                } else if (WHEN_KEY.equalsIgnoreCase(paramname)) {
                    SizeComparisons scmp = new SizeComparisons();
                    scmp.setValue(parameter.getValue());
                    setWhen(scmp);
                } else {
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
        } else if (multiplier < 1) {
            setError("Invalid Units supplied, must be K,Ki,M,Mi,G,Gi,T,or Ti");
        } else if (sizelimit < 0) {
            setError("Internal error: Code is not setting sizelimit correctly");
        }
    }

    /**
     * The heart of the matter. This is where the selector gets to decide
     * on the inclusion of a file in a particular fileset.
     *
     * @param basedir A java.io.File object for the base directory.
     * @param filename The name of the file to check.
     * @param file A File object for this filename.
     * @return whether the file should be selected or not.
     */
    @Override
    public boolean isSelected(File basedir, String filename, File file) {

        // throw BuildException on error
        validate();

        // Directory size never selected for
        if (file.isDirectory()) {
            return true;
        }
        long diff = file.length() - sizelimit;
        return when.evaluate(diff == 0 ? 0 : (int) (diff / Math.abs(diff)));
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
        /**
         * @return the values as an array of strings
         */
        public String[] getValues() {
            return new String[]{"K", "k", "kilo", "KILO",
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
    public static class SizeComparisons extends Comparison {
    }

}

