# Copyright (c) 2021, Alibaba Group Holding Limited. All Rights Reserved.
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

"""
1. Find hot files which have many commit messages, output looks like:

269   2021-04-14      src/hotspot/share/gc/g1/g1CollectedHeap.cpp                                             
217   2021-03-30      src/hotspot/share/gc/shenandoah/shenandoahHeap.cpp                                      
134   2021-04-12      src/hotspot/share/gc/g1/g1CollectedHeap.hpp                                             
127   2021-04-06      src/hotspot/share/gc/g1/g1ConcurrentMark.cpp                                            
99    2021-04-14      src/hotspot/share/gc/shared/genCollectedHeap.cpp                                        
95    2021-04-15      src/hotspot/share/gc/parallel/psParallelCompact.cpp                                     
...

2. Sort files by last commit date, output looks like:
17    2021-04-15      src/hotspot/share/services/diagnosticCommand.hpp                                        
64    2021-04-15      src/hotspot/share/prims/jvmtiExport.cpp                                                 
79    2021-04-15      src/hotspot/share/classfile/classLoader.cpp                                             
16    2021-04-15      src/hotspot/share/gc/g1/g1FullGCPrepareTask.cpp                                         
17    2021-04-15      src/hotspot/share/gc/g1/g1FullGCMarker.inline.hpp                                       
19    2021-04-15      src/hotspot/share/jvmci/compilerRuntime.cpp                                             
...
"""
import hashlib
import json
import os
import sys
import threading

g = []


def analysis(files):
    for file in files:
        result = os.popen(f"git --no-pager log --pretty=format:'%ad' --date=short {file}").read()
        nof_commit = len(result.splitlines())
        lst_mod = result.splitlines()[0]
        g.append({"file": file, "nof_commit": nof_commit, "lst_mod": lst_mod})


def save_array_to_file(arr, file_name):
    with open(file_name, 'w') as f:
        for item in arr:
            f.write("%-5s %-15s %-88s\n" % (item['nof_commit'], item['lst_mod'], item['file']))


def digest(text):
    return hashlib.md5(text.encode('utf-8')).hexdigest()


def find_interesting(use_cache):
    save_prefix = digest(sys.argv[1])

    if not use_cache:
        with open(save_prefix + ".json", 'w') as outfile:
            json.dump(g, outfile)
    hot_files = sorted(g, key=lambda k: k['nof_commit'], reverse=True)
    save_array_to_file(hot_files, save_prefix + "hot_files.log")

    hot_compiler_files = [f for f in hot_files
                          if 'opto/' in f['file'] or
                          'c1/' in f['file'] or
                          'code/' in f['file'] or
                          'jvmci/' in f['file'] or
                          'ci/' in f['file']]
    save_array_to_file(hot_compiler_files, save_prefix + "hot_compiler_files.log")

    hot_gc_files = [f for f in hot_files
                    if 'gc/' in f['file']]
    save_array_to_file(hot_gc_files, save_prefix + "hot_gc_files.log")

    lst_mod = sorted(g, key=lambda k: k['lst_mod'], reverse=True)
    save_array_to_file(lst_mod, save_prefix + "last_modify.log")


def main():
    if len(sys.argv) != 2 or len(sys.argv[1]) == 0:
        print(f"Usage: python {__file__} JDK_SOURCE_DIR")
        return 

    # try loading data from cached file, otherwise start threads to analyze
    save_prefix = digest(sys.argv[1])
    use_cache = False
    if os.path.isfile(save_prefix + ".json"):
        with open(save_prefix + ".json", 'r') as outfile:
            global g
            g = json.load(outfile)
        print(f"Using cached {save_prefix}.json file")
        use_cache = True
    else:
        # change working directory to given path
        old_cwd = os.getcwd()
        os.chdir(os.path.expanduser(sys.argv[1]))
        # split files into each chunk
        files = []
        for (path, name, filenames) in os.walk("src/hotspot"):
            files += [os.path.join(path, file) for file in filenames]
        chunks = [files[i:i + 100] for i in range(0, len(files), 100)]
        # do analysis based on each chunk
        threads = []
        for chunk in chunks:
            t = threading.Thread(target=analysis, args=(chunk,))
            t.start()
            threads.append(t)

        print(f"Analysing... [thread:{len(threads)}]")
        for t in threads:
            t.join()

        # restore old path and save analyzed data into it
        os.chdir(old_cwd)

    find_interesting(use_cache)

    print("Analysis done!")


if __name__ == '__main__':
    main()

