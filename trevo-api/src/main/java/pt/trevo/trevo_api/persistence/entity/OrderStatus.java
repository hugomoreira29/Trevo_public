package pt.trevo.trevo_api.persistence.entity;

public enum OrderStatus {
    PENDING_PAYMENT,
    PLACED,
    CONFIRMED,
    SHIPPED,
    DELIVERED,
    RETURNED,
    CANCELLED
}
