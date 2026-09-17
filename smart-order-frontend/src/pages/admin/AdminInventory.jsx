import { useState, useEffect } from 'react';
import { adminAPI } from '../../api';
import { FiPlus } from 'react-icons/fi';

export default function AdminInventory() {
  const [branches, setBranches] = useState([]);
  const [selectedBranch, setSelectedBranch] = useState('');
  const [inventory, setInventory] = useState([]);
  const [products, setProducts] = useState([]);
  const [loading, setLoading] = useState(true);
  const [showModal, setShowModal] = useState(false);
  const [selectedProduct, setSelectedProduct] = useState('');
  const [initialQty, setInitialQty] = useState(25);
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    Promise.all([adminAPI.getBranches(), adminAPI.getProducts()])
      .then(([bRes, pRes]) => {
        setBranches(bRes.data);
        setProducts(pRes.data);
        if (bRes.data.length > 0) {
          setSelectedBranch(bRes.data[0].id);
        }
        if (pRes.data.length > 0) {
          setSelectedProduct(pRes.data[0].id);
        }
      })
      .finally(() => setLoading(false));
  }, []);

  useEffect(() => {
    if (selectedBranch) {
      adminAPI.getInventory(selectedBranch).then((res) => setInventory(res.data));
    }
  }, [selectedBranch]);

  const handleStockUpdate = async (invId, newQty) => {
    try {
      await adminAPI.updateInventory(invId, { stockQuantity: parseInt(newQty) });
      adminAPI.getInventory(selectedBranch).then((res) => setInventory(res.data));
    } catch (err) {
      alert('Failed to update stock');
    }
  };

  const handleAddInventory = async () => {
    if (!selectedBranch || !selectedProduct) return;
    setSaving(true);
    try {
      await adminAPI.createInventory({
        branchId: selectedBranch,
        productId: selectedProduct,
        stockQuantity: parseInt(initialQty) || 0
      });
      setShowModal(false);
      adminAPI.getInventory(selectedBranch).then((res) => setInventory(res.data));
    } catch (err) {
      alert(err.response?.data?.error || 'Failed to add product to branch');
    } finally {
      setSaving(false);
    }
  };

  const getProductName = (productId) => products.find((p) => p.id === productId)?.name || 'Unknown';

  if (loading) return <div className="loading"><div className="spinner" />Loading...</div>;

  return (
    <>
      <div className="page-header" style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'start' }}>
        <div>
          <h2>Manage Inventory</h2>
          <p>Update stock levels for each branch</p>
        </div>
        <button className="btn btn-primary" onClick={() => setShowModal(true)}>
          <FiPlus /> Add Product Stock
        </button>
      </div>

      <div className="search-bar">
        <select value={selectedBranch} onChange={(e) => setSelectedBranch(e.target.value)} style={{ minWidth: 250 }}>
          {branches.map((b) => (
            <option key={b.id} value={b.id}>{b.name}</option>
          ))}
        </select>
      </div>

      <div className="card">
        <div className="table-container">
          <table>
            <thead>
              <tr><th>Product</th><th>Current Stock</th><th>Update Stock</th></tr>
            </thead>
            <tbody>
              {inventory.map((inv) => (
                <tr key={inv.id}>
                  <td style={{ fontWeight: 600 }}>{getProductName(inv.productId)}</td>
                  <td>
                    <span className={`badge ${inv.stockQuantity > 10 ? 'badge-delivered' : inv.stockQuantity > 0 ? 'badge-pending' : 'badge-cancelled'}`}>
                      {inv.stockQuantity} units
                    </span>
                  </td>
                  <td>
                    <div style={{ display: 'flex', gap: 8, alignItems: 'center' }}>
                      <input type="number" min="0" defaultValue={inv.stockQuantity} style={{ width: 100, padding: '6px 10px', border: '1px solid var(--border)', borderRadius: 'var(--radius)', fontSize: 14 }}
                        onBlur={(e) => {
                          if (parseInt(e.target.value) !== inv.stockQuantity) {
                            handleStockUpdate(inv.id, e.target.value);
                          }
                        }}
                        onKeyDown={(e) => {
                          if (e.key === 'Enter') {
                            handleStockUpdate(inv.id, e.target.value);
                            e.target.blur();
                          }
                        }}
                      />
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
        {inventory.length === 0 && (
          <div className="empty-state"><h3>No inventory records</h3><p>Select a branch or click "+ Add Product Stock" to add stock to this branch.</p></div>
        )}
      </div>

      {showModal && (
        <div className="modal-overlay" onClick={() => setShowModal(false)}>
          <div className="modal" onClick={(e) => e.stopPropagation()}>
            <h3>Add Product Stock to Branch</h3>

            <div className="form-group">
              <label>Target Branch</label>
              <select value={selectedBranch} onChange={(e) => setSelectedBranch(e.target.value)} className="form-control" style={{ width: '100%', padding: '10px 12px', borderRadius: 6, border: '1px solid var(--border-color)', backgroundColor: 'var(--bg-secondary)', color: 'var(--text-primary)' }}>
                {branches.map((b) => (
                  <option key={b.id} value={b.id}>{b.name}</option>
                ))}
              </select>
            </div>

            <div className="form-group">
              <label>Select Product</label>
              <select value={selectedProduct} onChange={(e) => setSelectedProduct(e.target.value)} className="form-control" style={{ width: '100%', padding: '10px 12px', borderRadius: 6, border: '1px solid var(--border-color)', backgroundColor: 'var(--bg-secondary)', color: 'var(--text-primary)' }}>
                {products.map((p) => (
                  <option key={p.id} value={p.id}>{p.name} ({p.category})</option>
                ))}
              </select>
            </div>

            <div className="form-group">
              <label>Stock Quantity</label>
              <input type="number" min="0" value={initialQty} onChange={(e) => setInitialQty(e.target.value)} />
            </div>

            <div className="modal-actions">
              <button className="btn btn-outline" onClick={() => setShowModal(false)}>Cancel</button>
              <button className="btn btn-primary" onClick={handleAddInventory} disabled={saving}>
                {saving ? 'Adding...' : 'Add Stock'}
              </button>
            </div>
          </div>
        </div>
      )}
    </>
  );
}
