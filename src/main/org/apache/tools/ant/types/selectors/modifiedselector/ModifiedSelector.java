/*
 * Copyright  2003-2004 The Apache Software Foundation
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
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

package org.apache.tools.ant.types.selectors.modifiedselector;


// Java
import java.util.Comparator;
import java.util.Vector;
import java.util.Iterator;
import java.io.File;

// Ant
import org.apache.tools.ant.Project;
import org.apache.tools.ant.IntrospectionHelper;
import org.apache.tools.ant.types.EnumeratedAttribute;
import org.apache.tools.ant.types.Parameter;
import org.apache.tools.ant.types.selectors.BaseExtendSelector;


/**
 * <p>Selector class that uses <i>Algorithm</i>, <i>Cache</i> and <i>Comparator</i>
 * for its work.
 * The <i>Algorithm</i> is used for computing a hashvalue for a file.
 * The <i>Comparator</i> decides whether to select or not.
 * The <i>Cache</i> stores the other value for comparison by the <i>Comparator</i>
 * in a persistent manner.</p>
 *
 * <p>The ModifiedSelector is implemented as a <b>CoreSelector</b> and uses default
 * values for all its attributes therefore the simpliest example is <pre>
 *   <copy todir="dest">
 *       <filelist dir="src">
 *           <modified/>
 *       </filelist>
 *   </copy>
 * </pre></p>
 *
 * <p>The same example rewritten as CoreSelector with setting the all values
 * (same as defaults are) would be <pre>
 *   <copy todir="dest">
 *       <filelist dir="src">
 *           <modified update="true"
 *                     cache="propertyfile"
 *                     algorithm="digest"
 *                     comparator="equal">
 *               <param name="cache.cachefile"     value="cache.properties"/>
 *               <param name="algorithm.algorithm" value="MD5"/>
 *           </modified>
 *       </filelist>
 *   </copy>
 * </pre></p>
 *
 * <p>And the same rewritten as CustomSelector would be<pre>
 *   <copy todir="dest">
 *       <filelist dir="src">
 *           <custom class="org.apache.tools.ant.type.selectors.ModifiedSelector">
 *               <param name="update"     value="true"/>
 *               <param name="cache"      value="propertyfile"/>
 *               <param name="algorithm"  value="digest"/>
 *               <param name="comparator" value="equal"/>
 *               <param name="cache.cachefile"     value="cache.properties"/>
 *               <param name="algorithm.algorithm" value="MD5"/>
 *           </custom>
 *       </filelist>
 *   </copy>
 * </pre></p>
 *
 * <p>All these three examples copy the files from <i>src</i> to <i>dest</i>
 * using the ModifiedSelector. The ModifiedSelector uses the <i>PropertyfileCache
 * </i>, the <i>DigestAlgorithm</i> and the <i>EqualComparator</i> for its
 * work. The PropertyfileCache stores key-value-pairs in a simple java
 * properties file. The filename is <i>cache.properties</i>. The <i>update</i>
 * flag lets the selector update the values in the cache (and on first call
 * creates the cache). The <i>DigestAlgorithm</i> computes a hashvalue using the
 * java.security.MessageDigest class with its MD5-Algorithm and its standard
 * provider. The new computed hashvalue and the stored one are compared by
 * the <i>EqualComparator</i> which returns 'true' (more correct a value not
 * equals zero (1)) if the values are not the same using simple String
 * comparison.</p>
 *
 * <p>A useful scenario for this selector is inside a build environment
 * for homepage generation (e.g. with <a href="http://xml.apache.org/forrest/">
 * Apache Forrest</a>). <pre>
 * <target name="generate-and-upload-site">
 *     <echo> generate the site using forrest </echo>
 *     <antcall target="site"/>
 *
 *     <echo> upload the changed files </echo>
 *     <ftp server="${ftp.server}" userid="${ftp.user}" password="${ftp.pwd}">
 *         <fileset dir="htdocs/manual">
 *             <modified/>
 *         </fileset>
 *     </ftp>
 * </target>
 * </pre> Here all <b>changed</b> files are uploaded to the server. The
 * ModifiedSelector saves therefore much upload time.</p>
 *
 * <p>This selector supports the following nested param's:
 * <table>
 * <tr><th>name</th><th>values</th><th>description</th><th>required</th></tr>
 * <tr>
 *     <td> cache </td>
 *     <td> propertyfile </td>
 *     <td> which cache implementation should be used <ul>
 *          <li><b>propertyfile</b> - using java.util.Properties </li>
 *     </td>
 *     <td> no, defaults to 'propertyfile' </td>
 * </tr>
 * <tr>
 *     <td> algorithm </td>
 *     <td> hashvalue | digest </td>
 *     <td> which algorithm implementation should be used
 *          <li><b>hashvalue</b> - loads the file content into a String and
 *                                 uses its hashValue() method </li>
 *          <li><b>digest</b> - uses java.security.MessageDigest class </i>
 *     </td>
 *     <td> no, defaults to digest </td>
 * </tr>
 * <tr>
 *     <td> comparator </td>
 *     <td> equal | role </td>
 *     <td> which comparator implementation should be used
 *          <li><b>equal</b> - simple comparison using String.equals() </li>
 *          <li><b>role</b> - uses java.text.RuleBasedCollator class </i>
 *     </td>
 *     <td> no, defaults to equal </td>
 * </tr>
 * <tr>
 *     <td> update </td>
 *     <td> true | false </td>
 *     <td> If set to <i>true</i>, the cache will be stored, otherwise the values
 *          will be lost. </td>
 *     <td> no, defaults to true </td>
 * </tr>
 * <tr>
 *     <td> seldirs </td>
 *     <td> true | false </td>
 *     <td> If set to <i>true</i>, directories will be selected otherwise not </td>
 *     <td> no, defaults to true </td>
 * </tr>
 * <tr>
 *     <td> cache.* </td>
 *     <td> depends on used cache </td>
 *     <td> value is stored and given to the Cache-Object for initialisation </td>
 *     <td> depends on used cache </td>
 * </tr>
 * <tr>
 *     <td> algorithm.* </td>
 *     <td> depends on used algorithm </td>
 *     <td> value is stored and given to the Algorithm-Object for initialisation </td>
 *     <td> depends on used algorithm </td>
 * </tr>
 * <tr>
 *     <td> comparator.* </td>
 *     <td> depends on used comparator </td>
 *     <td> value is stored and given to the Comparator-Object for initialisation </td>
 *     <td> depends on used comparator </td>
 * </tr>
 * </table>
 * If another name is used a BuildException "Invalid parameter" is thrown. </p>
 *
 * <p>This selector uses reflection for setting the values of its three interfaces
 * (using org.apache.tools.ant.IntrospectionHelper) therefore no special
 * 'configuration interfaces' has to be implemented by new caches, algorithms or
 * comparators. All present <i>set</i>XX methods can be used. E.g. the DigestAlgorithm
 * can use a specified provider for computing its value. For selecting this
 * there is a <i>setProvider(String providername)</i> method. So you can use
 * a nested <i><param name="algorithm.provider" value="MyProvider"/></i>.
 *
 *
 * @version 2003-09-13
 * @since  Ant 1.6
*/
public class ModifiedSelector extends BaseExtendSelector {


    // -----  member variables - configuration


    /** The Cache containing the old values. */
    private Cache cache = null;

    /** Algorithm for computing new values and updating the cache. */
    private Algorithm algorithm = null;

    /** How should the cached value and the new one compared? */
    private Comparator comparator = null;

    /** Should the cache be updated? */
    private boolean update = true;

    /** Are directories selected? */
    private boolean selectDirectories = true;


    // -----  member variables - internal use


    /** Flag whether this object is configured. Configuration is only done once. */
    private boolean isConfigured = false;

    /** Algorithm name for later instantiation. */
    private AlgorithmName algoName = null;

    /** Cache name for later instantiation. */
    private CacheName cacheName = null;

    /** Comparator name for later instantiation. */
    private ComparatorName compName = null;


    /**
     * Parameter vector with parameters for later initialization.
     * @see #configure
     */
    private Vector configParameter = new Vector();

    /**
     * Parameter vector with special parameters for later initialization.
     * The names have the pattern '*.*', e.g. 'cache.cachefile'.
     * These parameters are used <b>after</b> the parameters with the pattern '*'.
     * @see #configure
     */
    private Vector specialParameter = new Vector();


    // -----  constructors  -----


    /** Bean-Constructor. */
    public ModifiedSelector() {
    }


    // ----- configuration  -----


    /** Overrides BaseSelector.verifySettings(). */
    public void verifySettings() {
        configure();
        if (cache == null) {
            setError("Cache must be set.");
        } else if (algorithm == null) {
            setError("Algorithm must be set.");
        } else if (!cache.isValid()) {
            setError("Cache must be proper configured.");
        } else if (!algorithm.isValid()) {
            setError("Algorithm must be proper configured.");
        }
    }


    /**
     * Configures this Selector.
     * Does this work only once per Selector object.
     * <p>Because some problems while configuring from <custom>Selector
     * the configuration is done in the following order:<ol>
     * <li> collect the configuration data </li>
     * <li> wait for the first isSelected() call </li>
     * <li> set the default values </li>
     * <li> set values for name pattern '*': update, cache, algorithm, comparator </li>
     * <li> set values for name pattern '*.*: cache.cachefile, ... </li>
     * </ol></p>
     * <p>This configuration algorithm is needed because you don't know
     * the order of arriving config-data. E.g. if you first set the
     * <i>cache.cachefilename</i> and after that the <i>cache</i> itself,
     * the default value for cachefilename is used, because setting the
     * cache implies creating a new Cache instance - with its defaults.</p>
     */
    public void configure() {
        //
        // -----  The "Singleton"  -----
        //
        if (isConfigured) {
            return;
        }
        isConfigured = true;

        //
        // -----  Set default values  -----
        //
        org.apache.tools.ant.Project project = getProject();
        String filename = "cache.properties";
        File cachefile = null;
        if (project != null) {
            // normal use inside Ant
            cachefile = new File(project.getBaseDir(), filename);
        } else {
            // no reference to project - e.g. during JUnit tests
            cachefile = new File(filename);
        }
        cache = new PropertiesfileCache(cachefile);
        algorithm = new DigestAlgorithm();
        comparator = new EqualComparator();
        update = true;
        selectDirectories = true;


        //
        // -----  Set the main attributes, pattern '*'  -----
        //
        for (Iterator itConfig = configParameter.iterator(); itConfig.hasNext();) {
            Parameter par = (Parameter) itConfig.next();
            if (par.getName().indexOf(".") > 0) {
                // this is a *.* parameter for later use
                specialParameter.add(par);
            } else {
                useParameter(par);
            }
        }
        configParameter = new Vector();

        //
        // -----  Instantiate the interfaces  -----
        //
        String className = null;
        String pkg = "org.apache.tools.ant.types.selectors.cacheselector";

        // the algorithm
        if (algorithm == null) {
            if ("hashvalue".equals(algoName.getValue())) {
                className = pkg + ".HashvalueAlgorithm";
            } else if ("digest".equals(algoName.getValue())) {
                className = pkg + ".DigestAlgorithm";
            }
            if (className != null) {
                try {
                    // load the specified Algorithm, save the reference and configure it
                    algorithm = (Algorithm) Class.forName(className).newInstance();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        // the cache
        if (cache == null) {
            if ("propertyfile".equals(cacheName.getValue())) {
                className = pkg + ".PropertiesfileCache";
            }
            if (className != null) {
                try {
                    // load the specified Cache, save the reference and configure it
                    cache = (Cache) Class.forName(className).newInstance();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        // the comparator
        if (comparator == null) {
            if ("equal".equals(compName.getValue())) {
                className = pkg + ".EqualComparator";
            } else if ("role".equals(compName.getValue())) {
                className = "java.text.RuleBasedCollator";
            }
            if (className != null) {
                try {
                    // load the specified Cache, save the reference and configure it
                    comparator = (Comparator) Class.forName(className).newInstance();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        //
        // -----  Set the special attributes, pattern '*.*'  -----
        //
        for (Iterator itSpecial = specialParameter.iterator(); itSpecial.hasNext();) {
            Parameter par = (Parameter) itSpecial.next();
            useParameter(par);
        }
        specialParameter = new Vector();
    }


    // -----  the selection work  -----


    /**
     * Implementation of BaseExtendSelector.isSelected().
     * @param basedir as described in BaseExtendSelector
     * @param filename as described in BaseExtendSelector
     * @param file as described in BaseExtendSelector
     * @return as described in BaseExtendSelector
     */
    public boolean isSelected(File basedir, String filename, File file) {
        validate();
        File f = new File(basedir, filename);

        // You can not compute a value for a directory
        if (f.isDirectory()) {
            return selectDirectories;
        }

        // Get the values and do the comparison
        String cachedValue = String.valueOf(cache.get(f.getAbsolutePath()));
        String newValue    = algorithm.getValue(f);
        boolean rv = (comparator.compare(cachedValue, newValue) != 0);

        // Maybe update the cache
        if (update && !cachedValue.equals(newValue)) {
            cache.put(f.getAbsolutePath(), newValue);
            cache.save();
        }

        return rv;
    }


    // -----  attribute and nested element support  -----


    /**
     * Support for <i>update</i> attribute.
     * @param update new value
     */
    public void setUpdate(boolean update) {
        this.update = update;
    }


    /**
     * Support for <i>seldirs</i> attribute.
     * @param seldirs new value
     */
    public void setSeldirs(boolean seldirs) {
        selectDirectories = seldirs;
    }


    /**
     * Support for nested &lt;param&gt; tags.
     * @param key the key of the parameter
     * @param value the value of the parameter
     */
    public void addParam(String key, Object value) {
        Parameter par = new Parameter();
        par.setName(key);
        par.setValue(String.valueOf(value));
        configParameter.add(par);
    }


    /**
     * Support for nested &lt;param&gt; tags.
     * @param parameter the parameter object
     */
    public void addParam(Parameter parameter) {
        configParameter.add(parameter);
    }


    /**
     * Defined in org.apache.tools.ant.types.Parameterizable.
     * Overwrite implementation in superclass because only special
     * parameters are valid.
     * @see #addParam(String,String).
     */
    public void setParameters(Parameter[] parameters) {
        if (parameters != null) {
            for (int i = 0; i < parameters.length; i++) {
                configParameter.add(parameters[i]);
            }
        }
    }


    /**
     * Support for nested <param name="" value=""/> tags.
     * Parameter named <i>cache</i>, <i>algorithm</i>,
     * <i>comparator</i> or <i>update</i> are mapped to
     * the respective set-Method.
     * Parameter which names starts with <i>cache.</i> or
     * <i>algorithm.</i> or <i>comparator.</i> are tried
     * to set on the appropriate object via its set-methods.
     * Other parameters are invalid and an BuildException will
     * be thrown.
     *
     * @param parameter  Key and value as parameter object
     */
    public void useParameter(Parameter parameter) {
        String key = parameter.getName();
        String value = parameter.getValue();
        if ("cache".equals(key)) {
            CacheName cn = new CacheName();
            cn.setValue(value);
            setCache(cn);
        } else if ("algorithm".equals(key)) {
            AlgorithmName an = new AlgorithmName();
            an.setValue(value);
            setAlgorithm(an);
        } else if ("comparator".equals(key)) {
            ComparatorName cn = new ComparatorName();
            cn.setValue(value);
            setComparator(cn);
        } else if ("update".equals(key)) {
            boolean updateValue =
                ("true".equalsIgnoreCase(value))
                ? true
                : false;
            setUpdate(updateValue);
        } else if ("seldirs".equals(key)) {
            boolean sdValue =
                ("true".equalsIgnoreCase(value))
                ? true
                : false;
            setSeldirs(sdValue);
        } else if (key.startsWith("cache.")) {
            String name = key.substring(6);
            tryToSetAParameter(cache, name, value);
        } else if (key.startsWith("algorithm.")) {
            String name = key.substring(10);
            tryToSetAParameter(algorithm, name, value);
        } else if (key.startsWith("comparator.")) {
            String name = key.substring(11);
            tryToSetAParameter(comparator, name, value);
        } else {
            setError("Invalid parameter " + key);
        }
    }


    /**
     * Try to set a value on an object using reflection.
     * Helper method for easier access to IntrospectionHelper.setAttribute().
     * @param obj the object on which the attribute should be set
     * @param name the attributename
     * @param value the new value
     */
    protected void tryToSetAParameter(Object obj, String name, String value) {
        Project prj = (getProject() != null) ? getProject() : new Project();
        IntrospectionHelper iHelper
            = IntrospectionHelper.getHelper(prj, obj.getClass());

        try {
            iHelper.setAttribute(prj, obj, name, value);
        } catch (org.apache.tools.ant.BuildException e) {
            // no-op
        }
    }


    // ----- 'beautiful' output -----


    /**
     * Override Object.toString().
     * @return information about this selector
     */
    public String toString() {
        StringBuffer buf = new StringBuffer("{modifiedselector");
        buf.append(" update=").append(update);
        buf.append(" seldirs=").append(selectDirectories);
        buf.append(" cache=").append(cache);
        buf.append(" algorithm=").append(algorithm);
        buf.append(" comparator=").append(comparator);
        buf.append("}");
        return buf.toString();
    }


    // The EnumeratedAttributes for the three interface implementations.
    // Name-Classname mapping is done in the configure() method.


    public Cache getCache() { return cache; }
    public void setCache(CacheName name) {
        cacheName = name;
    }
    public static class CacheName extends EnumeratedAttribute {
        public String[] getValues() {
            return new String[] {"propertyfile" };
        }
    }


    public Algorithm getAlgorithm() { return algorithm; }
    public void setAlgorithm(AlgorithmName name) {
        algoName = name;
    }
    public static class AlgorithmName extends EnumeratedAttribute {
        public String[] getValues() {
            return new String[] {"hashvalue", "digest" };
        }
    }


    public Comparator getComparator() { return comparator; }
    public void setComparator(ComparatorName name) {
        compName = name;
    }
    public static class ComparatorName extends EnumeratedAttribute {
        public String[] getValues() {
            return new String[] {"equal", "rule" };
        }
    }

}
