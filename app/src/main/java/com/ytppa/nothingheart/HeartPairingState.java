package com.ytppa.nothingheart;

public final class HeartPairingState {
    private final String myUserId;
    private final String pairCode;
    private final String partnerId;
    private final String partnerPairCode;
    private final HeartPairingStatus pairStatus;

    public HeartPairingState(
            String myUserId,
            String pairCode,
            String partnerId,
            HeartPairingStatus pairStatus
    ) {
        this(myUserId, pairCode, partnerId, "", pairStatus);
    }

    public HeartPairingState(
            String myUserId,
            String pairCode,
            String partnerId,
            String partnerPairCode,
            HeartPairingStatus pairStatus
    ) {
        this.myUserId = nullToEmpty(myUserId);
        this.pairCode = nullToEmpty(pairCode);
        this.partnerId = nullToEmpty(partnerId);
        this.partnerPairCode = nullToEmpty(partnerPairCode);
        this.pairStatus = pairStatus == null ? HeartPairingStatus.NONE : pairStatus;
    }

    public String getMyUserId() {
        return myUserId;
    }

    public String getPairCode() {
        return pairCode;
    }

    public String getPartnerId() {
        return partnerId;
    }

    public String getPartnerPairCode() {
        return partnerPairCode;
    }

    public HeartPairingStatus getPairStatus() {
        return pairStatus;
    }

    public boolean hasLocalIdentity() {
        return !myUserId.isEmpty() && !pairCode.isEmpty();
    }

    public boolean hasPartner() {
        return !partnerId.isEmpty();
    }

    public boolean hasPartnerPairCode() {
        return !partnerPairCode.isEmpty();
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
