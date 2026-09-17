import { useEffect } from 'react';
import { BrowserRouter, Routes, Route, Navigate, NavLink, useNavigate } from 'react-router-dom';
import { AuthProvider, useAuth } from './context/AuthContext';
import { CartProvider, useCart } from './context/CartContext';
import { FiGrid, FiShoppingBag, FiPackage, FiMapPin, FiBox, FiLayers, FiLogOut, FiShoppingCart } from 'react-icons/fi';

import Login from './pages/Login';
import Register from './pages/Register';
import Products from './pages/Products';
import Checkout from './pages/Checkout';
import MyOrders from './pages/MyOrders';
import OrderDetail from './pages/OrderDetail';
import Dashboard from './pages/admin/Dashboard';
import AdminOrders from './pages/admin/AdminOrders';
import AdminBranches from './pages/admin/AdminBranches';
import AdminProducts from './pages/admin/AdminProducts';
import AdminInventory from './pages/admin/AdminInventory';

import './index.css';

function ProtectedRoute({ children, adminOnly = false }) {
  const { isAuthenticated, isAdmin, loading } = useAuth();
  if (loading) return <div className="loading"><div className="spinner" />Loading...</div>;
  if (!isAuthenticated) return <Navigate to="/login" />;
  if (adminOnly && !isAdmin) return <Navigate to="/products" />;
  return children;
}

function AppLayout() {
  const { user, isAdmin, logout } = useAuth();
  const { itemCount, fetchCart } = useCart();
  const navigate = useNavigate();

  useEffect(() => {
    fetchCart();
  }, [user]);

  const handleLogout = () => {
    logout();
    navigate('/login');
  };

  return (
    <div className="app-layout">
      <aside className="sidebar">
        <div className="sidebar-logo">
          <h1>SmartOrder</h1>
          <span>{user?.name} ({user?.role})</span>
        </div>
        <nav className="sidebar-nav">
          {isAdmin ? (
            <>
              <div className="sidebar-section-label">Admin</div>
              <NavLink to="/admin" end><FiGrid /> Dashboard</NavLink>
              <NavLink to="/admin/orders"><FiPackage /> Orders</NavLink>
              <NavLink to="/admin/branches"><FiMapPin /> Branches</NavLink>
              <NavLink to="/admin/products"><FiBox /> Products</NavLink>
              <NavLink to="/admin/inventory"><FiLayers /> Inventory</NavLink>
            </>
          ) : (
            <>
              <div className="sidebar-section-label">Shop</div>
              <NavLink to="/products"><FiShoppingBag /> Products</NavLink>
              <NavLink to="/checkout">
                <FiShoppingCart /> Cart {itemCount > 0 && <span className="badge badge-allocated" style={{ marginLeft: 'auto' }}>{itemCount}</span>}
              </NavLink>
              <NavLink to="/my-orders"><FiPackage /> My Orders</NavLink>
            </>
          )}
        </nav>
        <div style={{ padding: '0 12px', marginTop: 'auto' }}>
          <button className="sidebar-logout-btn" onClick={handleLogout}><FiLogOut /> Sign Out</button>
        </div>
      </aside>
      <main className="main-content">
        <Routes>
          <Route path="/products" element={<Products />} />
          <Route path="/checkout" element={<Checkout />} />
          <Route path="/my-orders" element={<MyOrders />} />
          <Route path="/orders/:id" element={<OrderDetail />} />
          <Route path="/admin" element={<ProtectedRoute adminOnly><Dashboard /></ProtectedRoute>} />
          <Route path="/admin/orders" element={<ProtectedRoute adminOnly><AdminOrders /></ProtectedRoute>} />
          <Route path="/admin/branches" element={<ProtectedRoute adminOnly><AdminBranches /></ProtectedRoute>} />
          <Route path="/admin/products" element={<ProtectedRoute adminOnly><AdminProducts /></ProtectedRoute>} />
          <Route path="/admin/inventory" element={<ProtectedRoute adminOnly><AdminInventory /></ProtectedRoute>} />
        </Routes>
      </main>
    </div>
  );
}

export default function App() {
  return (
    <BrowserRouter>
      <AuthProvider>
        <CartProvider>
          <Routes>
            <Route path="/login" element={<Login />} />
            <Route path="/register" element={<Register />} />
            <Route path="/*" element={
              <ProtectedRoute>
                <AppLayout />
              </ProtectedRoute>
            } />
            <Route path="/" element={<Navigate to="/products" />} />
          </Routes>
        </CartProvider>
      </AuthProvider>
    </BrowserRouter>
  );
}
