Contributing to Apache Ant
==========================

You have found a bug or think you know how to code a great feature
that all other people could benefit from?  This is great, we'd love to
hear from you.

Ant's primary code repository is the
[git repository at Apache](https://git-wip-us.apache.org/repos/asf/ant.git)
and we've also got a [github mirror](https://github.com/apache/ant/).

There are two ways you can contribute, you can either use the
"traditional" approach of creating a patch and attaching it to a
Bugzilla issue or you use a github pull request.  We do not plan to
use github issues, so if you are reporting a bug, please raise a
[Bugzilla issue](https://issues.apache.org/bugzilla/).

Before reporting a bug, please also review http://ant.apache.org/problems.html

If you're planning to implement a new feature please discuss you're
changes on the
[dev list](http://ant.apache.org/mail.html#Developer%20List:%20dev@ant.apache.org)
first. This way you can make sure you're not wasting your time on
something that isn't considered to be in Ant's scope.

Making Changes
--------------

+ Create a topic branch from where you want to base your work (this is
  usually the master branch).
+ Make commits of logical units.
+ Respect the original code style:
  + Only use spaces for indentation.
  + Create minimal diffs - disable on save actions like reformat
    source code or organize imports. If you feel the source code
    should be reformatted create a separate issue/PR for this change.
  + Check for unnecessary whitespace with `git diff --check` before committing.
+ Make sure your commit messages are in the proper format. Your commit
  message should contain the key of the Bugzilla issue if you created one.
+ Make sure you have added the necessary tests for your changes.
+ Run all the tests with `./build.sh clean test` to assure nothing
  else was accidentally broken.

Submitting Changes
------------------

+ Sign the [Contributor License Agreement][cla] unless your change is
  really small or you have already signed one.
+ If you want to create a patch for your Bugzilla issue use `git
  format-patch` to create it (or a set of patches), this way we can
  keep your author information.  Attach the patch(es) to the issue. 
+ Alternatively push your changes to a topic branch in your fork of the repository.
  + Submit a pull request to the repository in the apache organization.

[cla]:http://www.apache.org/licenses/#clas
