#!/usr/bin/env python

import tarfile
import sys

def grep_download_time(f):
    for l in f:
        if l.startswith('DOWN') or l.startswith('FINALCRE'):
            print l,

def grep_all_download_times(fnm):
    tf = tarfile.open(fnm)
    for e in tf:
        if e.isfile():
            f = tf.extractfile(e)
            grep_download_time(f)

argv = sys.argv
for fnm in argv[1:]:
    grep_all_download_times(fnm)
