package pt.trevo.trevo_api.persistence.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import org.hibernate.annotations.Check;
import org.junit.jupiter.api.Test;
import pt.trevo.trevo_api.BaseUnitTest;

class EntityMappingReflectionTest extends BaseUnitTest {

    @Test
    void shouldMapCartEntity() {
        assertEntityTable(Cart.class, "carts");
        assertFieldNames(Cart.class, "id", "buyer", "items");
        assertIdField(Cart.class, "id");

        OneToOne buyerRelation = getField(Cart.class, "buyer").getAnnotation(OneToOne.class);
        assertNotNull(buyerRelation);
        assertEquals(FetchType.LAZY, buyerRelation.fetch());

        JoinColumn buyerJoinColumn = getField(Cart.class, "buyer").getAnnotation(JoinColumn.class);
        assertNotNull(buyerJoinColumn);
        assertEquals("buyer_id", buyerJoinColumn.name());
        assertTrue(buyerJoinColumn.unique());

        OneToMany itemsRelation = getField(Cart.class, "items").getAnnotation(OneToMany.class);
        assertNotNull(itemsRelation);
        assertEquals("cart", itemsRelation.mappedBy());
    }

    @Test
    void shouldMapCartItemEntity() {
        assertEntityTable(CartItem.class, "cart_items");
        assertFieldNames(CartItem.class, "id", "cart", "variant", "quantity");
        assertIdField(CartItem.class, "id");
        assertManyToOneJoinColumn(CartItem.class, "cart", "cart_id");
        assertManyToOneJoinColumn(CartItem.class, "variant", "variant_id");
        assertNonNullableColumn(CartItem.class, "quantity");
        assertCheckConstraint(CartItem.class, "quantity > 0");
    }

    @Test
    void shouldMapOrderEntity() {
        assertEntityTable(Order.class, "orders");
        assertFieldNames(
                Order.class,
                "id",
                "buyer",
                "shop",
                "status",
                "trackingNumber",
                "totalAmount",
                "commissionAmount",
                "paymentMethod",
                "paymentReference",
                "createdAt",
                "updatedAt",
                "items"
        );
        assertIdField(Order.class, "id");
        assertManyToOneJoinColumn(Order.class, "buyer", "buyer_id");
        assertManyToOneJoinColumn(Order.class, "shop", "shop_id");
        assertEnumeratedAsString(Order.class, "status");
        assertEnumeratedAsString(Order.class, "paymentMethod");
        assertColumnName(Order.class, "trackingNumber", "tracking_number");
        assertColumnName(Order.class, "totalAmount", "total_amount");
        assertColumnName(Order.class, "commissionAmount", "commission_amount");
        assertColumnName(Order.class, "paymentMethod", "payment_method");
        assertColumnName(Order.class, "paymentReference", "payment_reference");
        assertColumnName(Order.class, "createdAt", "created_at");
        assertDatabaseManagedColumn(Order.class, "updatedAt", "updated_at");

        OneToMany itemsRelation = getField(Order.class, "items").getAnnotation(OneToMany.class);
        assertNotNull(itemsRelation);
        assertEquals("order", itemsRelation.mappedBy());
    }

    @Test
    void shouldMapOrderItemEntity() {
        assertEntityTable(OrderItem.class, "order_items");
        assertFieldNames(OrderItem.class, "id", "order", "variant", "quantity", "unitPrice");
        assertIdField(OrderItem.class, "id");
        assertManyToOneJoinColumn(OrderItem.class, "order", "order_id");
        assertManyToOneJoinColumn(OrderItem.class, "variant", "variant_id");
        assertColumnName(OrderItem.class, "unitPrice", "unit_price");
    }

    @Test
    void shouldMapReviewEntity() {
        assertEntityTable(Review.class, "reviews");
        assertFieldNames(Review.class, "id", "buyer", "shop", "rating", "comment", "response", "createdAt", "updatedAt");
        assertIdField(Review.class, "id");
        assertManyToOneJoinColumn(Review.class, "buyer", "buyer_id");
        assertManyToOneJoinColumn(Review.class, "shop", "shop_id");
        assertColumnName(Review.class, "createdAt", "created_at");
        assertDatabaseManagedColumn(Review.class, "updatedAt", "updated_at");
        assertCheckConstraint(Review.class, "rating between 1 and 5");

        Table table = Review.class.getAnnotation(Table.class);
        assertNotNull(table);
        assertEquals(1, table.uniqueConstraints().length);
        UniqueConstraint uniqueConstraint = table.uniqueConstraints()[0];
        assertEquals(List.of("buyer_id", "shop_id"), Arrays.asList(uniqueConstraint.columnNames()));
    }

    @Test
    void shouldMapSubscriptionEntity() {
        assertEntityTable(Subscription.class, "subscriptions");
        assertFieldNames(Subscription.class, "id", "buyer", "status", "stripeSubscriptionId", "startDate", "endDate", "createdAt");
        assertIdField(Subscription.class, "id");
        assertEnumeratedAsString(Subscription.class, "status");
        assertColumnName(Subscription.class, "stripeSubscriptionId", "stripe_subscription_id");
        assertColumnName(Subscription.class, "startDate", "start_date");
        assertColumnName(Subscription.class, "endDate", "end_date");
        assertColumnName(Subscription.class, "createdAt", "created_at");

        OneToOne buyerRelation = getField(Subscription.class, "buyer").getAnnotation(OneToOne.class);
        assertNotNull(buyerRelation);

        JoinColumn buyerJoinColumn = getField(Subscription.class, "buyer").getAnnotation(JoinColumn.class);
        assertNotNull(buyerJoinColumn);
        assertEquals("buyer_id", buyerJoinColumn.name());
        assertTrue(buyerJoinColumn.unique());
    }

    @Test
    void shouldMapTryOnUsageEntity() {
        assertEntityTable(TryOnUsage.class, "tryon_usage");
        assertFieldNames(TryOnUsage.class, "id", "buyer", "product", "generationCount");
        assertIdField(TryOnUsage.class, "id");
        assertManyToOneJoinColumn(TryOnUsage.class, "buyer", "buyer_id");
        assertManyToOneJoinColumn(TryOnUsage.class, "product", "product_id");
        assertColumnName(TryOnUsage.class, "generationCount", "generation_count");

        Table table = TryOnUsage.class.getAnnotation(Table.class);
        assertNotNull(table);
        assertEquals(1, table.uniqueConstraints().length);
        UniqueConstraint uniqueConstraint = table.uniqueConstraints()[0];
        assertEquals(List.of("buyer_id", "product_id"), Arrays.asList(uniqueConstraint.columnNames()));
    }

    @Test
    void shouldMapUserPhotoEntity() {
        assertEntityTable(UserPhoto.class, "user_photos");
        assertFieldNames(UserPhoto.class, "id", "buyer", "photoUrl", "cloudinaryPublicId", "uploadedAt");
        assertIdField(UserPhoto.class, "id");
        assertManyToOneJoinColumn(UserPhoto.class, "buyer", "buyer_id");
        assertColumnName(UserPhoto.class, "photoUrl", "photo_url");
        assertColumnName(UserPhoto.class, "cloudinaryPublicId", "cloudinary_public_id");
        assertColumnName(UserPhoto.class, "uploadedAt", "uploaded_at");
    }

    @Test
    void shouldMapRefreshTokenEntity() {
        assertEntityTable(RefreshToken.class, "refresh_tokens");
        assertFieldNames(RefreshToken.class, "id", "user", "token", "expiresAt", "revoked", "createdAt");
        assertIdField(RefreshToken.class, "id");
        assertManyToOneJoinColumn(RefreshToken.class, "user", "user_id");
        assertColumnName(RefreshToken.class, "expiresAt", "expires_at");
        assertColumnName(RefreshToken.class, "createdAt", "created_at");

        Column tokenColumn = getField(RefreshToken.class, "token").getAnnotation(Column.class);
        assertNotNull(tokenColumn);
        assertEquals("token", tokenColumn.name());
        assertTrue(tokenColumn.unique());
    }

    @Test
    void shouldMapPasswordResetTokenEntity() {
        assertEntityTable(PasswordResetToken.class, "password_reset_tokens");
        assertFieldNames(PasswordResetToken.class, "id", "user", "tokenHash", "expiresAt", "used", "createdAt");
        assertIdField(PasswordResetToken.class, "id");
        assertManyToOneJoinColumn(PasswordResetToken.class, "user", "user_id");
        assertColumnName(PasswordResetToken.class, "expiresAt", "expires_at");
        assertColumnName(PasswordResetToken.class, "createdAt", "created_at");

        Column tokenColumn = getField(PasswordResetToken.class, "tokenHash").getAnnotation(Column.class);
        assertNotNull(tokenColumn);
        assertEquals("token_hash", tokenColumn.name());
        assertTrue(tokenColumn.unique());
    }

    private void assertEntityTable(Class<?> type, String expectedTableName) {
        assertTrue(type.isAnnotationPresent(Entity.class));
        Table table = type.getAnnotation(Table.class);
        assertNotNull(table);
        assertEquals(expectedTableName, table.name());
    }

    private void assertFieldNames(Class<?> type, String... expectedFieldNames) {
        List<String> actualFieldNames = Arrays.stream(type.getDeclaredFields())
                .filter(field -> !field.isSynthetic())
                .map(Field::getName)
                .toList();

        assertEquals(List.of(expectedFieldNames), actualFieldNames);
    }

    private void assertIdField(Class<?> type, String fieldName) {
        Field field = getField(type, fieldName);
        assertTrue(field.isAnnotationPresent(Id.class));
        GeneratedValue generatedValue = field.getAnnotation(GeneratedValue.class);
        assertNotNull(generatedValue);
        assertEquals(GenerationType.IDENTITY, generatedValue.strategy());
    }

    private void assertManyToOneJoinColumn(Class<?> type, String fieldName, String expectedColumnName) {
        Field field = getField(type, fieldName);
        ManyToOne manyToOne = field.getAnnotation(ManyToOne.class);
        assertNotNull(manyToOne);
        assertEquals(FetchType.LAZY, manyToOne.fetch());

        JoinColumn joinColumn = field.getAnnotation(JoinColumn.class);
        assertNotNull(joinColumn);
        assertEquals(expectedColumnName, joinColumn.name());
    }

    private void assertEnumeratedAsString(Class<?> type, String fieldName) {
        Enumerated enumerated = getField(type, fieldName).getAnnotation(Enumerated.class);
        assertNotNull(enumerated);
        assertEquals(EnumType.STRING, enumerated.value());
    }

    private void assertColumnName(Class<?> type, String fieldName, String expectedColumnName) {
        Column column = getField(type, fieldName).getAnnotation(Column.class);
        assertNotNull(column);
        assertEquals(expectedColumnName, column.name());
    }

    private void assertNonNullableColumn(Class<?> type, String fieldName) {
        Column column = getField(type, fieldName).getAnnotation(Column.class);
        assertNotNull(column);
        assertFalse(column.nullable());
    }

    private void assertDatabaseManagedColumn(Class<?> type, String fieldName, String expectedColumnName) {
        Column column = getField(type, fieldName).getAnnotation(Column.class);
        assertNotNull(column);
        assertEquals(expectedColumnName, column.name());
        assertFalse(column.insertable());
        assertFalse(column.updatable());
    }

    private void assertCheckConstraint(Class<?> type, String expectedConstraint) {
        @SuppressWarnings("deprecation")
        Check check = type.getAnnotation(Check.class);
        assertNotNull(check);
        assertEquals(expectedConstraint, check.constraints());
    }

    private Field getField(Class<?> type, String fieldName) {
        try {
            return type.getDeclaredField(fieldName);
        } catch (NoSuchFieldException exception) {
            throw new AssertionError("Field not found: " + fieldName, exception);
        }
    }
}
