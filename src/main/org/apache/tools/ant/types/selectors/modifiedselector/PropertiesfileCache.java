/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2003 The Apache Software Foundation.  All rights
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

package org.apache.tools.ant.types.selectors.modifiedselector;


import java.util.Iterator;
import java.util.Vector;
import java.util.Enumeration;
import java.util.Properties;
import java.io.File;
import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.BufferedOutputStream;
import java.io.FileOutputStream;


/**
 * Use java.util.Properties for storing the values.
 * The use of this Cache-implementation requires the use of the parameter
 * <param name="cache.cachefile" .../> for defining, where to store the
 * properties file.
 *
 * The ModifiedSelector sets the <i>cachefile</i> to the default value
 * <i>cache.properties</i>.
 *
 * Supported <param>s are:
 * <table>
 * <tr>
 *   <th>name</th><th>values</th><th>description</th><th>required</th>
 * </tr>
 * <tr>
 *   <td> cache.cachefile </td>
 *   <td> <i>path to file</i> </td>
 *   <td> the name of the properties file </td>
 *   <td> yes </td>
 * </tr>
 * </table>
 *
 * @author Jan Mat\u00e8rne
 * @version 2003-09-13
 * @since  Ant 1.6
 */
public class PropertiesfileCache implements Cache {


    // -----  member variables - configuration  -----


    /** Where to store the properties? */
    private File cachefile = null;

    /** Object for storing the key-value-pairs. */
    private Properties cache = new Properties();


    // -----  member variables - internal use  -----


    /** Is the cache already loaded? Prevents from multiple load operations. */
    private boolean cacheLoaded = false;

    /** Must the cache be saved? Prevents from multiple save operations. */
    private boolean cacheDirty  = true;


    // -----  Constructors  -----


    /** Bean-Constructor. */
    public PropertiesfileCache() {
    }

    /**
     * Constructor.
     * @param cachefile set the cachefile
     */
    public PropertiesfileCache(File cachefile) {
        this.cachefile = cachefile;
    }


    // -----  Cache-Configuration  -----


    public void setCachefile(File file) {
        cachefile = file;
    }

    public File getCachefile() { return cachefile; }

    public boolean isValid() {
        return (cachefile != null);
    }


    // -----  Data Access


    public void load() {
        if ((cachefile != null) && cachefile.isFile() && cachefile.canRead()) {
            try {
                BufferedInputStream bis = new BufferedInputStream(
                    new FileInputStream(cachefile));
                cache.load(bis);
                bis.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        // after loading the cache is up to date with the file
        cacheLoaded = true;
        cacheDirty  = false;
    }

    /**
     * Saves modification of the cache.
     * Cache is only saved if there is one ore more entries.
     * Because entries can not be deleted by this API, this Cache
     * implementation checks the existence of entries before creating the file
     * for performance optimisation.
     */
    public void save() {
        if (!cacheDirty) {
            return;
        }
        if ((cachefile != null) && cache.propertyNames().hasMoreElements()) {
            try {
                BufferedOutputStream bos = new BufferedOutputStream(
                      new FileOutputStream(cachefile));
                cache.store(bos, null);
                bos.flush();
                bos.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        cacheDirty = false;
    }

    /** Deletes the cache and its underlying file. */
    public void delete() {
        cache = new Properties();
        cachefile.delete();
        cacheLoaded = true;
        cacheDirty = false;
    }

    /**
     * Returns a value for a given key from the cache.
     * @param key the key
     * @return the stored value
     */
    public Object get(Object key) {
        if (!cacheLoaded) {
            load();
        }
        try {
            return cache.getProperty(String.valueOf(key));
        } catch (ClassCastException e) {
            return null;
        }
    }

    /**
     * Saves a key-value-pair in the cache.
     * @param key the key
     * @param value the value
     */
    public void put(Object key, Object value) {
        cache.put(String.valueOf(key), String.valueOf(value));
        cacheDirty = true;
    }

    /**
     * Returns an iterator over the keys in the cache.
     * @return An iterator over the keys.
     */
    public Iterator iterator() {
        Vector v = new java.util.Vector();
        Enumeration en = cache.propertyNames();
        while (en.hasMoreElements()) {
            v.add(en.nextElement());
        }
        return v.iterator();
    }


    // -----  additional  -----


    /**
     * Override Object.toString().
     * @return information about this cache
     */
    public String toString() {
        StringBuffer buf = new StringBuffer();
        buf.append("<PropertiesfileCache:");
        buf.append("cachefile=").append(cachefile);
        buf.append(";noOfEntries=").append(cache.size());
        buf.append(">");
        return buf.toString();
    }
}
