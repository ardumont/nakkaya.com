---
title: A Guide to Raw Traffic in Clojure
tags: clojure libpcap
---

This tutorial will cover capturing raw network traffic going via your
network interface. We will implement a arp sweeper. 
[Arp](http://en.wikipedia.org/wiki/Address_Resolution_Protocol) is the
protocol that is used to find out who uses which IP on the network. So
using a arp sweeper you can locate machines on your network pretty
quickly.


For this script to work you need libpcap library and jpcap, libpcap's
java bindings. libpcap is a packet capture library that allows you to
read traffic going through your network card and disassemble captured
packets.


#### Preflight Checks

 - Install libpcap if not installed.
 - Build and install jpcap.
 - Download the [script](/code/clojure/arp-sweep.clj)

Code needs to run as root if you want it to capture anything.

#### Theory

What we are going to do is ask the network, for each IP in the block
that who has it.

 - Who has 192.1.1.1?
 - Who has 192.1.1.2?
 - Who has 192.1.1.3?
 - so on...

Then we start listening, machines on the network will respond...

 - 192.1.1.1 is at 00:00:00:00:00:00
 - 192.1.1.2 is at 01:01:01:01:01:01
 - so on...

#### Code

Let's dissect the code.

     (defn interface-info []
       (doseq [device  (JpcapCaptor/getDeviceList)]
         (let  [name   (.name device)
                mac    (mac-byte-to-string (.mac_address device))
                ip     (interface-ip device)]
           (println (print-device-info name mac) ip))))

interface-info will print name, mac and ip of the all interfaces on your
machine. (JpcapCaptor/getDeviceList) returns an array of interfaces. For
each device on the machine we query it's name, mac and ip. Note that this
will only recognize interface's ipv4 address. But you can easily modify
it for ipv6.

    (defn mac-byte-to-string [mac-bytes]
      (let [v  (apply vector 
                      (map #(Integer/toHexString (bit-and % 0xff)) mac-bytes))]
        (apply str (interpose ":" v))))

Interfaces mac id is represented as a byte array to visualize it we need
to convert it to familiar hex representation.

    (defn ipv4? [inet-addrs]
      (if (instance? java.net.Inet4Address inet-addrs)
        true false))

    (defn ipv4-addrs [addr-list]
      (filter ipv4? (apply vector (map (fn[i] (.address i)) addr-list))))

Since interface may contain both v4 and v6 IP. We filter only v4
IP's. Now we know all the information about the interface's on the
machine we are ready to start capturing some traffic from the
network. We first need to open the device for listening.

    (defn open-captor [interface]
      (JpcapCaptor/openDevice interface 50 true 0))

This will open the device for listening, 50 bytes will be captured at most
using promiscuous mode, which means all packets whether they are meant
for your machine or not can be read, and a 0 milisecond timeout.


     (defn arp-sweep [interface]
       (let  [interface (interface-by-name interface)
              captor (open-captor interface)]
    
         (send-arp-probe captor interface (generateip-ip-list interface))

         (doto (Thread. #(.loopPacket captor -1 (packet-callback)))
           (.start))
    
         (Thread/sleep 3000)
         (.breakLoop captor)))

This is where the actual scanning takes place. We open the interface for
listening. We send arp-request packages for all IP's on the
network. Wait for replies on another thread. Since we don't know which
machines are on and which ones are off the best we do is wait for a
while in this case 3 seconds then assume all responded. 3 seconds is
a lot in this case most programs set it well below 1 second.

loopPacket will loop forever waiting for packets, that's why we run it on
a separate thread, when it receives a packet it will call our call back
function.

     (defn packet-callback []
       (proxy [PacketReceiver] []
         (receivePacket
          [packet]
          (if (instance? ARPPacket packet)
            (let  [src-ip (.getSenderProtocolAddress packet)
                   src-mac (.getSenderHardwareAddress packet)] 
              (println (.getHostAddress src-ip) " is at " src-mac))))))

For each packet received we check that if it is a arp packet. Remember we
are capturing all traffic going through the device. If it is of type
ARPPacket we disassemble it getting it's source IP and MAC which is a
machine on the network.

    (defn create-arp-request [interface target]
      (let  [broadcast    (into-array (Byte/TYPE) (repeat 6 (byte 255)))
             srcip        (interface-ip interface)
             arp-packet   (ARPPacket.)
             ether-packet (EthernetPacket.)]
        ;;arp
        (set! (.hardtype arp-packet) ARPPacket/HARDTYPE_ETHER)
        (set! (.prototype arp-packet) ARPPacket/PROTOTYPE_IP)
        (set! (.operation arp-packet) ARPPacket/ARP_REQUEST)
        (set! (.hlen arp-packet) 6)
        (set! (.plen arp-packet) 4)
        (set! (.sender_hardaddr arp-packet) (.mac_address interface))
        (set! (.sender_protoaddr arp-packet) (.getAddress srcip))
        (set! (.target_hardaddr arp-packet) broadcast)
        (set! (.target_protoaddr arp-packet) 
              (.getAddress (InetAddress/getByName target)))
        ;;ether
        (set! (.frametype ether-packet) EthernetPacket/ETHERTYPE_ARP)
        (set! (.src_mac ether-packet) (.mac_address interface))
        (set! (.dst_mac ether-packet) broadcast)
        ;;wire
        (set! (.datalink arp-packet) ether-packet)
        arp-packet))

This is our request packet that asks Who has 192.1.1.5?. Two things to
note here is that. First it is of type ARPPacket/ARP_REQUEST and we send
it to FF:FF:FF:FF:FF:FF which is the broadcast mac address. For more
information on the 
[ARP Packet Structure](http://en.wikipedia.org/wiki/Address_Resolution_Protocol#Packet_structure)
refer to Wikipedia.

     (defn generateip-ip-list [interface]
       (let [ip (.getHostAddress (interface-ip interface))
             block (.substring ip 0 (+ 1 (.lastIndexOf ip ".")))]
         (vec (map #(str block %) (range 1 255)))))

A very quick way to a get list of all IP's on the block. This will
return a vector of IP's. Such as [.. 192.1.1.5 192.1.1.6 ..]


That's all after you run the script, you should see a list of machines on
your network with their IP's and MAC id's.

