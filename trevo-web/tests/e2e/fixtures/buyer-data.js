// JWT payload: {"sub":"1","role":"BUYER"}
// AuthContext reads claims.sub (userId) and claims.role from the token.
const BUYER_JWT_PAYLOAD = btoa(JSON.stringify({ sub: "1", role: "BUYER" }));

export const testTokens = {
  accessToken: `eyJhbGciOiJIUzI1NiJ9.${BUYER_JWT_PAYLOAD}.fake-signature`,
  refreshToken: "fake-refresh-token",
};

export const testBuyer = {
  id: 1,
  fullName: "Buyer E2E",
  email: "buyer-e2e@example.com",
  phone: "912345678",
  role: "BUYER",
  status: "ACTIVE",
};

export const testProducts = {
  content: [
    {
      id: 101,
      name: "Vestido Floral",
      brand: "TrevoStyle",
      price: 49.99,
      imageUrl: "https://placehold.co/400x400?text=Vestido",
    },
    {
      id: 102,
      name: "Casaco Inverno",
      brand: "TrevoStyle",
      price: 89.99,
      imageUrl: "https://placehold.co/400x400?text=Casaco",
    },
  ],
  number: 0,
  totalPages: 1,
};

export const testProductDetail = {
  id: 101,
  name: "Vestido Floral",
  brand: "TrevoStyle",
  description: "Um vestido floral elegante para todas as ocasioes.",
  price: 49.99,
  materials: "Algodao 100%",
  shopId: 10,
  images: [
    { id: 1, imageUrl: "https://placehold.co/400x400?text=Vestido1", displayOrder: 0 },
    { id: 2, imageUrl: "https://placehold.co/400x400?text=Vestido2", displayOrder: 1 },
  ],
  variants: [
    { id: 201, size: "M", color: "Vermelho", stock: 5, price: 49.99 },
    { id: 202, size: "L", color: "Vermelho", stock: 3, price: 49.99 },
    { id: 203, size: "M", color: "Azul", stock: 0, price: 49.99 },
  ],
};

export const testCart = {
  shops: [
    {
      shopId: 10,
      shopName: "Loja TrevoStyle",
      subtotal: 49.99,
      items: [
        {
          cartItemId: 1,
          variantId: 201,
          productName: "Vestido Floral",
          size: "M",
          color: "Vermelho",
          quantity: 1,
          unitPrice: 49.99,
          subtotal: 49.99,
          image: "https://placehold.co/400x400?text=Vestido1",
        },
      ],
    },
  ],
  grandTotal: 49.99,
};

export const testMultiShopCart = {
  shops: [
    {
      shopId: 10,
      shopName: "Loja TrevoStyle",
      subtotal: 49.99,
      items: [
        {
          cartItemId: 1,
          variantId: 201,
          productName: "Vestido Floral",
          size: "M",
          color: "Vermelho",
          quantity: 1,
          unitPrice: 49.99,
          subtotal: 49.99,
          image: "https://placehold.co/400x400?text=Vestido1",
        },
      ],
    },
    {
      shopId: 20,
      shopName: "Loja UrbanWear",
      subtotal: 89.99,
      items: [
        {
          cartItemId: 2,
          variantId: 301,
          productName: "Casaco Premium",
          size: "L",
          color: "Preto",
          quantity: 1,
          unitPrice: 89.99,
          subtotal: 89.99,
          image: "https://placehold.co/400x400?text=Casaco",
        },
      ],
    },
  ],
  grandTotal: 139.98,
};

export const testOrders = [
  {
    id: 501,
    shopName: "Loja TrevoStyle",
    status: "PLACED",
    totalAmount: 49.99,
    paymentMethod: "MBWAY",
    createdAt: "2026-05-25T10:00:00Z",
  },
];

export const testOrderDetail = {
  id: 501,
  shopName: "Loja TrevoStyle",
  status: "PLACED",
  totalAmount: 49.99,
  paymentMethod: "MBWAY",
  createdAt: "2026-05-25T10:00:00Z",
  items: [
    {
      id: 1,
      productName: "Vestido Floral",
      size: "M",
      color: "Vermelho",
      quantity: 1,
      unitPrice: 49.99,
      subtotal: 49.99,
    },
  ],
};

export const testSplitOrders = [
  {
    id: 501,
    shopName: "Loja TrevoStyle",
    status: "PLACED",
    totalAmount: 49.99,
    paymentMethod: "MBWAY",
    createdAt: "2026-05-25T10:00:00Z",
  },
  {
    id: 502,
    shopName: "Loja UrbanWear",
    status: "PLACED",
    totalAmount: 89.99,
    paymentMethod: "MBWAY",
    createdAt: "2026-05-25T10:00:00Z",
  },
];

export const testOrdersPage = {
  orders: testOrders,
  totalPages: 1,
};

export const testSplitOrdersPage = {
  orders: testSplitOrders,
  totalPages: 1,
};

export const stockConflictError = {
  message: "Stock insuficiente para alguns itens",
  affectedItems: [{ variantId: 201, productName: "Vestido Floral", available: 0 }],
};
