#!/bin/sh

if test -d boot ; then
    rm -rf boot
fi

if test -d temp ; then
    rm -rf temp
fi

if test -d dist ; then
    rm -rf dist
fi
