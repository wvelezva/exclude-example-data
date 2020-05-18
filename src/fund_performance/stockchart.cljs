(ns fund-performance.stockchart
  (:require [reagent.core :as r]
            [re-frame.core :as rf]
            [cljs-time.format :as time-format]
            [cljs-time.coerce :as time-coerce]))

(defonce custom-formatter (time-format/formatter "dd-MMM-yyyy"))

(defn reset-date-btn [{:keys [label period]}]
  (let [btn-selected? @(rf/subscribe [:period-selected? period])]
    [:a
     {:class   (str "btn btn-small" (when btn-selected? " btn-primary"))
      :onClick #(when (not btn-selected?)
                  (rf/dispatch [:compare-last period]))}
     label]))

(defn chart
  []
  (let [performance @(rf/subscribe [:series])
        nav-init-date @(rf/subscribe [:nav-init-date])
        formatted-date (when nav-init-date (time-format/unparse custom-formatter (time-coerce/from-long nav-init-date)))]
    [:<>
     [:div.row
      [:div.span4 "Performance Since: " formatted-date]
      [:div.span8
       [:div.pull-right "Compare Last "
        [reset-date-btn {:label "Month" :period :1-months}]
        [reset-date-btn {:label "3 Months" :period :3-months}]
        [reset-date-btn {:label "YTD" :period :ytd}]
        [reset-date-btn {:label "Year" :period :1-years}]
        [reset-date-btn {:label "3 Years" :period :3-years}]
        [reset-date-btn {:label "5 Years" :period :5-years}]
        " | "
        [reset-date-btn {:label "All" :period :10-years}]]]]]))
