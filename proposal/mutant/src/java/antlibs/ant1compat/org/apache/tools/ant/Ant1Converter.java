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
package org.apache.tools.ant;

import org.apache.ant.common.antlib.AbstractConverter;
import org.apache.ant.common.antlib.ConverterException;
import org.apache.tools.ant.types.EnumeratedAttribute;
import org.apache.tools.ant.types.Path;

/**
 * A converter to convert to the types supported by the Ant1 Ant library
 *
 * @author Conor MacNeill
 * @created 1 February 2002
 */
public class Ant1Converter extends AbstractConverter {

    /** The project instance for this converter */
    private Project project;

    /**
     * Constructor for the Ant1Converter object
     *
     * @param project the project for this converter. It is used in the
     *      conversion of some of the supported types.
     */
    public Ant1Converter(Project project) {
        this.project = project;
    }

    /**
     * Get the list of classes this converter is able to convert to.
     *
     * @return an array of Class objects representing the classes this
     *      converter handles.
     */
    public Class[] getTypes() {
        return new Class[]{Path.class, EnumeratedAttribute.class};
    }

    /**
     * Convert a string from the value given to an instance of the given
     * type.
     *
     * @param value The value to be converted
     * @param type the desired type of the converted object
     * @return the value of the converted object
     * @exception ConverterException if the conversion cannot be made
     */
    public Object convert(String value, Class type) throws ConverterException {
        if (type.equals(Path.class)) {
            return new Path(project, value);
        } else if (EnumeratedAttribute.class.isAssignableFrom(type)) {
            try {
                EnumeratedAttribute ea
                     = (EnumeratedAttribute) type.newInstance();
                ea.setValue(value);
                return ea;
            } catch (InstantiationException e) {
                throw new ConverterException(e);
            } catch (IllegalAccessException e) {
                throw new ConverterException(e);
            }

        } else {
            throw new ConverterException("This converter does not handle "
                 + type.getName());
        }
    }

    /**
     * This method allows a converter to indicate whether it can create the
     * given type which is a sub-type of one of the converter's main types
     * indicated in getTypes. Most converters can return false here.
     *
     * @param subType the sub-type
     * @return true if this converter can convert a string representation to
     *      the given subclass of one of its main class
     */
    public boolean canConvertSubType(Class subType) {
        return EnumeratedAttribute.class.isAssignableFrom(subType);
    }

}

