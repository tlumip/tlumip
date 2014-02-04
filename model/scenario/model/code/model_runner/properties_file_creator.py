#properties_file_creator.py
"""
    Create a detemplified property file. The input file will have 
    a series of tokens, which this process will replace with values.
    The resultant text will be written to an output file.
    
    crf March 18, 2013
"""

import os

class PropertyTokens:
    """ list of valid property detemplification tokens"""
    SCENARIO_NAME                 = '@SCENARIO.NAME@'
                                  
    ROOT_DIR                      = '@ROOT.DIR@'
    #USER_INPUTS_DIR              = '@USER.INPUTS@'
    SCENARIO_INPUTS_DIR           = '@SCENARIO.INPUTS@'
    SCENARIO_OUTPUTS_DIR          = '@SCENARIO.OUTPUTS@'
                                 
    CURRENT_T_YEAR                = '@CURRENT.INTERVAL@'
                                 
    SI_LAST_RUN_YEAR              = '@SI.LAST.RUN@'
    NED_LAST_RUN_YEAR             = '@NED.LAST.RUN@'
    ALD_LAST_RUN_YEAR             = '@ALD.LAST.RUN@'
    SPG1_LAST_RUN_YEAR            = '@SPG1.LAST.RUN@'
    AA_LAST_RUN_YEAR              = '@AA.LAST.RUN@'
    AA_PRIOR_RUN_YEAR             = '@AA.PRIOR.RUN@'
    PT_LAST_RUN_YEAR              = '@PT.LAST.RUN@'
    SPG2_LAST_RUN_YEAR            = '@SPG2.LAST.RUN@'
    CT_LAST_RUN_YEAR              = '@CT.LAST.RUN@'
    ET_LAST_RUN_YEAR              = '@ET.LAST.RUN@'
    TS_LAST_RUN_YEAR              = '@TS.LAST.RUN@'
    TA_LAST_RUN_YEAR              = '@TA.LAST.RUN@'
    TR_LAST_RUN_YEAR              = '@TR.LAST.RUN@'
                                 
    TS_DAILY                      = '@TS.DAILY@'
    SL_MODE                       = '@SL.MODE@'
    PT_LOGSUMS                    = '@PT.LOGSUMS@'
    PT_LDT                        = '@PT.LDT@'
    PT_SDT                        = '@PT.SDT@'
    TRANSIT_ON                    = '@TRANSIT.ON@'
    TRANSIT_OFF                   = '@TRANSIT.OFF@'
    TS_LAST_TRANSIT_RUN_YEAR      = '@TS.LAST.TRANSIT.RUN@'


def detemplifyFile(input_file,output_file,detemplification_map,templified_property_update={}):
    """
        Detemplify the text in input_file and write it to output_file. detemplification_map has
        {key:value} pairs for which "key" will be replaced by "value" in the input text.
        templified_property_update has the same format as detemplification_map and will take 
        precedence over it.
    """
    #templified_property_update is a set of (templified) properties which are to replace those in the
    # base file; this can be used to update a (templified) property file before detemplification
    # note that this is done by matching keys: if a key is templified, then the update key must also
    # be (identically) templified
    f = open(output_file,'wb')
    for line in open(input_file):
        line = line.strip()
        for key in templified_property_update:
            if line.find(key) == 0 and line.find('=') > -1:
                #want to preserve all whitespace up to beginning of value
                updated_line = line[:(line.find('=') + 1)] #everything up to and including the '='
                updated_line += ' '*(len(line.replace(updated_line,'',1)) - len(line.replace(updated_line,'',1).strip())) #add spaces up to value
                updated_line += templified_property_update[key] #add value update
                line = updated_line
                break # all done
        for key in detemplification_map:
            line = line.replace(key,detemplification_map[key])
        f.write(line + os.linesep)
    f.close()
