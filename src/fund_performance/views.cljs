(ns fund-performance.views
  (:require [re-frame.core :as rf]
            [fund-performance.stockchart :refer [chart]]
            [fund-performance.subs :as subs]
            [fund.filters.views :refer [filters]]
            [fund.list.views :refer [fund-list]]
            [select.core :refer [select]]))

(def callback
  {:before (fn [fund selected?] (.log js/console "loading"))
   :after (fn [fund selected?] (rf/dispatch-sync [:load-navs (:id fund) selected?]))})

(defmulti custom-filter identity)

(defmethod custom-filter :default
  [type]
  [:div])

(defmethod custom-filter :advisor
  [type]
  (let [selected-account @(rf/subscribe [::subs/selected-account])
        accounts-as-options @(rf/subscribe [::subs/accounts-as-options])]
    [:div.span3
     [:label "Plan:"]
     [select {:name  "plans"
              :value (when selected-account (:acctid selected-account))
              :options accounts-as-options
              :on-change #(rf/dispatch [:change-account (get (js->clj %) "value")])}]]))

(defn main-panel []
  (let [fund-filters-loaded? @(rf/subscribe [:fund.filters.subs/loaded])
        logged-profile @(rf/subscribe [:logged-profile])]
    [:<>
     [chart]
     (if fund-filters-loaded?
       [:<>
        [filters {:suspended-enabled? false
                  :custom-filter #(custom-filter logged-profile)}]
        [fund-list callback]]
       [:div "loading..."])]))