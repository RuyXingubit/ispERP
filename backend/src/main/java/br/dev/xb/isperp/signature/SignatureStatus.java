package br.dev.xb.isperp.signature;

public enum SignatureStatus {
    PENDING,
    SIGNED,
    REJECTED_DIVERGENT_DOCUMENT,
    EXPIRED,
    CANCELED
}
