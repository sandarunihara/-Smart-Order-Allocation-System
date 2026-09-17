import { useState, useEffect } from 'react';
import { productAPI } from '../api';
import { useCart } from '../context/CartContext';
import { FiShoppingCart, FiPlus, FiMinus } from 'react-icons/fi';

export default function Products() {
  const [products, setProducts] = useState([]);
  const [loading, setLoading] = useState(true);
  const [search, setSearch] = useState('');
  const [category, setCategory] = useState('');
  const { addItem, items, updateQuantity } = useCart();

  useEffect(() => {
    loadProducts();
  }, [search, category]);

  const loadProducts = async () => {
    try {
      const params = {};
      if (search) params.search = search;
      if (category) params.category = category;
      const res = await productAPI.getAll(params);
      setProducts(res.data);
    } catch (err) {
      console.error('Failed to load products', err);
    } finally {
      setLoading(false);
    }
  };

  const categories = [...new Set(products.map((p) => p.category).filter(Boolean))];
  const getCartQty = (productId) => items.find((i) => i.product.id === productId)?.quantity || 0;

  if (loading) return <div className="loading"><div className="spinner" />Loading products...</div>;

  return (
    <>
      <div className="page-header">
        <h2>Products</h2>
        <p>Browse our catalog and add items to your cart</p>
      </div>

      <div className="search-bar">
        <input type="text" placeholder="Search products..." value={search}
          onChange={(e) => setSearch(e.target.value)} />
        <select value={category} onChange={(e) => setCategory(e.target.value)}>
          <option value="">All Categories</option>
          {categories.map((c) => <option key={c} value={c}>{c}</option>)}
        </select>
      </div>

      <div className="product-grid">
        {products.map((product) => {
          const qty = getCartQty(product.id);
          return (
            <div key={product.id} className="product-card">
              <div className="product-category">{product.category}</div>
              <h3>{product.name}</h3>
              <p className="product-desc">{product.description}</p>
              <div className="product-price">Rs. {parseFloat(product.price).toLocaleString()}</div>
              <div className="product-actions">
                {qty > 0 ? (
                  <div className="qty-control">
                    <button onClick={() => updateQuantity(product.id, qty - 1)}><FiMinus /></button>
                    <span>{qty}</span>
                    <button onClick={() => updateQuantity(product.id, qty + 1)}><FiPlus /></button>
                  </div>
                ) : (
                  <button className="btn btn-primary btn-sm" onClick={() => addItem(product)}>
                    <FiShoppingCart /> Add to Cart
                  </button>
                )}
              </div>
            </div>
          );
        })}
      </div>

      {products.length === 0 && (
        <div className="empty-state">
          <h3>No products found</h3>
          <p>Try adjusting your search or filter.</p>
        </div>
      )}
    </>
  );
}
