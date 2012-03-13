
all: olsrd/olsrd

olsrd/olsrd: $(wildcard olsrd/*/*.[cho])
	make -C olsrd OS=android DEBUG=0

clean:
	make -C olsrd OS=android clean
