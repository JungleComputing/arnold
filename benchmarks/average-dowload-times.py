#!/usr/bin/env python

import tarfile
import sys

#fnm = 'plain-12-s0-fs0-leecher.tar'

table = {}

peersPerProcessor=2

def grep_download_time(f):
    for l in f:
        if l.startswith('DOWNLOAD'):
            words = l.split()
            return int( words[1] )*1e-3
    return None

def grep_credit(f):
    for l in f:
        if l.startswith('FINALCREDIT'):
            words = l.split()
            return float( words[1] )
    return None

def grep_isseeder(f):
    for l in f:
        ix = l.find( 'seeder=true' )
        if ix>=0:
            return True
    return False

def collect_all_download_times(lbl,fnm):
    tf = tarfile.open(fnm)
    for e in tf:
        if e.isfile():
            f = tf.extractfile(e)
            isseeder = grep_isseeder(f) 
            f = tf.extractfile(e)
            t = grep_download_time(f)
            if t != None:
                if not (lbl in table):
                    table[lbl] = []
                table[lbl].append(t)
            elif not isseeder:
                print "File '%s' contains no download time" % e.name

def compute_statistics( l ):
    min = l[0]
    max = l[0]
    sum = 0
    for e in l:
        if e<min:
            min = e
        if e>max:
            max = e
        sum += e
    average = sum/len(l)
    return (min, average, max, len(l))

def dump_statistics():
    keys = table.keys()
    keys.sort()
    for key in keys:
        (min, average, max, samples) = compute_statistics( table[key] )
        (role,sz) = key
        print "%s %d: min=%f av=%f max=%f samples=%d" % (role, sz, min, average, max, samples)

def extract_label(fnm):
    cl = fnm.split('-')
    return (cl[0],peersPerProcessor*int(cl[1]))

argv = sys.argv
for fnm in argv[1:]:
    lbl = extract_label(fnm)
    collect_all_download_times(lbl,fnm)
dump_statistics()
