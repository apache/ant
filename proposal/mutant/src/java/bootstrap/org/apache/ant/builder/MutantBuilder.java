package org.apache.ant.builder;
public class MutantBuilder {
    protected void _init(BuildHelper helper) {
        helper.setProperty("src.dir", "src");
        helper.setProperty("lib.dir", "lib");
        helper.setProperty("java.dir", "${src.dir}/java");
        helper.setProperty("bin.dir", "bin");
        helper.setProperty("dist.dir", "dist");
        helper.setProperty("javadocs.dir", "${dist.dir}/javadocs");
        helper.setProperty("distlib.dir", "${dist.dir}/lib");
        helper.setProperty("debug", "true");
        helper.createPath("classpath.parser");
        helper.addFileSetToPath("classpath.parser", 
                        "${lib.dir}/parser", "*.jar");
        helper.createPath("classpath.common");
        helper.addPathElementToPath("classpath.common", "${distlib.dir}/init.jar");
        helper.createPath("classpath.antcore");
        helper.addPathElementToPath("classpath.antcore", "${distlib.dir}/common/common.jar");
        helper.addPathToPath("classpath.antcore", "classpath.common");
        helper.addPathToPath("classpath.antcore", "classpath.parser");
        helper.createPath("classpath.frontend");
        helper.addPathElementToPath("classpath.frontend", "${distlib.dir}/antcore/antcore.jar");
        helper.addPathToPath("classpath.frontend", "classpath.antcore");
        helper.createPath("classpath.start");
        helper.addPathElementToPath("classpath.start", "${distlib.dir}/init.jar");
    }
    protected void buildsetup(BuildHelper helper) {
        helper.mkdir("${bin.dir}");
        helper.mkdir("${distlib.dir}");
        helper.copyFileset("${lib.dir}/parser", "${distlib.dir}/parser");
    }
    protected void init(BuildHelper helper) {
        helper.mkdir("${bin.dir}/init");
        helper.javac("${java.dir}/init", "${bin.dir}/init", null);
        helper.jar("${bin.dir}/init", "${distlib.dir}/init.jar",
                   null, null, null, null);
    }
    protected void common(BuildHelper helper) {
        helper.mkdir("${bin.dir}/common");
        helper.mkdir("${distlib.dir}/common");
        helper.javac("${java.dir}/common", "${bin.dir}/common", "classpath.common");
        helper.jar("${bin.dir}/common", "${distlib.dir}/common/common.jar",
                   null, null, null, null);
    }
    protected void antcore(BuildHelper helper) {
        helper.mkdir("${bin.dir}/antcore");
        helper.mkdir("${distlib.dir}/antcore");
        helper.javac("${java.dir}/antcore", "${bin.dir}/antcore", "classpath.antcore");
        helper.jar("${bin.dir}/antcore", "${distlib.dir}/antcore/antcore.jar",
                   null, null, null, null);
    }
    protected void frontend(BuildHelper helper) {
        helper.mkdir("${bin.dir}/frontend");
        helper.mkdir("${distlib.dir}/frontend");
        helper.javac("${java.dir}/frontend", "${bin.dir}/frontend", "classpath.frontend");
        helper.jar("${bin.dir}/frontend", "${distlib.dir}/frontend/frontend.jar",
                   null, null, null, null);
        helper.jar("${bin.dir}/frontend", "${distlib.dir}/frontend/cli.jar",
                   null, null, "frontend.jar", "org.apache.ant.cli.Commandline");
    }
    protected void start(BuildHelper helper) {
        helper.mkdir("${bin.dir}/start");
        helper.javac("${java.dir}/start", "${bin.dir}/start", "classpath.start");
        helper.jar("${bin.dir}/start", "${distlib.dir}/start.jar",
                   null, null, "init.jar", "org.apache.ant.start.Main");
        helper.jar("${bin.dir}/start", "${distlib.dir}/ant.jar",
                   null, null, "start.jar", "org.apache.tools.ant.Main");
    }
    protected void ant1compat(BuildHelper helper) {
    }
    protected void remote(BuildHelper helper) {
        helper.mkdir("${bin.dir}/remote");
        helper.javac("${java.dir}/remote", "${bin.dir}/remote", "classpath.start");
        helper.jar("${bin.dir}/remote", "${distlib.dir}/remote.jar",
                   null, null, null, "org.apache.ant.remote.RemoteMain");
    }
    protected void clean(BuildHelper helper) {
    }
    protected void antlibs(BuildHelper helper) {
    }
    protected void systemlib(BuildHelper helper) {
        helper.mkdir("${bin.dir}/antlibs/system");
        helper.mkdir("${distlib.dir}/syslibs");
        helper.createPath("classpath.antlibs");
        helper.addPathElementToPath("classpath.antlibs", "${distlib.dir}/common/common.jar");
        helper.addPathToPath("classpath.antlibs", "classpath.common");
        helper.javac("${java.dir}/antlibs/system", "${bin.dir}/antlibs/system", "classpath.antlibs");
        helper.jar("${bin.dir}/antlibs/system", "${distlib.dir}/syslibs/system.jar",
                   "${java.dir}/antlibs/system", "antlib.xml", null, null);
    }
    protected void main(BuildHelper helper) {
    }
    protected void checkstyle(BuildHelper helper) {
        helper.mkdir("${bin.dir}/check");
    }
    protected void javadocs(BuildHelper helper) {
        helper.mkdir("${javadocs.dir}");
    }
}
