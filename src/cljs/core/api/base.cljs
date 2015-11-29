(ns core.api.base)

;;create page-info general api purpose object.
(defn paging-info [pagenr pagesize]
  {:nr pagenr, :size pagesize})
