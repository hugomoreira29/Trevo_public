export const ROLES = {
  ADMIN: "ADMIN",
  BUYER: "BUYER",
  SHOP_OWNER: "SHOP_OWNER",
};

const ROLE_ALIASES = {
  admin: ROLES.ADMIN,
  ADMIN: ROLES.ADMIN,
  buyer: ROLES.BUYER,
  BUYER: ROLES.BUYER,
  shop_owner: ROLES.SHOP_OWNER,
  SHOP_OWNER: ROLES.SHOP_OWNER,
  shopOwner: ROLES.SHOP_OWNER,
  ShopOwner: ROLES.SHOP_OWNER,
};

export function normalizeRole(role) {
  if (!role) {
    return null;
  }

  return ROLE_ALIASES[role] || null;
}
