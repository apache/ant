/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package org.apache.tools.ant.types;

import org.apache.tools.ant.BuildException;

/**
 * EnumeratedAttribute for quantifier comparisons. Evaluates a
 * <code>boolean[]</code> or raw <code>true</code> and <code>false</code>
 * counts. Accepts the following values:<ul>
 * <li>"all"</li> - none <code>false</code>
 * <li>"each"</li> - none <code>false</code>
 * <li>"every"</li> - none <code>false</code>
 * <li>"any"</li> - at least one <code>true</code>
 * <li>"some"</li> - at least one <code>true</code>
 * <li>"one"</li> - exactly one <code>true</code>
 * <li>"majority"</li> - more <code>true</code> than <code>false</code>
 * <li>"most"</li> - more <code>true</code> than <code>false</code>
 * <li>"none"</li> - none <code>true</code>
 * </ul>
 * @since Ant 1.7
 */
public class Quantifier extends EnumeratedAttribute {
    private static final String[] VALUES
        = new String[] {"all", "each", "every", "any", "some", "one",
                        "majority", "most", "none"};

    /** ALL instance */
    public static final Quantifier ALL = new Quantifier("all");
    /** ANY instance */
    public static final Quantifier ANY = new Quantifier("any");
    /** ONE instance */
    public static final Quantifier ONE = new Quantifier("one");
    /** MAJORITY instance */
    public static final Quantifier MAJORITY = new Quantifier("majority");
    /** NONE instance */
    public static final Quantifier NONE = new Quantifier("none");

    private abstract static class Predicate {
        abstract boolean eval(int t, int f);
    }

    private static final Predicate ALL_PRED = new Predicate() {
        boolean eval(int t, int f) { return f == 0; }
    };

    private static final Predicate ANY_PRED = new Predicate() {
        boolean eval(int t, int f) { return t > 0; }
    };

    private static final Predicate ONE_PRED = new Predicate() {
        boolean eval(int t, int f) { return t == 1; }
    };

    private static final Predicate MAJORITY_PRED = new Predicate() {
        boolean eval(int t, int f) { return t > f; }
    };

    private static final Predicate NONE_PRED = new Predicate() {
        boolean eval(int t, int f) { return t == 0; }
    };

    private static final Predicate[] PREDS = new Predicate[VALUES.length];

    static {
        // CheckStyle:MagicNumber OFF
        PREDS[0] = ALL_PRED;
        PREDS[1] = ALL_PRED;
        PREDS[2] = ALL_PRED;
        PREDS[3] = ANY_PRED;
        PREDS[4] = ANY_PRED;
        PREDS[5] = ONE_PRED;
        PREDS[6] = MAJORITY_PRED;
        PREDS[7] = MAJORITY_PRED;
        PREDS[8] = NONE_PRED;
        // CheckStyle:MagicNumber ON
    }

    /**
     * Default constructor.
     */
    public Quantifier() {
    }

    /**
     * Construct a new Quantifier with the specified value.
     * @param value the EnumeratedAttribute value.
     */
    public Quantifier(String value) {
        setValue(value);
    }

    /**
     * Return the possible values.
     * @return String[] of EnumeratedAttribute values.
     */
    public String[] getValues() {
        return VALUES;
    }

    /**
     * Evaluate a <code>boolean<code> array.
     * @param b the <code>boolean[]</code> to evaluate.
     * @return true if the argument fell within the parameters of this Quantifier.
     */
    public boolean evaluate(boolean[] b) {
        int t = 0;
        for (int i = 0; i < b.length; i++) {
            if (b[i]) {
                t++;
            }
        }
        return evaluate(t, b.length - t);
    }

    /**
     * Evaluate integer <code>true</code> vs. <code>false</code> counts.
     * @param t the number of <code>true</code> values.
     * @param f the number of <code>false</code> values.
     * @return true if the arguments fell within the parameters of this Quantifier.
     */
    public boolean evaluate(int t, int f) {
        int index = getIndex();
        if (index == -1) {
            throw new BuildException("Quantifier value not set.");
        }
        return PREDS[index].eval(t, f);
    }

}

