package org.apache.ant.builder;
public class Ant1CompatBuilder {
    protected void _init(BuildHelper helper) {
        helper.setProperty("debug", "true");
        helper.setProperty("deprecation", "false");
        helper.setProperty("optimize", "true");
        helper.setProperty("junit.fork", "false");
        helper.setProperty("junit.filtertrace", "off");
        helper.setProperty("junit.summary", "no");
        helper.setProperty("ant1base.dir", "../..");
        helper.setProperty("ant1src.dir", "${ant1base.dir}/src");
        helper.setProperty("ant1java.dir", "${ant1src.dir}/main");
        helper.setProperty("ant1etc.dir", "${ant1src.dir}/etc");
        helper.setProperty("ant1.tests.dir", "${ant1src.dir}/etc/testcases");
        helper.setProperty("src.dir", "src");
        helper.setProperty("java.dir", "${src.dir}/java");
        helper.setProperty("lib.dir", "lib");
        helper.setProperty("tests.dir", "${ant1src.dir}/testcases");
        helper.setProperty("tests.etc.dir", "${src.dir}/etc/testcases");
        helper.setProperty("bin.dir", "bin");
        helper.setProperty("dist.dir", "dist");
        helper.setProperty("dist.lib.dir", "${dist.dir}/lib");
        helper.setProperty("dist.core.dir", "${dist.lib.dir}/core");
        helper.setProperty("dist.antlibs.dir", "${dist.lib.dir}/antlibs");
        helper.setProperty("dist.syslibs.dir", "${dist.core.dir}/syslibs");
        helper.setProperty("ant.package", "org/apache/tools/ant");
        helper.setProperty("optional.package", "${ant.package}/taskdefs/optional");
        helper.setProperty("optional.type.package", "${ant.package}/types/optional");
        helper.setProperty("util.package", "${ant.package}/util");
        helper.setProperty("regexp.package", "${util.package}/regexp");
        helper.setProperty("build.tests", "${bin.dir}/testcases");
        helper.createPath("classpath");
        helper.addFileSetToPath("classpath", 
                        "${dist.core.dir}/parser", "*.jar");
        helper.addPathElementToPath("classpath", "${dist.core.dir}/start/init.jar");
        helper.addPathElementToPath("classpath", "${dist.core.dir}/common/common.jar");
        helper.addPathElementToPath("classpath", "${dist.syslibs.dir}/system.jar");
        helper.createPath("tests-classpath");
        helper.addPathElementToPath("tests-classpath", "${build.classes}");
        helper.addPathElementToPath("tests-classpath", "${build.tests}");
        helper.addPathElementToPath("tests-classpath", "${tests.dir}");
        helper.addPathElementToPath("tests-classpath", "${tests.etc.dir}");
        helper.addPathToPath("tests-classpath", "classpath");
    }
    protected void check_for_optional_packages(BuildHelper helper) {
        helper.runDepends(this, "check_for_optional_packages", "");
        System.out.println("check_for_optional_packages: ");
        helper.setProperty("build.tests.resolved", "");
    }
    protected void ant1compat(BuildHelper helper) {
        helper.runDepends(this, "ant1compat", "check_for_optional_packages");
        System.out.println("ant1compat: ");
        helper.mkdir("${bin.dir}/ant1src_copy");
        helper.mkdir("${bin.dir}/ant1compat");
        helper.copyFilesetRef("ant1src_tocopy", "${bin.dir}/ant1src_copy");
        helper.javac("${bin.dir}/ant1src_copy:${java.dir}/antlibs/ant1compat", "${bin.dir}/ant1compat", "classpath");
        helper.copyFileset("${bin.dir}/ant1src_copy", "${bin.dir}/ant1compat");
        helper.copyFileset("${ant1etc.dir}", "${bin.dir}/ant1compat/${optional.package}/junit/xsl");
        helper.mkdir("${dist.antlibs.dir}");
        helper.jar("${bin.dir}/ant1compat", "${dist.antlibs.dir}/ant1compat.jar",
                   "${java.dir}/antlibs/ant1compat", "antlib.xml", null, null);
    }
    protected void compile_tests(BuildHelper helper) {
        helper.runDepends(this, "compile_tests", "check_for_optional_packages");
        System.out.println("compile-tests: ");
        helper.mkdir("${build.tests}");
        helper.javac("${tests.dir}", "${build.tests}", "tests-classpath");
        helper.copyFilesetRef("ant1testcases_tocopy", "${tests.etc.dir}");
    }
    protected void dump_info(BuildHelper helper) {
        helper.runDepends(this, "dump_info", "dump_sys_properties,run_which");
        System.out.println("dump-info: ");
    }
    protected void dump_sys_properties(BuildHelper helper) {
        helper.runDepends(this, "dump_sys_properties", "xml_check");
        System.out.println("dump-sys-properties: ");
    }
    protected void xml_check(BuildHelper helper) {
        helper.runDepends(this, "xml_check", "check_for_optional_packages");
        System.out.println("xml-check: ");
    }
    protected void run_which(BuildHelper helper) {
        helper.runDepends(this, "run_which", "check_for_optional_packages");
        System.out.println("run-which: ");
    }
    protected void probe_offline(BuildHelper helper) {
        helper.runDepends(this, "probe_offline", "");
        System.out.println("probe-offline: ");
    }
    protected void test(BuildHelper helper) {
        helper.runDepends(this, "test", "run_tests");
        System.out.println("test: ");
    }
    protected void run_tests(BuildHelper helper) {
        helper.runDepends(this, "run_tests", "dump_info,compile_tests,probe_offline");
        System.out.println("run-tests: ");
    }
    protected void run_single_test(BuildHelper helper) {
        helper.runDepends(this, "run_single_test", "compile_tests");
        System.out.println("run-single-test: ");
    }
    protected void clean(BuildHelper helper) {
        helper.runDepends(this, "clean", "");
        System.out.println("clean: ");
    }
}
