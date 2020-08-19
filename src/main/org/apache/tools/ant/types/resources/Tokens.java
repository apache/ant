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
package org.apache.tools.ant.types.resources;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Stack;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.types.CharSet;
import org.apache.tools.ant.types.DataType;
import org.apache.tools.ant.types.Resource;
import org.apache.tools.ant.types.ResourceCollection;
import org.apache.tools.ant.util.ConcatResourceInputStream;
import org.apache.tools.ant.util.LineTokenizer;
import org.apache.tools.ant.util.Tokenizer;

/**
 * ResourceCollection consisting of StringResources gathered from tokenizing
 * another ResourceCollection with a Tokenizer implementation.
 * @since Ant 1.7
 */
public class Tokens extends BaseResourceCollectionWrapper {

    private Tokenizer tokenizer;
    private CharSet charSet = CharSet.getDefault();

    /**
     * Sort the contained elements.
     * @return a Collection of Resources.
     */
    protected synchronized Collection<Resource> getCollection() {
        ResourceCollection rc = getResourceCollection();
        if (rc.isEmpty()) {
            return Collections.emptySet();
        }
        if (tokenizer == null) {
            tokenizer = new LineTokenizer();
        }
        try (ConcatResourceInputStream cat = new ConcatResourceInputStream(rc);
             InputStreamReader rdr = new InputStreamReader(cat, charSet.getCharset())) {
            cat.setManagingComponent(this);
            List<Resource> result = new ArrayList<>();
            for (String s = tokenizer.getToken(rdr); s != null; s =
                tokenizer.getToken(rdr)) {
                // do not send the Project to the constructor of StringResource, since
                // the semantics of that constructor clearly state that property value
                // replacement takes place on the passed string value. We don't want
                // that to happen.
                final StringResource resource = new StringResource(s);
                resource.setProject(getProject());
                result.add(resource);
            }
            return result;
        } catch (IOException e) {
            throw new BuildException("Error reading tokens", e);
        }
    }

    /**
     * Set the encoding used to create the tokens.
     * @param encoding the encoding to use.
     */
    public void setEncoding(String encoding) {
        setEncoding(new CharSet(encoding));
    }

    /**
     * Set the encoding used to create the tokens.
     * @param charSet the CharSet to use.
     */
    public synchronized void setEncoding(CharSet charSet) {
        this.charSet = charSet;
    }

    /**
     * Add the nested Tokenizer to this Tokens ResourceCollection.
     * A LineTokenizer will be used by default.
     * @param tokenizer the tokenizer to add.
     */
    public synchronized void add(Tokenizer tokenizer) {
        if (isReference()) {
            throw noChildrenAllowed();
        }
        if (this.tokenizer != null) {
            throw new BuildException("Only one nested tokenizer allowed.");
        }
        this.tokenizer = tokenizer;
        setChecked(false);
    }

    /**
     * Overrides the BaseResourceCollectionContainer version
     * to check the nested Tokenizer.
     * @param stk the stack of data types to use (recursively).
     * @param p   the project to use to dereference the references.
     * @throws BuildException on error.
     */
    @Override
    protected synchronized void dieOnCircularReference(Stack<Object> stk, Project p)
        throws BuildException {
        if (isChecked()) {
            return;
        }

        // check nested collection
        super.dieOnCircularReference(stk, p);

        if (!isReference()) {
            if (tokenizer instanceof DataType) {
                pushAndInvokeCircularReferenceCheck((DataType) tokenizer, stk,
                                                    p);
            }
            setChecked(true);
        }
    }

}
