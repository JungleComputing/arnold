#!/usr/bin/env python

import tarfile
import sys

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
            l.append(1e-3*int(words[2]))
    f.close()
    return l

argv = sys.argv
start = []
for fnm in argv[1:]:
     start = collect_all_download_times(start, fnm)

start.sort()
dump_plot(start)
