package com.legacycodes.smarthome.domain.model;

/**
 * The kinds of appliances a user can add.
 *
 * Stored in the DB as the enum NAME (e.g. "LIGHT") — not the ordinal —
 * so reordering this enum later will never corrupt existing data.
 *
 * displayName → what shows in the UI
 * iconName    → string name the UI layer resolves to a drawable
 *               (kept as a string so this class has zero Android imports)
 */
public enum ApplianceType {

    LIGHT       ("Light",           "ic_light"),
    FAN         ("Fan",             "ic_fan"),
    AC          ("Air Conditioner", "ic_ac"),
    TV          ("TV",              "ic_tv"),
    SOCKET      ("Socket",          "ic_socket"),
    HEATER      ("Heater",          "ic_heater"),
    PUMP        ("Water Pump",      "ic_pump"),
    DOOR_LOCK   ("Door Lock",       "ic_lock"),
    SPEAKER     ("Speaker",         "ic_speaker"),
    OTHER       ("Other",           "ic_other");

    private final String displayName;
    private final String iconName;

    ApplianceType(String displayName, String iconName) {
        this.displayName = displayName;
        this.iconName = iconName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getIconName() {
        return iconName;
    }

    /**
     * Safe lookup by name — used when reading from the DB.
     * Returns OTHER if the stored name doesn't match any enum
     * (e.g. a type was removed in a future version).
     */
    public static ApplianceType fromName(String name) {
        if (name == null) return OTHER;
        try {
            return ApplianceType.valueOf(name);
        } catch (IllegalArgumentException e) {
            return OTHER;
        }
    }
}