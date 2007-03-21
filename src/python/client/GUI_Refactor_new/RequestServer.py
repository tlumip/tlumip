from SimpleXMLRPCServer import SimpleXMLRPCServer as XMLRPCServer
from threading import Thread

class RequestServer(object):
    """
    Base class for an xml-rpc server.
    """
    def __init__(self, ip, port):
        server = XMLRPCServer((ip, port), logRequests = False)
        server.register_instance(self)
        Thread(target=server.serve_forever).start()