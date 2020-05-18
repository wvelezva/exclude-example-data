(ns fund-performance.core
  (:require [reagent.core :as r]
            [re-frame.core :as rf]
            [favorite.events]
            [fund-performance.handlers]
            [fund-performance.subs]
            [fund-performance.views :as views]
            [fund-performance.utils :refer [build-navs]]
            [fund-performance.effects]
            [fund.filters.handlers]
            [fund.filters.subs]
            [fund.list.subs]
            [utils.core :refer [debug?]]
            [clojure.walk :refer [stringify-keys]]
            [fund-performance.example-data :as data]))

(defn- prepare-spec [spec]
  (if (and debug? (not (:dev spec)))
    (dissoc spec :fund-filters ::nav-info)
    spec))

(defn ^:dev/after-load mount-root
  []
  (r/render [views/main-panel] (.getElementById js/document "fund-performance-app")))

(defn ^:export init
  [spec-js]
  (let [spec (js->clj spec-js :keywordize-keys true)
        prod-spec (prepare-spec spec)]

    (rf/dispatch-sync [:initialize-db prod-spec])

    (when debug?
      (rf/dispatch [:assoc-navs data/stock-data]))

    (mount-root)))