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

package org.apache.tools.ant.types.selectors;


// Java
import java.io.File;
import java.io.FileWriter;
import java.util.Comparator;
import java.util.Iterator;
import java.text.RuleBasedCollator;

// Ant
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.types.Parameter;

// The classes to test
import org.apache.tools.ant.types.selectors.modifiedselector.*;


/**
 * Unit tests for ModifiedSelector.
 *
 * @author Jan Mat\u00e8rne
 * @version 2003-09-13
 * @since  Ant 1.6
 */
public class ModifiedSelectorTest extends BaseSelectorTest {

    /** Package of the CacheSelector classes. */
    private static String pkg = "org.apache.tools.ant.types.selectors.modifiedselector";


    public ModifiedSelectorTest(String name) {
        super(name);
    }


    /**
     * Factory method from base class. This should be overriden in child
     * classes to return a specific Selector class (like here).
     */
    public BaseSelector getInstance() {
        return new ModifiedSelector();
    }


    /** Test right use of cache names. */
    public void testValidateWrongCache() {
        String name = "this-is-not-a-valid-cache-name";
        try {
            ModifiedSelector.CacheName cacheName = new ModifiedSelector.CacheName();
            cacheName.setValue(name);
            fail("CacheSelector.CacheName accepted invalid value.");
        } catch (BuildException be) {
            assertEquals(name + " is not a legal value for this attribute",
                         be.getMessage());
        }
    }


    /** Test right use of cache names. */
    public void testValidateWrongAlgorithm() {
        String name = "this-is-not-a-valid-algorithm-name";
        try {
            ModifiedSelector.AlgorithmName algoName
                = new ModifiedSelector.AlgorithmName();
            algoName.setValue(name);
            fail("CacheSelector.AlgorithmName accepted invalid value.");
        } catch (BuildException be) {
            assertEquals(name + " is not a legal value for this attribute",
                         be.getMessage());
        }
    }


    /** Test right use of comparator names. */
    public void testValidateWrongComparator() {
        String name = "this-is-not-a-valid-comparator-name";
        try {
            ModifiedSelector.ComparatorName compName
                = new ModifiedSelector.ComparatorName();
            compName.setValue(name);
            fail("ModifiedSelector.ComparatorName accepted invalid value.");
        } catch (BuildException be) {
            assertEquals(name + " is not a legal value for this attribute",
                         be.getMessage());
        }
    }


    /**
     * Propertycache must have a set 'cachefile' attribute.
     * The default in ModifiedSelector "cache.properties" is set by the selector.
     */
    public void testPropcacheInvalid() {
        Cache cache = new PropertiesfileCache();
        if (cache.isValid())
            fail("PropertyfilesCache does not check its configuration.");
    }


    /**
     * Tests whether the seldirs attribute is used.
     */
    public void testSeldirs() {
        ModifiedSelector s = (ModifiedSelector)getSelector();
        try {
            makeBed();

            StringBuffer sbTrue  = new StringBuffer();
            StringBuffer sbFalse = new StringBuffer();
            for (int i=0; i<filenames.length; i++) {
                if (files[i].isDirectory()) {
                    sbTrue.append("T");
                    sbFalse.append("F");
                } else {
                    sbTrue.append("T");
                    sbFalse.append("T");
                }
            }


            s.setSeldirs(true);
            performTests(s, sbTrue.toString());
            s.getCache().delete();

            s.setSeldirs(false);
            performTests(s, sbFalse.toString());
            s.getCache().delete();

        } finally {
            cleanupBed();
            if (s!=null) s.getCache().delete();
        }
    }


    /**
     * Complex test scenario using default values (DigestAlgorithm with MD5,
     * PropertiesfileCache with file=cache.properties, EqualComparator
     * and update=true). <ol>
     * <li> try fist time --> should select all </li>
     * <li> try second time --> should select no files (only directories) </li>
     * <li> modify timestamp of one file and content of a nother one </li>
     * <li> try third time --> should select only the file with modified
     *      content </li>
     */
    public void testScenario1() {
        BFT bft = null;
        ModifiedSelector s = null;
        try {
            //
            // *****  initialize test environment (called "bed")  *****
            //
            makeBed();
            String results = null;

            // Configure the selector - only defaults are used
            s = (ModifiedSelector)getSelector();

            //
            // *****  First Run  *****
            // the first call should get all files, because nothing is in
            // the cache
            //
            performTests(s, "TTTTTTTTTTTT");

            //
            // *****  Second Run  *****
            // the second call should get no files, because no content
            // has changed
            //
            performTests(s, "TFFFFFFFFFFT");

            //
            // *****  make some files dirty  *****
            //

            // these files are made dirty --> 3+4 with different content
            String f2name = "tar/bz2/asf-logo-huge.tar.bz2";
            String f3name = "asf-logo.gif.md5";
            String f4name = "copy.filterset.filtered";

            // AccessObject to the test-Ant-environment
            bft = new BFT();
            // give some values (via property file) to that environment
            bft.writeProperties("f2name="+f2name);
            bft.writeProperties("f3name="+f3name);
            bft.writeProperties("f4name="+f4name);
            // call the target for making the files dirty
            bft.doTarget("modifiedselectortest-makeDirty");


            //
            // *****  Third Run  *****
            // third call should get only those files, which CONTENT changed
            // (no timestamp changes required!)
            results = selectionString(s);

            //
            // *****  Check the result  *****
            //

            // Mark all files which should be selected as (T)rue and all others
            // as (F)alse. Directories are always selected so they always are
            // (T)rue.
            StringBuffer expected = new StringBuffer();
            for (int i=0; i<filenames.length; i++) {
                String ch = "F";
                if (files[i].isDirectory()) ch = "T";
                // f2name shouldn't be selected: only timestamp has changed!
                if (filenames[i].equalsIgnoreCase(f3name)) ch = "T";
                if (filenames[i].equalsIgnoreCase(f4name)) ch = "T";
                expected.append(ch);
            }

            assertEquals(
                "Wrong files selected. Differing files: "       // info text
                + resolve(diff(expected.toString(), results)),  // list of files
                expected.toString(),                            // expected result
                results                                         // result
            );

        } finally {
            // cleanup the environment
            cleanupBed();
            if (s!=null) s.getCache().delete();
            if (bft!=null) bft.deletePropertiesfile();
        }
    }



    /**
     * This scenario is based on scenario 1, but does not use any
     * default value and its based on <custom> selector. Used values are:<ul>
     * <li><b>Cache: </b> Propertyfile,
     *                    cachefile={java.io.tmpdir}/mycache.txt </li>
     * <li><b>Algorithm: </b> Digest
     *                    algorithm=SHA, Provider=null </li>
     * <li><b>Comparator: </b> java.text.RuleBasedCollator
     * <li><b>Update: </b> true </li>
     */
    public void testScenario2() {
        ExtendSelector s = new ExtendSelector();
        BFT bft = new BFT();
        String cachefile = System.getProperty("java.io.tmpdir")+"/mycache.txt";
        try {
            makeBed();

            s.setClassname("org.apache.tools.ant.types.selectors.modifiedselector.ModifiedSelector");

            s.addParam(createParam("cache.cachefile", cachefile));
            //s.addParam(createParam("algorithm.provider","---")); // i don't know any valid
            s.addParam(createParam("cache","propertyfile"));
            s.addParam(createParam("update","true"));
            s.addParam(createParam("comparator","rule"));
            s.addParam(createParam("algorithm.name","sha"));
            s.addParam(createParam("algorithm","digest"));

            // first and second run
            performTests(s, "TTTTTTTTTTTT");
            performTests(s, "TFFFFFFFFFFT");
            // make dirty
            String f2name = "tar/bz2/asf-logo-huge.tar.bz2";
            String f3name = "asf-logo.gif.md5";
            String f4name = "copy.filterset.filtered";
            bft.writeProperties("f2name="+f2name);
            bft.writeProperties("f3name="+f3name);
            bft.writeProperties("f4name="+f4name);
            bft.doTarget("modifiedselectortest-makeDirty");
            // third run
            String results = selectionString(s);
            StringBuffer expected = new StringBuffer();
            for (int i=0; i<filenames.length; i++) {
                String ch = "F";
                if (files[i].isDirectory()) ch = "T";
                if (filenames[i].equalsIgnoreCase(f3name)) ch = "T";
                if (filenames[i].equalsIgnoreCase(f4name)) ch = "T";
                expected.append(ch);
            }
            assertEquals(
                "Wrong files selected. Differing files: "       // info text
                + resolve(diff(expected.toString(), results)),  // list of files
                expected.toString(),                            // expected result
                results                                         // result
            );
        } finally {
            // cleanup the environment
            cleanupBed();
            (new java.io.File(cachefile)).delete();
            if (bft!=null) bft.deletePropertiesfile();
        }
    }


    /** Checks whether a cache file is created. */
    public void testCreatePropertiesCacheDirect() {
        File basedir   = getSelector().getProject().getBaseDir();
        File cachefile = new File(basedir, "cachefile.properties");

        PropertiesfileCache cache = new PropertiesfileCache();
        cache.setCachefile(cachefile);

        cache.put("key", "value");
        cache.save();

        assertTrue("Cachefile not created.", cachefile.exists());

        cache.delete();
        assertFalse("Cachefile not deleted.", cachefile.exists());
    }


    /** Checks whether a cache file is created. */
    public void testCreatePropertiesCacheViaModifiedSelector() {
        File basedir   = getSelector().getProject().getBaseDir();
        File cachefile = new File(basedir, "cachefile.properties");
        try {

            // initialize test environment (called "bed")
            makeBed();

            // Configure the selector
            ModifiedSelector s = (ModifiedSelector)getSelector();
            s.addParam("cache.cachefile", cachefile);

            ModifiedSelector.CacheName cacheName = new ModifiedSelector.CacheName();
            cacheName.setValue("propertyfile");
            s.setCache(cacheName);

            s.setUpdate(true);

            // does the selection
            String results = selectionString(s);

            // evaluate correctness
            assertTrue("Cache file is not created.", cachefile.exists());
        } finally {
            cleanupBed();
            if (cachefile!=null) cachefile.delete();
        }
    }


    /**
     * In earlier implementations there were problems with the <i>order</i>
     * of the <param>s. The scenario was <pre>
     *   <custom class="ModifiedSelector">
     *       <param name="cache.cachefile" value="mycache.properties" />
     *       <param name="cache" value="propertyfiles" />
     *   </custom>
     * </pre> It was important first to set the cache and then to set
     * the cache's configuration parameters. That results in the reorganized
     * configure() method of ModifiedSelector. This testcase tests that.
     */
    public void testCreatePropertiesCacheViaCustomSelector() {
        File cachefile = org.apache.tools.ant.util.FileUtils.newFileUtils()
                         .createTempFile("tmp-cache-", ".properties", null);
        try {
            // initialize test environment (called "bed")
            makeBed();

            // Configure the selector

            ExtendSelector s = new ExtendSelector();
            s.setClassname("org.apache.tools.ant.types.selectors.modifiedselector.ModifiedSelector");
            s.addParam(createParam("update", "true"));
            s.addParam(createParam("cache.cachefile", cachefile.getAbsolutePath()));
            s.addParam(createParam("cache", "propertyfile"));

            // does the selection
            String results = selectionString(s);

            // evaluate correctness
            assertTrue("Cache file is not created.", cachefile.exists());
        } finally {
            cleanupBed();
            if (cachefile!=null) cachefile.delete();
        }
    }


    public void testEqualComparatorViaSelector() {
        ModifiedSelector s = (ModifiedSelector)getSelector();
        ModifiedSelector.ComparatorName compName = new ModifiedSelector.ComparatorName();
        compName.setValue("equal");
        s.setComparator(compName);
        try {
            performTests(s, "TTTTTTTTTTTT");
        } finally {
            s.getCache().delete();
        }
    }


    public void testRuleComparatorViaSelector() {
        ModifiedSelector s = (ModifiedSelector)getSelector();
        ModifiedSelector.ComparatorName compName = new ModifiedSelector.ComparatorName();
        compName.setValue("rule");
        s.setComparator(compName);
        try {
            performTests(s, "TTTTTTTTTTTT");
        } finally {
            s.getCache().delete();
        }
    }


    public void testHashvalueAlgorithm() {
        HashvalueAlgorithm algo = new HashvalueAlgorithm();
        doTest(algo);
    }

    public void testDigestAlgorithmMD5() {
        DigestAlgorithm algo = new DigestAlgorithm();
        algo.setAlgorithm("MD5");
        doTest(algo);
    }

    public void testDigestAlgorithmSHA() {
        DigestAlgorithm algo = new DigestAlgorithm();
        algo.setAlgorithm("SHA");
        doTest(algo);
    }


    public void testPropertyfileCache() {
        PropertiesfileCache cache = new PropertiesfileCache();
        File cachefile = new File("cache.properties");
        cache.setCachefile(cachefile);
        doTest(cache);
        assertFalse("Cache file not deleted.", cachefile.exists());
    }


    public void testEqualComparator() {
        EqualComparator comp = new EqualComparator();
        doTest(comp);
    }


    public void testRuleComparator() {
        RuleBasedCollator comp = (RuleBasedCollator)RuleBasedCollator.getInstance();
        doTest(comp);
    }


    public void testScenarioCoreSelectorDefaults() {
        doScenarioTest("modifiedselectortest-scenario-coreselector-defaults", "cache.properties");
    }



    public void testSceanrioCoreSelectorSettings() {
        doScenarioTest("modifiedselectortest-scenario-coreselector-settings", "core.cache.properties");
    }


    public void testScenarioCustomSelectorSettings() {
        doScenarioTest("modifiedselectortest-scenario-customselector-settings", "core.cache.properties");
    }


    public void doScenarioTest(String target, String cachefilename) {
        BFT bft = new BFT();
        bft.setUp();
        File basedir = bft.getProject().getBaseDir();
        File cachefile = new File(basedir, cachefilename);
        try {
            // do the actions
            bft.doTarget("modifiedselectortest-scenario-clean");
            bft.doTarget(target);

            // the directories to check
            File to1 = new File(basedir, "selectortest/to-1");
            File to2 = new File(basedir, "selectortest/to-2");
            File to3 = new File(basedir, "selectortest/to-3");

            // do the checks
            assertTrue("Cache file not created.", cachefile.exists());
            assertTrue("Not enough files copied on first time.", to1.list().length>5);
            assertTrue("Too much files copied on second time.", to2.list().length==0);
            assertTrue("Too much files copied on third time.", to3.list().length==2);
        // don't catch the JUnit exceptions
        } finally {
            bft.doTarget("modifiedselectortest-scenario-clean");
            bft.deletePropertiesfile();
            bft.tearDown();
            cachefile.delete();
        }
    }


    //  ====================  Test interface semantic  ===================


    /**
     * This method does some common test for algorithm implementations.
     * An algorithm must return always the same value for the same file and
     * it must not return <i>null</i>.
     *
     * @param algo   configured test object
     */
    protected void doTest(Algorithm algo) {
        assertTrue("Algorithm not proper configured.", algo.isValid());
        try {
            makeBed();

            for (int i=0; i<files.length; i++) {
                File file = files[i];  // must not be a directory
                if (file.isFile()) {
                    // get the Hashvalues
                    String hash1 = algo.getValue(file);
                    String hash2 = algo.getValue(file);
                    String hash3 = algo.getValue(file);
                    String hash4 = algo.getValue(file);
                    String hash5 = algo.getValue(new File(file.getAbsolutePath()));

                    // Assert !=null and equality
                    assertNotNull("Hashvalue was null for "+file.getAbsolutePath(), hash1);
                    assertNotNull("Hashvalue was null for "+file.getAbsolutePath(), hash2);
                    assertNotNull("Hashvalue was null for "+file.getAbsolutePath(), hash3);
                    assertNotNull("Hashvalue was null for "+file.getAbsolutePath(), hash4);
                    assertNotNull("Hashvalue was null for "+file.getAbsolutePath(), hash5);
                    assertEquals("getHashvalue() returned different value for "+file.getAbsolutePath(), hash1, hash2);
                    assertEquals("getHashvalue() returned different value for "+file.getAbsolutePath(), hash1, hash3);
                    assertEquals("getHashvalue() returned different value for "+file.getAbsolutePath(), hash1, hash4);
                    assertEquals("getHashvalue() returned different value for "+file.getAbsolutePath(), hash1, hash5);
                }//if-isFile
            }//for
        } finally {
            cleanupBed();
        }
    }


    /**
     * This method does some common test for cache implementations.
     * A cache must return a stored value and a valid iterator.
     * After calling the delete() the cache must be empty.
     *
     * @param algo   configured test object
     */
    protected void doTest(Cache cache) {
        assertTrue("Cache not proper configured.", cache.isValid());

        String key1   = "key1";
        String value1 = "value1";
        String key2   = "key2";
        String value2 = "value2";

        // given cache must be empty
        Iterator it1 = cache.iterator();
        assertFalse("Cache is not empty", it1.hasNext());

        // cache must return a stored value
        cache.put(key1, value1);
        cache.put(key2, value2);
        assertEquals("cache returned wrong value", value1, cache.get(key1));
        assertEquals("cache returned wrong value", value2, cache.get(key2));

        // test the iterator
        Iterator it2 = cache.iterator();
        Object   returned = it2.next();
        boolean ok = (key1.equals(returned) || key2.equals(returned));
        String msg = "Iterator returned unexpected value."
                   + "  key1.equals(returned)="+key1.equals(returned)
                   + "  key2.equals(returned)="+key2.equals(returned)
                   + "  returned="+returned
                   + "  ok="+ok;
        assertTrue(msg, ok);

        // clear the cache
        cache.delete();
        Iterator it3 = cache.iterator();
        assertFalse("Cache is not empty", it1.hasNext());
    }


    /**
     * This method does some common test for comparator implementations.
     *
     * @param algo   configured test object
     */
    protected void doTest(Comparator comp) {
        Object o1 = new String("string1");
        Object o2 = new String("string2");
        Object o3 = new String("string2"); // really "2"

        assertTrue("Comparator gave wrong value.", comp.compare(o1, o2) != 0);
        assertTrue("Comparator gave wrong value.", comp.compare(o1, o3) != 0);
        assertTrue("Comparator gave wrong value.", comp.compare(o2, o3) == 0);
    }


    //  ========================  Helper methods  ========================


    private Parameter createParam(String name, String value) {
        Parameter p = new Parameter();
        p.setName(name);
        p.setValue(value);
        return p;
    }


    private class BFT extends org.apache.tools.ant.BuildFileTest {
        BFT() { super("nothing"); }
        BFT(String name) {
            super(name);
        }
        String propfile = "ModifiedSelectorTest.properties";

        boolean isConfigured = false;

        public void setUp() {
            configureProject("src/etc/testcases/types/selectors.xml");
            isConfigured = true;
        }

        public void tearDown() { }

        public void doTarget(String target) {
            if (!isConfigured) setUp();
            executeTarget(target);
        }

        public void writeProperties(String line) {
            if (!isConfigured) setUp();
            File dir = getProject().getBaseDir();
            File file = new File(dir, propfile);
            try {
                java.io.FileWriter out =
                    new java.io.FileWriter(file.getAbsolutePath(), true);
                out.write(line);
                out.write(System.getProperty("line.separator"));
                out.flush();
                out.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        public void deletePropertiesfile() {
            if (!isConfigured) setUp();
            new File(getProject().getBaseDir(), propfile).delete();
        }

        public org.apache.tools.ant.Project getProject() {
            return super.getProject();
        }
    }//class-BFT

}//class-ModifiedSelectorTest
