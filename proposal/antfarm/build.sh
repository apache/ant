#!/bin/sh

java -classpath boot/ant.jar -Dant.project.path=.:jaxp ant ant:all
