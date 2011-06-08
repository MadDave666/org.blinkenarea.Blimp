#! /bin/sh

NAME=Blimp
VERSION_FILE=Makefile

VERSION=$(cat $VERSION_FILE | grep 'version [0-9]\+\.[0-9]\+\(\.[0-9]\+\)\?' | sed 's/^.*version \([0-9]\+\.[0-9]\+\(\.[0-9]\+\)\?\).*$/\1/')
DATE=$(cat $VERSION_FILE | grep 'date [0-9]\+-[0-9]\+-[0-9]\+' | sed 's/^.*date \([0-9]\+-[0-9]\+-[0-9]\+\).*$/\1/')

PACK="${NAME}-${VERSION}_${DATE}"

FILES="ChangeLog Makefile org/blinkenarea/Blimp/*.java org/blinkenarea/Blimp/images/* Blimp.mf Blimp.html mkpack.sh"

rm -rf $PACK
mkdir $PACK

for FILE in $FILES
do
  mkdir -p $(dirname $PACK/$FILE)
  cp $FILE $PACK/$FILE
done

ARCH=$PACK.tar.bz2
if [ -f $ARCH ]
then
  ARCH=$ARCH.new
fi

tar jcf $ARCH $PACK
rm -rf $PACK

