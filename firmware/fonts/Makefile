# Makefile that generates C code and/or GNU Unifont files from BDF font files

# C_CONVERTER=../../tools/bdf-to-struct.pl
# C_CONVERTER=../../tools/bdf-to-pgmspace.pl
C_CONVERTER=../../tools/bdf-to-array.pl
HEX_CONVERTER=../../tools/bdf-to-unifont.pl
HEXDRAW=hexdraw

TARGETS=micro.c 6x10.c 6x9.c 5x8.c 5x7.c Misc-Fixed-Medium-R-Normal-6.c Misc-Fixed-Medium-R-Normal-7.c Misc-Fixed-Medium-R-Normal-8.c

all: $(TARGETS)

%.c: %.bdf
	$(C_CONVERTER) $*.bdf > $@

%.hex: %.bdf
	$(HEX_CONVERTER) $*.bdf > $@

%.txt: %.hex
	$(HEXDRAW) $*.hex

clean:
	rm -f $(TARGETS) *.hex
