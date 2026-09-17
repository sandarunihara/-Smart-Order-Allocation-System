import { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { adminAPI } from '../../api';

const STATUS_OPTIONS = ['ALLOCATED', 'PREPARING', 'READY', 'DELIVERED'];

export default function AdminOrders() {
  const [orders, setOrders] = useState([]);
  const [loading, setLoading] = useState(true);
  const [filter, setFilter] = useState('');
  const [search, setSearch] = useState('');

  useEffect(() => {
    adminAPI.getAllOrders().then((res) => setOrders(res.data)).finally(() => setLoading(false));
  }, []);

  const handleStatusUpdate = async (orderId, newStatus) => {
    try {
      const res = await adminAPI.updateOrderStatus(orderId, newStatus);
      setOrders((prev) => prev.map((o) => (o.id === orderId ? res.data : o)));
    } catch (err) {
      alert(err.response?.data?.error || 'Status update failed');
    }
  };

  const filteredOrders = orders.filter((o) => {
    if (filter && o.status !== filter) return false;
    if (search) {
      const s = search.toLowerCase();
      return (
        o.id.toLowerCase().includes(s) ||
        o.customerName?.toLowerCase().includes(s) ||
        o.customerEmail?.toLowerCase().includes(s) ||
        o.allocatedBranch?.name?.toLowerCase().includes(s)
      );
    }
    return true;
  });

  if (loading) return <div className="loading"><div className="spinner" />Loading orders...</div>;

  return (
    <>
      <div className="page-header">
        <h2>Manage Orders</h2>
        <p>{orders.length} total orders</p>
      </div>

      <div className="search-bar">
        <input type="text" placeholder="Search by ID, customer, branch..." value={search}
          onChange={(e) => setSearch(e.target.value)} />
        <select value={filter} onChange={(e) => setFilter(e.target.value)}>
          <option value="">All Status</option>
          <option value="ALLOCATED">Allocated</option>
          <option value="PREPARING">Preparing</option>
          <option value="READY">Ready</option>
          <option value="DELIVERED">Delivered</option>
          <option value="CANCELLED">Cancelled</option>
          <option value="REJECTED">Rejected</option>
        </select>
      </div>

      <div className="card">
        <div className="table-container">
          <table>
            <thead>
              <tr>
                <th>Order ID</th>
                <th>Customer</th>
                <th>Items</th>
                <th>Total</th>
                <th>Branch</th>
                <th>Status</th>
                <th>AI Category</th>
                <th>Date</th>
                <th>Actions</th>
              </tr>
            </thead>
            <tbody>
              {filteredOrders.map((order) => {
                const nextStatus = {
                  ALLOCATED: 'PREPARING',
                  PREPARING: 'READY',
                };
                const next = nextStatus[order.status];

                return (
                  <tr key={order.id}>
                    <td style={{ fontFamily: 'monospace', fontSize: 12 }}>{order.id.substring(0, 8)}</td>
                    <td>
                      <div style={{ fontWeight: 500 }}>{order.customerName}</div>
                      <div style={{ fontSize: 12, color: 'var(--text-secondary)' }}>{order.customerEmail}</div>
                    </td>
                    <td>{order.items?.length || 0}</td>
                    <td style={{ fontWeight: 600 }}>Rs. {parseFloat(order.totalAmount).toLocaleString()}</td>
                    <td>{order.allocatedBranch?.name || '—'}</td>
                    <td><span className={`badge badge-${order.status.toLowerCase()}`}>{order.status}</span></td>
                    <td style={{ fontSize: 12 }}>
                      {order.classifiedCategory && (
                        <span title={`Confidence: ${order.classificationConfidence?.toFixed(1)}%`}>
                          {order.classifiedCategory}
                        </span>
                      )}
                    </td>
                    <td style={{ fontSize: 12, color: 'var(--text-secondary)' }}>
                      {order.createdAt ? new Date(order.createdAt).toLocaleDateString() : '—'}
                    </td>
                    <td>
                      <div style={{ display: 'flex', gap: 4 }}>
                        <Link to={`/orders/${order.id}`} className="btn btn-sm btn-outline">View</Link>
                        {next && (
                          <button className="btn btn-sm btn-primary"
                            onClick={() => handleStatusUpdate(order.id, next)}>
                            → {next}
                          </button>
                        )}
                      </div>
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        </div>
        {filteredOrders.length === 0 && (
          <div className="empty-state"><h3>No orders found</h3></div>
        )}
      </div>
    </>
  );
}
