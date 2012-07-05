
all: olsrd/olsrd olsrinfo

clean:
	make -C olsrd OS=android clean_all
	rm -f -- $(OLSRINFO_JAR)

.PHONY: olsrinfo
.SUFFIXES: .java .class

#--olsrd-----------------------------------------------------------------------#

OLSRD_SOURCE := $(wildcard olsrd/*/*.[cho]) \
		$(wildcard olsrd/*/*/*.[cho])

olsrd/olsrd: $(OLSRD_SOURCE)
	make -C olsrd OS=android NDK_BASE=$(NDK_BASE) DEBUG=0 all
# build only the plugins we are likely to use to keep the build stable
	make -C olsrd OS=android NDK_BASE=$(NDK_BASE) DEBUG=0 arprefresh
	make -C olsrd OS=android NDK_BASE=$(NDK_BASE) DEBUG=0 bmf
	make -C olsrd OS=android NDK_BASE=$(NDK_BASE) DEBUG=0 dot_draw
	make -C olsrd OS=android NDK_BASE=$(NDK_BASE) DEBUG=0 dyn_gw_plain
	make -C olsrd OS=android NDK_BASE=$(NDK_BASE) DEBUG=0 httpinfo
	make -C olsrd OS=android NDK_BASE=$(NDK_BASE) DEBUG=0 jsoninfo
	make -C olsrd OS=android NDK_BASE=$(NDK_BASE) DEBUG=0 nameservice
	make -C olsrd OS=android NDK_BASE=$(NDK_BASE) DEBUG=0 pgraph
	make -C olsrd OS=android NDK_BASE=$(NDK_BASE) DEBUG=0 secure
	make -C olsrd OS=android NDK_BASE=$(NDK_BASE) DEBUG=0 txtinfo
	make -C olsrd OS=android NDK_BASE=$(NDK_BASE) DEBUG=0 watchdog

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
