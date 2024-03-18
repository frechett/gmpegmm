# GmpeGmm

PGA calculator program that uses the ground‐motion model (GMM) code from the U.S. Geological Survey (USGS)<br>
[National Seismic Hazard Mapping Project (NSHMP) Hazard Model Processing Library](https://code.usgs.gov/ghsc/nshmp/nshmp-lib).

### To build
Run [Apache Ant](https://ant.apache.org/) from the root of this repository.
> ant

### To get and build the latest version of the NSHMP Hazard Model Processing Library
1. git clone https://code.usgs.gov/ghsc/nshmp/nshmp-lib.git
2. cd nshmp-lib
3. ./gradlew fatjar

The fatjar containing all of the necessary class files will be found here:
> build/libs/nshmp-lib.jar
