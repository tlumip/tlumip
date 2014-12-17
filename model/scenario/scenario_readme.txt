crf 2/6/2014

This model folder currently holds the "base" scenario for the swim/tlumip model. This base scenario should be used for setting up a model run, and any general improvements/changes should be committed back to this repository. It represents a generic t20 (2010) base run, and will not have any scenario-specific updates.

The workflow for using this repository with a model setup is as follows:

    1) Pull the scenario directory from the SVN repository. That is, the following SVN path

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
       
    3) Extract the two zip files containing generic model files (software libraries and census data):
        
        [base directory]/base_scenario/model/lib.zip
        [base directory]/base_scenario/model/census.zip
       
       A folder called model should be created in the base directory. Then the above files should be extracted into the folders:
        
        [base directory]/model/lib/
        [base directory]/model/census/

       Once unzipped, these zip files can (and should) be deleted.

    4) The base_scenario is now ready to use. To make a new scenario, copy the base_scenario directory and name the directory after the scneario. If there are any changes to the base scenario data that should be migrated back to the repository, copy it into the appropriate place in [base directory]/scenario/, and then commit the changes. 

    While a little cumbersome to have a copy of the scenario/ directory acting as the base scenario, it prevents the necessary .svn/ folder from being propogated to all the child scenarios, as well as isolating the repository base from inadverent changes/commits.

