package com.ytppa.nothingheart;

public enum HeartPairingStatus {
    NONE("none"),
    PENDING("pending"),
    PAIRED("paired");

    private final String storedValue;

    HeartPairingStatus(String storedValue) {
        this.storedValue = storedValue;
    }

    public String getStoredValue() {
        return storedValue;
    }

    public static HeartPairingStatus fromStoredValue(String storedValue) {
        for (HeartPairingStatus status : values()) {
            if (status.storedValue.equals(storedValue)) {
                return status;
            }
        }
        return NONE;
    }
}
