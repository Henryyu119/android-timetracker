#!/bin/bash

# Gradle Wrapper 脚本
# 用于在没有安装 Gradle 的情况下运行 Gradle 命令

set -e

# 默认 JVM 选项
DEFAULT_JVM_OPTS='"-Xmx64m" "-Xms64m"'

# 确定操作系统
case "$(uname)" in
  Darwin* )
    darwin=true
    ;;
  MINGW* )
    msys=true
    ;;
  * )
    darwin=false
    msys=false
    ;;
esac

# 确定项目根目录
APP_HOME="$(cd "$(dirname "$0")" && pwd -P)"

# 添加默认 JVM 选项
GRADLE_OPTS="$GRADLE_OPTS $DEFAULT_JVM_OPTS"

# 查找 Java
if [ -n "$JAVA_HOME" ] ; then
    if [ -x "$JAVA_HOME/jre/sh/java" ] ; then
        JAVACMD="$JAVA_HOME/jre/sh/java"
    else
        JAVACMD="$JAVA_HOME/bin/java"
    fi
    if [ ! -x "$JAVACMD" ] ; then
        echo "ERROR: JAVA_HOME is set to an invalid directory: $JAVA_HOME"
        exit 1
    fi
else
    JAVACMD="java"
    which java >/dev/null 2>&1 || {
        echo "ERROR: JAVA_HOME is not set and no 'java' command could be found in your PATH."
        exit 1
    }
fi

# 执行 Gradle
exec "$JAVACMD" $GRADLE_OPTS \
    -classpath "$APP_HOME/gradle/wrapper/gradle-wrapper.jar" \
    org.gradle.wrapper.GradleWrapperMain \
    "$@"
