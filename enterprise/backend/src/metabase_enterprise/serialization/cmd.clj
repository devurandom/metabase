(ns metabase-enterprise.serialization.cmd
  (:refer-clojure :exclude [load])
  (:require [clojure.tools.logging :as log]
            [metabase-enterprise.serialization.dump :as dump]
            [metabase-enterprise.serialization.load :as load]
            [metabase.db :as mdb]
            [metabase.models.card :refer [Card]]
            [metabase.models.collection :refer [Collection]]
            [metabase.models.dashboard :refer [Dashboard]]
            [metabase.models.database :refer [Database]]
            [metabase.models.field :as field :refer [Field]]
            [metabase.models.metric :refer [Metric]]
            [metabase.models.pulse :refer [Pulse]]
            [metabase.models.segment :refer [Segment]]
            [metabase.models.table :refer [Table]]
            [metabase.models.user :refer [User]]
            [metabase.plugins :as plugins]
            [metabase.util :as u]
            [metabase.util.i18n :refer [deferred-trs trs]]
            [metabase.util.schema :as su]
            [schema.core :as s]
            [toucan.db :as db]))

(def ^:private Mode
  (su/with-api-error-message (s/enum :skip :update)
    (deferred-trs "invalid --mode value")))

(def ^:private OnError
  (su/with-api-error-message (s/enum :continue :abort)
    (deferred-trs "invalid --on-error value")))

(def ^:private Context
  (su/with-api-error-message
    {(s/optional-key :on-error) OnError
     (s/optional-key :mode)     Mode}
    (deferred-trs "invalid context seed value")))

(s/defn load
  "Load serialized metabase instance as created by `dump` command from directory `path`."
  [path context :- Context]
  (plugins/load-plugins!)               ;
  (mdb/setup-db!)
  (when-not (load/compatible? path)
    (log/warn (trs "Dump was produced using a different version of Metabase. Things may break!")))
  (let [context (merge {:mode     :skip
                        :on-error :continue}
                       context)]
    (try
      (do
        (log/infof "BEGIN LOAD from %s with context %s" path context)
        (let [all-res    [(load/load (str path "/users") context)
                          (load/load (str path "/databases") context)
                          (load/load (str path "/collections") context)
                          (load/load-settings path context)
                          (load/load-dependencies path context)]
              reload-fns (filter fn? all-res)]
          (if-not (empty? reload-fns)
            (do (log/info (trs "Finished first pass of load; now performing second pass"))
                (doseq [reload-fn reload-fns]
                  (reload-fn))))
          (log/infof "END LOAD from %s with context %s" path context)))
      (catch Throwable e
        (log/error e (trs "ERROR LOAD from {0}: {1}" path (.getMessage e)))))))

(defn- select-entities-in-collections
  ([model collections]
   (select-entities-in-collections model collections :all))
  ([model collections state]
   (let [state-filter (case state
                        :all nil
                        :active [:= :archived false])]
     (db/select model {:where [:and
                               [:or [:= :collection_id nil]
                                (if (not-empty collections)
                                  [:in :collection_id (map u/the-id collections)]
                                  false)]
                               state-filter]}))))

(defn- select-segments-in-tables
  ([tables]
   (select-segments-in-tables tables :all))
  ([tables state]
   (case state
     :all
     (mapcat #(db/select Segment :table_id (u/the-id %)) tables)
     :active
     (filter
      #(not (:archived %))
      (mapcat #(db/select Segment :table_id (u/the-id %)) tables)))))

(defn- select-collections
  ([users]
   (select-collections users :active))
  ([users state]
   (let [state-filter (case state
                        :all nil
                        :active [:= :archived false])]
     (db/select Collection
                       {:where [:and
                                [:or
                                 [:= :personal_owner_id nil]
                                 [:= :personal_owner_id (some-> users first u/the-id)]]
                                state-filter]}))))

(defn dump
  "Serialized metabase instance into directory `path`."
  ([path user]
   (dump path user :active {}))
  ([path user opts]
   (dump path user :active opts))
  ([path user state opts]
   (mdb/setup-db!)
   (log/infof "BEGIN DUMP to %s via user %s" path user)
   (let [users       (if user
                       (let [user (db/select-one User
                                    :email        user
                                    :is_superuser true)]
                         (assert user (trs "{0} is not a valid user" user))
                         [user])
                       [])
         databases   (if (contains? opts :only-db-ids)
                       (db/select Database :id [:in (:only-db-ids opts)])
                       (Database))
         tables      (if (contains? opts :only-db-ids)
                       (db/select Table :db_id [:in (:only-db-ids opts)])
                       (Table))
         fields      (if (contains? opts :only-db-ids)
                       (db/select Field :table_id [:in (map :id tables)])
                       (Field))
         metrics     (if (contains? opts :only-db-ids)
                       (db/select Metric :table_id [:in (map :id tables)])
                       (Metric))
         collections (Collection)] ;(select-collections users state)]
     (dump/dump path
                databases
                tables
                (field/with-values fields)
                metrics
                (select-segments-in-tables tables state)
                collections
                (select-entities-in-collections Card collections state)
                (select-entities-in-collections Dashboard collections state)
                (select-entities-in-collections Pulse collections state)
                users))
   (dump/dump-settings path)
   (dump/dump-dependencies path)
   (dump/dump-dimensions path)
   (log/infof "END DUMP to %s via user %s" path user)))
