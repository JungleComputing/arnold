#!/usr/bin/env python

import tarfile
import sys

#fnm = 'plain-12-s0-fs0-leecher.tar'

def grep_hasError(f):
    for l in f:
        ix = l.find( 'Internal error' )
        if ix>=0:
            print l.strip()
            return True
    return False

def grep_errors(fnm):
    tf = tarfile.open(fnm)
    n = 0
    for e in tf:
        if e.isfile():
            f = tf.extractfile(e)
            hasError = grep_hasError(f) 
            if hasError:
                n += 1
    return n

argv = sys.argv
for fnm in argv[1:]:
    n = grep_errors(fnm)
    if n>0:
        if n == 1:
           p = ""
        else:
            p = "s"
        print "File '%s' has %d log%s with internal errors" % (fnm, n, p)
