# check for command line argument (media path)
if [ $# -ne 1 ]
then
    echo "Usage: ./deploy_server.sh <Media Directory>"
    exit
fi
# Obtain the IP address of this machine
ip_address=`ifconfig | awk -F':' '/inet addr/&&!/127.0.0.1/{split($2,_," ");print _[1]}'`
echo "INFO: Obtianed IP address:$ip_address"

# Compile the Server Code
rm fileServer/class/*
javac -d fileServer/class -cp fileServer/jetty-all-uber.jar fileServer/FileServer.java
class_count=`ls fileServer/class | wc -l`
if [ $class_count == 2 ]
then
    echo "INFO: Compilation success"
else
    echo "ERROR: Compilation failed"
fi

dir=`pwd`
# Start the RTSP server
killall live555MediaServer
cp rtspServer/live555MediaServer $1
cd $1
#cd rtspServer/
./live555MediaServer &
#cd ../
cd $dir
echo "INFO: RTSP server running.."


cp fileServer/jetty-all-uber.jar fileServer/class
cd fileServer/class
echo "INFO: File server running.."
java -cp .:jetty-all-uber.jar FileServer $ip_address $1
