
include $(all-subdir-makefiles)

# redirect stdout to stderr otherwise make eats it
MAKE_EXTERNAL_LOG := $(shell make -C external 1>&2)
