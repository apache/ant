Summary: Java build tool
Name: ant
Version: 1.0
Release: 0
Group: Development/Tools
Copyright: Apache - free
Provides: ant
Url: http://jakarta.apache.org

Source: http://jakarta.apache.org/builds/nightly/ant/jakarta-tools.src.zip
Prefix: /opt

%description
Platform-independent build tool for java.

%prep
rm -rf ${RPM_BUILD_DIR}/jakarta-tools
unzip -x $RPM_SOURCE_DIR/jakarta-tools.src.zip

%build
cd ${RPM_BUILD_DIR}/jakarta-tools
cd ant
sh bootstrap.sh
sh build.sh 

%install
cd ${RPM_BUILD_DIR}/jakarta-tools
cd ant
sh build.sh -Ddist.dir /opt  dist

%clean

%post
ln -s /opt/ant/bin/ant /usr/bin

%preun
  
%files
## %defattr(-,root,root)
%dir /opt/ant
%dir /opt/ant/bin
%dir /opt/ant/lib
%dir /opt/ant/docs
/opt/ant/lib/ant.jar
/opt/ant/lib/xml.jar
/opt/ant/lib/moo.jar
%config /opt/ant/lib/build.xml
/opt/ant/bin/ant
/opt/ant/bin/antRun
/opt/ant/docs/index.html

%changelog
