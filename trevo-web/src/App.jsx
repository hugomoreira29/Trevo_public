import { Routes, Route } from "react-router-dom";
import Home from "./pages/Home";
import Login from "./pages/Login";
import Register from "./pages/Register";
import ForgotPassword from "./pages/ForgotPassword";
import ResetPassword from "./pages/ResetPassword";
import Dashboard from "./pages/Dashboard";
import PublicShopProfile from "./pages/PublicShopProfile";
import CollectionManager from "./pages/CollectionManager";
import ShopDashboard from "./pages/ShopDashboard";
import CreateShop from "./pages/CreateShop";
import ProductDetail from "./pages/ProductDetail";
import ProductForm from "./pages/ProductForm";
import ProductSearch from "./pages/ProductSearch";
import StockManagement from "./pages/StockManagement";
import ShopProductManagement from "./pages/ShopProductManagement";
import ShopProducts from "./pages/ShopProducts";
import Profile from "./pages/Profile";
import Admin from "./pages/Admin";
import OrderHistory from "./pages/OrderHistory";
import OrderManagement from "./pages/OrderManagement";
import ProSubscription from "./pages/ProSubscription";
import ReceiptView from "./pages/ReceiptView";
import PayoutDashboard from "./pages/PayoutDashboard";
import Checkout from "./pages/Checkout";
import PayPalReturn from "./pages/PayPalReturn";
import Cart from "./pages/Cart";
import Unauthorized from "./pages/Unauthorized";
import Header from "./components/Header";
import NotificationIndicator from "./components/NotificationIndicator";
import ProtectedRoute from "./routes/ProtectedRoute";
import RoleRoute from "./routes/RoleRoute";
import { ROLES } from "./lib/roles";

export default function App() {
  return (
    <div className="min-h-screen bg-white text-ink-900 font-sans antialiased">
      <Header />
      <NotificationIndicator />
      <main>
        <Routes>
          <Route path="/" element={<Home />} />
          <Route path="/login" element={<Login />} />
          <Route path="/register" element={<Register />} />
          <Route path="/forgot-password" element={<ForgotPassword />} />
          <Route path="/reset-password" element={<ResetPassword />} />
          <Route path="/products/:productId" element={<ProductDetail />} />
          <Route path="/unauthorized" element={<Unauthorized />} />
          <Route path="/shops/:shopId" element={<PublicShopProfile />} />
          <Route path="/shops/:shopId/products" element={<ShopProducts />} />
          <Route path="/products" element={<ProductSearch />} />

          <Route element={<ProtectedRoute />}>
            <Route path="/dashboard" element={<Dashboard />} />
            <Route path="/profile" element={<Profile />} />
          </Route>

          <Route element={<RoleRoute allowedRoles={[ROLES.BUYER]} />}>
            <Route path="/cart" element={<Cart />} />
            <Route path="/checkout" element={<Checkout />} />
            <Route path="/checkout/paypal/return" element={<PayPalReturn />} />
            <Route path="/orders" element={<OrderHistory />} />
            <Route path="/pro" element={<ProSubscription />} />
            <Route path="/orders/:orderId/receipt" element={<ReceiptView />} />
          </Route>

          <Route element={<RoleRoute allowedRoles={[ROLES.SHOP_OWNER]} />}>
            <Route path="/shops/new" element={<CreateShop />} />
          </Route>

          <Route element={<RoleRoute allowedRoles={[ROLES.SHOP_OWNER]} />}>
            <Route path="/shop/dashboard" element={<ShopDashboard />} />
            <Route path="/shop/products" element={<ShopProductManagement />} />
            <Route path="/shop/orders" element={<OrderManagement />} />
            <Route path="/shop/payouts" element={<PayoutDashboard />} />
          </Route>

          <Route element={<RoleRoute allowedRoles={[ROLES.SHOP_OWNER]} />}>
            <Route path="/shops/:shopId/collections" element={<CollectionManager />} />
          </Route>

          <Route element={<RoleRoute allowedRoles={[ROLES.SHOP_OWNER]} />}>
            <Route path="/products/new" element={<ProductForm mode="create" />} />
            <Route path="/products/:productId/edit" element={<ProductForm mode="edit" />} />
            <Route path="/products/:productId/stock" element={<StockManagement />} />
          </Route>

          <Route element={<RoleRoute allowedRoles={[ROLES.ADMIN]} />}>
            <Route path="/admin" element={<Admin />} />
          </Route>
        </Routes>
      </main>
    </div>
  );
}
