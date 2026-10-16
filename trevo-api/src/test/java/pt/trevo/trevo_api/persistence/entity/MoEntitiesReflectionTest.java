package pt.trevo.trevo_api.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;
import jakarta.persistence.ManyToOne;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.*;

class MoEntitiesReflectionTest {

    // Helper method to check for basic entity setup
    private void assertValidEntity(Class<?> clazz) {
        assertTrue(clazz.isAnnotationPresent(Entity.class),
                clazz.getSimpleName() + " should be annotated with @Entity");

        boolean hasId = false;
        for (Field field : clazz.getDeclaredFields()) {
            if (field.isAnnotationPresent(Id.class)) {
                hasId = true;
                break;
            }
        }
        assertTrue(hasId, clazz.getSimpleName() + " must have an @Id field");
    }

    // Helper method to check the strict updatedAt constraint
    private void assertUpdatedAtConstraint(Class<?> clazz) throws NoSuchFieldException {
        Field updatedAt = clazz.getDeclaredField("updatedAt");
        Column column = updatedAt.getAnnotation(Column.class);

        assertNotNull(column, clazz.getSimpleName() + ".updatedAt must have @Column annotation");
        assertFalse(column.insertable(), clazz.getSimpleName() + ".updatedAt must be insertable = false");
        assertFalse(column.updatable(), clazz.getSimpleName() + ".updatedAt must be updatable = false");
        assertEquals("updated_at", column.name());
    }

    @Test
    void testUserEntity() throws NoSuchFieldException {
        assertValidEntity(User.class);
        assertUpdatedAtConstraint(User.class);

        Field email = User.class.getDeclaredField("email");
        assertTrue(email.getAnnotation(Column.class).unique(), "User email must be unique");
    }

    @Test
    void testShopEntity() throws NoSuchFieldException {
        assertValidEntity(Shop.class);
        assertUpdatedAtConstraint(Shop.class);

        Field owner = Shop.class.getDeclaredField("owner");
        assertTrue(owner.isAnnotationPresent(OneToOne.class), "Shop must have a @OneToOne relationship with User");
    }

    @Test
    void testProductEntity() throws NoSuchFieldException {
        assertValidEntity(Product.class);
        assertUpdatedAtConstraint(Product.class);

        Field shop = Product.class.getDeclaredField("shop");
        assertTrue(shop.isAnnotationPresent(ManyToOne.class), "Product must have a @ManyToOne relationship with Shop");
    }

    @Test
    void testVariantEntity() throws NoSuchFieldException {
        assertValidEntity(Variant.class);
        // Variant doesn't have updatedAt in the schema, so we just check relationships
        Field product = Variant.class.getDeclaredField("product");
        assertTrue(product.isAnnotationPresent(ManyToOne.class), "Variant must have a @ManyToOne relationship with Product");
    }

    @Test
    void testCollectionEntity() throws NoSuchFieldException {
        assertValidEntity(Collection.class);
        Field shop = Collection.class.getDeclaredField("shop");
        assertTrue(shop.isAnnotationPresent(ManyToOne.class), "Collection must have a @ManyToOne relationship with Shop");
    }
}