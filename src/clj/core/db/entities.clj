(ns core.db.entities)

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
