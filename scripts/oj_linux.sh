#!/bin/sh

## uncomment and put in the path where oj log, settings should end up
## if unset defaults to
##   JUMP_HOME (oj app folder) if writable or $HOME/.openjump (user home)
#JUMP_SETTINGS="/tmp/foobar"

## uncomment and put the path to your jre here
#JAVA_HOME="/home/ed/jre1.6.0_21"

## uncomment and change your memory configuration here 
## Xms is initial size, Xmx is maximum size
## values are ##M for ## Megabytes, ##G for ## Gigabytes
#JAVA_MAXMEM="-Xmx512M"

## uncomment and change your language/country here 
## to overwrite OS default locale setting
#JAVA_LANG="-Duser.language=de -Duser.country=DE"

## set some defaults (as macosx.command uses this script, it might define other defaults)
MAIN="com.vividsolutions.jump.workbench.JUMPWorkbench"
JAVA_SAXDRIVER=${JAVA_SAXDRIVER-org.apache.xerces.parsers.SAXParser}
JAVA_LOOKANDFEEL=${JAVA_LOOKANDFEEL-javax.swing.plaf.metal.MetalLookAndFeel}
JAVA_MAXMEM=${JAVA_MAXMEM--Xmx512M}

## end function, delays closing of terminal
end(){
  # show error for some time to prohibit window closing on X
  if [ "$ERROR" != "0" ]; then
    sleep 15
    exit 1
  else
    exit 0
  fi
}

postinstall(){
  [ ! -d "$1" ] && echo Missing app folder && exit 1
  # fix permissions
  find "$1" -type f -exec chmod 644 {} \; &&\
  find "$1" -type d -exec chmod 755 {} \; &&\
  find "$1" -type f \( -name \*.sh -o -name \*.command -o -name script -o -name OpenJUMP \) -print -exec chmod 755 {} \; &&\
  echo permissions fixed
  which xdg-desktop-menu && xdg-desktop-menu forceupdate && echo reloaded desktop
}

macinstall(){
  # create app package
  cp -R -a "$1"/bin/OpenJUMP.app/Contents "$1" &&\
  awk '{sub(/..\/oj_/,"bin/oj_",$0)}1' "$1"/bin/OpenJUMP.app/Contents/Resources/script > "$1"/Contents/Resources/script &&\
  echo patched oj.app
  # do postinstall
  postinstall "$1"
}

## detect home folder
if(test -L "$0") then
  auxlink=`ls -l "$0" | sed 's/^[^>]*-> //g'`
  JUMP_HOME=`dirname "$auxlink"`/..
else 
  JUMP_HOME=`dirname "$0"`/..
fi

## run postinstalls only, if requested
case "$1" in
 --post-install)
  postinstall "$JUMP_HOME" 2>&1 | tee "$JUMP_HOME"/postinstall.log
  exit
 ;;
 --mac-install) 
  macinstall "$JUMP_HOME" 2>&1 | tee "$JUMP_HOME"/macinstall.log
  exit
 ;;
esac

## cd into jump home
OLD_DIR=`pwd`
cd "$JUMP_HOME"

## determine where to place settings, if no path given
[ -z "$JUMP_SETTINGS" ] && \
JUMP_SETTINGS="$JUMP_HOME"; \
if [ -d "$JUMP_SETTINGS" ]; then
  if [ ! -w "$JUMP_SETTINGS" ]; then
    # try users home dir
    JUMP_SETTINGS="$HOME/.openjump"
    # create if missing
    [ ! -e "$JUMP_SETTINGS" ] && mkdir "$JUMP_SETTINGS"
    # try to make it writable
    [ ! -w "$JUMP_SETTINGS" ] && chmod u+wX -R "$JUMP_SETTINGS"
    # check availability and issue warning in case
  fi
fi
[ ! -d "$JUMP_SETTINGS" ] || [ ! -w "$JUMP_SETTINGS" ] && \
  echo "Warning: Cannot access settings folder '$JUMP_SETTINGS' for writing."

## search java, order is:
# 1. first in oj_home/jre
# 2. in configured java_home
# 3. in path
if [ -f "$JUMP_HOME/jre/bin/java" ]; then
  JAVA="$JUMP_HOME/jre/bin/java"
# is there a jre defined by env var?
elif [ -n "$JAVA_HOME" ]; then
  JAVA=$JAVA_HOME/bin/java
# well, let's look what we've got in the path
else
  JAVA=`which java`
fi

# java available
[ -z "$JAVA" ] && \
 echo "Couldn't find java in your PATH ($PATH). Please install java or 
add the location of java to your PATH environment variable." && ERROR=1 && end

# resolve recursive links to java binary
relPath(){ echo $1 | awk '/^\//{exit 1}'; }
relPath "$JAVA" && JAVA="$(pwd)/$JAVA"
while [ -L "${JAVA}" ]; do
  JDIR=$(dirname "$JAVA")
  JAVA=$(readlink -n "${JAVA}")
  relPath "$JAVA" && JAVA="${JDIR}/${JAVA}"
done
# java executable file?
[ ! -x "$JAVA" ] && \
 echo "The found java binary '$JAVA' is no executable file." && ERROR=1 && end

# java version check
JAVA_VERSION=$("$JAVA" -version 2>&1 | awk -F'"' '/^java version/{print $2}' | awk -F'.' '{print $1"."$2}')
JAVA_NEEDED="1.5"
if ! awk "BEGIN{if($JAVA_VERSION < $JAVA_NEEDED)exit 1}"; then
  echo "Your java version '$JAVA_VERSION' is insufficient to run openjump.
Please provide an at least version '$JAVA_NEEDED' java runtime."
  ERROR=1
fi

# always print java infos
echo "Running -> '${JAVA}'; " $("$JAVA" -version 2>&1|awk 'BEGIN{ORS=""}{print $0"; "}')

JUMP_PROFILE=~/.jump/openjump.profile
if [ -f "$JUMP_PROFILE" ]; then
  source $JUMP_PROFILE
fi

# setup some lib paths
if [ -z "$JUMP_LIB" ]; then
  JUMP_LIB="./lib"
fi
JUMP_NATIVE_DIR="$JUMP_LIB/native"
JUMP_PLUGIN_DIR="${JUMP_PLUGIN_DIR:=$JUMP_LIB/ext}"

JUMP_PLUGINS=./bin/default-plugins.xml
if [ -z "$JUMP_PLUGINS" ] || [ ! -f "$JUMP_PLUGINS" ]; then
  JUMP_PLUGINS="./bin/default-plugins.xml"
  if [ ! -f "$JUMP_PLUGINS" ]; then
    JUMP_PLUGINS="./scripts/default-plugins.xml"
  fi
fi

# include every jar/zip in lib and native dir
for libfile in "$JUMP_LIB/"*.zip "$JUMP_LIB/"*.jar "$JUMP_NATIVE_DIR/"*.jar
do
  CLASSPATH="$libfile":"$CLASSPATH";
done
CLASSPATH=.:./bin:./conf:$CLASSPATH
export CLASSPATH;

## compile jump opts
#
JUMP_OPTS="-plug-in-directory $JUMP_PLUGIN_DIR"
if [ -f "$JUMP_PLUGINS" ]; then
  JUMP_OPTS="$JUMP_OPTS -default-plugins $JUMP_PLUGINS"
fi
# workbench-properties.xml is used to manually load plugins (ISA uses this)
JUMP_PROPERTIES=./bin/workbench-properties.xml
if [ -n "$JUMP_PROPERTIES" ] && [ -f "$JUMP_PROPERTIES" ]; then
  JUMP_OPTS="$JUMP_OPTS -properties $JUMP_PROPERTIES"
fi

# compile jre opts, respect already set ones from e.g. mac
JAVA_OPTS=""
JAVA_OPTS="$JAVA_OPTS $JAVA_MAXMEM $JAVA_LANG"
JAVA_OPTS="$JAVA_OPTS -Djump.home=."
[ -n "JAVA_SAXDRIVER"    ] && JAVA_OPTS="$JAVA_OPTS -Dorg.xml.sax.driver=$JAVA_SAXDRIVER"
[ -n "$JAVA_LOOKANDFEEL" ] && JAVA_OPTS="$JAVA_OPTS -Dswing.defaultlaf=$JAVA_LOOKANDFEEL"
JAVA_OPTS="$JAVA_OPTS $JAVA_OPTS_OVERRIDE"

# extract zipped files in native dir (our way to ship symlinks to desktops)
for file in "$JUMP_NATIVE_DIR/"*.tar.gz "$JUMP_NATIVE_DIR/"*.tgz
do
  file=$(basename "$file")
  done=".$file.unzipped"
  # we create a marker file symbolizing previous successful extraction
  ( cd "$JUMP_NATIVE_DIR/"; [ -f "$file" ] && [ ! -f "$done" ] && tar -xvf "$file" && touch "$done" );
done

# allow jre to find native libraries in native dir, lib/ext (backwards compatibility)
# NOTE: mac osx DYLD_LIBRARY_PATH is set in oj_macosx.command only
export LD_LIBRARY_PATH="$LD_LIBRARY_PATH:$JUMP_NATIVE_DIR:$JUMP_HOME/lib/ext"
# allow jre to find binaries located under the native folder
export PATH="$JUMP_NATIVE_DIR:$PATH"

# try to start if no errors so far
if [ -z "$ERROR" ]; then
  # log.dir needs a trailing slash for path concatenation in log4j.xml
  "$JAVA" -cp "$CLASSPATH" -Dlog.dir="$JUMP_SETTINGS/" $JAVA_OPTS $MAIN -state "$JUMP_SETTINGS/" $JUMP_OPTS "$@"
  # result of jre call
  ERROR=$?
fi

## return to old working dir
cd "$OLD_DIR"

## run end function
end
