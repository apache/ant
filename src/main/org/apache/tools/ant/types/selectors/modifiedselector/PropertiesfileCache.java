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

package org.apache.tools.ant.types.selectors.modifiedselector;


import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.nio.file.Files;
import java.util.Iterator;
import java.util.Properties;


/**
 * Use java.util.Properties for storing the values.
 * The use of this Cache-implementation requires the use of the parameter
 * &lt;param name="cache.cachefile" .../&gt; for defining, where to store the
 * properties file.
 *
 * The ModifiedSelector sets the <i>cachefile</i> to the default value
 * <i>cache.properties</i>.
 *
 * Supported &lt;param&gt;s are:
 * <table>
 * <caption>Cache parameters</caption>
 * <tr>
 *   <th>name</th><th>values</th><th>description</th><th>required</th>
 * </tr>
 * <tr>
 *   <td>cache.cachefile</td>
 *   <td><i>path to file</i></td>
 *   <td>the name of the properties file</td>
 *   <td>yes</td>
 * </tr>
 * </table>
 *
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


    /**
     * Setter.
     * @param file new value
     */
    public void setCachefile(File file) {
        cachefile = file;
    }


    /**
     * Getter.
     * @return the cachefile
     */
    public File getCachefile() {
        return cachefile;
    }

    /**
     * This cache is valid if the cachefile is set.
     * @return true if all is ok false otherwise
     */
    @Override
    public boolean isValid() {
        return (cachefile != null);
    }


    // -----  Data Access


    /**
     * Load the cache from underlying properties file.
     */
    @Override
    public void load() {
        if (cachefile != null && cachefile.isFile() && cachefile.canRead()) {
            try (BufferedInputStream bis = new BufferedInputStream(
                Files.newInputStream(cachefile.toPath()))) {
                cache.load(bis);
            } catch (Exception e) {
                e.printStackTrace(); //NOSONAR
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
    @Override
    public void save() {
        if (!cacheDirty) {
            return;
        }
        if (cachefile != null && cache.propertyNames().hasMoreElements()) {
            try (BufferedOutputStream bos = new BufferedOutputStream(
                Files.newOutputStream(cachefile.toPath()))) {
                cache.store(bos, null);
                bos.flush();
            } catch (Exception e) {
                e.printStackTrace(); //NOSONAR
            }
        }
        cacheDirty = false;
    }

    /** Deletes the cache and its underlying file. */
    @Override
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
    @Override
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
    @Override
    public void put(Object key, Object value) {
        cache.put(String.valueOf(key), String.valueOf(value));
        cacheDirty = true;
    }

    /**
     * Returns an iterator over the keys in the cache.
     * @return An iterator over the keys.
     */
    @Override
    public Iterator<String> iterator() {
        return cache.stringPropertyNames().iterator();
    }


    // -----  additional  -----


    /**
     * Override Object.toString().
     * @return information about this cache
     */
    @Override
    public String toString() {
        return String.format("<PropertiesfileCache:cachefile=%s;noOfEntries=%d>",
                cachefile, cache.size());
    }
}
