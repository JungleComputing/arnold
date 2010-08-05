#!/usr/bin/env python

import tarfile
import sys

#fnm = 'plain-12-s0-fs0-leecher.tar'

peersPerProcessor=2

def dump_plot(l):
    n = 0
    for v in l:
        print "%f %d" %(v,n)
        n += 1

def collect_all_download_times(l,fnm):
    f = open( fnm, 'r' )
    for line in f.readlines():
        if line.startswith('DOWNLOAD'):
            words = line.split()
            l.append(1e-3*int(words[1]))
    f.close()
    return l

argv = sys.argv
completion = []
for fnm in argv[1:]:
     completion = collect_all_download_times(completion, fnm)

completion.sort()
dump_plot(completion)
