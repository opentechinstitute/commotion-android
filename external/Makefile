
all: olsrd/olsrd

olsrd/olsrd: $(wildcard olsrd/*/*.[cho])
	make -C olsrd OS=android NDK_BASE=$(NDK_BASE) DEBUG=0 build_all

clean:
	make -C olsrd OS=android clean_all
