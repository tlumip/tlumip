TSModule output file naming conventions

Mode Choice Logsums: (example: w4mcls for alpha-alpaZone or w4mcls_beta for beta-betaZone)

Character 1: trip purpose

pktime.zip	Peak Drive Time
pkdist.zip	Peak Drive Distance
optime.zip 	Off Peak Drive Time
opdist.zip	Off Peak Drive Distance
pwtivt.zip	Peak Walk-Transit In-Vehicle Time
pwtfwt.zip	Peak Walk-Transit First Wait Time
pwttwt.zip	Peak Walk-Transit Total Wait Time
pwtaux.zip	Peak Walk-Transit Aux. Transit (Walk) time
pwtbrd.zip	Peak Walk-Transit Boardings
pwtfar.zip	Peak Walk-Transit Fare
owtivt.zip	Off-Peak Walk-Transit In-Vehicle Time
owtfwt.zip	Off-Peak Walk-Transit First Wait Time
owttwt.zip	Off-Peak Walk-Transit Total Wait Time
owtaux.zip	Off-Peak Walk-Transit WAux. Transit (Walk) time
owtbrd.zip	Off-Peak Walk-Transit Boardings
owtfar.zip	Off-Peak Walk-Transit Fare
pdtivt.zip	Peak Drive-Transit In-Vehicle Time
pdtfwt.zip	Peak Drive-Transit First Wait Time
pdttwt.zip	Peak Drive-Transit Total Wait Time
pdtwlk.zip	Peak Drive-Transit Walk Time
pdtbrd.zip	Peak Drive-Transit Boardings
pdtdrv.zip	Peak Drive-Transit Drive Time
pdtfar.zip	Peak Drive-Transit Fare
odtivt.zip	Off-Peak Drive-Transit In-Vehicle Time
odtfwt.zip	Off-Peak Drive-Transit First Wait Time
odttwt.zip	Off-Peak Drive-Transit Total Wait Time
odtwlk.zip	Off-Peak Drive-Transit Walk Time
odtbrd.zip	Off-Peak Drive-Transit Boardings
odtdrv.zip	Off-Peak Drive-Transit Drive Time
odtfar.zip	Off-Peak Drive-Transit Fare


TS Assignment Results:
*period**reporttype* (e.g., ampeakassignmentresults.csv or ntoffpeakrouteboardingsreport.txt)

where: 
*period* options are:
ampeak = (7-9AM AM peak)
mdoff = (9AM-4PM mid-day off-peak)
ntoff = (6PM-7AM night off-peak)
pmpeak = (4-6PM night off-peak)

*reporttype* options are:
assignment results.csv = Roadway Assignment Results by link
routeboardings.csv = TransitRoute boardings by link
routeboardingsreport.txt = TXT version of above CSV reporttype



TS transit Travel attributes (skims):
*period**SDT-LDT**mode**reporttype* (e.g. 
    -- ask Jim if need to output since no one uses

where: 
*period* options are:
pk = (7-9AM AM peak)
op = (9AM-4PM mid-day off-peak)
nu or nt = (6PM-7AM night off-peak)-- not used
pmpeak = (4-6PM night off-peak) -- not used

*SDT-LDT* options are:
ld = 
lw = 

*mode*

*reporttype*

ld air drv
ld air far
ld air fwt
ld air ivt

ld ic drv
ld ic far
ld ic fwt
ld ic ivt

ld icr drv
ld icr far
ld icr fwt
ld icr ivt

ld t drv
ld t far
ld t fwt
ld t ivt

ld icr twt --???
ld icr xwk
ld ic  twt
ld ic  xwk
ld t   brd
ld t   ewk
ld t   twt
ld t   xwk

lw ic awk
lw ic brd
lw ic ewk
lw ic far
lw ic fwt
lw ic ivt
lw ic twt
lw ic xwk

lw icr awk -- no brd?
lw icr ewk
lw icr far
lw icr fwt
lw icr ivt
lw icr twt
lw icr xwk

lw t awk
lw t brd
lw t ewk
lw t far
lw t fwt
lw t ivt
lw t twt
lw t xwk

TS output truck skims:
*period**mode**reporttype*.zmx (e.g. )

note: AlphZone-to-AlphaZone data unless filename preceded by "beta"

where: 
*period* options are:
pk = (7-9AM AM peak)
op = (9AM-4PM mid-day off-peak)
nu or nt = (6PM-7AM night off-peak)-- not used
pmpeak = (4-6PM night off-peak) -- not used

*mode* options are defined in the TS.propertiesfile (up to 5 types), default 3-truck class assigment follows:
trk1 = light truck
trk2 = medium truck
trk3 = heavy truck

*reporttype* options are:
dist = distance (miles)
time = travel time (minutes)
toll = toll (1990cents?)


TS Intercity access mode output:
*period*_*accessmode*_*mode*.listing (e.g., )

where: 
*period* options are:
ampeak = (7-9AM AM peak)
mdoffpeak = (9AM-4PM mid-day off-peak)
ntoffpeak = (6PM-7AM night off-peak)
pmpeak = (4-6PM night off-peak)

*accessmode* options are:
driveldt
drive
walk

*mode* options are:
air = intercity air (within modelarea only)
hsr = intercity high speed rail (not used, but files reqiured)
intercity = intercity bus or rail/Amtrak
intracity = intracity MPO bus or rail


TS output Trip Length distribuions (miles): 
tld_*class*_*period*.csv (e.g., 

where
*class* options are defined in ts.propertiesfile (up to 5 types), default class assignment folows:
a = auto
b = bus?
d = light truck
e = medium truck
f = heavy truck


*period* options are:
ampeak = (7-9AM AM peak)
mdoffpeak = (9AM-4PM mid-day off-peak)
ntoffpeak = (6PM-7AM night off-peak)
pmpeak = (4-6PM night off-peak)
