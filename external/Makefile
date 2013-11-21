
CWD = $(shell pwd)
PROJECT_ROOT = $(CWD)/..
EXTERNAL_ROOT = $(PROJECT_ROOT)/external
ASSETS = $(PROJECT_ROOT)/assets

# Read README in base directory for information
# about each of these variables and how to
# customize them.
SDK_BASE ?= /opt/android-sdk
NDK_BASE ?=  /opt/android-ndk
TOOLCHAIN_ARCHITECTURE ?= x86
NDK_PLATFORM_LEVEL ?= 9
SDK_PLATFORM_LEVEL ?= 4.2.2
NDK_SYSROOT=$(NDK_BASE)/platforms/android-$(NDK_PLATFORM_LEVEL)/arch-arm
SDK_PLATFORM=$(SDK_BASE)/platforms/android-$(SDK_PLATFORM_LEVEL)/
NDK_UNAME := $(shell uname -s | tr '[A-Z]' '[a-z]')
NDK_TOOLCHAIN=$(NDK_BASE)/toolchains/arm-linux-androideabi-4.4.3/prebuilt/$(NDK_UNAME)-$(TOOLCHAIN_ARCHITECTURE)

# to use the real HOST tag, you need the latest libtool files:
# http://stackoverflow.com/questions/4594736/configure-does-not-recognize-androideabi
HOST := arm-linux-androideabi

# install root for built files
DESTDIR = $(CWD)
# TODO try adding the Android-style /data/app.name here
prefix = /data/data/info.guardianproject.lildebi/app_opt
LOCAL := $(DESTDIR)$(prefix)

PATH := ${PATH}:$(NDK_TOOLCHAIN)/bin:$(LOCAL)/bin

CC := $(NDK_TOOLCHAIN)/bin/arm-linux-androideabi-gcc --sysroot=$(NDK_SYSROOT)
CXX := $(NDK_TOOLCHAIN)/bin/arm-linux-androideabi-g++
CPP := $(NDK_TOOLCHAIN)/bin/arm-linux-androideabi-cpp
LD := $(NDK_TOOLCHAIN)/bin/arm-linux-androideabi-ld
AR := $(NDK_TOOLCHAIN)/bin/arm-linux-androideabi-ar
RANLIB := $(NDK_TOOLCHAIN)/bin/arm-linux-androideabi-ranlib
STRIP := $(NDK_TOOLCHAIN)/bin/arm-linux-androideabi-strip \
	--strip-unneeded -R .note -R .comment

ALL_CFLAGS = -DANDROID -I$(LOCAL)/include
ALL_LDFLAGS = -L$(LOCAL)/lib -Wl,--rpath,$(LOCAL)/lib

# build as small as possible, mostly useful for static binaries
ALL_CFLAGS += -fdata-sections -ffunction-sections -Os
ALL_LDFLAGS += -Wl,--gc-sections

all: olsrd busybox

clean: olsrd-clean olsrinfo-clean busybox-clean

.PHONY: olsrd olsrd-clean olsrinfo olsrinfo-clean busybox busybox-clean
.SUFFIXES: .java .class

#--olsrd-----------------------------------------------------------------------#

OLSRD_SOURCE := $(wildcard olsrd/[*/*.[ch]) \
		$(wildcard olsrd/*/*/*.[ch])

olsrd: olsrd-build-stamp

olsrd-build-stamp: $(OLSRD_SOURCE)
	make -C olsrd OS=android NO_DEBUG_MESSAGES=1 JNI_MAIN=1 NDK_BASE=$(NDK_BASE) TOOLCHAIN_ARCHITECTURE=$(TOOLCHAIN_ARCHITECTURE) libolsrd.so DEBUG=0
	make -C olsrd OS=android NO_DEBUG_MESSAGES=1 JNI_MAIN=1 NDK_BASE=$(NDK_BASE) TOOLCHAIN_ARCHITECTURE=$(TOOLCHAIN_ARCHITECTURE) libjniolsrd.so DEBUG=0
# build only the plugins we are likely to use to keep the build stable
	make -C olsrd OS=android NDK_BASE=$(NDK_BASE) TOOLCHAIN_ARCHITECTURE=$(TOOLCHAIN_ARCHITECTURE) DEBUG=0 arprefresh
	make -C olsrd OS=android NDK_BASE=$(NDK_BASE) TOOLCHAIN_ARCHITECTURE=$(TOOLCHAIN_ARCHITECTURE) DEBUG=0 bmf
	make -C olsrd OS=android NDK_BASE=$(NDK_BASE) TOOLCHAIN_ARCHITECTURE=$(TOOLCHAIN_ARCHITECTURE) DEBUG=0 dot_draw
	make -C olsrd OS=android NDK_BASE=$(NDK_BASE) TOOLCHAIN_ARCHITECTURE=$(TOOLCHAIN_ARCHITECTURE) DEBUG=0 dyn_gw_plain
	make -C olsrd OS=android NDK_BASE=$(NDK_BASE) TOOLCHAIN_ARCHITECTURE=$(TOOLCHAIN_ARCHITECTURE) DEBUG=0 httpinfo
	make -C olsrd OS=android NDK_BASE=$(NDK_BASE) TOOLCHAIN_ARCHITECTURE=$(TOOLCHAIN_ARCHITECTURE) DEBUG=0 jsoninfo
	make -C olsrd OS=android NDK_BASE=$(NDK_BASE) TOOLCHAIN_ARCHITECTURE=$(TOOLCHAIN_ARCHITECTURE) DEBUG=0 nameservice
	make -C olsrd OS=android NDK_BASE=$(NDK_BASE) TOOLCHAIN_ARCHITECTURE=$(TOOLCHAIN_ARCHITECTURE) DEBUG=0 pgraph
	make -C olsrd OS=android NDK_BASE=$(NDK_BASE) TOOLCHAIN_ARCHITECTURE=$(TOOLCHAIN_ARCHITECTURE) DEBUG=0 secure
	make -C olsrd OS=android NDK_BASE=$(NDK_BASE) TOOLCHAIN_ARCHITECTURE=$(TOOLCHAIN_ARCHITECTURE) DEBUG=0 txtinfo
	make -C olsrd OS=android NDK_BASE=$(NDK_BASE) TOOLCHAIN_ARCHITECTURE=$(TOOLCHAIN_ARCHITECTURE) DEBUG=0 watchdog
	make -C olsrd OS=android NDK_BASE=$(NDK_BASE) TOOLCHAIN_ARCHITECTURE=$(TOOLCHAIN_ARCHITECTURE) DEBUG=0 stdout
	make -C olsrd OS=android NDK_BASE=$(NDK_BASE) TOOLCHAIN_ARCHITECTURE=$(TOOLCHAIN_ARCHITECTURE) CFLAGS="-DHAVE_BZERO -DHAVE_BCMP -DHAVE_BCOPY" SERVALD_ROOT=$(EXTERNAL_ROOT)/serval-dna/ SERVALD_LIB_ROOT=$(PROJECT_ROOT)/libs/armeabi/ DEBUG=0 mdp
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
