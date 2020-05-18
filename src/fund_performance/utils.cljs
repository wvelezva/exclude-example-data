(ns fund-performance.utils)

(defn build-navs [nav-values]
  "use to transform the array of nav from a plain structure like this {:fund-id [[timestamp1 nav1][timestamp2 nav2]]}
  to a sorted map like {:fund-id {timestamp1 nav1, timestamp2 nav2}}"
  (reduce (fn [m fv]
            (assoc m
              (key fv)
              (into (sorted-map) (val fv))))
          {}
          nav-values))
