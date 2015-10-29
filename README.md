# SWIM-TLUMIP
This is the model repository for the Oregon Statewide Integrated land use/transport model (SWIM).

## Setting up the model
There are a few steps that the analyst needs to take after cloning a fresh version of the repository in order to set up the model.
1. _Download the ACS PUMS files._ Follow the instructions in `root/model/census/README.md`
2. _Unpack and install the model software._ Follow the instructions in `root/model/lib/README.md`

Additionally, the computer needs to have Visum $>=$ 14.0 under an active license.

### Preparing a Scenario
The model is able to run two different types of scenario: a full scenario and a "small" scenario with a limited number of zones to quickly test new model functionality. The actual model code is installed in the full scenario.
- _To create a full scenario_, copy the `root/scenario/` folder with a new name.
- _To create a small scenario_, copy the `root/swim_small/` folder with a new name and copy the `root/scenario/model`^[**NOT** the `root/model/` directory!] folder containing the model code into the new small scenario folder.

#### Initial Highway Skims
The model requires initial year highway skims that are not included in the repository for space reasons. The analyst needs to create these skims for bare scenarios or scenarios where the zone geometry has changed from the reference scenario. To make these skims, open `root/%scenario_name%/inputs/t18/globalTemplateUpdate.properties` and edit the token

```py
new.zone.system=True
```

#### Running a scenario
To run a scenario, run the two batch batch files in the scenario directory in sequence.

```
build_run.bat 
run_model.bat
```
