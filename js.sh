#!/usr/bin/env bash

#echo '################ RIMOZIONE node_modules ###############################################'
#rm -r src/main/webapp/node_modules
#rm -r node_modules

echo '################ RIMOZIONE bower_components ###############################################'
rm -r src/main/webapp/bower_components
#rm -r bower_components


#echo '################ INSTALL NPM ###############################################'
#npm install


echo '################ BOWER install ###############################################'
#bower install src/main/webapp/node_modules
bower install
