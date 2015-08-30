(ns core.db.entities)

; Below mapping should be defined using concise macro defined api as follows:
; (defentity user-login
;   (from :username to :user/name     with {:required true})
;   (from :password to :user/password with {:required true})
;   (from :roles    to :user/roles    with {:required true})
;   (from :tenant   to :user/tenant   with {:lookup-ref #([:user/name %])}))
; Above defentity is a macro and from is also macro which should be expanded.
; Entity will add datomic attribute :shared/type representing type of mapping, when pulling entities.
(def user-login {
                 :type :user-login,
                 :write-mapping {
                                 :username {:key :user/name,
                                            :required true},
                                 :password {:key :user/password,
                                            :required true},
                                 :roles    {:key  :user/roles,
                                            :required true},
                                 :tenant   {:key :user/tenant,
                                            :lookup-ref #([:user/name %])}}
                 :read-mapping {
                                :user/name     {:key :username},
                                :user/password {:key :password}}
                 })

(def tenant-login {
                   :type :tenant-login,
                   :extends :user-login,
                   :write-mapping {
                                   :db-url {:key :tenant/db-url},
                                   :required true}
                   })

(def user-info {
                })
