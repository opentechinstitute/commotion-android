
CWD = $(shell pwd)
PROJECT_ROOT = $(CWD)/..
EXTERNAL_ROOT = $(PROJECT_ROOT)/external
ASSETS = $(PROJECT_ROOT)/assets


# Android SDK setup for shellfork
SDK_BASE ?= /opt/android-sdk
SDK_PLATFORM_LEVEL ?= 9
SDK_PLATFORM=$(SDK_BASE)/platforms/android-$(SDK_PLATFORM_LEVEL)/


# Android NDK setup

# Android now has 64-bit and 32-bit versions of the NDK for GNU/Linux.  We
# assume that the build platform uses the appropriate version, otherwise the
# user building this will have to manually set NDK_PROCESSOR or NDK_TOOLCHAIN.
CPU := $(shell uname -m)
ifeq ($(CPU),x86_64)
 NDK_PROCESSOR=x86_64
else
 NDK_PROCESSOR=x86
endif

NDK_BASE ?= /opt/android-ndk
NDK_PLATFORM_LEVEL ?= 9
NDK_ABI=arm
NDK_COMPILER_VERSION=4.6

NDK_SYSROOT=$(NDK_BASE)/platforms/android-$(NDK_PLATFORM_LEVEL)/arch-$(NDK_ABI)
NDK_UNAME := $(shell uname -s | tr '[A-Z]' '[a-z]')
NDK_TOOLCHAIN = $(NDK_ABI)-linux-androideabi-$(NDK_COMPILER_VERSION)
NDK_TOOLCHAIN_BASE=$(NDK_BASE)/toolchains/$(NDK_TOOLCHAIN)/prebuilt/$(NDK_UNAME)-$(NDK_PROCESSOR)

# to use the real HOST tag, you need the latest libtool files:
# http://stackoverflow.com/questions/4594736/configure-does-not-recognize-androideabi
HOST := arm-linux-androideabi

# install root for built files
DESTDIR = $(CWD)
# TODO try adding the Android-style /data/app.name here
prefix = /data/data/info.guardianproject.lildebi/app_opt
LOCAL := $(DESTDIR)$(prefix)

PATH := ${PATH}:$(NDK_TOOLCHAIN_BASE)/bin:$(LOCAL)/bin

CC := $(NDK_TOOLCHAIN_BASE)/bin/arm-linux-androideabi-gcc --sysroot=$(NDK_SYSROOT)
CXX := $(NDK_TOOLCHAIN_BASE)/bin/arm-linux-androideabi-g++
CPP := $(NDK_TOOLCHAIN_BASE)/bin/arm-linux-androideabi-cpp
LD := $(NDK_TOOLCHAIN_BASE)/bin/arm-linux-androideabi-ld
AR := $(NDK_TOOLCHAIN_BASE)/bin/arm-linux-androideabi-ar
RANLIB := $(NDK_TOOLCHAIN_BASE)/bin/arm-linux-androideabi-ranlib
STRIP := $(NDK_TOOLCHAIN_BASE)/bin/arm-linux-androideabi-strip \
	--strip-unneeded -R .note -R .comment

ALL_CFLAGS = -DANDROID -I$(LOCAL)/include
ALL_LDFLAGS = -L$(LOCAL)/lib -Wl,--rpath,$(LOCAL)/lib

# build as small as possible, mostly useful for static binaries
ALL_CFLAGS += -fdata-sections -ffunction-sections -Os
ALL_LDFLAGS += -Wl,--gc-sections

all: olsrd busybox shell-fork

clean: olsrd-clean olsrinfo-clean busybox-clean shell-fork-clean

.PHONY: shell-fork olsrd olsrd-clean olsrinfo olsrinfo-clean busybox busybox-clean shell-fork-clean
.SUFFIXES: .java .class

#--olsrd-----------------------------------------------------------------------#

OLSRD_SOURCE := $(wildcard olsrd/[*/*.[ch]) \
		$(wildcard olsrd/*/*/*.[ch])

# the meaning of NDK_TOOLCHAIN has changed, since it was added as an official
# env var to the Android NDK build system.  the olsrd still has the old version,
# which we are not calling NDK_TOOLCHAIN_BASE
OLSRD_NDK_FLAGS = 	NDK_BASE="$(NDK_BASE)" \
					NDK_ABI="$(NDK_ABI)" \
					NDK_PLATFORM_LEVEL="$(NDK_PLATFORM_LEVEL)" \
					NDK_COMPILER_VERSION="$(NDK_COMPILER_VERSION)" \
					NDK_TOOLCHAIN="$(NDK_TOOLCHAIN_BASE)" \
					NDK_SYSROOT="$(NDK_SYSROOT)" \
					NDK_UNAME="$(NDK_UNAME)" \
					HOST="$(HOST)" \
					CC="$(CC)" \
					LD="$(LD)" \
					AR="$(AR)" \
					STRIP="$(STRIP)"

olsrd: olsrd-build-stamp

olsrd-build-stamp: $(OLSRD_SOURCE)
	make -C olsrd OS=android $(OLSRD_NDK_FLAGS) DEBUG=0 VERBOSE=1 all
# build only the plugins we are likely to use to keep the build stable
	make -C olsrd OS=android $(OLSRD_NDK_FLAGS) DEBUG=0 arprefresh
	make -C olsrd OS=android $(OLSRD_NDK_FLAGS) DEBUG=0 bmf
	make -C olsrd OS=android $(OLSRD_NDK_FLAGS) DEBUG=0 dot_draw
	make -C olsrd OS=android $(OLSRD_NDK_FLAGS) DEBUG=0 dyn_gw_plain
	make -C olsrd OS=android $(OLSRD_NDK_FLAGS) DEBUG=0 httpinfo
	make -C olsrd OS=android $(OLSRD_NDK_FLAGS) DEBUG=0 jsoninfo
	make -C olsrd OS=android $(OLSRD_NDK_FLAGS) DEBUG=0 nameservice
	make -C olsrd OS=android $(OLSRD_NDK_FLAGS) DEBUG=0 pgraph
	make -C olsrd OS=android $(OLSRD_NDK_FLAGS) DEBUG=0 secure
	make -C olsrd OS=android $(OLSRD_NDK_FLAGS) DEBUG=0 txtinfo
	make -C olsrd OS=android $(OLSRD_NDK_FLAGS) DEBUG=0 watchdog
	touch olsrd-build-stamp

olsrd-clean:
	make -C olsrd OS=android clean_all
	rm -f olsrd-build-stamp

#--olsrinfo--------------------------------------------------------------------#

OLSRINFO_CLASSPATH := src:libs/jackson-mapper-asl-1.9.7.jar:libs/jackson-core-asl-1.9.7.jar
OLSRINFO_SOURCE := \
	$(wildcard olsrinfo/src/net/commotionwireless/olsrinfo/*.java) \
	$(wildcard olsrinfo/src/net/commotionwireless/olsrinfo/datatypes/*.java)
OLSRINFO_CLASSES := $(OLSRINFO_SOURCE:%.java=%.class)
OLSRINFO_JAR = ../libs/olsrinfo.jar

olsrinfo: $(OLSRINFO_JAR)

$(OLSRINFO_JAR): $(OLSRINFO_CLASSES)
	jar cvf $(OLSRINFO_JAR) -C olsrinfo/bin .

$(OLSRINFO_CLASSES): $(OLSRINFO_SOURCE)

olsrinfo/bin:
	install -d olsrinfo/bin

%.class: %.java olsrinfo/bin
	cd olsrinfo && javac -g \
		-source 1.6 -target 1.6 \
		-d bin \
		-classpath $(OLSRINFO_CLASSPATH) \
		$(<:olsrinfo/%=%)


#--busybox--------------------------------------------------------------------#

busybox: $(EXTERNAL_ROOT)/busybox/busybox

$(EXTERNAL_ROOT)/busybox/.config: $(EXTERNAL_ROOT)/busybox_config
	sed s/^CONFIG_SYSROOT.*$$/CONFIG_SYSROOT=\"$(subst /,\\/,$(NDK_SYSROOT))\"/ busybox_config > $(EXTERNAL_ROOT)/busybox/.config

$(EXTERNAL_ROOT)/busybox/busybox: $(EXTERNAL_ROOT)/busybox/.config
	make -C $(EXTERNAL_ROOT)/busybox/

busybox-clean:
	rm -f -- $(EXTERNAL_ROOT)/busybox/.config
	rm -f -- $(EXTERNAL_ROOT)/busybox/busybox
	make -C $(EXTERNAL_ROOT)/busybox/ clean

FORK_SOURCE := $(wildcard shell-fork/*.[ch])
SHELL_JAVA_SOURCE := $(wildcard shell-fork/src/net/net.commotionwireless.shell/*.java)
shell-fork: fork-timestamp shell-jar-timestamp

fork-timestamp: $(FORK_SOURCE)
	make -C shell-fork/ OS=android fork

shell-jar-timestamp: $(SHELL_JAVA_SOURCE)
	ant -Dbasedir=$(EXTERNAL_ROOT)/shell-fork -f $(EXTERNAL_ROOT)/shell-fork/build.xml \
		-DandroidJarFilePath="$(SDK_PLATFORM)" -DjunitJarFile=/usr/share/java/junit4.jar \
		jar

shell-fork-clean:
	make -C shell-fork/ clean
	ant -Dbasedir=$(EXTERNAL_ROOT)/shell-fork -f $(EXTERNAL_ROOT)/shell-fork/build.xml clean

