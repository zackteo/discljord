(ns discljord.permissions
  "Functions for determining users' permissions.")

(def permissions-bit
  "Map from permission names to the binary flag representation of it."
  {:create-instant-invite 0x1
   :kick-members 0x2
   :ban-members 0x4
   :administrator 0x8
   :manage-channels 0x10
   :manage-guild 0x20
   :add-reactions 0x40
   :view-audit-log 0x80
   :priority-speaker 0x100
   :stream 0x200
   :view-channel 0x400
   :send-messages 0x800
   :send-tts-messages 0x1000
   :manage-messages 0x2000
   :embed-links 0x4000
   :attach-files 0x8000
   :read-message-history 0x10000
   :mention-everyone 0x20000
   :use-external-emojis 0x40000
   :view-guild-insights 0x80000
   :connect 0x100000
   :speak 0x200000
   :mute-members 0x400000
   :deafen-members 0x800000
   :move-members 0x1000000
   :use-vad 0x2000000
   :change-nickname 0x4000000
   :manage-nicknames 0x8000000
   :manage-roles 0x10000000
   :manage-webooks 0x20000000
   :manage-emojis 0x40000000})

(defn has-permission-flag?
  [perm perms-int]
  (when perms-int
    (when-let [bit (permissions-bit perm)]
      (not (zero? (bit-and bit perms-int))))))

(defn has-permission-flags?
  [perms perms-int]
  (every? #(has-permission-flag? % perms-int) perms))

(defn permission-int
  ([everyone roles]
   (let [perms-int (reduce bit-or 0 (conj roles everyone))]
     (if (has-permission-flag? :administrator perms-int)
       0xFFFFFFFF
       perms-int)))
  ([everyone roles everyone-overrides roles-overrides user-overrides]
   (let [override (fn [perms-int overrides]
                    (let [allow (reduce bit-or 0 (map :allow overrides))
                          deny (reduce bit-or 0 (map :deny overrides))]
                      (bit-or
                       (bit-and
                        perms-int
                        (bit-not deny))
                       allow)))

         perms-int (permission-int everyone roles)
         perms-int (override perms-int everyone-overrides)
         perms-int (override perms-int roles-overrides)]
     (override perms-int user-overrides))))

(defn- user-roles
  [guild user-id]
  (map :permissions (vals (select-keys (:roles guild) (:roles ((:members guild) user-id))))))

(defn has-permission?
  {:arglists '([perm everyone roles] [perm guild user-id] [perm guild user-id channel-id]
               [perm everyone roles everyone-overrides roles-overrides user-overrides])}
  ([perm everyone-or-guild roles-or-user-id]
   (if (map? everyone-or-guild)
     (has-permission-flag? perm (permission-int (:permissions ((:roles everyone-or-guild) (:id everyone-or-guild)))
                                                (user-roles everyone-or-guild roles-or-user-id)))
     (has-permission-flag? perm (permission-int everyone-or-guild roles-or-user-id))))
  ([perm guild user-id channel-id]
   (let [everyone (:permissions ((:roles guild) (:id guild)))
         roles (user-roles guild user-id)]
     (has-permission-flag?
      perm
      (permission-int everyone roles))))
  ([perm everyone roles everyone-overrides roles-overrides user-overrides]
   (has-permission-flag?
    perm
    (permission-int everyone roles everyone-overrides roles-overrides user-overrides))))

(defn has-permissions?
  {:arglists '([perms everyone roles] [perms guild user-id] [perms guild user-id channel-id]
               [perms everyone roles everyone-overrides roles-overrides user-overrides])}
  ([perms everyone-or-guild roles-or-user-id]
   (if (map? everyone-or-guild)
     (has-permission-flags?
      perms
      (permission-int (:permissions ((:roles everyone-or-guild) (:id everyone-or-guild)))
                      (user-roles everyone-or-guild roles-or-user-id)))
     (has-permission-flags? perms (permission-int everyone-or-guild roles-or-user-id))))
  ([perms guild user-id channel-id]
   (let [everyone (:permissions ((:roles guild) (:id guild)))
         roles (user-roles guild user-id)]
     (has-permission-flags?
      perms
      (permission-int everyone roles))))
  ([perms everyone roles everyone-overrides roles-overrides user-overrides]
   (has-permission-flags?
    perms
    (permission-int everyone roles everyone-overrides roles-overrides user-overrides))))
