(ns clj-uuid.digest-test
  (:require [clojure.test    :refer :all]
            [clj-uuid.bitmop :refer :all]
            [clj-uuid.digest :refer :all]))


(deftest check-md5-digest
  (testing "md5..."    
    (is (=
          (clojure.core/map ub8 (md5 "xyz"))
          [209 111 179 111 9 17 248 120 153 140 19 97 145 175 112 94]))
    (is (=
          (md5 "xyz")
          [-47 111 -77 111 9 17 -8 120 -103 -116 19 97 -111 -81 112 94]))
    (is (=
          (md5 "xyzabc")
          [71 45 -55 14 -44 57 -47 24 -83 58 32 93 -38 -106 -22 -16]))
    (is (=
          (md5  "x")
          [-99 -44 -28 97 38 -116 -128 52 -11 -56 86 78 21 92 103 -90]))))


(deftest check-sha1-digest
  (testing "sha1..."    
    (is (=
          (map ub8 (sha1 "xyz"))
          [102 178 116 23 211 126 2 76 70 82 108 47 109 53 138
           117 79 197 82 243]))
    (is (=
          (sha1 "xyz")
          [102 -78 116 23 -45 126 2 76 70 82 108 47 109 53
           -118 117 79 -59 82 -13]))    
    (is (=
          (sha1 "abcdef")
          [31 -118 -63 15 35 -59 -75 -68 17 103 -67 -88 75
           -125 62 92 5 122 119 -46]))
    (is (=
          (sha1 "x")
          [17 -10 -83 -114 -59 42 41 -124 -85 -86 -3 124 59
           81 101 3 120 92 32 114]))))

(deftest check-digest-uuid
  (let [x500 [107 -89 -72 20 -99 -83 17 -47 -128 -76 0 -64 79 -44 48 -56]
        url  [107 -89 -72 17 -99 -83 17 -47 -128 -76 0 -64 79 -44 48 -56]
        oid  [107 -89 -72 18 -99 -83 17 -47 -128 -76 0 -64 79 -44 48 -56]
        dns  [107 -89 -72 16 -99 -83 17 -47 -128 -76 0 -64 79 -44 48 -56]]    
    (testing "digest-uuid with md5..."
      (is (= (digest-uuid-bytes md5 x500 "")
            [122 -81 17 -116 -15 116 -2 -70 -98 -59 104 12 -41 -111 -96 32]))
      (is (= (digest-uuid-bytes md5 dns  "")
            [-56 126 -26 116 77 -36 62 -2 39 78 -33 -30 93 -91 -41 -77]))
      (is (= (digest-uuid-bytes md5 dns  "hello ladies")
            [28 -67 105 -127 88 41 -43 -88 106 84 94 125 -98 -42 -23 99]))
      (is (= (digest-uuid-bytes md5 dns  "get lost creep")
            [118 6 -49 51 -41 -44 -111 -51 -40 -90 -111 -37 40 -82 32 89])))
    (testing "digest-uuid with sha1..."
      (is (= (digest-uuid-bytes sha1 x500 "")
            [-76 -67 -8 116 -116 3 75 -40 -113 -41 94 64 -99 -3 -126 -64]))    
      (is (= (digest-uuid-bytes sha1 dns  "")
            [78 -67 2 8 -125 40 125 105 -52 68 -20 80 -109 -100 9 103]))    
      (is (= (digest-uuid-bytes sha1 dns  "hello ladies")
            [-93 -86 -19 -47 -116 6 -101 -60 -10 -98 -117 -106 69 111 -30 2]))
      (is (= (digest-uuid-bytes sha1 dns  "get lost creep")
            [100 -106 107 -32 -18 -59 -63 33 60 -71 119 46 -15 77 -119 -91])))))
