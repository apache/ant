#!/usr/bin/env python

# Licensed to the Apache Software Foundation (ASF) under one or more
# contributor license agreements.  See the NOTICE file distributed with
# this work for additional information regarding copyright ownership.
# The ASF licenses this file to You under the Apache License, Version 2.0
# (the "License"); you may not use this file except in compliance with
# the License.  You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

# Builds a Mac OS X .pkg from a binary ZIP archive of Apache Ant.

import collections
import contextlib
import os

ApacheAntURL = collections.namedtuple(
    'ApacheAntURL',
    ('url', 'url_scheme', 'version', 'directory_name'))

@contextlib.contextmanager
def make_temp_directory():
    '''Creates a temporary directory which is recursively deleted when out of scope.'''
    import shutil
    import tempfile
    temp_dir = tempfile.mkdtemp()
    yield temp_dir
    shutil.rmtree(temp_dir)

@contextlib.contextmanager
def self_closing_url(url):
    '''Opens a URL and returns a self-closing file-like object.'''
    import urllib2
    url_fp = urllib2.urlopen(url)
    yield url_fp
    url_fp.close()

def apache_ant_url(url_string):
    '''Parses a URL string into an ApacheAntURL object.'''
    import argparse, collections, os.path, urlparse
    parse_result = urlparse.urlparse(url_string)
    filename = os.path.split(parse_result.path)[1]
    if not (filename.startswith('apache-ant-') and filename.endswith('-bin.zip')):
        raise argparse.ArgumentTypeError(
            'Expected [%s] to end with apache-ant-X.Y.Z-bin.zip' % (url_string))
    extracted_directory = filename.replace('-bin.zip', '')
    extracted_version = extracted_directory.replace('apache-ant-', '')
    return ApacheAntURL(
        url=url_string,
        url_scheme=parse_result.scheme,
        version=extracted_version,
        directory_name=extracted_directory)

def fetch_url(url, local_output_file):
    '''Downloads the contents of 'url' and writes them the opened file 'output_file'.'''
    import shutil
    import urllib2
    CHUNK_SIZE = 16 * 1024
    print 'Fetching {url}...'.format(url=url)
    with self_closing_url(url) as url_input_file:
        while True:
            chunk = url_input_file.read(CHUNK_SIZE)
            if not chunk:
                break
            local_output_file.write(chunk)
        local_output_file.seek(0)

def fetch_apache_ant_url(apache_ant_url, temp_dir):
    '''If the ApacheAntURL object is remote, fetches and returns the local file object.
    Otherwise, opens and returns a file object.'''
    import tempfile
    if apache_ant_url.url_scheme == '' or apache_ant_url.url_scheme == 'file':
        return open(apache_ant_url.url, 'rb')
    else:
        fp = tempfile.TemporaryFile(dir=temp_dir)
        fetch_url(apache_ant_url.url, fp)
        return fp

def uncompress_contents(temp_dir, archive_file, directory_name, path_prefix):
    '''Uncompresses the contents of 'archive_file' to 'temp_dir'.

    Strips the prefix 'directory_name' and prepends 'path_prefix' to each entry
    of the zip file.
    '''
    import shutil, zipfile
    output_path = os.path.join(temp_dir, 'pkg')
    os.mkdir(output_path)
    z = zipfile.ZipFile(archive_file)
    print 'Extracting archive to {output_path}...'.format(
        output_path=output_path)
    for entry in z.infolist():
        # We can't just extract directly, since we want to map:
        #
        # apache-ant-X.Y.Z/bin/foo
        #
        # to
        #
        # usr/local/ant/bin/foo
        #
        # So, we strip out the apache-ant-X.Y.Z prefix, then instead of
        # using ZipFile.extract(), we use ZipFile.open() to get a read fd to
        # the source file, then os.fdopen() with the appropriate permissions
        # to geta write fd to the modified destination path.
        expected_prefix = directory_name + '/'
        if not entry.filename.startswith(expected_prefix):
            raise Exeption('Unexpected entry in zip file: [{filename}]'.format(
                    filename=entry.filename))
        entry_path = entry.filename.replace(expected_prefix, '', 1)

        # Using os.path.join is annoying here (we'd have to explode output_path
        # and entry_path).
        entry_output_path = output_path + path_prefix + '/' + entry_path

        # Zip file paths are normalized with '/' at the end for directories.
        if entry_output_path.endswith('/'):
            print 'Creating directory {path}'.format(path=entry_output_path)
            os.makedirs(entry_output_path)
        else:
            # Yes, this is really how you extract permissions from a ZipInfo entry.
            perms = (entry.external_attr >> 16L) & 0777
            print 'Extracting {entry_filename} to {path} with mode 0{mode:o}'.format(
                entry_filename=entry.filename, path=entry_output_path, mode=perms)
            with z.open(entry) as source:
                with os.fdopen(
                    os.open(entry_output_path, os.O_WRONLY | os.O_CREAT, perms), 'w') \
                    as destination:
                    shutil.copyfileobj(source, destination)
    return output_path

def write_paths_d_entry(paths_d_directory, filename):
    os.makedirs(paths_d_directory)
    output_file = os.path.join(paths_d_directory, filename)
    with open(output_file, 'w') as f:
        print >>f, '/usr/local/ant/bin'

def make_pkg(pkg_dir, pkg_identifier, pkg_version, output_pkg_path):
    import subprocess
    print 'Building package at {output_pkg_path}...'.format(
        output_pkg_path=output_pkg_path)
    subprocess.call(
        ['pkgbuild',
         '--root', pkg_dir,
         '--identifier', pkg_identifier,
         '--version', pkg_version,
         output_pkg_path])

def main():
    import argparse
    parser = argparse.ArgumentParser(description='Builds a Mac OS X .pkg of ant.')
    parser.add_argument(
        'apache_ant_url',
        metavar='file-or-url',
        help='Source file or URL from which to uncompress apache-ant-X.Y.Z-bin.zip',
        type=apache_ant_url)
    parser.add_argument(
        '--output-dir',
        default='.',
        help='Directory to which .pkg will be written. Defaults to current directory.')
    args = parser.parse_args()
    with make_temp_directory() as temp_dir:
        archive_file = fetch_apache_ant_url(args.apache_ant_url, temp_dir)
        pkg_dir = uncompress_contents(
            temp_dir, archive_file, args.apache_ant_url.directory_name, '/usr/local/ant')
        etc_paths_d_dir = os.path.join(pkg_dir, 'etc', 'paths.d')
        write_paths_d_entry(etc_paths_d_dir, 'org.apache.ant')
        pkg_identifier = 'org.apache.ant'
        output_pkg_path = os.path.join(
            args.output_dir, args.apache_ant_url.directory_name + '.pkg')
        make_pkg(pkg_dir, pkg_identifier, args.apache_ant_url.version, output_pkg_path)

if __name__ == '__main__':
    main()
