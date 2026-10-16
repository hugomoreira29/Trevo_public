-- ============================================================
-- V999: Development seed data
-- Only runs in dev profile (configure Flyway locations accordingly)
-- ============================================================

-- ============================================================
-- 1. USERS
-- ============================================================

-- Buyers (password: "password123" - bcrypt hash)
INSERT INTO users (email, password_hash, full_name, phone, role, status) VALUES
('buyer1@test.com', '$2a$12$J69wp6V2TKcJEGipoy7Ja.p.jEekTWAGONrV23.wazV9e8piJvSgy', 'Ana Silva', '+351912345678', 'BUYER', 'ACTIVE'),
('buyer2@test.com', '$2a$12$J69wp6V2TKcJEGipoy7Ja.p.jEekTWAGONrV23.wazV9e8piJvSgy', 'João Santos', '+351923456789', 'BUYER', 'ACTIVE');

-- Shop Owners
INSERT INTO users (email, password_hash, full_name, phone, role, status) VALUES
('shop1@test.com', '$2a$12$J69wp6V2TKcJEGipoy7Ja.p.jEekTWAGONrV23.wazV9e8piJvSgy', 'Maria Costa', '+351934567890', 'SHOP_OWNER', 'ACTIVE'),
('shop2@test.com', '$2a$12$J69wp6V2TKcJEGipoy7Ja.p.jEekTWAGONrV23.wazV9e8piJvSgy', 'Pedro Ferreira', '+351945678901', 'SHOP_OWNER', 'ACTIVE');

-- IDs: buyer1=1, buyer2=2, shop1=3, shop2=4

-- ============================================================
-- 2. SHOPS
-- ============================================================

INSERT INTO shops (owner_id, name, description, nif, status) VALUES
(3, 'Vintage Porto', 'Roupa vintage curada com carinho no coração do Porto.', '123456789', 'ACTIVE'),
(4, 'EcoModa Lisboa', 'Moda sustentável e segunda mão de qualidade.', '987654321', 'ACTIVE');

-- IDs: vintage_porto=1, ecomoda=2

-- ============================================================
-- 3. COLLECTIONS
-- ============================================================

INSERT INTO collections (shop_id, name, display_order) VALUES
(1, 'Verão 2026', 1),
(1, 'Vestidos', 2),
(1, 'Casacos', 3),
(2, 'Novidades', 1),
(2, 'Básicos', 2);

-- IDs: verao=1, vestidos=2, casacos=3, novidades=4, basicos=5

-- ============================================================
-- 4. PRODUCTS
-- ============================================================

INSERT INTO products (shop_id, name, description, price, brand, materials, garment_type, status) VALUES
(1, 'Vestido Floral Vintage', 'Vestido floral dos anos 90 em excelente estado.', 35.00, 'Zara', 'Algodão 100%', 'DRESSES', 'ACTIVE'),
(1, 'Casaco de Ganga Oversized', 'Casaco de ganga lavagem clara, tamanho oversize.', 45.00, 'Levis', 'Ganga 100%', 'UPPER_BODY', 'ACTIVE'),
(1, 'Saia Plissada', 'Saia plissada em tom verde oliva.', 28.00, 'H&M', 'Poliéster 95%, Elastano 5%', 'LOWER_BODY', 'ACTIVE'),
(2, 'T-shirt Básica Orgânica', 'T-shirt em algodão orgânico certificado.', 15.00, 'COS', 'Algodão orgânico 100%', 'UPPER_BODY', 'ACTIVE'),
(2, 'Calças Cargo Recicladas', 'Calças cargo feitas com tecido reciclado.', 40.00, 'Patagonia', 'Poliéster reciclado 100%', 'LOWER_BODY', 'ACTIVE');

-- IDs: vestido=1, casaco=2, saia=3, tshirt=4, calcas=5

-- ============================================================
-- 5. PRODUCT IMAGES
-- ============================================================

INSERT INTO product_images (product_id, image_url, display_order) VALUES
(1, 'https://res.cloudinary.com/djdnk4fdy/image/upload/v1779893881/products/vestido_floral_1.jpg', 0),
(1, 'https://res.cloudinary.com/djdnk4fdy/image/upload/v1779893882/products/vestido_floral_2.jpg', 1),
(1, 'https://res.cloudinary.com/djdnk4fdy/image/upload/v1779893884/products/vestido_floral_3.jpg', 2),
(2, 'https://res.cloudinary.com/djdnk4fdy/image/upload/v1779893886/products/casaco_ganga_1.jpg', 0),
(2, 'https://res.cloudinary.com/djdnk4fdy/image/upload/v1779893888/products/casaco_ganga_2.jpg', 1),
(2, 'https://res.cloudinary.com/djdnk4fdy/image/upload/v1779893890/products/casaco_ganga_3.jpg', 2),
(3, 'https://res.cloudinary.com/djdnk4fdy/image/upload/v1779893891/products/saia_plissada_1.jpg', 0),
(4, 'https://res.cloudinary.com/djdnk4fdy/image/upload/v1779893893/products/tshirt_organica_1.jpg', 0),
(5, 'https://res.cloudinary.com/djdnk4fdy/image/upload/v1779893894/products/calcas_cargo_1.jpg', 0),
(5, 'https://res.cloudinary.com/djdnk4fdy/image/upload/v1779893896/products/calcas_cargo_2.jpg', 1),
(5, 'https://res.cloudinary.com/djdnk4fdy/image/upload/v1779893897/products/calcas_cargo_3.jpg', 2);

-- ============================================================
-- 6. VARIANTS
-- ============================================================

INSERT INTO variants (product_id, size, color, stock) VALUES
(1, 'S', 'Floral', 15),
(1, 'M', 'Floral', 12),
(2, 'M', 'Azul Claro', 15),
(2, 'L', 'Azul Claro', 12),
(2, 'XL', 'Azul Claro', 10),
(3, 'S', 'Verde Oliva', 12),
(3, 'M', 'Verde Oliva', 15),
(4, 'S', 'Branco', 20),
(4, 'M', 'Branco', 20),
(4, 'L', 'Branco', 15),
(4, 'S', 'Preto', 18),
(4, 'M', 'Preto', 20),
(5, 'M', 'Bege', 12),
(5, 'L', 'Bege', 15),
(5, 'M', 'Preto', 12);

-- IDs: vest_S=1, vest_M=2, cas_M=3, cas_L=4, cas_XL=5,
--      saia_S=6, saia_M=7, tsh_S_br=8, tsh_M_br=9, tsh_L_br=10,
--      tsh_S_pr=11, tsh_M_pr=12, cal_M_bg=13, cal_L_bg=14, cal_M_pr=15

-- ============================================================
-- 7. PRODUCT ↔ COLLECTIONS
-- ============================================================

INSERT INTO product_collections (product_id, collection_id) VALUES
(1, 1), (1, 2),
(2, 3),
(3, 1), (3, 2),
(4, 4), (4, 5),
(5, 4);

-- ============================================================
-- 8. CARTS + CART ITEMS
-- ============================================================

INSERT INTO carts (buyer_id) VALUES (1), (2);

-- buyer2 tem itens no carrinho
INSERT INTO cart_items (cart_id, variant_id, quantity) VALUES
(2, 3, 1),
(2, 8, 2);

-- ============================================================
-- 9. ORDERS + ORDER ITEMS
-- ============================================================

-- buyer1 fez 1 encomenda na Vintage Porto (já entregue)
INSERT INTO orders (buyer_id, shop_id, status, total_amount, commission_amount, payment_method) VALUES
(1, 1, 'DELIVERED', 35.00, 3.50, 'STRIPE');

INSERT INTO order_items (order_id, variant_id, quantity, unit_price) VALUES
(1, 1, 1, 35.00);

-- buyer2 fez 1 encomenda na EcoModa (confirmada)
INSERT INTO orders (buyer_id, shop_id, status, total_amount, commission_amount, payment_method, payment_reference) VALUES
(2, 2, 'CONFIRMED', 55.00, 5.50, 'MBWAY', '+351923456789');

INSERT INTO order_items (order_id, variant_id, quantity, unit_price) VALUES
(2, 9, 1, 15.00),
(2, 13, 1, 40.00);

-- ============================================================
-- 10. REVIEWS
-- ============================================================

INSERT INTO reviews (buyer_id, shop_id, rating, comment) VALUES
(1, 1, 5, 'Excelente qualidade! O vestido chegou em perfeito estado.');

-- ============================================================
-- 11. SUBSCRIPTIONS
-- ============================================================

-- buyer1 tem subscrição Pro ativa
INSERT INTO subscriptions (buyer_id, status, stripe_subscription_id, start_date) VALUES
(1, 'ACTIVE', 'sub_test_abc123', NOW());

-- ============================================================
-- 12. TRYON USAGE + TRYON IMAGES
-- ============================================================

-- buyer1 (Pro) usou try-on no vestido floral (3 gerações)
INSERT INTO tryon_usage (buyer_id, product_id, generation_count) VALUES
(1, 1, 3);

INSERT INTO tryon_images (usage_id, image_url, generated_at) VALUES
(1, 'https://res.cloudinary.com/trevo/image/upload/v1/tryon/buyer1_prod1_gen1.jpg', NOW() - INTERVAL '2 days'),
(1, 'https://res.cloudinary.com/trevo/image/upload/v1/tryon/buyer1_prod1_gen2.jpg', NOW() - INTERVAL '1 day'),
(1, 'https://res.cloudinary.com/trevo/image/upload/v1/tryon/buyer1_prod1_gen3.jpg', NOW());

-- buyer2 (free) usou 1 geração gratuita no casaco
INSERT INTO tryon_usage (buyer_id, product_id, generation_count) VALUES
(2, 2, 1);

INSERT INTO tryon_images (usage_id, image_url, generated_at) VALUES
(2, 'https://res.cloudinary.com/trevo/image/upload/v1/tryon/buyer2_prod2_gen1.jpg', NOW());

-- ============================================================
-- 13. USER PHOTOS ("See it on yourself" — buyer1 Pro)
-- ============================================================

INSERT INTO user_photos (buyer_id, photo_url, cloudinary_public_id, uploaded_at) VALUES
(1, 'https://res.cloudinary.com/trevo/image/upload/v1/user_photos/buyer1_photo1.jpg', 'user_photos/buyer1_photo1', NOW()),
(1, 'https://res.cloudinary.com/trevo/image/upload/v1/user_photos/buyer1_photo2.jpg', 'user_photos/buyer1_photo2', NOW()),
(1, 'https://res.cloudinary.com/trevo/image/upload/v1/user_photos/buyer1_photo3.jpg', 'user_photos/buyer1_photo3', NOW());

-- ============================================================
-- Nota: refresh_tokens e password_reset_tokens não são
-- seedados porque são gerados em runtime pela aplicação.
-- ============================================================
