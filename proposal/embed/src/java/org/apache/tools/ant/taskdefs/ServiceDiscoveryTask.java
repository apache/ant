/*
 * ====================================================================
 *
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 1999-2002 The Apache Software Foundation.  All rights
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
 * 4. The names "The Jakarta Project", "Commons", and "Apache Software
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
 *
 */

package org.apache.tools.ant.taskdefs;

import java.util.Vector;

import org.apache.commons.discovery.ResourceName;
import org.apache.commons.discovery.ResourceNameIterator;
import org.apache.commons.discovery.jdk.JDKHooks;
import org.apache.commons.discovery.resource.DiscoverResources;


/**
 * Small ant task that will use discovery to locate a particular impl.
 * and display all values.
 *
 * You can execute this and save it with an id, then other classes can use it.
 *
 * @author Costin Manolache
 */
public class ServiceDiscoveryTask
{
    String name;
    int debug=0;
    ResourceName[] drivers = null;
        
    public void setServiceName(String name ) {
        this.name=name;
    }

    public void setDebug(int i) {
        this.debug=debug;
    }

    public ResourceName[] getServiceInfo() {
        return drivers;
    }

    public void execute() throws Exception {
        System.out.println("XXX ");
        
        DiscoverResources disc = new DiscoverResources();
        disc.addClassLoader( JDKHooks.getJDKHooks().getThreadContextClassLoader() );
        disc.addClassLoader( this.getClass().getClassLoader() );
        
        ResourceNameIterator enum = disc.findResources(name);

        Vector vector = new Vector();
        while (enum.hasNext()) {
            ResourceName resourceInfo = enum.nextResourceName();
            vector.add(resourceInfo);
            if( debug > 0 ) {
                System.out.println("Found " + resourceInfo);
            }
        }
        
        drivers = new ResourceName[vector.size()];
        vector.copyInto(drivers);
    }
        
}
