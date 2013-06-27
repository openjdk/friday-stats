#!/bin/bash
# Copyright (c) 2013, Oracle and/or its affiliates. All rights reserved.
# DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
#
# This code is free software; you can redistribute it and/or modify it
# under the terms of the GNU General Public License version 2 only, as
# published by the Free Software Foundation.
#
# This code is distributed in the hope that it will be useful, but WITHOUT
# ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
# FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
# version 2 for more details (a copy is included in the LICENSE file that
# accompanied this code).
#
# You should have received a copy of the GNU General Public License version
# 2 along with this work; if not, write to the Free Software Foundation,
# Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
#
# Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
# or visit www.oracle.com if you need additional information or have any
# questions.

TMPDIR=/tmp/classvers$$
trap 'rm -rf $TMPDIR; exit 0' 0 1 2 3 15

# oneclass classfile prefix
# extracts class version byte in hex
# od output is like:
#     0000007    34                                                            
function oneclass {
    vers=$(od -t u1 -j 7 -N 1 "$1" | awk '$1 == 0000007 { print $2 }')
    echo $vers $2$1
}

# findclasses [prefix]
function findclasses {
    find . -name '*.class' -print |
    while read clname ; do
        clname=${clname#./}
        oneclass "$clname" "$1"
    done
}

# onejar jarfile
function onejar {
    mkdir $TMPDIR
    ( cd $TMPDIR
      jar -x
      findclasses "${1}!/"
    ) < $1
    rm -rf $TMPDIR
}

function classorjar {
    case "$1" in
        *.class)
            oneclass "$1"
            ;;
        *.jar)
            onejar "$1"
            ;;
    esac
}

if [ $# -eq 0 ]; then
    find . \( -name '*.class' -o -name '*.jar' \) -print |
    while read f ; do
        classorjar "${f#./}"
    done
else
    for arg in "$@" ; do
        classorjar "$arg"
    done
fi
