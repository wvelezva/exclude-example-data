(ns fund-performance.subs
  (:require [re-frame.core :refer [reg-sub]]))

(reg-sub
  :internal?
  (fn [db _]
    (get db :internal? false)))

(reg-sub
  :base-url
  (fn [db _]
    (get db :base-url)))

(reg-sub
  :favorites-pref-name
  (fn [db _]
    (get db :favorites-pref-name)))

(reg-sub
  :selected-pref-name
  (fn [db _]
    (get db :selected-pref-name)))

(reg-sub
  :period-selected?
  (fn [db [_ period_selected]]
    (let [current-period-selected (get db :period-selected)]
      (= period_selected current-period-selected))))

(reg-sub
  :nav-init-date
  (fn [db _]
    (get-in db [:nav-info :nav-init-date])))

(reg-sub
  :nav-values
  (fn [db _]
    (get-in db [:nav-info :values])))

(defn- calc-performance [navs]
  (let [first-nav (val (first navs))]
    (reduce (fn [m nav]
              (conj m [(key nav) (* 100 (- (/ (val nav) first-nav) 1))]))
            []
            navs)))

(reg-sub
  :series
  :<- [:nav-init-date]
  :<- [:nav-values]
  :<- [:fund.filters.subs/selected-funds [:fund.list.subs/funds]]
  (fn [[nav-init-date nav-values selected-funds] _]
    (let [selected-fund-keys (map :id selected-funds)
          selected-fund-navs (select-keys nav-values selected-fund-keys)
          navs-since-date (reduce (fn [m fund-nav]
                                    (assoc m (key fund-nav) (subseq (val fund-nav) >= nav-init-date)))
                                  {}
                                  selected-fund-navs)
          valid-navs (into {} (filter (comp some? val) navs-since-date))]
      (reduce (fn [m navs]
                (let [fund (first (filter #(= (:id %) (key navs)) selected-funds))]
                  (conj m
                        {:name    (str (:code fund) " " (:name fund))
                         :color   (:color fund)
                         :tooltip {:pointFormat (str (:code fund) ": <b>{point.y:.2f}%</b>")}
                         :data    (calc-performance (val navs))})))
              []
              valid-navs))))

(reg-sub
  :logged-profile
  (fn [db _]
    (get db :logged-profile)))

(reg-sub
  ::selected-account
  (fn [db _]
    (get db :selected-account)))

(reg-sub
  :accounts
  (fn [db _]
    (get db :accounts)))

(defn account-as-option
  [account]
  (if account
    {:value (:acctid account)
     :label (str (:acctnum account) " " (:name account))}
    {}))

(reg-sub
  ::accounts-as-options
  :<- [:accounts]
  (fn [accounts _]
    (sort-by :label (map account-as-option accounts))))