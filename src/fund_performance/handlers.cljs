(ns fund-performance.handlers
  (:require [re-frame.core :as rf :refer [reg-event-db reg-event-fx]]
            [re-frame.loggers :refer [console]]
            [utils.interceptors :refer [chain-interceptors]]
            [fund-performance.db]
            [fund-performance.utils :refer [build-navs]]
            [day8.re-frame.http-fx]
            [ajax.core :as ajax]
            [cljs-time.core :refer [date-time year months years today minus]]
            [cljs-time.coerce :refer [to-long from-long]]))

(reg-event-db
  :initialize-db
  (chain-interceptors :fund-performance.db/db)
  (fn [db [_ src-spec]]
    (let [logged-profile (->> [:client :advisor :admin]
                              (select-keys src-spec)
                              (filter val)
                              (first)
                              (key))]
      (merge {:logged-profile logged-profile}
        (select-keys src-spec [:base-url
                               :favorites-pref-name
                               :selected-pref-name
                               :fund-list
                               :fund-filters
                               :nav-info
                               :issue-date
                               :accounts
                               :selected-account])))))

(reg-event-db
  :assoc-navs
  (chain-interceptors :fund-performance.db/db)
  (fn [db [_ fund-navs]]
    (if (empty? fund-navs)
      db
      (let [max-date-from-result (apply min (map #(get (last (val %)) 0) fund-navs))
            current-max-nav-date (get-in db [:nav-info :max-nav-date])
            new-max-nav-date (if (nil? current-max-nav-date)
                               max-date-from-result
                               (min current-max-nav-date max-date-from-result))

            current-nav-init-date (get db :issue-date (get-in db [:nav-info :nav-init-date]))
            init-date-from-result (apply max (map #(get-in (val %) [0 0]) fund-navs))
            new-nav-init-date (if (nil? current-nav-init-date)
                                init-date-from-result
                                (if (> current-nav-init-date new-max-nav-date)
                                  init-date-from-result
                                  (max current-nav-init-date init-date-from-result)))

            current-min-nav-date (get-in db [:nav-info :min-nav-date])
            new-min-nav-date (if (nil? current-min-nav-date)
                               init-date-from-result
                               (max current-min-nav-date init-date-from-result))]
        (-> db
            (dissoc :issue-date)
            (assoc-in [:nav-info :nav-init-date] new-nav-init-date)
            (assoc-in [:nav-info :min-nav-date] new-min-nav-date)
            (assoc-in [:nav-info :max-nav-date] new-max-nav-date)
            (update-in [:nav-info :values] merge (build-navs fund-navs)))))))

(reg-event-fx
  :load-navs
  (fn [{:keys [db]} [_ fund-id selected?]]
    (let [load-navs? (or
                       (nil? fund-id)
                       (and selected? (not (get-in db [:nav-info :values (str fund-id)]))))]

      (if load-navs?
        (let [base-url @(rf/subscribe [:base-url])]
          {:http-xhrio {:method          :get
                        :uri             (str base-url "/rest/nav/load")
                        :timeout         8000                     ;; optional see API docs
                        :params          (when fund-id
                                           {:fund-id fund-id})
                        :format          (ajax/json-request-format)
                        :response-format (ajax/json-response-format) ;; IMPORTANT!: You must provide this.
                        :on-success      [:assoc-navs]}})
        (when (not-any? :selected? (get-in db fund.list.subs/funds-path))
           {:db (-> db
                    (dissoc :period-selected) ;revisar si no hay selected funds y retornar {:db } con sin period-selected)))))
                    (update :nav-info dissoc :nav-init-date))})))))

(reg-event-db
  :change-nav-init-date
  (chain-interceptors :fund-performance.db/db)
  (fn [db [_ new-nav-init-date]]
    (-> db
      (assoc-in [:nav-info :nav-init-date] new-nav-init-date)
      (dissoc :period-selected))))

(defonce millis-in-a-day 86400000)

(defn build-period [period-selected]
  (case period-selected
    :1-months (months 1)
    :3-months (months 3)
    :1-years (years 1)
    :3-years (years 3)
    :5-years (years 5)
    :10-years (years 10)))

(defn lower-limit [upper-limit period-selected]
  (if (= period-selected :ytd)
    (date-time (year (today)))
    (minus upper-limit (build-period period-selected))))


(reg-event-db
  :compare-last
  (chain-interceptors :fund-performance.db/db)
  (fn [db [_ period-selected]]
    (let [max-nav-date (get-in db [:nav-info :max-nav-date])
          upper-limit-date (if max-nav-date
                             (from-long max-nav-date)
                             (today))
          result-date (to-long (lower-limit upper-limit-date period-selected))
          min-nav-date (get-in db [:nav-info :min-nav-date] 0)
          new-nav-date (if (< result-date min-nav-date)
                         min-nav-date
                         (if (> result-date upper-limit-date)
                           (get-in db [:nav-info :nav-init-date])
                           result-date))]
      (-> db
          (assoc :period-selected period-selected)
          (assoc-in [:nav-info :nav-init-date] new-nav-date)))))

(reg-event-fx
  :change-account
  (fn [db [_ new-account]]
    (when new-account
      {:redirect new-account})))


;;I tried to show the chart loading animation using an interceptor, it seem promising but the :before function from the
;; interceptor, take to long to be called so I am calling the loading before the dispatch calls in view manually
;;(def show-loading
;;  "An interceptor to start the loading animation on the chart ratom from fund-performance.stockchart.
;;  If the db doesn't change the date it stops the animation"
;;  (->interceptor
;;    :id     :show-loading
;;    :before (fn debug-before
;;              [context]
;;              (let [chart @fund-performance.stockchart/js-chart]
;;                (when (not (nil? chart))
;;                  (do
;;                    (console :log "start chart loading")
;;                    (.showLoading chart))))
;;              context)
;;    :after  (fn debug-after
;;              [context]
;;              (let [orig-db (get-coeffect context :db)
;;                    new-db  (get-effect   context :db ::not-found)
;;                    date-change? (and (not= new-db ::not-found)
;;                                      (not= (get-in orig-db [:nav-info :nav-init-date]) (get-in new-db [:nav-info :nav-init-date])))]
;;                (when-not date-change?
;;                  (let [chart @fund-performance.stockchart/js-chart]
;;                    (when (not (nil? chart))
;;                      (do
;;                        (console :log "stop chart loading since there are no changes")
;;                        (.hideLoading chart)))))
;;                context))))