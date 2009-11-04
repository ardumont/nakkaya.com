---
title: Linux Firewall
tags: debian linux firewall
---

My trusty and old firewall script. Simple but effective, deny all
incoming connections except SSH and already established connections.
It is a good starting point to customize it to your needs.

    #!/bin/sh
    #

    #reject other connections...
    /sbin/iptables -P INPUT DROP
    /sbin/iptables -P FORWARD DROP

    #accept loopback interface
    /sbin/iptables -A INPUT -s 127.0.0.1 -d 127.0.0.1 -i lo -j ACCEPT

    #accept established connection to pass
    /sbin/iptables -A INPUT -m state --state ESTABLISHED,RELATED -j ACCEPT

    #enable clients to connect to ssh
    /sbin/iptables -A INPUT -m multiport -p tcp --dport ssh  -j ACCEPT

    #log activity (uncomment if needed)
    #/sbin/iptables -A INPUT -j LOG -m limit
