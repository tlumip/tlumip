//Define TS handler classes - these are the rpc end points

//Default port to listen for connections for this node
listenPort = 6001

//Define node URLs
nodes[0].name = "thinkPad"
nodes[0].url = "tcp://localhost:6001"
//nodes[0].url = "tcp://192.168.1.103:6001"

nodes[1].name = "isis"
nodes[1].url = "tcp://192.168.1.214:6001"

//Define handler classes, these are the rpc-end points on this node
handlers[0].name = "networkHandler"
handlers[0].className = "com.pb.tlumip.ts.NetworkHandler"
handlers[0].node = "thinkPad"

handlers[1].name = "demandHandler"
handlers[1].className = "com.pb.tlumip.ts.DemandHandler"
handlers[1].node = "thinkPad"

handlers[2].name = "shortestPathTreeHandler"
handlers[2].className = "com.pb.tlumip.ts.ShortestPathTreeHandler"
handlers[2].node = "thinkPad"

handlers[3].name = "aonFlowHandler"
handlers[3].className = "com.pb.tlumip.ts.AonFlowHandler"
handlers[3].node = "thinkPad"
