package org.apache.ant.builder;
public class Ant1CompatBuilder {
    protected void _init(BuildHelper helper) {
        helper.setProperty("src.dir", "src");
        helper.setProperty("lib.dir", "lib");
        helper.setProperty("java.dir", "${src.dir}/java");
        helper.setProperty("bin.dir", "bin");
        helper.setProperty("dist.dir", "dist");
        helper.setProperty("javadocs.dir", "${dist.dir}/javadocs");
        helper.setProperty("distlib.dir", "${dist.dir}/lib");
        helper.setProperty("ant1base.dir", "../..");
        helper.setProperty("ant1src.dir", "${ant1base.dir}/src");
        helper.setProperty("ant1java.dir", "${ant1src.dir}/main");
        helper.setProperty("ant1etc.dir", "${ant1src.dir}/etc");
        helper.setProperty("debug", "true");
        helper.setProperty("ant.package", "org/apache/tools/ant");
        helper.setProperty("optional.package", "${ant.package}/taskdefs/optional");
        helper.setProperty("optional.type.package", "${ant.package}/types/optional");
        helper.setProperty("util.package", "${ant.package}/util");
        helper.setProperty("regexp.package", "${util.package}/regexp");
        helper.createPath("classpath");
        helper.addFileSetToPath("classpath", 
                        "${lib.dir}/parser", "*.jar");
        helper.addFileSetToPath("classpath", 
                        "${lib.dir}/ant1compat", "*.jar");
        helper.addPathElementToPath("classpath", "${distlib.dir}/init.jar");
        helper.addPathElementToPath("classpath", "${distlib.dir}/common/common.jar");
        helper.addPathElementToPath("classpath", "${distlib.dir}/syslibs/system.jar");
    }
    protected void check_for_optional_packages(BuildHelper helper) {
    }
    protected void ant1compat(BuildHelper helper) {
        helper.mkdir("${bin.dir}/ant1src_copy");
        helper.mkdir("${bin.dir}/ant1compat");
        helper.copyFilesetRef("ant1src_tocopy", "${bin.dir}/ant1src_copy");
        helper.javac("${bin.dir}/ant1src_copy:${java.dir}/antlibs/ant1compat", "${bin.dir}/ant1compat", "classpath");
        helper.copyFileset("${bin.dir}/ant1src_copy", "${bin.dir}/ant1compat");
        helper.copyFileset("${ant1etc.dir}", "${bin.dir}/ant1compat/${optional.package}/junit/xsl");
        helper.mkdir("${distlib.dir}/antlibs/");
        helper.jar("${bin.dir}/ant1compat", "${distlib.dir}/antlibs/ant1compat.jar",
                   "${java.dir}/antlibs/ant1compat", "antlib.xml", null, null);
    }
    protected void clean(BuildHelper helper) {
    }
}
