#!/usr/bin/env python

import tarfile
import sys

#fnm = 'plain-12-s0-fs0-leecher.tar'

peersPerProcessor=2

def grep_credit(f):
    for l in f:
        if l.startswith('FINALCREDI'):
            words = l.split()
            return float( words[1] )
    return None

def grep_all_credits(lbl,fnm):
    tf = tarfile.open(fnm)
    for e in tf:
        if e.isfile():
            f = tf.extractfile(e)
            t = grep_credit(f)
            if t != None:
                print lbl, t
            else:
                print "File '%s' contains no credit" % e.name

def extract_label(mode,fnm):
    cl = fnm.split('-')
    if( mode == 'plain' or mode == 'credit' or mode == 'onetrack'):
        return peersPerProcessor*int(cl[1])
    if( mode == 'proxy' ):
        s = cl[2]
        return int(s[1:])
    print 'Unknown mode ' + mode
    sys.exit(1)

argv = sys.argv
mode = argv[1]
for fnm in argv[2:]:
    lbl = extract_label(mode,fnm)
    grep_all_credits(lbl,fnm)
