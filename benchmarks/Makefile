#

ARTICLEDIR=~/eclipse-workspace/CreditTorrent

SMALLRANGE=1 2 4 8 15
RANGE=$(SMALLRANGE) 20 30 40 50 60
#SAMPLES=0 1 2 3 4 5 6 7 8 9
SAMPLES=0 1 2 3 4 5 6
#SAMPLES=0 1

#STANDARDSWARMPEERS=50
#STANDARDSWARMPEERS=40
STANDARDSWARMPEERS=30
#STANDARDSWARMPEERS=16


PLAINLOGFILES = $(foreach N,$(RANGE),$(foreach S,$(SAMPLES),plain-$N-s0-n$S-leecher.tar.gz))
CREDITLOGFILES = $(foreach N,$(RANGE),$(foreach S,$(SAMPLES),credit-$N-s0-n$S-leecher.tar.gz))
ONETRACKLOGFILES = $(foreach N,$(RANGE),$(foreach S,$(SAMPLES),onetrack-$N-s0-n$S-leecher.tar.gz))
PLAINSTAYLOGFILES = $(foreach N,$(RANGE),$(foreach S,$(SAMPLES),plainstay-$N-s0-n$S-leecher.tar.gz))
CREDITSTAYLOGFILES = $(foreach N,$(RANGE),$(foreach S,$(SAMPLES),creditstay-$N-s0-n$S-leecher.tar.gz))
ONETRACKSTAYLOGFILES = $(foreach N,$(RANGE),$(foreach S,$(SAMPLES),onetrackstay-$N-s0-n$S-leecher.tar.gz))
POINTS= $(PLAINLOGFILES) $(ONETRACKLOGFILES) $(CREDITLOGFILES) $(PLAINSTAYLOGFILES) $(ONETRACKSTAYLOGFILES) $(CREDITSTAYLOGFILES)
RESULTS = $(POINTS:.tar.gz=.dt)
CREDITS = $(POINTS:.tar.gz=.credit)

#STANDARDSWARMSAMPLES=${foreach T,plain credit onetrack plainstay creditstay onetrackstay plainimp creditimp onetrackimp,${foreach S,$(SAMPLES),$T-$(STANDARDSWARMPEERS)-s0-n$S-leecher.dt}}
STANDARDSWARMSAMPLES=${foreach T,plain credit plainstay creditstay onetrackstay,${foreach S,$(SAMPLES),$T-$(STANDARDSWARMPEERS)-s0-n$S-leecher.dt}}



SMALLPLAINLOGFILES = $(foreach N,$(SMALLRANGE),$(foreach S,$(SAMPLES),plain-$N-s0-n$S-leecher.tar.gz))
SMALLCREDITLOGFILES = $(foreach N,$(SMALLRANGE),$(foreach S,$(SAMPLES),credit-$N-s0-n$S-leecher.tar.gz))
SMALLONETRACKLOGFILES = $(foreach N,$(SMALLRANGE),$(foreach S,$(SAMPLES),onetrack-$N-s0-n$S-leecher.tar.gz))
SMALLPLAINSTAYLOGFILES = $(foreach N,$(SMALLRANGE),$(foreach S,$(SAMPLES),plainstay-$N-s0-n$S-leecher.tar.gz))
SMALLCREDITSTAYLOGFILES = $(foreach N,$(SMALLRANGE),$(foreach S,$(SAMPLES),creditstay-$N-s0-n$S-leecher.tar.gz))
SMALLONETRACKSTAYLOGFILES = $(foreach N,$(SMALLRANGE),$(foreach S,$(SAMPLES),onetrackstay-$N-s0-n$S-leecher.tar.gz))
SMALLPOINTS= $(SMALLPLAINLOGFILES) $(SMALLONETRACKLOGFILES) $(SMALLCREDITLOGFILES) $(SMALLPLAINSTAYLOGFILES) $(SMALLONETRACKSTAYLOGFILES) $(SMALLCREDITSTAYLOGFILES)
SMALLRESULTS = $(SMALLPOINTS:.tar.gz=.dt)
SMALLCREDITS = $(SMALLPOINTS:.tar.gz=.credit)

SEEDINGPROXYLOGFILES= plain-32-s0-n0-leecher.tar.gz plain-32-s4-n0-leecher.tar.gz plain-32-s8-n0-leecher.tar.gz plain-32-s16-n0-leecher.tar.gz

ARNOLD_JAR = arnold-0.0.1.jar

SHAREDFILE = ubuntu-karmic-desktop-i386.iso

DEPLOYFILES=das3.grid das3.script arnold.applications

DTTABLES=plain.dtt plainstay.dtt plainimp.dtt credit.dtt creditstay.dtt creditimp.dtt onetrack.dtt onetrackstay.dtt onetrackimp.dtt
CTABLES=finalcredit.ct finalcreditstay.ct
#PLOTS=$(DTTABLES:.dtt=.eps) $(CTABLES:.ct=.eps)
PLOTS= \
  plain.staircase.eps \
  plainstay.staircase.eps \
  plainimp.staircase.eps \
  credit.staircase.eps \
  creditstay.staircase.eps \
  creditimp.staircase.eps \
  onetrack.staircase.eps \
  onetrackstay.staircase.eps \
  onetrackimp.staircase.eps

TEXTABLES=performance.tex
TABLES=$(TEXTABLES)

performance.tex: build-performance-table.py $(STANDARDSWARMSAMPLES)
	python build-performance-table.py $(STANDARDSWARMSAMPLES) > $@


%.experiment: build-experiment.py
	python build-experiment.py $* > $@

%-leecher.tar.gz %-coordinator.tar.gz: %.experiment run-experiment $(DEPLOYFILES) $(ARNOLD_JAR)
	echo "export TAG=$*" > settag-$*-leecher.sh
	cp settag-$*-leecher.sh settag-$*-helper.sh
	cp settag-$*-leecher.sh settag-$*-coordinator.sh
	python compute-runtime.py leecher $* >> settag-$*-leecher.sh
	python compute-runtime.py helper $* >> settag-$*-helper.sh
	python compute-runtime.py coordinator $* >> settag-$*-coordinator.sh
	echo "export ROLE=leecher" >> settag-$*-leecher.sh
	echo "export ROLE=helper" >> settag-$*-helper.sh
	echo "export ROLE=coordinator" >> settag-$*-coordinator.sh
	./run-experiment $*.experiment
	rm -f settag-$*-leecher.sh settag-$*-coordinator.sh settag-$*-helper.sh

%.dt: %.tar.gz grep-download-times.py
	python grep-download-times.py $*.tar.gz > $@

all: $(PLOTS) $(TABLES)

small: $(SMALLRESULTS) $(SMALLCREDITS)

install: $(PLOTS)
	cp $(PLOTS) $(ARTICLEDIR)

tables: $(DTTABLES) $(CTABLES)

x:
	echo $(STANDARDSWARMSAMPLES)

%.start.staircase.table: %.dt extract-start-staircase-table.py
	python extract-start-staircase-table.py $*.dt > $@

%.completion.staircase.table: %.dt extract-completion-staircase-table.py
	python extract-completion-staircase-table.py $*.dt > $@

seedingproxy.dtt: $(SEEDINGPROXYLOGFILES) extract-download-times.py
	python extract-download-times.py proxy $(SEEDINGPROXYLOGFILES) > $@

plainstay.dtt: $(PLAINSTAYLOGFILES:.tar.gz=.dt) 
	cat $^ > $@

onetrackstay.dtt: $(ONETRACKSTAYLOGFILES:.tar.gz=.dt)
	cat $^ > $@

creditstay.dtt: $(CREDITSTAYLOGFILES:.tar.gz=.dt)
	cat $^ > $@

plain.dtt: $(PLAINLOGFILES:.tar.gz=.dt)
	cat $^ > $@

onetrack.dtt: $(ONETRACKLOGFILES:.tar.gz=.dt)
	cat $^ > $@

credit.dtt: $(CREDITLOGFILES:.tar.gz=.dt)
	cat $^ > $@

finalcreditstay.ct: $(CREDITSTAYLOGFILES) $(CREDITSTAYSEEDERLOGFILES) extract-credits.py
	python extract-credits.py credit $(CREDITSTAYLOGFILES) $(CREDITSTAYSEEDERLOGFILES) > finalcreditstay.ct

finalcredit.ct: $(CREDITLOGFILES) $(CREDITSEEDERLOGFILES) extract-credits.py
	python extract-credits.py credit $(CREDITLOGFILES) $(CREDITSEEDERLOGFILES) > finalcredit.ct

%.events: %.tar.gz extract-download-events.py
	python extract-download-events.py $*.tar.gz > $*.events


.PRECIOUS: $(POINTS) $(RESULTS) $(CREDITS)

PRECIOUSJUNK=$(POINTS) $(RESULTS) $(CREDITS)
JUNK=*.experiment settag-*.sh *.err *.out $(DTTABLES) $(CTABLES)

$(ARNOLD_JAR): ../lib/$(ARNOLD_JAR)
	cp -p ../lib/$(ARNOLD_JAR) $(ARNOLD_JAR)

%.pdf: %.eps
	ps2pdf $*.eps -eps > $@

%.staircase.eps: %-$(STANDARDSWARMPEERS)-s0-n0-leecher.start.staircase.table %-$(STANDARDSWARMPEERS)-s0-n0-leecher.completion.staircase.table staircase.plo
	ploticus staircase.plo STARTFILE=$*-$(STANDARDSWARMPEERS)-s0-n0-leecher.start.staircase.table COMPLETIONFILE=$*-$(STANDARDSWARMPEERS)-s0-n0-leecher.completion.staircase.table -eps -o $@

%.eps: download-times.plo %.dtt
	ploticus download-times.plo TABLEFILE=$*.dtt -eps -o $@

%.eps: download-times.plo %.ct
	ploticus credit.plo TABLEFILE=$*.ct -eps -o $@

clean:
	rm -f $(JUNK)

empty:
	rm -f $(JUNK) $(PRECIOUSJUNK)

###
