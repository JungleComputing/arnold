#!/usr/bin/env python

import sys

def usage():
    print "Usage: " + sys.argv[0] + " <specification>"

if len(sys.argv) != 3:
    print "I get " + `(len(sys.argv)-1)` + " arguments, while I need 2"
    usage()
    sys.exit( 1 )

arg = sys.argv[2]
elements = arg.split( '-' )
role = sys.argv[1]

runtime = 1000
if role != 'leecher':
    runtime = runtime*4
print "RUNTIME=%d" % runtime
