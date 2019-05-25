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
package org.apache.tools.ant.types;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Stream;

import org.apache.tools.ant.BuildException;

/**
 * EnumeratedAttribute for quantifier comparisons. Evaluates a
 * <code>boolean[]</code> or raw <code>true</code> and <code>false</code>
 * counts. Accepts the following values:<dl>
 * <dt>"all"</dt><dd>none <code>false</code></dd>
 * <dt>"each"</dt><dd>none <code>false</code></dd>
 * <dt>"every"</dt><dd>none <code>false</code></dd>
 * <dt>"any"</dt><dd>at least one <code>true</code></dd>
 * <dt>"some"</dt><dd>at least one <code>true</code></dd>
 * <dt>"one"</dt><dd>exactly one <code>true</code></dd>
 * <dt>"majority"</dt><dd>more <code>true</code> than <code>false</code></dd>
 * <dt>"most"</dt><dd>more <code>true</code> than <code>false</code></dd>
 * <dt>"none"</dt><dd>none <code>true</code></dd>
 * </dl>
 * @since Ant 1.7
 */
public class Quantifier extends EnumeratedAttribute {
    private static final String[] VALUES =
            Stream.of(Predicate.values()).map(Predicate::getNames)
                .flatMap(Collection::stream).toArray(String[]::new);

    /** ALL instance */
    public static final Quantifier ALL = new Quantifier(Predicate.ALL);

    /** ANY instance */
    public static final Quantifier ANY = new Quantifier(Predicate.ANY);

    /** ONE instance */
    public static final Quantifier ONE = new Quantifier(Predicate.ONE);

    /** MAJORITY instance */
    public static final Quantifier MAJORITY =
        new Quantifier(Predicate.MAJORITY);

    /** NONE instance */
    public static final Quantifier NONE = new Quantifier(Predicate.NONE);

    private enum Predicate {
        ALL("all", "each", "every") {
            @Override
            boolean eval(int t, int f) {
                return f == 0;
            }
        },

        ANY("any", "some") {
            @Override
            boolean eval(int t, int f) {
                return t > 0;
            }
        },

        ONE("one") {
            @Override
            boolean eval(int t, int f) {
                return t == 1;
            }
        },

        MAJORITY("majority", "most") {
            @Override
            boolean eval(int t, int f) {
                return t > f;
            }
        },

        NONE("none") {
            @Override
            boolean eval(int t, int f) {
                return t == 0;
            }
        };

        static Predicate get(String name) {
            return Stream.of(values()).filter(p -> p.names.contains(name))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(name));
        }

        final Set<String> names;

        Predicate(String primaryName, String... additionalNames) {
            Set<String> names = new LinkedHashSet<>();
            names.add(primaryName);
            Collections.addAll(names, additionalNames);
            this.names = Collections.unmodifiableSet(names);
        }

        Set<String> getNames() {
            return names;
        }

        abstract boolean eval(int t, int f);
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

    private Quantifier(Predicate impl) {
        setValue(impl.getNames().iterator().next());
    }

    /**
     * Return the possible values.
     * @return String[] of EnumeratedAttribute values.
     */
    @Override
    public String[] getValues() {
        return VALUES;
    }

    /**
     * Evaluate a <code>boolean</code> array.
     * @param b the <code>boolean[]</code> to evaluate.
     * @return true if the argument fell within the parameters of this Quantifier.
     */
    public boolean evaluate(boolean[] b) {
        int t = 0;
        for (boolean bn : b) {
            if (bn) {
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
        return Predicate.get(VALUES[index]).eval(t, f);
    }

}

