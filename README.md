## 基于Netty 原生实现的 websocket 或者 tcp通道，可以实现两个协议间的相互转换

## usage

java -jar Tcp_Over_Websockets.jar  [websocket|tcp] [portArr]

example:
1. websocket to tcp:
	* java -jar Tcp_Over_Websockets.jar websocket port1,port2
2. tcp to websocket:
	* java -jar Tcp_Over_Websockets.jar tcp port1,port2