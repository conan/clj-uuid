(ns clj-uuid.node
  (:require [clj-uuid.util   :refer [java6? compile-if]])
  (:require [clj-uuid.bitmop :refer [sb8]])
  (:import  [java.net         InetAddress NetworkInterface]
            [java.security    MessageDigest]
            [java.util        Properties]))



;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; NodeID Representation
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
;; The representation of NodeID used for consutruction of time-based (v1) UUIDs
;; is a LIST with the following encoding semantics:
;;
;;               SIZE    TYPE      REPRESENTATION
;;  -----------+------+---------+---------------------------------------------
;;  node       |    6 |  ub48   |  (<BYTE> <BYTE> <BYTE> <BYTE> <BYTE> <BYTE>)
;;
;; The reason that a list of bytes is used is that the v1 lsb computation
;; requires prepending two other (computed) bytes to the node-id before
;; bitwise assembly.  A list is an efficient, immutable data structure that
;; can be continually reused for the calculation by simply 'cons'ing twice to
;; the head of the list and then quick, linear scan of the resulting eight
;; bytes to construct the final ^long lsb.  
;; 
;;  (cons clock-high (cons clock-low +node-id+))
;;
;;  
;;      ( <BYTE> . <BYTE> . <BYTE> <BYTE> <BYTE> <BYTE> <BYTE> <BYTE>)
;;

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; NodeID Calculation
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
;; This turns out to be surprisingly problematic.  I've tried various
;; approaches.  The most straightforward is the use of IEEE 802 MAC Address:
;;
;;     (.getHardwareAddress
;;       (java.net.NetworkInterface/getByInetAddress 
;;         (java.net.InetAddress/getLocalHost))))))
;;
;; Unfortunately got reports of NPE on some platforms (openjdk?).  Also, it
;; discloses the hardware address of the host system -- this is how the 
;; creator of the melissa virus was actually tracked down and caught.
;;
;; choosing node-id randomly does not provide consistent generation of UUID's
;; across runtimes.
;;
;; This topic is specifically addressed by the RFC:
;; 
;;
;;   "A better solution is to obtain a 47-bit cryptographic quality random
;;   number and use it as the low 47-bits of the Node-ID, with the least
;;   significant bit of the first octet of the Node-ID set to one.  This
;;   bit is the unicast/multicast bit, which will never be set in IEEE 802
;;   addresses obtained from network cards.  Hence, there can never be a
;;   conflict between UUID's generated by machines with and without network
;;   cards."
;;
;;                               . . .
;;
;;   "In addition, items such as the computer's name and the name of the
;;   operating system, while not strictly speaking random, will help
;;   differentiate the results from those obtained by other systems...
;;   ... A generic approach... IS TO ACCUMULATE AS MANY SOURCES AS POSSIBLE
;;   INTO A BUFFER, USE A MESSAGE DIGEST SUCH AS MD5 OR SHA1, TAKE AN
;;   ARBITRARY 6 BYTES FROM THE HASH VALUE, AND SET THE MULTICAST BIT
;;   AS DESCRIBED ABOVE."
;;
;;     -- [RFC4122:4.5 "Node IDs that do not Identify the Host"]
;;
;;
;; We do exactly that.  Taking into account that the term "first octet"
;; in the above excerpt refers to network transmission order, and we
;; 'bit-or' the corresponding bytes:
;;                                                      
;;     hi-byte | byte5 | byte4 | byte3 | byte2 | lo-byte
;;    ---------+-------+-------+-------+-------+---------
;;       0x00  |  0x00 |  0x00 |  0x00 |  0x00 |   0x01      
;;                                                      
;; Thanks to Datastax and to @jjcomer for submitting the original patch
;; from which this current implementation is largely derived.
;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
 


(defn- all-local-addresses []
  (let [^InetAddress local-host (InetAddress/getLocalHost)
        host-name (.getCanonicalHostName local-host)
        base-addresses #{(str local-host) host-name}
        network-interfaces (reduce (fn [acc ^NetworkInterface ni]
                                     (apply conj acc
                                       (map str (enumeration-seq
                                                  (.getInetAddresses ni)))))
                             base-addresses
                             (enumeration-seq
                               (NetworkInterface/getNetworkInterfaces)))]
    (reduce conj network-interfaces
      (map str (InetAddress/getAllByName host-name)))))


(defn- make-node-id []
    (let [addresses (all-local-addresses)
          ^MessageDigest digest (MessageDigest/getInstance "MD5")
          ^Properties    props  (System/getProperties)
          to-digest (reduce (fn [acc key]
                              (conj acc (.getProperty props key)))
                      addresses ["java.vendor" "java.vendor.url"
                                 "java.version" "os.arch"
                                 "os.name" "os.version"])]
      (doseq [^String d to-digest]
        (compile-if (java6?)
          (.update digest (.getBytes d))
          (.update digest
            (.getBytes d java.nio.charset.StandardCharsets/UTF_8))))
      (map bit-or
        [0x00 0x00 0x00 0x00 0x00 0x01]
        (take 6 (seq (.digest digest))))))



;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Public NodeID API
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def node-id   (memoize make-node-id))

(def +node-id+ (node-id))

