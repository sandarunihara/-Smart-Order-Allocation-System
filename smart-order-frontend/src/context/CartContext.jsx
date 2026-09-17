import { createContext, useContext, useState, useEffect } from 'react';
import { cartAPI } from '../api';

const CartContext = createContext(null);

export function CartProvider({ children }) {
  const [items, setItems] = useState([]);

  const fetchCart = async () => {
    const token = localStorage.getItem('accessToken');
    if (token) {
      try {
        const res = await cartAPI.getCart();
        setItems(res.data);
      } catch (err) {
        console.error('Failed to fetch cart:', err);
      }
    } else {
      setItems([]);
    }
  };

  useEffect(() => {
    fetchCart();
  }, []);

  const addItem = async (product) => {
    // Optimistic UI update
    setItems((prev) => {
      const existing = prev.find((i) => i.product.id === product.id);
      if (existing) {
        return prev.map((i) =>
          i.product.id === product.id ? { ...i, quantity: i.quantity + 1 } : i
        );
      }
      return [...prev, { product, quantity: 1 }];
    });

    try {
      const res = await cartAPI.addToCart(product.id, 1);
      setItems(res.data);
    } catch (err) {
      console.error('Failed to sync add to cart:', err);
    }
  };

  const removeItem = async (productId) => {
    setItems((prev) => prev.filter((i) => i.product.id !== productId));
    try {
      const res = await cartAPI.removeItem(productId);
      setItems(res.data);
    } catch (err) {
      console.error('Failed to remove item from cart:', err);
    }
  };

  const updateQuantity = async (productId, quantity) => {
    if (quantity <= 0) {
      removeItem(productId);
      return;
    }
    setItems((prev) =>
      prev.map((i) => (i.product.id === productId ? { ...i, quantity } : i))
    );
    try {
      const res = await cartAPI.updateQuantity(productId, quantity);
      setItems(res.data);
    } catch (err) {
      console.error('Failed to update cart quantity:', err);
    }
  };

  const clearCart = async () => {
    setItems([]);
    try {
      await cartAPI.clearCart();
    } catch (err) {
      console.error('Failed to clear cart:', err);
    }
  };

  const total = items.reduce(
    (sum, i) => sum + parseFloat(i.product.price) * i.quantity, 0
  );

  const itemCount = items.reduce((sum, i) => sum + i.quantity, 0);

  return (
    <CartContext.Provider value={{ items, addItem, removeItem, updateQuantity, clearCart, fetchCart, total, itemCount }}>
      {children}
    </CartContext.Provider>
  );
}

export function useCart() {
  const context = useContext(CartContext);
  if (!context) throw new Error('useCart must be used within CartProvider');
  return context;
}
