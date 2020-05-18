(ns fund-performance.effects
  (:require
    [re-frame.core :refer [reg-fx]]
    [clojure.string :refer [last-index-of replace]]))

(reg-fx
  :redirect
  (fn [account]
    (let [href (.. js/window -location -href)
          clean-path (replace href #"/\d*$" "")
          account-performance-link (str clean-path "/" account)]
      (set! js/window.location.href account-performance-link))))