%define	packname    jakarta-ant
%define	applibdir   /usr/share/ant
%define manualdir   /usr/share/doc/%{name}

Summary: A Java based build tool.
Name: ant
Version: @VERSION@
Release: @RPM_RELEASE@
Group: Development/Tools
Copyright: Apache Software License
Provides: ant
Url: http://jakarta.apache.org/ant
BuildArch: noarch
Source: http://jakarta.apache.org/builds/jakarta-ant/@RPM_SOURCE@/src/%{packname}-%{version}-src.tar.gz
BuildRoot: /var/tmp/ant-root
Vendor: Apache Software Foundation
Packager: Apache Software Foundation

%description
Apache Ant is a platform-independent build tool implemented in Java.
It is used to build a number of projects including the Apache Jakarta 
and XML projects.

%prep
%setup -n %{packname}-%{version}

%build
sh build.sh

%install
mkdir -p $RPM_BUILD_ROOT
export ANT_HOME=$RPM_BUILD_ROOT/%{applibdir} 
sh build.sh install 
cp -r $RPM_BUILD_ROOT/%{applibdir}/docs $RPM_BUILD_DIR
cp -r $RPM_BUILD_ROOT/%{applibdir}/LICENSE $RPM_BUILD_DIR
cp -r $RPM_BUILD_ROOT/%{applibdir}/README $RPM_BUILD_DIR
cp -r $RPM_BUILD_ROOT/%{applibdir}/WHATSNEW $RPM_BUILD_DIR
cp -r $RPM_BUILD_ROOT/%{applibdir}/KEYS $RPM_BUILD_DIR

%clean
[ "$RPM_BUILD_ROOT" != "/" ] && rm -rf $RPM_BUILD_ROOT

%post

%preun
  
%files
%defattr(-,root,root)
%doc LICENSE README WHATSNEW KEYS
%doc docs
%{applibdir}/lib
%{applibdir}/bin

%changelog
