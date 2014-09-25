#! /bin/bash

#############################################################################
# This file installs the kairos database on the machine
#
# Parameters the should be exported beforehand:
# 	$LUS_IP_ADDRESS - Ip of the head node that runs a LUS and ESM. May be my IP. (Required)
#   $GSA_MODE - 'agent' if this node should join an already running node. Otherwise, any value.
#   $NO_WEB_SERVICES - 'true' if web-services (rest, webui) should not be deployed (only if GSA_MODE != 'agent')
#   $NO_MANAGEMENT_SPACE - 'true' if cloudifyManagementSpace should not be deployed (only if GSA_MODE != 'agent')
#   $NO_MANAGEMENT_SPACE_CONTAINER - 'true' if container for cloudifyManagementSpace should not be started(only if GSA_MODE != 'agent')
#   $MACHINE_IP_ADDRESS - The IP of this server (Useful if multiple NICs exist)
# 	$WORKING_HOME_DIRECTORY - This is where the files were copied to (cloudify installation, etc..)
#	$GIGASPACES_LINK - If this url is found, it will be downloaded to $WORKING_HOME_DIRECTORY/gigaspaces.zip
#	$GIGASPACES_OVERRIDES_LINK - If this url is found, it will be downloaded and unzipped into the same location as cloudify
#	$CLOUD_FILE - Location of the cloud configuration file. Only available in bootstrap of management machines.
#	$GIGASPACES_CLOUD_IMAGE_ID - If set, indicates the image ID for this machine.
#	$GIGASPACES_CLOUD_HARDWARE_ID - If set, indicates the hardware ID for this machine.
#	$AUTO_RESTART_AGENT - If set to 'true', will allow to perform reboot of agent machine.
#	$PASSWORD - the machine password.
#############################################################################
# some distro do not have which installed so we're checking if the file exists 
if [ -f /usr/bin/wget ]; then
	DOWNLOADER="wget"
elif [ -f /usr/bin/curl ]; then
	DOWNLOADER="curl"
fi


# args:
# $2 the error code of the last command (should be explicitly passed)
# $3 the message to print in case of an error
# 
# an error message is printed and the script exists with the provided error code
function error_exit {
	echo "$3 : error code: $2"
	exit ${2}
}

# args:
# $1 the error code of the last command (should be explicitly passed)
# $2 the message to print in case of an error 
# $3 the threshold to exit on
#
# if (last_error_code [$1]) >= (threshold [$4]) (defaults to 0), the script
# exits with the provided error code [$2] and the provided message [$3] is printed
function error_exit_on_level {
	if [ ${1} -ge ${4} ]; then
		error_exit ${2} ${3}
	fi
}

# args:
# $1 download description.
# $2 download link.
# $3 output file.
# $4 the error code.
function download {
	echo Downloading $1 from $2
	if [ "$DOWNLOADER" = "wget" ];then
		Q_FLAG="-q"
		O_FLAG="-O" 
		LINK_FLAG=""
	elif [ "$DOWNLOADER" = "curl" ];then
		Q_FLAG="--silent"
		O_FLAG="-o"
		LINK_FLAG="-O"
	fi
	$DOWNLOADER $Q_FLAG $O_FLAG $3 $LINK_FLAG $2 || error_exit $? $4 "Failed downloading $1"
}

# download path for kairos
KAIROS_DB_URL="http://dl.bintray.com/brianhks/generic/kairosdb-0.9.3.tar.gz"

# download the kairos database
download "Kairos Database" $KAIROS_DB_URL $WORKING_HOME_DIRECTORY/kairos.tar.gz 901

# untar the kairosdb
tar xfz $WORKING_HOME_DIRECTORY/kairos.tar.gz -C ~/kairos || error_exit_on_level $? 902 "Failed extracting kairos database." 2

# configure the kairosdb
sed -i '/kairosdb.jetty.port/c\kairosdb.jetty.port=9001' ~/kairos/conf/kairosdb.properties || error_exit $? 903 "Error configuring the kairos database"

# start the kairos db as deamon
~/kairos/bin/kairosdb.sh start || error_exit $2 904 "Error starting the kairos database"

exit 0;