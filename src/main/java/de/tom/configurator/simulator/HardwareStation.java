package de.tom.configurator.simulator;

/**
 * Representation of an industrial hardware scanning station.
 */
public enum HardwareStation {
    STATION_01_INBOUND("STATION_01_INBOUND", "Wareneingang Palettenscanner (Sick CLV690)"),
    STATION_02_SORTING("STATION_02_SORTING", "Sortierstrecke Paketband (Cognex DataMan 370)"),
    STATION_03_ASSEMBLY("STATION_03_ASSEMBLY", "Baugruppen-Montage (Zebra DS3608)"),
    STATION_04_DISPATCH("STATION_04_DISPATCH", "Warenausgang Express-Dispatch (Datalogic Matrix)");

    private final String id;
    private final String description;

    HardwareStation(String id, String description) {
        this.id = id;
        this.description = description;
    }

    public String getId() {
        return id;
    }

    public String getDescription() {
        return description;
    }

    @Override
    public String toString() {
        return id + " - " + description;
    }
}
