from xmlrpclib import ServerProxy
import sys, math, re, traceback


class Link(object):
    def __init__(self, id, an, bn, linkType, flow, z):
        self.id = id
        self.an = an
        self.bn = bn
        self.linkType = linkType
        self.flow = flow
        self.taz = z

class Count(object):
    def __init__(self, an, bn, value):
        self.an = an
        self.bn = bn
        self.value = value

class A2B(object):
    def __init__(self, a, b, st, ct, mpoModel, puma00, puma90):
        self.alpha = int(a)
        self.beta = int(b)
        self.state = st
        self.county = ct
        self.mpo = mpoModel

class NetworkLinks(dict):
    def __init__(self):
        try:
            print 'getting network attributes from server...'
            ia = server.networkDataServer.getIa()
            ib = server.networkDataServer.getIb()
            indexNode = server.networkDataServer.getIndexNode()
            linkIndices = server.networkDataServer.getLinkIndexArray()
            linkType = server.networkDataServer.getLinkType()
            taz = server.networkDataServer.getTaz()
            flow = server.networkDataServer.getVolau()
            
            inEx = {}
            for index, node in enumerate(indexNode):
                inEx[index] = node
                
    
            print 'building network attributes table...'
            for i, node in enumerate(ia):
                an = inEx[ia[i]]
                bn = inEx[ib[i]]
                id = linkIndices[i]
                lt = linkType[i]
                fl = flow[i]
                z = taz[i]
    
                self[id] = Link(id, an, bn, lt, fl, z)
                
            print '%d links read from network database.' % len(self)
        except Exception, e:
            traceback.print_exc(None,sys.stdout)
            raise e

class NetworkCounts(dict):
    def __init__(self, period, countsFile):
        self.counts = self.readCounts(period,countsFile)

    def readCounts(self, period, countsFile):
        try:
            print 'reading counts table...'
            counts = {}
            f = open(countsFile,'r')
            for line in f:
                year, an, bn, id, amValue, mdValue, pmValue, ntValue = [int(i.strip()) for i in line.split()]
                id = server.networkDataServer.getLinkId(an,bn)
                if id > 0:
            
                    if period == "ampeak":
                        value = amValue
                    elif period == "mdoffpeak":
                        value = mdValue
                    elif period == "pmpeak":
                        value = pmValue
                    elif period == "ntoffpeak":
                        value = ntValue
                        
                    count = Count(an, bn, value)
                    if counts.has_key(year):
                        yearCounts = counts[year]
                    else:
                        yearCounts = {}
                    yearCounts[id] = count
                    counts[year] = yearCounts
                
            for year in counts.keys():
                print '%d link counts read from counts file for %d.' % (len(counts[year]), year)
        except Exception, e:
            traceback.print_exc(None,sys.stdout)
            raise e
            
        return counts
        
    # return a dict of Count objects, keyed off link id
    def getCounts(self, year):
        return self.counts[year]
        


class Alpha2Beta(dict):
    def __init__(self, a2bFileName):
        self.a2b = self.readA2B(a2bFileName)

    def readA2B(self, a2bFileName):
        print 'reading alpha2beta table...'
        a2b = {}
        f = open(a2bFileName,'r')
        k = 0
        for line in f:
            if k > 0:
                #a, b, pr, ar, st, ct, sfips, cfips, puma00, puma90, acres, hprk, dprk, lu, mpoCalib, mpoModel, act, aggAct, fare, hpms = [ i.strip() for i in line.split(",") ]
                a, b, pr, ar, st, ct, sfips, cfips, puma00, puma90, acres, hprk, dprk, lu, mpoCalib, mpoModel, act, aggAct, fare, hpms, areaType, nc = [ i.strip() for i in line.split(",") ]
                a2b[int(a)] = A2B(a, b, st, ct, mpoModel, puma00, puma90)
            k += 1
        print '%d alpha zone records read from aplha2beta file for.' % (len(a2b))
            
        return a2b
        
    # return a dict of MPO strings, keyed off alpha zones
    def getAlphaMpo(self):
        mpoMap = {}
        for a in self.a2b.keys():
            mpoMap[self.a2b[a].alpha] = self.a2b[a].mpo
            
        return mpoMap
        


class RmseTables(object):
    def __init__(self, year, period, a2bFileName, countsFile):
        netTable = NetworkLinks()
        
        allCounts = NetworkCounts(period, countsFile)
        if not allCounts.counts.has_key(year):
            print "No counts for year " + str(year) + ", rmse report will not be produced."
            sys.exit(0)
        counts = allCounts.getCounts(year)

        a2b = Alpha2Beta(a2bFileName)
        mpoStrings = a2b.getAlphaMpo()

        mpoMap = { 'Bend':0, 'Corvallis':1, 'EugeneSpringfield':2, 'Metro':3, 'RogueValley':4, 'SalemKeizer':5, 'NonMPO':6, 'OOO':7 }
        
        self.labels = []
        self.labels.append( { 0:'Rail/Air', 1:'Urb Freeway', 2:'Urb Reg Art', 3:'Urb Maj Art', 4:'Urb Min Art', 5:'Urb Maj Col', 6:'Urb Min Col', 7:'Urb Local', 8:'Urb Ramp', 9:'Urb Cent Conn', 10:'', 11:'Rur Freeway', 12:'Rur Reg Art', 13:'Rur Maj Art', 14:'Rur Min Art', 15:'Rur Maj Col', 16:'Rur Min Col', 17:'Rur Local', 18:'Rur Ramp', 19:'Rur Cent Conn' } )
        self.labels.append( { 0:'   0 to  499', 1:' 500 to  999', 2:'1000 to 4999', 3:'5000 to 9999', 4:'10000 +' } )
        self.labels.append( { 0:'Bend', 1:'Corvallis', 2:'EugSpgfld', 3:'Metro', 4:'RogueValley', 5:'SalemKeizer', 6:'NonMpo', 7:'OutsideOR' } )

        # keep track of order of attribute lables and values in their respective lists.
        self.attributeMap = { 'linkType':0, 'volRange':1, 'mpoAreas':2 }

            
        self.obs = []
        self.est = []
        cat0 = []
        cat1 = []
        cat2 = []
        self.cats = []


        for id in counts.keys():
            self.obs.append(counts[id].value)
            self.est.append(netTable[id].flow)
            cat0.append(netTable[id].linkType)

            # set default category to the last one, then change it if count falls into one of the other categories
            c = 4
            for i, upper in enumerate([500, 1000, 5000, 10000]):
                if counts[id].value < upper:
                    c = i
                    break
            cat1.append(c)
            
            alphaZone = netTable[id].taz
            mpoString = mpoStrings[alphaZone]
            mpoIndex = mpoMap[mpoString]
            
            cat2.append(mpoIndex)
            
        self.cats.append(cat0)
        self.cats.append(cat1)
        self.cats.append(cat2)
        
        print "RmseTables created."

    def getTables(self, attributeString):
        attributeIndex = self.attributeMap[attributeString]
        return ( self.obs, self.est, self.cats[attributeIndex], self.labels[attributeIndex] )
    
        
class RmseReport(object):
    def __init__(self, obs, est, indices, labels):
        self.obsValues = obs
        self.estValues = est
        self.categoryIndices = indices
        self.categoryLabels = labels

        # generate a count of each category value and the aggregate sum of the obs and est values
        self.aggregate()



    def aggregate(self):
        self.freq = {}
        self.obsCategories = {}
        self.estCategories = {}
        self.relError = {}
        self.rmse = {}
        sse = {}
        for i, cat in enumerate(self.categoryIndices):
            self.accumulate(self.freq, cat, 1)
            self.accumulate(self.obsCategories, cat, self.obsValues[i])
            self.accumulate(self.estCategories, cat, self.estValues[i])
            self.accumulate(sse, cat, (self.estValues[i] - self.obsValues[i])*(self.estValues[i] - self.obsValues[i]))
            
        self.obsTotal = 0
        self.estTotal = 0
        self.freqTotal = 0
        sseTotal = 0
        for i, cat in enumerate(self.categoryLabels):
            if self.obsCategories.has_key(cat):
                if self.obsCategories[cat] > 0 and self.freq[cat] > 1:
                    self.relError[cat] = (self.estCategories[cat] - self.obsCategories[cat])/self.obsCategories[cat]
                    self.rmse[cat] = math.sqrt(sse[cat]/(self.freq[cat] - 1)) / (self.obsCategories[cat]/self.freq[cat])
                self.obsTotal += self.obsCategories[cat]
                self.estTotal += self.estCategories[cat]
                self.freqTotal += self.freq[cat]
                sseTotal += sse[cat]
        self.reTotal = (self.estTotal - self.obsTotal)/self.obsTotal
        self.rmseTotal = math.sqrt(sseTotal/(self.freqTotal - 1)) / (self.obsTotal/self.freqTotal)
                
    def accumulate(self, dict, key, value):
        if dict.has_key(key) is True:
            temp = dict[key]
            dict[key] = temp + value
        else:
            dict[key] = value

    def printReport(self, title, period):
        i = 1
        print '\n\n%s Period %s\n' % ( period.title(), title )
        print '%-3s %8s %14s %6s %10s %10s %8s %8s' % ('', 'Category', 'Description', 'Freq', 'Observed', 'Estimated', '% Error', '% RMSE')
        for cat in self.categoryLabels.keys():
            if self.rmse.has_key(cat):
                print '%-3d %8s %14s %6d %10.0f %10.0f %7.1f%% %7.1f%%' % (i, cat, self.categoryLabels[cat], self.freq[cat], self.obsCategories[cat], self.estCategories[cat], 100*self.relError[cat], 100*self.rmse[cat])
                i += 1
        print '%-3s %8s %14s %6d %10.0f %10.0f %7.1f%% %7.1f%%' % ('', '', 'Total', self.freqTotal, self.obsTotal, self.estTotal, 100*self.reTotal, 100*self.rmseTotal)


    def writeReportToFile(self, title, period, outputFile):
        outputFile.write( '\n\n%s Period %s\n' % ( period.title(), title ) )
        outputFile.write( '%-3s %8s %14s %6s %10s %10s %8s %8s\n' % ('', 'Category', 'Description', 'Freq', 'Observed', 'Estimated', '% Error', '% RMSE'))
        for i, cat in enumerate( self.categoryLabels.keys() ):
            if self.rmse.has_key(cat):
                outputFile.write( '%-3d %8s %14s %6d %10.0f %10.0f %7.1f%% %7.1f%%\n' % (i+1, cat, self.categoryLabels[cat], self.freq[cat], self.obsCategories[cat], self.estCategories[cat], 100*self.relError[cat], 100*self.rmse[cat]))
        outputFile.write('%-3s %8s %14s %6d %10.0f %10.0f %7.1f%% %7.1f%%\n' % ('', '', 'Total', self.freqTotal, self.obsTotal, self.estTotal, 100*self.reTotal, 100*self.rmseTotal))


    def printTable(self):
        print '%-3s %8s %14s %10s %10s %8s' % ('', 'Category', 'Description', 'Observed', 'Estimated', 'Rel. Err.')
        for i, cat in enumerate(self.categoryIndices):
            print '%-3d %8s %14s %10.0f %10.0f %7.1f%%' % (i+1, cat, self.categoryLabels[cat], self.obsValues[i], self.estValues[i], 100*(self.estValues[i] - self.obsValues[i])/self.obsValues[i])

        
def rmseReports(year, period, outputFilename, a2bFileName, countsFile):

    rmseTables = RmseTables(year, period, a2bFileName, countsFile)
    
    titles = [ 'Link Count/Flow Comparison by Link Types', 'Link Count/Flow Comparison by Link Count Ranges', 'Link Count/Flow Comparison by MPO Area' ]
    
    outputFile = open(outputFilename, "w")
    
    for i, attribute in enumerate([ 'linkType', 'volRange', 'mpoAreas' ]):
        (obs, est, cat, labels) = rmseTables.getTables(attribute)
        report = RmseReport(obs, est, cat, labels)
        #report.printTable()      
        report.printReport( titles[i], period )
        report.writeReportToFile( titles[i], period, outputFile )

    outputFile.close()


year = int(sys.argv[1])
period = sys.argv[2]
filename = sys.argv[3]
a2bFileName = sys.argv[4]
countsFile = sys.argv[5]
networkServerHost = sys.argv[6]
networkServerPort = sys.argv[7]

outfilename = filename[:filename.rfind(".")] + "_" + str(year) + filename[filename.rfind("."):]

serverConnection = "http://" + networkServerHost + ":" + networkServerPort
server = ServerProxy(serverConnection)

rmseReports( year, period, outfilename, a2bFileName, countsFile )
