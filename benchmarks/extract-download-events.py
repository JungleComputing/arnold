#!/usr/bin/env python

import tarfile
import sys

#fnm = 'plain-12-s0-fs0-leecher.tar'

def contains(s,pat):
    ix = s.find(pat)
    return ix>=0

def grep_download_event(f):
    for l in f:
        if contains(l,'STARTDOWNLOAD') or contains(l,'CANCELEDDOWNLOAD') or contains(l,'COMPLETEDDOWNLOAD') or contains(l,'FAILEDDOWNLOAD'):
            print l.strip()

def grep_all_download_events(fnm):
    tf = tarfile.open(fnm)
    for e in tf:
        if e.isfile():
            f = tf.extractfile(e)
            grep_download_event(f)

argv = sys.argv
for fnm in argv[1:]:
    grep_all_download_events(fnm)
