
all: olsrd/olsrd

olsrd/olsrd: $(wildcard olsrd/*/*.[cho])
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

clean:
	make -C olsrd OS=android clean_all
