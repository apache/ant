package org.apache.ant.builder;
public class MutantBuilder {
    protected void _init(BuildHelper helper) {
        helper.setProperty("src.dir", "src");
        helper.setProperty("lib.dir", "lib");
        helper.setProperty("java.dir", "${src.dir}/java");
        helper.setProperty("script.dir", "${src.dir}/script");
        helper.setProperty("conf.dir", "${src.dir}/conf");
        helper.setProperty("bin.dir", "bin");
        helper.setProperty("dist.dir", "dist");
        helper.setProperty("dist.bin", "${dist.dir}/bin");
        helper.setProperty("dist.conf", "${dist.dir}/conf");
        helper.setProperty("javadocs.dir", "${dist.dir}/docs/manual/api");
        helper.setProperty("dist.lib.dir", "${dist.dir}/lib");
        helper.setProperty("dist.core.dir", "${dist.lib.dir}/core");
        helper.setProperty("dist.frontend.dir", "${dist.lib.dir}/frontend");
        helper.setProperty("dist.antlibs.dir", "${dist.lib.dir}/antlibs");
        helper.setProperty("dist.syslibs.dir", "${dist.core.dir}/syslibs");
        helper.setProperty("debug", "true");
        helper.setProperty("chmod.fail", "true");
        helper.createPath("classpath.parser");
        helper.addFileSetToPath("classpath.parser", 
                        "${lib.dir}/parser", "*.jar");
        helper.createPath("classpath.common");
        helper.addPathElementToPath("classpath.common", "${dist.core.dir}/start/init.jar");
        helper.createPath("classpath.antcore");
        helper.addPathElementToPath("classpath.antcore", "${dist.core.dir}/common/common.jar");
        helper.addPathToPath("classpath.antcore", "classpath.common");
        helper.addPathToPath("classpath.antcore", "classpath.parser");
        helper.createPath("classpath.frontend");
        helper.addPathElementToPath("classpath.frontend", "${dist.core.dir}/antcore/antcore.jar");
        helper.addPathToPath("classpath.frontend", "classpath.antcore");
        helper.createPath("classpath.start");
        helper.addPathElementToPath("classpath.start", "${dist.core.dir}/start/init.jar");
    }
    protected void buildsetup(BuildHelper helper) {
        helper.runDepends(this, "buildsetup", "");
        System.out.println("buildsetup: ");
        helper.mkdir("${bin.dir}");
        helper.mkdir("${dist.core.dir}");
        helper.copyFileset("${lib.dir}/parser", "${dist.core.dir}/parser");
    }
    protected void initjar(BuildHelper helper) {
        helper.runDepends(this, "initjar", "buildsetup");
        System.out.println("initjar: ");
        helper.mkdir("${bin.dir}/init");
        helper.mkdir("${dist.core.dir}/start");
        helper.javac("${java.dir}/init", "${bin.dir}/init", null);
        helper.jar("${bin.dir}/init", "${dist.core.dir}/start/init.jar",
                   null, null, null, null);
    }
    protected void common(BuildHelper helper) {
        helper.runDepends(this, "common", "initjar");
        System.out.println("common: ");
        helper.mkdir("${bin.dir}/common");
        helper.mkdir("${dist.core.dir}/common");
        helper.javac("${java.dir}/common", "${bin.dir}/common", "classpath.common");
        helper.jar("${bin.dir}/common", "${dist.core.dir}/common/common.jar",
                   null, null, null, null);
    }
    protected void antcore(BuildHelper helper) {
        helper.runDepends(this, "antcore", "common");
        System.out.println("antcore: ");
        helper.mkdir("${bin.dir}/antcore");
        helper.mkdir("${dist.core.dir}/antcore");
        helper.javac("${java.dir}/antcore", "${bin.dir}/antcore", "classpath.antcore");
        helper.jar("${bin.dir}/antcore", "${dist.core.dir}/antcore/antcore.jar",
                   null, null, null, null);
    }
    protected void frontend(BuildHelper helper) {
        helper.runDepends(this, "frontend", "antcore, startjar");
        System.out.println("frontend: ");
        helper.mkdir("${bin.dir}/frontend");
        helper.mkdir("${dist.frontend.dir}");
        helper.javac("${java.dir}/frontend", "${bin.dir}/frontend", "classpath.frontend");
        helper.jar("${bin.dir}/frontend", "${dist.frontend.dir}/cli.jar",
                   null, null, null, "org.apache.ant.cli.Commandline");
    }
    protected void startjar(BuildHelper helper) {
        helper.runDepends(this, "startjar", "initjar");
        System.out.println("startjar: ");
        helper.mkdir("${bin.dir}/start");
        helper.mkdir("${dist.core.dir}/start");
        helper.javac("${java.dir}/start", "${bin.dir}/start", "classpath.start");
        helper.jar("${bin.dir}/start", "${dist.core.dir}/start/start.jar",
                   null, null, "init.jar", "org.apache.ant.start.Main");
        helper.jar("${bin.dir}/start", "${dist.core.dir}/start/ant.jar",
                   null, null, "start.jar", "org.apache.tools.ant.Main");
    }
    protected void antlibs(BuildHelper helper) {
        helper.runDepends(this, "antlibs", "common");
        System.out.println("antlibs: ");
        {
            BuildHelper subHelper = new BuildHelper();
            subHelper.setProperty("libname", helper.resolve("system"));
            subHelper.setProperty("antlibdir", helper.resolve("${dist.syslibs.dir}"));
            subHelper.setParent(helper);
            _init(subHelper);
            buildlib(subHelper);
        }
        {
            BuildHelper subHelper = new BuildHelper();
            subHelper.setProperty("libname", helper.resolve("monitor"));
            subHelper.setProperty("antlibdir", helper.resolve("${dist.syslibs.dir}"));
            subHelper.setParent(helper);
            _init(subHelper);
            buildlib(subHelper);
        }
    }
    protected void buildlib(BuildHelper helper) {
        helper.runDepends(this, "buildlib", "");
        System.out.println("buildlib: ");
        helper.setProperty("antlib.build.dir", "${bin.dir}/antlibs/${libname}");
        helper.setProperty("antlib.src.dir", "${java.dir}/antlibs/${libname}");
        helper.mkdir("${antlib.build.dir}");
        helper.mkdir("${antlibdir}");
        helper.createPath("classpath.antlibs");
        helper.addPathElementToPath("classpath.antlibs", "${dist.core.dir}/common/common.jar");
        helper.addPathToPath("classpath.antlibs", "classpath.common");
        helper.javac("${antlib.src.dir}", "${antlib.build.dir}", "classpath.antlibs");
        helper.jar("${antlib.build.dir}", "${antlibdir}/${libname}.jar",
                   "${antlib.src.dir}", "antlib.xml", null, null);
    }
    protected void setup_bin(BuildHelper helper) {
        helper.runDepends(this, "setup_bin", "");
        System.out.println("setup-bin: ");
        helper.mkdir("${dist.bin}");
        helper.copyFileset("${script.dir}/", "${dist.bin}");
    }
    protected void setup_conf(BuildHelper helper) {
        helper.runDepends(this, "setup_conf", "");
        System.out.println("setup-conf: ");
        helper.mkdir("${dist.conf}");
        helper.copyFileset("${conf.dir}/", "${dist.conf}");
    }
    protected void clean(BuildHelper helper) {
        helper.runDepends(this, "clean", "");
        System.out.println("clean: ");
    }
    protected void checkstyle(BuildHelper helper) {
        helper.runDepends(this, "checkstyle", "");
        System.out.println("checkstyle: ");
        helper.mkdir("${bin.dir}/check");
    }
    protected void javadocs(BuildHelper helper) {
        helper.runDepends(this, "javadocs", "");
        System.out.println("javadocs: ");
        helper.mkdir("${javadocs.dir}");
    }
    protected void test(BuildHelper helper) {
        helper.runDepends(this, "test", "");
        System.out.println("test: ");
    }
    protected void jars(BuildHelper helper) {
        helper.runDepends(this, "jars", "initjar, startjar, antcore, frontend, antlibs");
        System.out.println("jars: ");
    }
    protected void dist_lite(BuildHelper helper) {
        helper.runDepends(this, "dist_lite", "jars, setup_bin, setup_conf");
        System.out.println("dist-lite: ");
    }
    protected void dist(BuildHelper helper) {
        helper.runDepends(this, "dist", "dist_lite, javadocs");
        System.out.println("dist: ");
    }
}
