(ns fund-performance.db
  (:require [clojure.spec.alpha :as s]
            [utils.core :as utils]
            [clojure.string :refer [blank?]]
            [fund.filters.db]
            [fund.list.db]))

(s/def ::base-url utils/isURI?)
(s/def ::favorites-pref-name (s/and string? #(not (blank? %))))
(s/def ::selected-pref-name (s/and string? #(not (blank? %))))
(s/def ::period-selected #{:1-months
                           :3-months
                           :ytd
                           :1-years
                           :3-years
                           :5-years
                           :10-years})

(s/def ::issue-date pos-int?)

(s/def ::nav-init-date pos-int?)
(s/def ::min-nav-date pos-int?)
(s/def ::max-nav-date pos-int?);;dates are saved as millis
(s/def ::nav map-entry?)
(s/def ::values (s/map-of :fund.list.db/code (s/coll-of ::nav)))
(s/def ::nav-info (s/keys :opt-un [::nav-init-date
                                   ::min-nav-date
                                   ::max-nav-date
                                   ::values]))

(s/def ::logged-profile #{:client :advisor :admin})

(s/def ::acctid pos-int?)
(s/def ::acctnum (s/and string? #(not (blank? %))))
(s/def ::name (s/and string? #(not (blank? %))))
(s/def ::account (s/keys :req-un [::acctid ::acctnum ::name]))
(s/def ::accounts (s/coll-of ::account))

(s/def ::selected-account ::account)

(s/def ::db (s/keys :req-un [::base-url
                             ::favorites-pref-name
                             ::selected-pref-name
                             ::fund.list.db/fund-list]
                    :opt-un [::fund.filters.db/fund-filters
                             ::nav-info
                             ::period-selected
                             ::issue-date
                             ::logged-profile
                             ::accounts
                             ::selected-account]))
