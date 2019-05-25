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

package org.apache.tools.ant.types.selectors;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.RuleBasedCollator;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.BuildFileRule;
import org.apache.tools.ant.MagicNames;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Target;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.types.Parameter;
import org.apache.tools.ant.types.Path;
import org.apache.tools.ant.types.selectors.modifiedselector.Algorithm;
import org.apache.tools.ant.types.selectors.modifiedselector.Cache;
import org.apache.tools.ant.types.selectors.modifiedselector.ChecksumAlgorithm;
import org.apache.tools.ant.types.selectors.modifiedselector.DigestAlgorithm;
import org.apache.tools.ant.types.selectors.modifiedselector.EqualComparator;
import org.apache.tools.ant.types.selectors.modifiedselector.HashvalueAlgorithm;
import org.apache.tools.ant.types.selectors.modifiedselector.ModifiedSelector;
import org.apache.tools.ant.types.selectors.modifiedselector.PropertiesfileCache;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

/**
 * Unit tests for ModifiedSelector.
 *
 * @since  Ant 1.6
 */
public class ModifiedSelectorTest {

    @Rule
    public final BaseSelectorRule selectorRule = new BaseSelectorRule();

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Rule
    public TemporaryFolder testFolder = new TemporaryFolder();

    //  =====================  fixtures  =====================

    /** Path where the testclasses are. */
    private Path testclasses;

    //  =====================  JUnit stuff  =====================

    @Before
    public void setUp() {
        // init the testclasses path object
        Project prj = selectorRule.getProject();
        testclasses = new Path(prj, prj.getProperty("build.tests.value"));
    }

    // =======  testcases for the attributes and nested elements of the selector  =====

    /** Test correct use of cache names. */
    @Test
    public void testValidateWrongCache() {
        String name = "this-is-not-a-valid-cache-name";
        thrown.expect(BuildException.class);
        thrown.expectMessage(name + " is not a legal value for this attribute");
        new ModifiedSelector.CacheName().setValue(name);
    }

    /** Test correct use of cache names. */
    @Test
    public void testValidateWrongAlgorithm() {
        String name = "this-is-not-a-valid-algorithm-name";
        thrown.expect(BuildException.class);
        thrown.expectMessage(name + " is not a legal value for this attribute");
        new ModifiedSelector.AlgorithmName().setValue(name);
    }

    /** Test correct use of comparator names. */
    @Test
    public void testValidateWrongComparator() {
        String name = "this-is-not-a-valid-comparator-name";
        thrown.expect(BuildException.class);
        thrown.expectMessage(name + " is not a legal value for this attribute");
        new ModifiedSelector.ComparatorName().setValue(name);
    }

    /** Test correct use of algorithm names. */
    @Test
    public void testIllegalCustomAlgorithm() {
        String className = "java.lang.Object";
        thrown.expect(BuildException.class);
        thrown.expectMessage("Specified class (" + className + ") is not an Algorithm.");
        getAlgoName(className);
    }

    /** Test correct use of algorithm names. */
    @Test
    public void testNonExistentCustomAlgorithm() {
        String className = "non.existent.custom.Algorithm";
        thrown.expect(BuildException.class);
        thrown.expectMessage("Specified class (" + className + ") not found.");
        getAlgoName(className);
    }

    @Test
    public void testCustomAlgorithm() {
        String algo = getAlgoName("org.apache.tools.ant.types.selectors.modifiedselector.HashvalueAlgorithm");
        assertThat("Wrong algorithm used: " + algo, algo, startsWith("HashvalueAlgorithm"));
    }

    @Test
    public void testCustomAlgorithm2() {
        String algo = getAlgoName("org.apache.tools.ant.types.selectors.MockAlgorithm");
        assertThat("Wrong algorithm used: " + algo, algo, startsWith("MockAlgorithm"));
    }

    @Test
    public void testCustomClasses() {
        assertNotNull("Ant home not set",
                selectorRule.getProject().getProperty(MagicNames.ANT_HOME));
        BFT bft = new BFT();
        bft.setUp();
        // don't catch the JUnit exceptions
        try {
            // do the actions
            bft.doTarget("modifiedselectortest-customClasses");
            // do the checks - the buildfile stores the fileset as property
            String fsFullValue = bft.getProperty("fs.full.value");
            String fsModValue  = bft.getProperty("fs.mod.value");

            assertNotNull("'fs.full.value' must be set.", fsFullValue);
            assertNotEquals("'fs.full.value' must not be null.", "", fsFullValue);
            assertThat("'fs.full.value' must contain ant.bat.", fsFullValue,
                    containsString("ant.bat"));

            assertNotNull("'fs.mod.value' must be set.", fsModValue);
            // must be empty according to the Mock* implementations
            assertEquals("'fs.mod.value' must be empty.", "", fsModValue);
        } finally {
            bft.doTarget("modifiedselectortest-scenario-clean");
            bft.deletePropertiesfile();
            bft.tearDown();
        }
    }

    @Test
    public void testDelayUpdateTaskFinished() {
        doDelayUpdateTest(1);
    }

    @Test
    public void testDelayUpdateTargetFinished() {
        doDelayUpdateTest(2);
    }

    @Test
    public void testDelayUpdateBuildFinished() {
        doDelayUpdateTest(3);
    }

    public void doDelayUpdateTest(int kind) {
        // no check for 1<=kind<=3 - only internal use therefore check it
        // while development

        // readable form of parameter kind
        String[] kinds = {"task", "target", "build"};

        // setup the "Ant project"
        MockProject project = new MockProject();
        File base  = new File("base");
        File file1 = new File("file1");
        File file2 = new File("file2");

        // setup the selector
        ModifiedSelector sel = new ModifiedSelector();
        sel.setProject(project);
        sel.setUpdate(true);
        sel.setDelayUpdate(true);
        // sorry - otherwise we will get a ClassCastException because the MockCache
        // is loaded by two different classloader ...
        sel.setClassLoader(this.getClass().getClassLoader());
        sel.addClasspath(testclasses);

        sel.setAlgorithmClass("org.apache.tools.ant.types.selectors.MockAlgorithm");
        sel.setCacheClass("org.apache.tools.ant.types.selectors.MockCache");
        sel.configure();

        // get the cache, so we can check our things
        MockCache cache = (MockCache) sel.getCache();

        // the test
        assertFalse("Cache must not be saved before 1st selection.", cache.saved);
        sel.isSelected(base, "file1", file1);
        assertFalse("Cache must not be saved after 1st selection.", cache.saved);
        sel.isSelected(base, "file2", file2);
        assertFalse("Cache must not be saved after 2nd selection.", cache.saved);
        switch (kind) {
            case 1 :
                project.fireTaskFinished();
                break;
            case 2 :
                project.fireTargetFinished();
                break;
            case 3 :
                project.fireBuildFinished();
                break;
        }
        assertTrue("Cache must be saved after " + kinds[kind - 1] + "Finished-Event.", cache.saved);
        // MockCache doesn't create a file - therefore no cleanup needed
    }

    /**
     * Extracts the real used algorithm name from the ModifiedSelector using
     * its toString() method.
     * @param classname  the classname from the algorithm to use
     * @return  the algorithm part from the toString() (without brackets)
     */
    private String getAlgoName(String classname) {
        ModifiedSelector sel = new ModifiedSelector();
        sel.setProject(selectorRule.getProject());
        // add the test classes to its classpath
        sel.addClasspath(testclasses);
        sel.setAlgorithmClass(classname);
        // let the selector do its checks
        sel.validate();
        // extract the algorithm name (and config) from the selectors output
        String s1 = sel.toString();
        int posStart = s1.indexOf("algorithm=") + 10;
        int posEnd   = s1.indexOf(" comparator=");
        String algo  = s1.substring(posStart, posEnd);
        // '<' and '>' are only used if the algorithm has properties
        if (algo.startsWith("<")) {
            algo = algo.substring(1);
        }
        if (algo.endsWith(">")) {
            algo = algo.substring(0, algo.length() - 1);
        }
        // return the clean value
        return algo;
    }

    // ================  testcases for the cache implementations  ================

    /**
     * Propertycache must have a set 'cachefile' attribute.
     * The default in ModifiedSelector "cache.properties" is set by the selector.
     */
    @Test
    public void testPropcacheInvalid() {
        Cache cache = new PropertiesfileCache();
        assertFalse("PropertyfilesCache does not check its configuration.", cache.isValid());
    }

    @Test
    public void testPropertyfileCache() {
        PropertiesfileCache cache = new PropertiesfileCache();
        File cachefile = new File("cache.properties");
        cache.setCachefile(cachefile);
        doTest(cache);
        assertFalse("Cache file not deleted.", cachefile.exists());
    }

    /** Checks whether a cache file is created. */
    @Test
    public void testCreatePropertiesCacheDirect() {
        File cachefile = new File(selectorRule.getProject().getBaseDir(), "cachefile.properties");

        PropertiesfileCache cache = new PropertiesfileCache();
        cache.setCachefile(cachefile);

        cache.put("key", "value");
        cache.save();

        assertTrue("Cachefile not created.", cachefile.exists());

        cache.delete();
        assertFalse("Cachefile not deleted.", cachefile.exists());
    }

    /** Checks whether a cache file is created. */
    @Test
    public void testCreatePropertiesCacheViaModifiedSelector() {
        File cachefile = new File(selectorRule.getProject().getBaseDir(), "cachefile.properties");

        // Configure the selector
        ModifiedSelector s = new ModifiedSelector();
        s.setDelayUpdate(false);
        s.addParam("cache.cachefile", cachefile);

        ModifiedSelector.CacheName cacheName = new ModifiedSelector.CacheName();
        cacheName.setValue("propertyfile");
        s.setCache(cacheName);

        s.setUpdate(true);

        selectorRule.selectionString(s);

        // evaluate correctness
        assertTrue("Cache file is not created.", cachefile.exists());
        cachefile.delete();
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
    @Test
    public void testCreatePropertiesCacheViaCustomSelector() throws IOException {
        File cachefile = testFolder.newFile("tmp-cache.properties");

        // Configure the selector
        ExtendSelector s = new ExtendSelector();
        s.setClassname("org.apache.tools.ant.types.selectors.modifiedselector.ModifiedSelector");
        s.addParam(createParam("update", "true"));
        s.addParam(createParam("cache.cachefile", cachefile.getAbsolutePath()));
        s.addParam(createParam("cache", "propertyfile"));

        selectorRule.selectionString(s);

        // evaluate correctness
        assertTrue("Cache file is not created.", cachefile.exists());
    }

    @Test
    @Ignore("same logic as on algorithm, no testcases created")
    public void testCustomCache() {
        // same logic as on algorithm, no testcases created
    }

    /**
     * Test the interface semantic of Caches.
     * This method does some common test for cache implementations.
     * A cache must return a stored value and a valid iterator.
     * After calling the delete() the cache must be empty.
     *
     * @param cache   configured test object
     */
    protected void doTest(Cache cache) {
        assertTrue("Cache not proper configured.", cache.isValid());

        String key1   = "key1";
        String value1 = "value1";
        String key2   = "key2";
        String value2 = "value2";

        // given cache must be empty
        Iterator<String> it1 = cache.iterator();
        assertFalse("Cache is not empty", it1.hasNext());

        // cache must return a stored value
        cache.put(key1, value1);
        cache.put(key2, value2);
        assertEquals("cache returned wrong value", value1, cache.get(key1));
        assertEquals("cache returned wrong value", value2, cache.get(key2));

        // test the iterator
        Iterator<String> it2 = cache.iterator();
        String returned = it2.next();
        boolean ok = key1.equals(returned) || key2.equals(returned);
        String msg = "Iterator returned unexpected value."
                   + "  key1.equals(returned)=" + key1.equals(returned)
                   + "  key2.equals(returned)=" + key2.equals(returned)
                   + "  returned=" + returned
                   + "  ok=" + ok;
        assertTrue(msg, ok);

        // clear the cache
        cache.delete();
        Iterator<String> it3 = cache.iterator();
        assertFalse("Cache is not empty", it3.hasNext());
    }

    // ==============  testcases for the algorithm implementations  ==============

    @Test
    public void testHashvalueAlgorithm() {
        HashvalueAlgorithm algo = new HashvalueAlgorithm();
        doTest(algo);
    }

    @Test
    public void testDigestAlgorithmMD5() {
        DigestAlgorithm algo = new DigestAlgorithm();
        algo.setAlgorithm("MD5");
        doTest(algo);
    }

    @Test
    public void testDigestAlgorithmSHA() {
        DigestAlgorithm algo = new DigestAlgorithm();
        algo.setAlgorithm("SHA");
        doTest(algo);
    }

    @Test
    public void testChecksumAlgorithm() {
        ChecksumAlgorithm algo = new ChecksumAlgorithm();
        doTest(algo);
    }

    @Test
    public void testChecksumAlgorithmCRC() {
        ChecksumAlgorithm algo = new ChecksumAlgorithm();
        algo.setAlgorithm("CRC");
        doTest(algo);
    }

    @Test
    public void testChecksumAlgorithmAdler() {
        ChecksumAlgorithm algo = new ChecksumAlgorithm();
        algo.setAlgorithm("Adler");
        doTest(algo);
    }

    /**
     * Test the interface semantic of Algorithms.
     * This method does some common test for algorithm implementations.
     * An algorithm must return always the same value for the same file and
     * it must not return <i>null</i>.
     *
     * @param algo   configured test object
     */
    protected void doTest(Algorithm algo) {
        assertTrue("Algorithm not proper configured.", algo.isValid());
        // must not be a directory
        Arrays.stream(selectorRule.getFiles()).filter(File::isFile).forEach(file -> {
            // get the Hashvalues
            String hash1 = algo.getValue(file);
            String hash2 = algo.getValue(file);
            String hash3 = algo.getValue(file);
            String hash4 = algo.getValue(file);
            String hash5 = algo.getValue(new File(file.getAbsolutePath()));
            // Assert !=null and equality
            assertNotNull("Hashvalue was null for " + file.getAbsolutePath(), hash1);
            assertNotNull("Hashvalue was null for " + file.getAbsolutePath(), hash2);
            assertNotNull("Hashvalue was null for " + file.getAbsolutePath(), hash3);
            assertNotNull("Hashvalue was null for " + file.getAbsolutePath(), hash4);
            assertNotNull("Hashvalue was null for " + file.getAbsolutePath(), hash5);
            assertEquals("getHashvalue() returned different value for " + file.getAbsolutePath(), hash1, hash2);
            assertEquals("getHashvalue() returned different value for " + file.getAbsolutePath(), hash1, hash3);
            assertEquals("getHashvalue() returned different value for " + file.getAbsolutePath(), hash1, hash4);
            assertEquals("getHashvalue() returned different value for " + file.getAbsolutePath(), hash1, hash5);
        });
    }

    // ==============  testcases for the comparator implementations  ==============

    @Test
    public void testEqualComparator() {
        EqualComparator comp = new EqualComparator();
        doTest(comp);
    }

    @Test
    public void testRuleComparator() {
        RuleBasedCollator comp = (RuleBasedCollator) RuleBasedCollator.getInstance();
        doTest(comp);
    }

    @Test
    public void testEqualComparatorViaSelector() {
        ModifiedSelector s = new ModifiedSelector();
        ModifiedSelector.ComparatorName compName = new ModifiedSelector.ComparatorName();
        compName.setValue("equal");
        s.setComparator(compName);
        try {
            performTests(s, "TTTTTTTTTTTT");
        } finally {
            s.getCache().delete();
        }
    }

    @Test
    @Ignore("not yet supported see note in selector")
    public void testRuleComparatorViaSelector() {
        ModifiedSelector s = new ModifiedSelector();
        ModifiedSelector.ComparatorName compName = new ModifiedSelector.ComparatorName();
        compName.setValue("rule");
        s.setComparator(compName);
        try {
            performTests(s, "TTTTTTTTTTTT");
        } finally {
            s.getCache().delete();
        }
    }

    @Test
    @Ignore("same logic as on algorithm, no testcases created")
    public void testCustomComparator() {
        // same logic as on algorithm, no testcases created
    }

    @Test
    public void testResourceSelectorSimple() {
        BFT bft = new BFT();
        bft.doTarget("modifiedselectortest-ResourceSimple");
        bft.deleteCachefile();
        //new File("src/etc/testcases/types/resources/selectors/cache.properties").delete();
    }

    @Test
    public void testResourceSelectorSelresTrue() {
        BFT bft = new BFT();
        bft.doTarget("modifiedselectortest-ResourceSelresTrue");
        assertThat(bft.getLog(), containsString("does not provide an InputStream"));
        bft.deleteCachefile();
    }

    @Test
    public void testResourceSelectorSelresFalse() {
        BFT bft = new BFT();
        bft.doTarget("modifiedselectortest-ResourceSelresFalse");
        bft.deleteCachefile();
    }

    @Test
    public void testResourceSelectorScenarioSimple() {
        assertNotNull("Ant home not set",
                selectorRule.getProject().getProperty(MagicNames.ANT_HOME));
        BFT bft = new BFT();
        bft.doTarget("modifiedselectortest-scenario-resourceSimple");
        bft.doTarget("modifiedselectortest-scenario-clean");
        bft.deleteCachefile();
    }

    /**
     * Test the interface semantic of Comparators.
     * This method does some common test for comparator implementations.
     *
     * @param comp   configured test object
     */
    protected void doTest(Comparator<Object> comp) {
        Object o1 = "string1";
        Object o2 = "string2";
        Object o3 = "string2"; // really "2"

        assertNotEquals("Comparator gave wrong value.", 0, comp.compare(o1, o2));
        assertNotEquals("Comparator gave wrong value.", 0, comp.compare(o1, o3));
        assertEquals("Comparator gave wrong value.", 0, comp.compare(o2, o3));
    }

    // =====================  scenario tests  =====================

    /**
     * Tests whether the seldirs attribute is used.
     */
    @Test
    public void testSeldirs() {
        ModifiedSelector s = new ModifiedSelector();
        StringBuilder sbTrue  = new StringBuilder();
        StringBuilder sbFalse = new StringBuilder();
        for (File file : selectorRule.getFiles()) {
            if (file.isDirectory()) {
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

        s.getCache().delete();
    }

    /**
     * Complex test scenario using default values (DigestAlgorithm with MD5,
     * PropertiesfileCache with file=cache.properties, EqualComparator
     * and update=true).
     * <ol><li>try fist time --&gt; should select all</li>
     * <li>try second time --&gt; should select no files (only directories)</li>
     * <li>modify timestamp of one file and content of another one</li>
     * <li>try third time --&gt; should select only the file with modified
     *      content</li></ol>
     */
    @Test
    public void testScenario1() {
        BFT bft = null;
        ModifiedSelector s = null;
        try {

            String results;

            // Configure the selector - only defaults are used
            s = new ModifiedSelector();

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
            bft.writeProperties("f2name=" + f2name);
            bft.writeProperties("f3name=" + f3name);
            bft.writeProperties("f4name=" + f4name);
            // call the target for making the files dirty
            bft.doTarget("modifiedselectortest-makeDirty");

            //
            // *****  Third Run  *****
            // third call should get only those files, which CONTENT changed
            // (no timestamp changes required!)
            results = selectorRule.selectionString(s);

            //
            // *****  Check the result  *****
            //

            // Mark all files which should be selected as (T)rue and all others
            // as (F)alse. Directories are always selected so they always are
            // (T)rue.
            StringBuilder expected = new StringBuilder();
            for (int i = 0; i < selectorRule.getFiles().length; i++) {
                String ch = "F";
                // f2name shouldn't be selected: only timestamp has changed!
                if (selectorRule.getFiles()[i].isDirectory()) {
                    ch = "T";
                }
                if (selectorRule.getFilenames()[i].equalsIgnoreCase(f3name)) {
                    ch = "T";
                }
                if (selectorRule.getFilenames()[i].equalsIgnoreCase(f4name)) {
                    ch = "T";
                }
                expected.append(ch);
            }

            assertEquals(
                "Wrong files selected. Differing files: "       // info text
                + resolve(diff(expected.toString(), results)),  // list of files
                expected.toString(),                            // expected result
                results);                                       // result
        } finally {
            // cleanup the environment
            if (s != null) {
                s.getCache().delete();
            }
            if (bft != null) {
                bft.deletePropertiesfile();
            }
        }
    }

    /**
     * This scenario is based on scenario 1, but does not use any
     * default value and its based on &lt;custom&gt; selector. Used values are:
     * <ul><li><b>Cache:</b> Propertyfile,
     *                    cachefile={java.io.tmpdir}/mycache.txt</li>
     * <li><b>Algorithm:</b> Digest
     *                    algorithm=SHA, Provider=null</li>
     * <li><b>Comparator:</b> java.text.RuleBasedCollator</li>
     * <li><b>Update:</b> true</li></ul>
     */
    @Test
    @Ignore("RuleBasedCollator not yet supported - see Selector:375 note")
    public void testScenario2() {
        ExtendSelector s = new ExtendSelector();
        BFT bft = new BFT();
        String cachefile = System.getProperty("java.io.tmpdir") + "/mycache.txt";
        try {
            s.setClassname("org.apache.tools.ant.types.selectors.modifiedselector.ModifiedSelector");

            s.addParam(createParam("cache.cachefile", cachefile));
            //s.addParam(createParam("algorithm.provider","---")); // i don't know any valid
            s.addParam(createParam("cache", "propertyfile"));
            s.addParam(createParam("update", "true"));
            s.addParam(createParam("comparator", "rule"));
            s.addParam(createParam("algorithm.name", "sha"));
            s.addParam(createParam("algorithm", "digest"));

            // first and second run
            performTests(s, "TTTTTTTTTTTT");
            performTests(s, "TFFFFFFFFFFT");
            // make dirty
            String f2name = "tar/bz2/asf-logo-huge.tar.bz2";
            String f3name = "asf-logo.gif.md5";
            String f4name = "copy.filterset.filtered";
            bft.writeProperties("f2name=" + f2name);
            bft.writeProperties("f3name=" + f3name);
            bft.writeProperties("f4name=" + f4name);
            bft.doTarget("modifiedselectortest-makeDirty");
            // third run
            String results = selectorRule.selectionString(s);
            StringBuilder expected = new StringBuilder();
            for (int i = 0; i < selectorRule.getFilenames().length; i++) {
                String ch = "F";
                if (selectorRule.getFiles()[i].isDirectory()) {
                    ch = "T";
                }
                if (selectorRule.getFilenames()[i].equalsIgnoreCase(f3name)) {
                    ch = "T";
                }
                if (selectorRule.getFilenames()[i].equalsIgnoreCase(f4name)) {
                    ch = "T";
                }
                expected.append(ch);
            }
            assertEquals(
                "Wrong files selected. Differing files: " // info text
                + resolve(diff(expected.toString(), results)),  // list of files
                expected.toString(),                            // expected result
                results);                                       // result
        } finally {
            // cleanup the environment
            (new File(cachefile)).delete();
            bft.deletePropertiesfile();
        }
    }

    @Test
    public void testScenarioCoreSelectorDefaults() {
        assertNotNull("Ant home not set",
                selectorRule.getProject().getProperty(MagicNames.ANT_HOME));
        doScenarioTest("modifiedselectortest-scenario-coreselector-defaults", "cache.properties");
    }

    @Test
    public void testScenarioCoreSelectorSettings() {
        assertNotNull("Ant home not set",
                selectorRule.getProject().getProperty(MagicNames.ANT_HOME));
        doScenarioTest("modifiedselectortest-scenario-coreselector-settings", "core.cache.properties");
    }

    @Test
    public void testScenarioCustomSelectorSettings() {
        assertNotNull("Ant home not set",
                selectorRule.getProject().getProperty(MagicNames.ANT_HOME));
        doScenarioTest("modifiedselectortest-scenario-customselector-settings", "core.cache.properties");
    }

    public void doScenarioTest(String target, String cachefilename) {
        BFT bft = new BFT();
        bft.setUp();
        File cachefile = new File(selectorRule.getProject().getBaseDir(), cachefilename);
        try {
            // do the actions
            bft.doTarget("modifiedselectortest-scenario-clean");
            bft.doTarget(target);

            // the directories to check
            File to1 = new File(selectorRule.getOutputDir(), "selectortest/to-1");
            File to2 = new File(selectorRule.getOutputDir(), "selectortest/to-2");
            File to3 = new File(selectorRule.getOutputDir(), "selectortest/to-3");

            // do the checks
            assertTrue("Cache file not created.", cachefile.exists());
            assertTrue("Not enough files copied on first time.", to1.list().length > 5);
            assertEquals("Too much files copied on second time.", 0, to2.list().length);
            assertEquals("Too much files copied on third time.", 2, to3.list().length);
        // don't catch the JUnit exceptions
        } finally {
            bft.doTarget("modifiedselectortest-scenario-clean");
            bft.deletePropertiesfile();
            bft.tearDown();
            cachefile.delete();
        }
    }

    //  =====================  helper methods and classes  ====================

    /**
     * Creates a configured parameter object.
     * @param name   name of the parameter
     * @param value  value of the parameter
     * @return the parameter object
     */
    private Parameter createParam(String name, String value) {
        Parameter p = new Parameter();
        p.setName(name);
        p.setValue(value);
        return p;
    }

    /**
     * The BFT class wrapps the selector test-builfile inside an
     * ant project. It supports target execution
     * and property transfer to that project.
     */
    private class BFT extends BuildFileRule {
        String buildfile = "src/etc/testcases/types/selectors.xml";

        String propfile = "ModifiedSelectorTest.properties";

        boolean isConfigured = false;

        public void setUp() {
            super.configureProject(buildfile);
            isConfigured = true;
        }

        /**
         * This stub teardown is here because the outer class needs to call the
         * tearDown method, and in the superclass it is protected.
         */
        public void tearDown() {
            super.after();
        }

        public void doTarget(String target) {
            if (!isConfigured) {
                setUp();
            }
            executeTarget(target);
        }

        public String getProperty(String property) {
            return super.getProject().getProperty(property);
        }

        public void writeProperties(String line) {
            if (!isConfigured) {
                setUp();
            }
            File dir = getProject().getBaseDir();
            File file = new File(dir, propfile);
            try {
                FileWriter out = new FileWriter(file.getAbsolutePath(), true);
                out.write(line);
                out.write(System.lineSeparator());
                out.flush();
                out.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        public void deletePropertiesfile() {
            if (!isConfigured) {
                setUp();
            }
            new File(getProject().getBaseDir(), propfile).delete();
        }

        public void deleteCachefile() {
            File basedir = new File(buildfile).getParentFile();
            File cacheFile = new File(basedir, "cache.properties");
            cacheFile.delete();
        }

    }

    /**
     * MockProject wraps a very small ant project (one target, one task)
     * but provides public methods to fire the build events.
     */
    private class MockProject extends Project {
        private Task   task;
        private Target target;

        public MockProject() {
            task = new Task() {
                public void execute() {
                }
            };
            task.setTaskName("testTask");
            target = new Target();
            target.setName("testTarget");
            target.setProject(this);
            target.addTask(task);
            task.setOwningTarget(target);
        }

        public void fireBuildFinished() {
            super.fireBuildFinished(null);
        }
        @SuppressWarnings("unused")
        public void fireSubBuildFinished() {
            super.fireSubBuildFinished(null);
        }
        @SuppressWarnings("unused")
        public void fireTargetStarted() {
            super.fireTargetStarted(target);
        }
        public void fireTargetFinished() {
            super.fireTargetFinished(target, null);
        }
        @SuppressWarnings("unused")
        public void fireTaskStarted() {
            super.fireTaskStarted(task);
        }
        public void fireTaskFinished() {
            super.fireTaskFinished(task, null);
        }
    }


    /**
     * Does the selection test for a given selector and prints the
     * filenames of the differing files (selected but shouldn't,
     * not selected but should).
     * @param selector  The selector to test
     * @param expected  The expected result
     */
    private void performTests(FileSelector selector, String expected) {
        String result = selectorRule.selectionString(selector);
        String diff = diff(expected, result);
        String resolved = resolve(diff);
        assertEquals("Differing files: " + resolved, result, expected);
    }
    /**
     *  Checks which files are selected and shouldn't be or which
     *  are not selected but should.
     *  @param expected    String containing 'F's and 'T's
     *  @param result      String containing 'F's and 'T's
     *  @return Difference as String containing '-' (equal) and
     *          'X' (difference).
     */
    private String diff(String expected, String result) {
        int length1 = expected.length();
        int length2 = result.length();
        int min = (length1 > length2) ? length2 : length1;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < min; i++) {
            sb.append((expected.charAt(i) == result.charAt(i)) ? "-" : "X");
        }
        return sb.toString();
    }

    /**
     * Resolves a diff-String (@see diff()) against the (inherited) filenames-
     * and files arrays.
     * @param filelist    Diff-String
     * @return String containing the filenames for all differing files,
     *         separated with semicolons ';'
     */
    private String resolve(String filelist) {
        StringBuilder sb = new StringBuilder();
        int min = (selectorRule.getFilenames().length > filelist.length())
                ? filelist.length() : selectorRule.getFilenames().length;
        for (int i = 0; i < min; i++) {
            if ('X' == filelist.charAt(i)) {
                sb.append(selectorRule.getFilenames()[i]).append(";");
            }
        }
        return sb.toString();
    }

}
