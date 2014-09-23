crf 2/6/2014

This model folder currently holds the "base" scenario for the swim/tlumip model. This base scenario should be used for setting up a model run, and any general improvements/changes should be committed back to this repository. However, it is not intended as a "plug-and-play" setup for a model run for the following reasons:

    1) The scenario is a generic t20 (2010) base run, and will not have any scenario-specific updates.
    2) Some of the larger data files are not stored as they should be in a true scenario, in order to save space (see below).
    3) The base model files are not included in this repository, for space reasons (again, see below).

The workflow for using this repository with a model setup is as follows:

    1) Setup the basic model file structure. It should look like this (ODOT or PB should provide this):
        
        [base directory]/
                        /model/
                              /lib/    - programs used to run the model (java, python, R, etc.)
                              /census/ - fixed (large) census data files

    2) Pull the scenario directory from the SVN repository. That is, the following SVN path

            http://216.243.97.229/svn/projects/trunk/tlumip/model/scenario

       should be put in the base directory, which should give a directory structure something like the following:

        [base directory]/
                        /scenario/
                                 /.svn/
                                 /inputs/
                                 /model/
                                 /outputs/

    2) Make a copy of the scenario directory in the base directory - this will be the "base" scenrio. From the base_scenario directory, delete the .svn/ directory, as this should not be treated as a repository folder, as well as this file. Something like the following should result:

        [base directory]/
                        /base_scenario/
                                      /inputs/
                                      /model/
                                      /outputs/
       
       Also, there are two zip files that need to have their files extracted into their containing directories:

        [base directory]/base_scenario/inputs/t0/swimNetworkAttributes.zip
        [base directory]/base_scenario/outputs/t19/matrices.zip

       Once unzipped, these zip files can (and should) be deleted.

    3) The base_scenario is now ready to use. To make a new scenario, copy the base_scenario directory and name the directory after the scneario. If there are any changes to the base scenario data that should be migrated back to the repository, copy it into the appropriate place in [base directory]/scenario/, and then commit the changes. 

    While a little cumbersome to have a copy of the scenario/ directory acting as the base scenario, it prevents the necessary .svn/ folder from being propogated to all the child scenarios, as well as isolating the repository base from inadverent changes/commits.

