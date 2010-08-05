#!/usr/bin/env python

import tarfile
import sys

table = {}

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

def collect_all_download_times(fnm):
    f = open(fnm,'r')
    for l in f.readlines():
        if l.startswith('DOWNLOAD'):
            words = l.split()
            starttime = 1e-3*int(words[1])
            key = (words[3],words[4])
            if key not in table:
                table[key] = []
            table[key].append(starttime)
    f.close()

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
        print "%s %s: min=%f av=%f max=%f samples=%d" % (role, sz, min, average, max, samples)

def collect_roles( table ):
    keys = table.keys()
    res = []
    for key in keys:
        (role,sz) = key
        if role not in res:
            res.append(role)
    res.sort()
    return res

def get_stats(key):
    if key not in table:
        return "\\multicolumn{3}{c}{-}"
    (min, average, max, samples) = compute_statistics( table[key] )
    return " %.1f & %.1f & %.1f" % (min, average, max)

def print_table():
    print "\\begin{tabular}{c|rrr|rrr|rrr|}"
    print " & \\multicolumn{3}{c}{TFT} & \\multicolumn{3}{c}{CRED} & \\multicolumn{3}{c}{FCFS} \\\\"
    print "H&R & " + get_stats(("TitForTatRankingPolicy","TFT")) + " & " + get_stats(("CreditRankingPolicy","TFT"))  + " & " + get_stats(("OneTrackRankingPolicy","TFT")) + " \\\\"
    print "ALT & " + get_stats(("TitForTatRankingPolicy","altruistic")) + " & " + get_stats(("CreditRankingPolicy","altruistic"))  + " & " + get_stats(("OneTrackRankingPolicy","altruistic")) + " \\\\"
    print "IMP & " + get_stats(("TitForTatRankingPolicy","impatient")) + " & " + get_stats(("CreditRankingPolicy","impatient"))  + " & " + get_stats(("OneTrackRankingPolicy","impatient"))
    print "\\end{tabular}"

argv = sys.argv
for fnm in argv[1:]:
    collect_all_download_times(fnm)
#dump_statistics()
print_table()

