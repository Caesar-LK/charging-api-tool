#!/bin/bash
cd /home/ubuntu/charging-api-tool
nohup java -jar target/charging-api-tool.jar > app.log 2>&1 &
echo "PID: $!"
sleep 10
tail -20 app.log
