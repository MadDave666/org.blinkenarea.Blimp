# BlinkenLightsInteractiveMovieProgram
# version 1.3 date 2006-10-10
# Copyright (C) 2004-2006: Stefan Schuermans <1stein@schuermans.info>
# Copyleft: GNU public license - http://www.gnu.org/copyleft/gpl.html
# a blinkenarea.org project
# powered by eventphone.de

BLINKEN_LIB=../BlinkenLibJava/BlinkenLib.jar

JAVAC=javac
JAR=jar
KEYTOOL=keytool
JARSIGNER=jarsigner
KEYPASS=BlinkenLightsInteractiveMovieProgram
JAVA=java
P=org/blinkenarea/Blimp
IMG=$(P)/images

CLASSPATH=.:$(BLINKEN_LIB)
CLASS_FILES=$(P)/BlinkenFileFilter.class $(P)/BlinkenFrameDisplay.class \
            $(P)/BlinkenFrameDisplayListener.class $(P)/BlinkenFrameDisplayInterceptor.class \
            $(P)/BlinkenFrameEditor.class $(P)/BlinkenFrameEditorListener.class \
            $(P)/Blimp.class

IMAGE_FILES=$(IMG)/ColorPicker.png $(IMG)/Dot.png $(IMG)/Line.png \
            $(IMG)/Rectangle.png $(IMG)/FilledRectangle.png \
            $(IMG)/Circle.png $(IMG)/FilledCircle.png \
            $(IMG)/Copy.png $(IMG)/Paste.png \
            $(IMG)/Invert.png $(IMG)/MirrorHor.png $(IMG)/RollLeft.png \
            $(IMG)/Rotate90.png $(IMG)/MirrorVer.png $(IMG)/RollRight.png \
            $(IMG)/Rotate180.png $(IMG)/MirrorDiag.png $(IMG)/RollUp.png \
            $(IMG)/Rotate270.png $(IMG)/MirrorDiag2.png $(IMG)/RollDown.png \
            $(IMG)/Undo.png $(IMG)/Redo.png \
            $(IMG)/InsertFrame.png $(IMG)/DuplicateFrame.png $(IMG)/DeleteFrame.png

.phony: all clean jar run

all: jar

clean:
	rm -f $(CLASS_FILES) Blimp.jar

jar: Blimp.jar

run: Blimp.jar
	$(JAVA) -jar Blimp.jar

%.class: %.java
	$(JAVAC) -classpath $(CLASSPATH) $<

Blimp.keystore:
	$(KEYTOOL) -genkey -alias Blimp -dname CN=Blimp,O=blinkenarea,C=org -keypass $(KEYPASS) -keystore Blimp.keystore -storepass $(KEYPASS) -validity 3652

Blimp.jar: Blimp.mf Blimp.keystore $(CLASS_FILES) $(IMAGE_FILES)
	$(JAR) cmf Blimp.mf Blimp.jar $(CLASS_FILES) $(IMAGE_FILES)
	rm -rf jar.tmp
	mkdir jar.tmp
	cat $(BLINKEN_LIB) | ( cd jar.tmp ; $(JAR) x )
	rm -rf jar.tmp/META-INF
	$(JAR) uf Blimp.jar -C jar.tmp .
	rm -rf jar.tmp
	$(JARSIGNER) -keystore Blimp.keystore -storepass $(KEYPASS) Blimp.jar Blimp

