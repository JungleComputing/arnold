#!/usr/bin/env python

import sys
from string import Template
import string
import constants

def usage():
    print "Usage: " + sys.argv[0] + " <specification>"

if len(sys.argv) != 2:
    print "I get " + `(len(sys.argv)-1)` + " arguments, while I need 1"
    usage()
    sys.exit( 1 )

jorrentFile = 'ubuntu-karmic-desktop-i386.jorrent'
sharedFile = 'ubuntu-karmic-desktop-i386.iso'
needSeeder = True
arg = sys.argv[1]
elements = arg.split( '-' )
leechers=int( elements[1] )
helpers=elements[2]
helperArguments = ['--helpersStay']
if helpers[0] == 's':
   coordinatorRole = '--seeder'
   needSeeder = False
elif helpers[0] == 'l':
   coordinatorRole = '--leecher'
else:
    print "Malformed helpers specification '" + helpers + "'"
    usage()
    sys.exit( 1 )
helpers = int( helpers[1:] )
if leechers<1 and helpers<1:
    print "No leechers AND no helpers??"
    usage()
    sys.exit( 1 )
seeders = 1
coordinators = 1

# Translation table from measurement type to command-line arguments
args = {
    'plain':['--dummyfile',jorrentFile],
    'credit':['--dummyfile','-Darnold.credit-based',jorrentFile],
    'onetrack':['--dummyfile','-Darnold.one-track-based',jorrentFile],
    'plainimp':['--dummyfile','--impatientLeechers',jorrentFile],
    'creditimp':['--dummyfile','--impatientLeechers','-Darnold.credit-based',jorrentFile],
    'onetrackimp':['--dummyfile','--impatientLeechers','-Darnold.one-track-based',jorrentFile],
    'poorcredit':['--dummyfile','-Darnold.credit-based','-Darnold.special-start-credit=-1e10',jorrentFile],
    'richcredit':['--dummyfile','-Darnold.credit-based','-Darnold.special-start-credit=1e10',jorrentFile],
    'plainstay':['--dummyfile','--leechersStay',jorrentFile],
    'onetrackstay':['--dummyfile','-Darnold.one-track-based','--leechersStay',jorrentFile],
    'creditstay':['--dummyfile','-Darnold.credit-based','--leechersStay',jorrentFile],
    'poorcreditstay':['--dummyfile','-Darnold.special-start-credit=-1e10','-Darnold.credit-based','--leechersStay',jorrentFile],
    'richcreditstay':['--dummyfile','-Darnold.special-start-credit=1e10','-Darnold.credit-based','--leechersStay',jorrentFile],
    'real':[jorrentFile],
    'realstay':['--leechersStay',jorrentFile],
}

if not elements[0] in args.keys():
    l = string.join( args.keys(), ',' )
    print "Unknown benchmark type '" + elements[0] + "'; I only know [" + l + "]"
    sys.exit( 1 )
arguments = args[elements[0]]

s = Template( """# Generated experiment file
run-leecher-$arg.application.name = Arnold
run-leecher-$arg.process.count = $leechers
run-leecher-$arg.resource.count = $processors
run-leecher-$arg.cluster.name = VU
run-leecher-$arg.pool.name = $arg-pool
run-leecher-$arg.application.input.files = $j,settag-$arg-leecher.sh
run-leecher-$arg.application.output.files = $arg-leecher.tar.gz
run-leecher-$arg.application.arguments = $args""" )
print s.substitute( f=sharedFile, j=jorrentFile, arg=arg, args=string.join( arguments, ',' ), processors=leechers, leechers=2*leechers)

if helpers>0:
    s = Template( """
run-helper-$arg.application.name = Arnold
run-helper-$arg.process.count = $helpers
run-helper-$arg.resource.count = $helpers
run-helper-$arg.cluster.name = VU
run-helper-$arg.pool.name = $arg-pool
run-helper-$arg.application.input.files = $j,settag-$arg-helper.sh
run-helper-$arg.application.output.files = $arg-helper.tar.gz
run-helper-$arg.application.arguments = --proxymode,--helper,$args,$f""" )
    print s.substitute( f=sharedFile, j=jorrentFile, arg=arg, args=string.join( helperArguments + arguments, ',' ), helpers=helpers, coordinators=coordinators, coordinatorRole=coordinatorRole )

    s = Template( """
run-coordinator-$arg.application.name = Arnold
run-coordinator-$arg.process.count = $coordinators
run-coordinator-$arg.resource.count = $coordinators
run-coordinator-$arg.cluster.name = VU
run-coordinator-$arg.pool.name = $arg-pool
run-coordinator-$arg.application.input.files = $j,settag-$arg-coordinator.sh
run-coordinator-$arg.application.output.files = $arg-coordinator.tar.gz
run-coordinator-$arg.application.arguments = --proxymode,$coordinatorRole,$args,$f""" )
    print s.substitute( f=sharedFile, j=jorrentFile, arg=arg, args=string.join( arguments, ',' ), helpers=helpers, coordinators=coordinators, coordinatorRole=coordinatorRole )

