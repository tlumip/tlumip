# SWIM-TLUMIP
This is the model repository for the Oregon Statewide Integrated land use/transport model (SWIM).

The complete user's guide is on the [project wiki](https://github.com/pbsag/tlumip/wiki).

## Installation
Check out the repository. This repository uses [`git-lfs`](https://git-lfs.github.com), which the user will need to install separately.

On initial checkout, the user will need to:

  1. Expand [`root/model/lib/dependencies64.zip`](root/model/lib): included versions of Python, Java, and R.
  2. Download ACS PUMS data into [`root/model/census`](root/model/census), using the [`download.bash`](root/model/census/download.bash) script.

SWIM uses [Visum 15](http://vision-traffic.ptvgroup.com/en-us/products/ptv-visum/) to manage network data and run highway assignments. The user will need to install a licensed version of this software prior to running SWIM.

The current version of the model requires a computer with a 64-bit Windows operating system. The computer should have 48 gigabytes or more of RAM, and at least a few hundred gigabytes of free space on the model hard drive (a given model run may take up to 100 GB or more of hard drive space when finished). 
