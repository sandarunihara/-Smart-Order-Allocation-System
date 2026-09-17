import { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { orderAPI } from '../api';
import { FiPackage } from 'react-icons/fi';

export default function MyOrders() {
  const [orders, setOrders] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    orderAPI.getMyOrders().then((res) => setOrders(res.data)).finally(() => setLoading(false));
  }, []);

  if (loading) return <div className="loading"><div className="spinner" />Loading orders...</div>;

  return (
    <>
      <div className="page-header">
        <h2>My Orders</h2>
        <p>Track and manage your orders</p>
      </div>

      {orders.length === 0 ? (
        <div className="empty-state">
          <FiPackage />
          <h3>No orders yet</h3>
          <p>Browse products and place your first order.</p>
        </div>
      ) : (
        <div className="card">
          <div className="table-container">
            <table>
              <thead>
                <tr>
                  <th>Order ID</th>
                  <th>Items</th>
                  <th>Total</th>
                  <th>Branch</th>
                  <th>Status</th>
                  <th>Date</th>
                  <th></th>
                </tr>
              </thead>
              <tbody>
                {orders.map((order) => (
                  <tr key={order.id}>
                    <td style={{ fontFamily: 'monospace', fontSize: 12 }}>{order.id.substring(0, 8)}...</td>
                    <td>{order.items?.length || 0} item(s)</td>
                    <td style={{ fontWeight: 600 }}>Rs. {parseFloat(order.totalAmount).toLocaleString()}</td>
                    <td>{order.allocatedBranch?.name || '—'}</td>
                    <td><span className={`badge badge-${order.status.toLowerCase()}`}>{order.status}</span></td>
                    <td style={{ fontSize: 13, color: 'var(--text-secondary)' }}>
                      {order.createdAt ? new Date(order.createdAt).toLocaleDateString() : '—'}
                    </td>
                    <td>
                      <Link to={`/orders/${order.id}`} className="btn btn-sm btn-outline">View</Link>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}
    </>
  );
}
