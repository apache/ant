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
        helper.setProperty("debug", "true");
        helper.setProperty("ant.package", "org/apache/tools/ant");
        helper.setProperty("optional.package", "${ant.package}/taskdefs/optional");
        helper.setProperty("optional.type.package", "${ant.package}/types/optional");
        helper.setProperty("util.package", "${ant.package}/util");
        helper.setProperty("regexp.package", "${util.package}/regexp");
        helper.createPath("classpath");
        helper.addFileSetToPath("classpath", "${lib.dir}/parser", "*.jar");
        helper.addFileSetToPath("classpath", "${lib.dir}/ant1compat", "*.jar");
        helper.addPathElementToPath("classpath", "${distlib.dir}/init.jar");
        helper.addPathElementToPath("classpath", "${distlib.dir}/common/common.jar");
    }
    protected void check_for_optional_packages(BuildHelper helper) {
    }
    protected void ant1compat(BuildHelper helper) {
        helper.mkdir("${bin.dir}/ant1src");
        helper.mkdir("${bin.dir}/ant1compat");
        helper.copyFilesetRef("ant1src", "${bin.dir}/ant1src");
        helper.javac("${bin.dir}/ant1src:${java.dir}/antlibs/ant1compat", "${bin.dir}/ant1compat", "classpath");
        helper.copyFileset("${bin.dir}/ant1src", "${bin.dir}/ant1compat");
        helper.jar("${bin.dir}/ant1compat", "${distlib.dir}/antlibs/ant1compat.jar",
                   "${java.dir}/antlibs/ant1compat", "antlib.xml");
    }
    protected void clean(BuildHelper helper) {
    }
}
