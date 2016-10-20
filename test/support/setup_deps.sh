#!/usr/bin/env bash

mkdir checkouts
cd checkouts && \
    git clone https://github.com/USGS-EROS/lcmap-config.git && \
    git clone https://github.com/USGS-EROS/lcmap-logger.git && \
    git clone https://github.com/USGS-EROS/lcmap-client-clj.git && \
    cd ../

mkdir ~/.usgs/
cp test/support/sample_config.ini ~/.usgs/lcmap.ini
mkdir /home/travis/.usgs
cp test/support/sample_config.ini /home/travis/.usgs/lcmap.ini
