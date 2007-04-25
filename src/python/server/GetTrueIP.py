import os, string, re
"""
For unix, Evan says:
If you're on a NAT network, there's no simple way to discover what the
external IP of the network is without connecting to a remote service and
asking it.  But if you want a list of interfaces, you can always use
"/sbin/ip addr list" if it's installed.  The network connection will be
on one of the interfaces that isn't "lo", which is the loopback interface.
"""
if 'OS' in os.environ:
    windows = "windows" in os.environ['OS'].lower()
else:
    windows = False

def trueIP():
    if windows:
        ipInfo = os.popen("ipconfig").read()
        ip = ipInfo.find("Local Area Connection")
        ip = ipInfo.find("IP Address", ip)
        ip = ipInfo.find(":", ip)
        if ip == -1:
            return "localhost"
        ipAddress = ipInfo[ip:].split()[1]
        return ipAddress
    else: # Assume some kind of *nix
        # Very hacky approach, but seems to work
        ipInfo = os.popen("/sbin/ifconfig").read()
        ips = re.findall("\d+\.\d+\.\d+\.\d+", ipInfo)
        ips.remove("127.0.0.1")
        return ips[0]

def machineName():
    if windows:
        return os.environ['COMPUTERNAME']
    else:
        return os.environ['HOSTNAME']


if __name__ == "__main__":
    print trueIP()
    print machineName()
