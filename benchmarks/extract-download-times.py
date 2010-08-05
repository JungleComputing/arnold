#!/usr/bin/env python

import tarfile
import sys

#fnm = 'plain-12-s0-fs0-leecher.tar'

peersPerProcessor=2

def grep_download_time(f):
    credit = None
    completion = None
    start = None
    for l in f:
        if l.startswith('DOWNL'):
            words = l.split()
            completion = int( words[1] )*1e-3
            start = int( words[2] )*1e-3
        if l.startswith('FINALC'):
            words = l.split()
            credit = float( words[1] )
    return (start, completion, credit)

def grep_isseeder(f):
    for l in f:
        ix = l.find( 'seeder=true' )
        if ix>=0:
            return True
    return False

def grep_all_download_times(lbl,fnm):
    tf = tarfile.open(fnm)
    for e in tf:
        if e.isfile():
            f = tf.extractfile(e)
            isseeder = grep_isseeder(f) 
            f = tf.extractfile(e)
            (start,completion,credit) = grep_download_time(f)
            if completion != None:
                print lbl, completion, start, credit
            elif not isseeder:
                print "File '%s' contains no download time" % e.name

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
    grep_all_download_times(lbl,fnm)
