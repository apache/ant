sleeptime=10
logfile=spawn.log
if  [ $# -ge 1 ]; then
   sleeptime=$1
   echo $sleeptime
fi
if  [ $# -ge 2 ]; then
   logfile=$2
   echo $logfile
fi
echo hello
rm  $logfile
sleep $sleeptime
echo bye bye > $logfile
echo bye bye
