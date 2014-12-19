#Create SWIM Highway Assignment Analysis Charts
#Ben Stabler, stabler@pbworld.com, 03/22/13
#Palvinder Singh, singhp@pbworld.com, 03/22/13
############################################################
        
#import libraries
import matplotlib.pyplot as plt, numpy as np
import VisumPy.helpers as VisumHelpers, math
from matplotlib.font_manager import FontProperties

#parameters
numIntervals = 15
dSeg = ['auto_peak', 'auto_offpeak', 'truck_peak', 'truck_offpeak']
distMatrixNo= [19,27,23,31]          #27-a_peak, 35-a_offpeak, 31-tr_peak, 39-tr_offpeak
demMatrixNo = [1,2,5,6]              #1-a_peak, 2-a_offpeak, 5-tr_peak, 6-tr_offpeak
Year_AADT   = '2006'                 #user specific - change to year for which AADT is required
AADT_volume = 'AADT_' + Year_AADT    #change if prefix to year changes in the fieldname
maxIterations = 50                   #needs to be changed if changed in procedure file
totalAssignedVol = 'TotalAssignmentVol'
classLength = [5,5,25,25]

class plotChart(object):

    def __init__(self,visum):

        self.visum = visum

        self.dSegCode = "DSegCode"
        self.iteration = "Iteration"
        self.dualityGap = "DualityGap"
        self.relativeGap = "RelativeGap"
        self.numIntervals = numIntervals
        self.distMatrixNo = distMatrixNo
        self.demMatrixNo = demMatrixNo
        self.dSeg = dSeg
        self.classLength = classLength
        self.totalAssignedVol = totalAssignedVol
        self.AADT_volume = AADT_volume
        
       
    def assignmentQuality(self):
        print("draw chart")
        PrTAssQualityList = self.visum.Lists.CreatePrTAssQualityList
        PrTAssQualityList.AddKeyColumns()
        PrTAssQualityList.AddColumn(self.dSegCode)
        PrTAssQualityList.AddColumn(self.iteration)
        PrTAssQualityList.AddColumn(self.dualityGap)
        PrTAssQualityList.AddColumn(self.relativeGap)
        self.assignmentList = PrTAssQualityList.SaveToArray()

        #separate array elements into list
        self.numLoops = len(self.assignmentList)/2
        self.auto_dSeg = []; self.auto_dualGap = []; self.auto_relGap = []
        self.truck_dSeg = []; self.truck_dualGap = []; self.truck_relGap = []

        for i in range(0, self.numLoops):
            self.auto_dSeg.append(str(self.assignmentList[2*i][0]))
            self.truck_dSeg.append(str(self.assignmentList[2*i+1][0]))
            self.auto_dualGap.append(self.assignmentList[2*i][2])
            self.truck_dualGap.append(self.assignmentList[2*i+1][2])
            self.auto_relGap.append(self.assignmentList[2*i][3])
            self.truck_relGap.append(self.assignmentList[2*i+1][3])

        #pass parameters (x_data, y_data1, y_data2, legend, x_label, y_label,title, output file) to function
        self.createMultiplelinesPlot(range(self.numLoops), self.auto_relGap, self.truck_relGap, self.relativeGap,
                         "Iteration No.", "Relative Gap","Assignment Convergence", self.relativeGap)

        self.createMultiplelinesPlot(range(self.numLoops), self.auto_dualGap, self.truck_dualGap, self.dualityGap,
                         "Iteration No.", "Duality Gap","Assignment Convergence", self.dualityGap)                         


    def createScatterLinePlot(self, x_series, y_series, line_legend, scatter_legend, x_label, y_label, title, fileName):

        #set up matplotlib and the figure
        fig = plt.figure()
        ax = fig.add_subplot(111)

        #set regression line        
        prod = [x * self.slope for x in x_series]
        y_line = [i + self.intercept for i in prod]
        #set font
        fontP = FontProperties()
        fontP.set_size('small')

        #plot data
        label_series1 = scatter_legend
        label_series2 = line_legend
        ax.scatter(x_series, y_series, s = 5, marker = 'x', label = label_series1, color = 'red')
        ax.plot(x_series, y_line, label = label_series2, color = 'blue')

        #add axes labels and chart title
        ax.set_xlabel(x_label, fontsize = 10)
        ax.set_ylabel(y_label, fontsize = 10)
        ax.set_title(title, fontsize = 12)

        #add limits and set tick marks to the x and y axis and add some text labels
        ax.set_xlim(0, max(max(x_series),max(y_series)))
        ax.set_ylim(0, max(max(x_series),max(y_series)))
        plt.xticks(np.arange(0, max(x_series) + 10000, 10000), fontsize = 10)
        plt.yticks(np.arange(0, max(y_series) + 5000, 5000), fontsize = 10)

        # shink current axis's height by 10% on the bottom
        box = ax.get_position()
        ax.set_position([box.x0, box.y0 + box.height * 0.1, box.width, box.height * 0.9])

        # put a legend below current axis
        ax.legend(loc = 'upper center', bbox_to_anchor = (0.5, -0.1), fancybox = True, shadow = True, ncol = 1, prop = fontP)

        #add grid to chart
        ax.grid(b = True, which = 'major', color = 'grey', linestyle = '--')
      
        #save figure to png
        fig.savefig(fileName + ".png")
        plt.close()

    def createMultiplelinesPlot(self, x_series, y_series_1, y_series_2, legend, x_label, y_label, title, fileName):
        #set up matplotlib and the figure
        fig = plt.figure()
        ax = fig.add_subplot(111)

        #set font
        fontP = FontProperties()
        fontP.set_size('small')

        #plot data
        label_series1 = "auto" + " - " + legend
        label_series2 = "truck" + " - " + legend
        ax.plot(x_series, y_series_1, label = label_series1, color = 'red')
        ax.plot(x_series, y_series_2, label = label_series2, color = 'blue')

        #add axes labels and chart title
        ax.set_xlabel(x_label, fontsize = 10)
        ax.set_ylabel(y_label, fontsize = 10)
        ax.set_title(title, fontsize = 12)

        #add limits and set tick marks to the x and y axis and add some text labels
        ax.set_xlim(0, self.numLoops-1)
        plt.xticks(np.arange(0, max(x_series)+ 2, 2), fontsize = 10)

        if legend == self.relativeGap:
            ax.set_ylim(0, 0.1)
            plt.yticks(np.arange(0, 0.11, 0.01), fontsize = 10)

        elif legend == self.dualityGap:
            ax.set_ylim(0, 0.1)
            plt.yticks(np.arange(0, 0.11, 0.01), fontsize = 10)

        # shink current axis's height by 10% on the bottom
        box = ax.get_position()
        ax.set_position([box.x0, box.y0 + box.height * 0.1, box.width, box.height * 0.9])

        # put a legend below current axis
        ax.legend(loc = 'upper center', bbox_to_anchor = (0.5, -0.1), fancybox = True, shadow = True, ncol = 5, prop = fontP)

        #add grid to chart
        ax.grid(b = True, which = 'major', color = 'grey', linestyle = '--')

        if self.numLoops < maxIterations:
            comment1 = 'Convergence reached at ' + str(self.numLoops - 1) + 'th iteration'
            comment2 = y_label + ' for auto mode is: ' + str(round(y_series_1[self.numLoops - 1],5))
            comment3 = y_label + ' for truck mode is: ' + str(round(y_series_2[self.numLoops - 1],5))
        else:
            comment1 = 'Convergence not reached after ' + str(maxIterations) + ' iterations'
            comment2 = y_label + ' for auto mode is: ' + str(round(y_series_1[self.numLoops - 1],5))
            comment3 = y_label + ' for truck mode is: ' + str(round(y_series_2[self.numLoops - 1],5))

        #attach some text labels
        ax.annotate(comment1, xy = (self.numLoops/2,0.090), fontsize=8, color = 'black')
        ax.annotate(comment2, xy = (self.numLoops/2,0.085), fontsize=8, color = 'red')
        ax.annotate(comment3, xy = (self.numLoops/2,0.080), fontsize=8, color = 'blue')

        #save figure to png
        timeperiod = self.auto_dSeg[0].split("_")[1]
        fig.savefig(timeperiod + "_" + fileName + ".png", dpi = 500)
        plt.close()

    def tripLengthFreq(self):
        print("create trip length frequencies for auto and truck for both time periods")

        for matList in range(len(self.distMatrixNo)):
            self.dist_mat = VisumHelpers.GetSkimMatrix(self.visum, self.distMatrixNo[matList])
            self.dem_mat = VisumHelpers.GetODMatrix(self.visum, self.demMatrixNo[matList])

            #define lowerbound, upperbound and class intervals
            self.lowerBound = range(0, self.numIntervals*self.classLength[matList], self.classLength[matList])
            self.upperBound = range(self.classLength[matList], self.numIntervals*self.classLength[matList], self.classLength[matList])
            self.upperBound.append(math.ceil(self.dem_mat.max()))
            self.classInterval = []
            for i in range(0, self.numIntervals):
                self.classInterval.append(str(self.lowerBound[i]) + "-" + str(self.upperBound[i]))
            self.classInterval[self.numIntervals-1] = str(max(self.lowerBound)) + '-' + 'inf'

            col_num = len(self.dem_mat[0])
            ro_num = len(self.dem_mat)
            freqList = [[] for x in xrange(self.numIntervals)]

            for num_int in range(self.numIntervals):
                for ro in range(ro_num):
                    for col in range(col_num):
                        if self.dist_mat[ro][col] >= self.lowerBound[num_int] and self.dist_mat[ro][col] < self.upperBound[num_int]:
                            freqList[num_int].append(self.dem_mat[ro][col])

            freq = []
            for num in range(self.numIntervals):
                freq.append(sum(freqList[num]))
            shares = [i / sum(freq) for i in freq]

            #create histogram of trip length frequency
            self.createbarChart(self.classInterval, shares, self.dSeg[matList], 'Class intervals', 'Shares', 'Trip length frequency')

    def createbarChart(self, x_series, y_series, label_series, x_label, y_label, title):

        #set up matplotlib and the figure
        fig = plt.figure()
        ax = fig.add_subplot(111)

        #set font
        fontP = FontProperties()
        fontP.set_size('small')

        #plot data
        width = 0.6
        tlf = ax.bar(np.arange(len(x_series)), y_series, width, label = label_series, color = 'orange')
        
        #add axes labels and chart title
        ax.set_xlabel(x_label, fontsize = 10)
        ax.set_ylabel(y_label, fontsize = 10)
        ax.set_title(title, fontsize = 12)

        #add limits and set tick marks to the x and y axis
        ax.set_xlim(-0.5, len(x_series))
        plt.xticks(np.arange(len(x_series))+ (width/2.0), fontsize = 6)
        ax.set_xticklabels(x_series)
        ax.set_ylim(0, 1)
        plt.yticks(np.arange(0, 1.1, 0.1), fontsize = 6)    

        # shink current axis's height by 10% on the bottom
        box = ax.get_position()
        ax.set_position([box.x0, box.y0 + box.height * 0.1, box.width, box.height * 0.9])

        # put a legend below current axis
        ax.legend(loc = 'upper center', bbox_to_anchor = (0.5, -0.1), fancybox = True, shadow = True, ncol = 5, prop = fontP)

        #add grid to chart
        ax.yaxis.grid(b = True, which = 'major', color = 'grey', linestyle = '--')

        #set ticks below the graph
        ax.set_axisbelow(b=True)

        # attach some text labels
        def autolabel(inps):
            for inp in inps:
                height = inp.get_height()
                ax.text(inp.get_x() + inp.get_width()/2., height + 0.01, str(round(height,2)),size = 'xx-small', ha = 'center', va = 'bottom')

        autolabel(tlf)

        #save figure to png
        fig.savefig(label_series + ".png", dpi = 500)
        plt.close()

    def filterObjects(self):
        filter = self.visum.Filters.LinkFilter()
        filter.Init()
        filter.AddCondition("OP_NONE", False, self.AADT_volume, 'ContainedIn', self.visum.Filters.FromRange(1))

    def regressionCompute(self, x_val, y_val):

        #calculate mean for x and y
        x_bar = np.mean(x_val)
        y_bar = np.mean(y_val)

        #calcualte difference arrays
        x_diff = x_val - x_bar
        y_diff = y_val - y_bar

        #convert x_diff and y_diff lists into arrays
        x_diff = np.array(x_diff); y_diff = np.array(y_diff)

        x_diff_squaredsum = sum(x_diff * x_diff)
        y_diff_squaredsum = sum(y_diff * y_diff)

        #coefficient of determination -need to be squared in output
        r_num = sum(x_diff * y_diff)
        r_den = np.sqrt(x_diff_squaredsum * y_diff_squaredsum)

        if r_den == 0.0:
            r = 0.0
        else:
            r = r_num / r_den

        if (r > 1.0):
            r = 1.0 # from numerical error

        #linear regression computations
        slope = r_num / x_diff_squaredsum
        intercept = y_bar - slope * x_bar

        return (r**2, slope, intercept)

    def volumeStatistics(self):
        observed_vol = []; model_vol = []
        obs_vol = self.visum.Net.Links.GetMultiAttValues(self.AADT_volume)
        mod_vol = self.visum.Net.Links.GetMultiAttValues(self.totalAssignedVol)
        for i in range(0,len(obs_vol)):
            observed_vol.append(obs_vol[i][1])
            model_vol.append(mod_vol[i][1])

        self.r_squared, self.slope, self.intercept  = self.regressionCompute(observed_vol, model_vol)

        #create plot of observed volumes and assigned volumes
        line_legend = 'Regression line: ' + 'y = ' + str(round(self.slope,2)) + ' * x + ' + str(round(self.intercept,2)) + ' and R-squared value: ' + str(round(self.r_squared,2))
        scatter_legend = 'model volumes and AADT counts'
        x_label = 'observed volumes (' + AADT_volume + ' counts)'
        y_label = 'assigned volumes (model outputs)'
        title = 'Plot of assigned volumes vs observed volumes'

        self.createScatterLinePlot(observed_vol, model_vol, line_legend, scatter_legend, x_label, y_label, title, 'validateVolumes')

###############################################################################

#run class methods        
#p = plotChart()
#p.assignmentQuality()
#p.tripLengthFreq()
#p.filterObjects()
#p.volumeStatistics()
