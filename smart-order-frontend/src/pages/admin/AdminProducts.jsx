import { useState, useEffect } from 'react';
import { adminAPI } from '../../api';
import { FiPlus, FiEdit2, FiTrash2 } from 'react-icons/fi';

export default function AdminProducts() {
  const [products, setProducts] = useState([]);
  const [loading, setLoading] = useState(true);
  const [showModal, setShowModal] = useState(false);
  const [editing, setEditing] = useState(null);
  const [form, setForm] = useState({ name: '', description: '', price: '', category: '', isActive: true });

  useEffect(() => { loadProducts(); }, []);

  const loadProducts = () => {
    adminAPI.getProducts().then((res) => setProducts(res.data)).finally(() => setLoading(false));
  };

  const handleSave = async () => {
    try {
      const data = { ...form, price: parseFloat(form.price) };
      if (editing) {
        await adminAPI.updateProduct(editing, data);
      } else {
        await adminAPI.createProduct(data);
      }
      setShowModal(false);
      setEditing(null);
      loadProducts();
    } catch (err) {
      alert(err.response?.data?.error || 'Failed to save');
    }
  };

  const handleEdit = (p) => {
    setEditing(p.id);
    setForm({ name: p.name, description: p.description || '', price: p.price, category: p.category || '', isActive: p.isActive });
    setShowModal(true);
  };

  const handleDelete = async (id) => {
    if (!confirm('Deactivate this product?')) return;
    await adminAPI.deleteProduct(id);
    loadProducts();
  };

  const openNew = () => {
    setEditing(null);
    setForm({ name: '', description: '', price: '', category: '', isActive: true });
    setShowModal(true);
  };

  if (loading) return <div className="loading"><div className="spinner" />Loading...</div>;

  return (
    <>
      <div className="page-header" style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'start' }}>
        <div>
          <h2>Manage Products</h2>
          <p>{products.length} products</p>
        </div>
        <button className="btn btn-primary" onClick={openNew}><FiPlus /> Add Product</button>
      </div>

      <div className="card">
        <div className="table-container">
          <table>
            <thead>
              <tr><th>Name</th><th>Category</th><th>Price</th><th>Status</th><th>Actions</th></tr>
            </thead>
            <tbody>
              {products.map((p) => (
                <tr key={p.id}>
                  <td>
                    <div style={{ fontWeight: 600 }}>{p.name}</div>
                    <div style={{ fontSize: 12, color: 'var(--text-secondary)' }}>{p.description?.substring(0, 60)}</div>
                  </td>
                  <td>{p.category}</td>
                  <td style={{ fontWeight: 600 }}>Rs. {parseFloat(p.price).toLocaleString()}</td>
                  <td><span className={`badge ${p.isActive ? 'badge-delivered' : 'badge-cancelled'}`}>{p.isActive ? 'Active' : 'Inactive'}</span></td>
                  <td>
                    <div style={{ display: 'flex', gap: 4 }}>
                      <button className="btn btn-sm btn-outline" onClick={() => handleEdit(p)}><FiEdit2 /></button>
                      <button className="btn btn-sm btn-outline" onClick={() => handleDelete(p.id)}><FiTrash2 /></button>
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>

      {showModal && (
        <div className="modal-overlay" onClick={() => setShowModal(false)}>
          <div className="modal" onClick={(e) => e.stopPropagation()}>
            <h3>{editing ? 'Edit Product' : 'Add Product'}</h3>
            <div className="form-group">
              <label>Name</label>
              <input value={form.name} onChange={(e) => setForm({ ...form, name: e.target.value })} />
            </div>
            <div className="form-group">
              <label>Description</label>
              <textarea rows={2} value={form.description} onChange={(e) => setForm({ ...form, description: e.target.value })} />
            </div>
            <div style={{ display: 'flex', gap: 12 }}>
              <div className="form-group" style={{ flex: 1 }}>
                <label>Price (Rs.)</label>
                <input type="number" step="0.01" value={form.price} onChange={(e) => setForm({ ...form, price: e.target.value })} />
              </div>
              <div className="form-group" style={{ flex: 1 }}>
                <label>Category</label>
                <input value={form.category} onChange={(e) => setForm({ ...form, category: e.target.value })} />
              </div>
            </div>
            <div className="modal-actions">
              <button className="btn btn-outline" onClick={() => setShowModal(false)}>Cancel</button>
              <button className="btn btn-primary" onClick={handleSave}>Save</button>
            </div>
          </div>
        </div>
      )}
    </>
  );
}
