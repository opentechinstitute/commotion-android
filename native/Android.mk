
include $(all-subdir-makefiles)

# redirect stdout to stderr otherwise make eats it
# TODO this also runs when 'ndk-build clean' is called...
#MAKE_EXTERNAL_LOG := $(shell make -C external 1>&2)
