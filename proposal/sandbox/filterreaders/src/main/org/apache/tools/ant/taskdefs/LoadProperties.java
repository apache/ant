/*
 *  The Apache Software License, Version 1.1
 *
 *  Copyright (c) 2002 The Apache Software Foundation.  All rights
 *  reserved.
 *
 *  Redistribution and use in source and binary forms, with or without
 *  modification, are permitted provided that the following conditions
 *  are met:
 *
 *  1. Redistributions of source code must retain the above copyright
 *  notice, this list of conditions and the following disclaimer.
 *
 *  2. Redistributions in binary form must reproduce the above copyright
 *  notice, this list of conditions and the following disclaimer in
 *  the documentation and/or other materials provided with the
 *  distribution.
 *
 *  3. The end-user documentation included with the redistribution, if
 *  any, must include the following acknowlegement:
 *  "This product includes software developed by the
 *  Apache Software Foundation (http://www.apache.org/)."
 *  Alternately, this acknowlegement may appear in the software itself,
 *  if and wherever such third-party acknowlegements normally appear.
 *
 *  4. The names "The Jakarta Project", "Ant", and "Apache Software
 *  Foundation" must not be used to endorse or promote products derived
 *  from this software without prior written permission. For written
 *  permission, please contact apache@apache.org.
 *
 *  5. Products derived from this software may not be called "Apache"
 *  nor may "Apache" appear in their names without prior written
 *  permission of the Apache Group.
 *
 *  THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 *  WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 *  OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 *  DISCLAIMED.  IN NO EVENT SHALL THE APACHE SOFTWARE FOUNDATION OR
 *  ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 *  SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 *  LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 *  USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 *  ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 *  OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 *  OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 *  SUCH DAMAGE.
 *  ====================================================================
 *
 *  This software consists of voluntary contributions made by many
 *  individuals on behalf of the Apache Software Foundation.  For more
 *  information on the Apache Software Foundation, please see
 *  <http://www.apache.org/>.
 */
package org.apache.tools.ant.taskdefs;

import org.apache.tools.ant.Task;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.types.AntFilterReader;
import org.apache.tools.ant.types.FilterReaderSet;
import org.apache.tools.ant.types.Parameter;
import org.apache.tools.ant.types.Parameterizable;

import java.io.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Vector;

/**
 * Load a file's contents as Ant Properties.
 *
 * @author <a href="mailto:umagesh@apache.org">Magesh Umasankar</a>
 * @created 20 February 2002
 */
public final class LoadProperties extends Task {

    /**
     * Source file
     */
    private File srcFile = null;

    /**
     * Holds filterReaderSets
     */
    private final Vector filterReaderSets = new Vector();

    /**
     * Sets the srcfile attribute.
     *
     * @param srcFile The new SrcFile value
     */
    public final void setSrcFile(final File srcFile) {
        this.srcFile = srcFile;
    }

    /**
     * read in a source file's contents and load them up as Ant properties
     *
     * @exception BuildException if something goes wrong with the build
     */
    public final void execute() throws BuildException {
        //validation
        if (srcFile == null) {
            throw new BuildException("Source file not defined.");
        }

        if (!srcFile.exists()) {
            throw new BuildException("Source file does not exist.");
        }

        if (!srcFile.isFile()) {
            throw new BuildException("Source file is not a file.");
        }

        FileInputStream fis = null;
        BufferedInputStream bis = null;
        Reader instream = null;

        try {
            final long len = srcFile.length();
            final int size=(int) len;

            //open up the file
            fis = new FileInputStream(srcFile);
            bis = new BufferedInputStream(fis);
            instream = new InputStreamReader(bis);

            String text = processStream(instream, size);
            if (!text.endsWith("\n")) {
                text = text + "\n";
            }

            int index = 0;

            if (text != null) {
                while (index != -1) {
                    int oldIndex = index;
                    index = text.indexOf("\n", oldIndex);
                    if (index != -1) {
                        String line = text.substring(oldIndex, index);
                        if (line.endsWith("\r")) {
                            line = line.substring(0, line.length() - 1);
                        }
                        int equalIndex = line.indexOf("=");
                        int spaceIndex = line.indexOf(" ");
                        int sepIndex = -1;

                        if (equalIndex != -1 || spaceIndex != -1) {
                            if (equalIndex == -1) {
                                sepIndex = spaceIndex;
                            } else if (spaceIndex == -1) {
                                sepIndex = equalIndex;
                            } else {
                                sepIndex = Math.min(spaceIndex, equalIndex);
                            }
                        }

                        if (sepIndex != -1) {
                            String key = line.substring(0, sepIndex);
                            String value = line.substring(sepIndex + 1);
                            if (value != null && value.trim().length() > 0) {
                                project.setNewProperty(key, value);
                            }
                        }

                        ++index;
                    }
                }
            }

        } catch (final IOException ioe) {
            final String message = "Unable to load file: " + ioe.toString();
            throw new BuildException(message, ioe, location);
        } catch (final BuildException be) {
            throw be;
        } finally {
            try {
                if (fis != null) {
                    fis.close();
                }
            } catch (IOException ioex) {
                //ignore
            }
        }
    }

    /**
     * Process the input by passing it through the reader chain.
     */
    private final String processStream(final Reader inputReader, final int size)
        throws BuildException, IOException {

        Reader instream = inputReader;
        final char[] buffer = new char[size];
        final int filterReadersCount = filterReaderSets.size();
        final Vector finalFilters = new Vector();

        for (int i = 0; i < filterReadersCount; i++) {
            final FilterReaderSet filterset =
                (FilterReaderSet) filterReaderSets.elementAt(i);
            final Vector filterReaders = filterset.getFilterReaders();
            final int readerCount = filterReaders.size();
            for (int j = 0; j < readerCount; j++) {
                final AntFilterReader afr =
                    (AntFilterReader) filterReaders.elementAt(j);
                finalFilters.addElement(afr);
            }
        }

        final int filtersCount = finalFilters.size();

        if (filtersCount > 0) {
            for (int i = 0; i < filtersCount; i++) {
                final AntFilterReader filter =
                    (AntFilterReader) finalFilters.elementAt(i);
                final String className = filter.getClassName();
                if (className != null) {
                    try {
                        final Class clazz = Class.forName(className);
                        if (clazz != null) {
                            final Constructor[] constructors =
                                clazz.getConstructors();
                            final Reader[] rdr = {instream};
                            instream =
                                (Reader) constructors[0].newInstance(rdr);
                            if (Parameterizable.class.isAssignableFrom(clazz)) {
                                final Parameter[] params = filter.getParams();
                                ((Parameterizable)
                                    instream).setParameters(params);
                            }
                        }
                    } catch (final ClassNotFoundException cnfe) {
                        throw new BuildException(cnfe, location);
                    } catch (final InstantiationException ie) {
                        throw new BuildException(ie, location);
                    } catch (final IllegalAccessException iae) {
                        throw new BuildException(iae, location);
                    } catch (final InvocationTargetException ite) {
                        throw new BuildException(ite, location);
                    }
                }
            }
        }

        int bufferLength = 0;
        String text = null;
        while (bufferLength != -1) {
            bufferLength = instream.read(buffer);
            if (bufferLength != -1) {
                if (text == null) {
                    text = new String(buffer, 0, bufferLength);
                } else {
                    text += new String(buffer, 0, bufferLength);
                }
            }
        }
        return text;
    }

    /**
     * Add the FilterReaderSet element.
     */
    public final void addFilterReaderSet(FilterReaderSet filter) {
        filterReaderSets.addElement(filter);
    }

//end class
}
