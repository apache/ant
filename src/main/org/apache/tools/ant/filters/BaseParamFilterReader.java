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
package org.apache.tools.ant.filters;

import java.io.Reader;

import org.apache.tools.ant.types.Parameter;
import org.apache.tools.ant.types.Parameterizable;

/**
 * Parameterized base class for core filter readers.
 *
 */
public abstract class BaseParamFilterReader
    extends BaseFilterReader
    implements Parameterizable {
    /** The passed in parameter array. */
    private Parameter[] parameters;

    /**
     * Constructor for "dummy" instances.
     *
     * @see BaseFilterReader#BaseFilterReader()
     */
    public BaseParamFilterReader() {
        super();
    }

    /**
     * Creates a new filtered reader.
     *
     * @param in A Reader object providing the underlying stream.
     *           Must not be <code>null</code>.
     */
    public BaseParamFilterReader(final Reader in) {
        super(in);
    }

    /**
     * Sets the parameters used by this filter, and sets
     * the filter to an uninitialized status.
     *
     * @param parameters The parameters to be used by this filter.
     *                   Should not be <code>null</code>.
     */
    public final void setParameters(final Parameter... parameters) {
        this.parameters = parameters;
        setInitialized(false);
    }

    /**
     * Returns the parameters to be used by this filter.
     *
     * @return the parameters to be used by this filter
     */
    protected final Parameter[] getParameters() {
        return parameters;
    }
}
