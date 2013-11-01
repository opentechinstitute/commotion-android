#!/bin/bash

function usage {
	cat << END_OF_USAGE
$0 [clean]
Use clean parameter to "reverse" the patches.
END_OF_USAGE
}

function check_exit {
	exit_value=$1
	clean_value=$2

	if [ $exit_value -eq 1 -a $clean_value -ne 1 ]; then
		echo "Failed to patch ..."
		exit 1
	fi
}

reverse="-N"
clean=0
if [ $# == 1 ]; then
	reverse_parameter=$(tr '[:upper:]' '[:lower:]' <<<$1)
	if [[ $reverse_parameter =~ clean ]]; then
		reverse+="R"
		clean=1
	else
		usage
		exit 1
	fi
fi

echo $reverse

cd external/serval-dna/
patch $reverse -p1 < ../../patches/Android.mk.serval-dna.patch 
check_exit $? $clean

patch $reverse -p1 < ../../patches/commandline.c.serval-dna.patch 
check_exit $? $clean

patch $reverse -p1 < ../../patches/servalwrap.c.serval-dna.patch
check_exit $? $clean
cd -
exit 0
