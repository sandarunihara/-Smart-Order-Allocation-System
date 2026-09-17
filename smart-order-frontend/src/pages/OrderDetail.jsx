import { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { orderAPI } from '../api';
import { useAuth } from '../context/AuthContext';
import { FiSend, FiMessageSquare } from 'react-icons/fi';

export default function OrderDetail() {
  const { id } = useParams();
  const { isAdmin } = useAuth();
  const [order, setOrder] = useState(null);
  const [loading, setLoading] = useState(true);
  const [cancelling, setCancelling] = useState(false);
  const [delivering, setDelivering] = useState(false);
  const [messages, setMessages] = useState([]);
  const [newMessage, setNewMessage] = useState('');
  const [sendingMsg, setSendingMsg] = useState(false);
  const navigate = useNavigate();

  useEffect(() => {
    orderAPI.getById(id).then((res) => setOrder(res.data)).catch(() => navigate('/my-orders')).finally(() => setLoading(false));
    orderAPI.getMessages(id).then((res) => setMessages(res.data)).catch(() => {});
  }, [id]);

  const handleCancel = async () => {
    if (!confirm('Are you sure you want to cancel this order?')) return;
    setCancelling(true);
    try {
      const res = await orderAPI.cancel(id);
      setOrder(res.data);
    } catch (err) {
      alert(err.response?.data?.error || 'Failed to cancel');
    } finally {
      setCancelling(false);
    }
  };

  const handleConfirmDelivery = async () => {
    if (!confirm('Confirm that you have received your order?')) return;
    setDelivering(true);
    try {
      const res = await orderAPI.confirmDelivery(id);
      setOrder(res.data);
    } catch (err) {
      alert(err.response?.data?.error || 'Failed to confirm delivery');
    } finally {
      setDelivering(false);
    }
  };

  const handleSendMessage = async (e) => {
    e.preventDefault();
    if (!newMessage || !newMessage.trim()) return;
    setSendingMsg(true);
    try {
      const res = await orderAPI.addMessage(id, newMessage.trim());
      setMessages((prev) => [...prev, res.data]);
      setNewMessage('');
      if (res.data.classifiedCategory) {
        setOrder((prev) => ({
          ...prev,
          customerMessage: res.data.message,
          classifiedCategory: res.data.classifiedCategory,
          classificationConfidence: res.data.classificationConfidence,
        }));
      }
    } catch (err) {
      alert(err.response?.data?.error || 'Failed to send message');
    } finally {
      setSendingMsg(false);
    }
  };

  if (loading) return <div className="loading"><div className="spinner" />Loading order...</div>;
  if (!order) return null;

  const canCancel = !['DELIVERED', 'CANCELLED', 'REJECTED', 'READY'].includes(order.status);
  const canConfirmDelivery = order.status === 'READY';

  return (
    <>
      <div className="page-header">
        <h2>Order Details</h2>
        <p>Order #{order.id.substring(0, 8)}</p>
      </div>

      <div className="order-detail-grid">
        <div>
          <div className="card" style={{ marginBottom: 16 }}>
            <div className="card-header">
              <h3>Items</h3>
              <span className={`badge badge-${order.status.toLowerCase()}`}>{order.status}</span>
            </div>
            <div className="table-container">
              <table>
                <thead>
                  <tr><th>Product</th><th>Qty</th><th>Price</th><th>Subtotal</th></tr>
                </thead>
                <tbody>
                  {order.items?.map((item) => (
                    <tr key={item.id}>
                      <td>{item.productName}</td>
                      <td>{item.quantity}</td>
                      <td>Rs. {parseFloat(item.unitPrice).toLocaleString()}</td>
                      <td style={{ fontWeight: 600 }}>
                        Rs. {(parseFloat(item.unitPrice) * item.quantity).toLocaleString()}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
            <div className="cart-total">
              <span>Total</span>
              <span>Rs. {parseFloat(order.totalAmount).toLocaleString()}</span>
            </div>
          </div>

          <div className="card" style={{ marginBottom: 16 }}>
            <h3 style={{ marginBottom: 16, display: 'flex', alignItems: 'center', gap: 8 }}>
              <FiMessageSquare /> Messages & Support {isAdmin && '( AI Classified)'}
            </h3>

            {messages.length === 0 ? (
              <div style={{ fontSize: 13, color: 'var(--text-secondary)', fontStyle: 'italic', marginBottom: 16 }}>
                No messages sent yet. Have a question or issue about your order? Send a message below.
              </div>
            ) : (
              <div style={{ display: 'flex', flexDirection: 'column', gap: 12, marginBottom: 16, maxHeight: 350, overflowY: 'auto', paddingRight: 4 }}>
                {messages.map((m) => (
                  <div key={m.id} style={{
                    padding: '12px 14px',
                    borderRadius: 8,
                    backgroundColor: m.senderRole === 'CUSTOMER' ? 'var(--bg-secondary)' : 'var(--bg-tertiary)',
                    borderLeft: `4px solid ${m.senderRole === 'CUSTOMER' ? 'var(--primary-color)' : 'var(--success-color)'}`
                  }}>
                    <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 6 }}>
                      <span style={{ fontWeight: 600, fontSize: 13 }}>
                        {m.senderRole === 'CUSTOMER'
                          ? `${m.senderName || 'Customer'} ${isAdmin ? '(Customer)' : '(You)'}`
                          : `${m.senderName || 'Branch Staff'} ${isAdmin ? '(You)' : '(Admin)'}`}
                      </span>
                      <span style={{ fontSize: 11, color: 'var(--text-secondary)' }}>
                        {m.createdAt ? new Date(m.createdAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }) : ''}
                      </span>
                    </div>

                    <div style={{ fontSize: 14, marginBottom: 8, lineHeight: 1.4 }}>
                      "{m.message}"
                    </div>

                    {isAdmin && m.senderRole === 'CUSTOMER' && m.classifiedCategory && (
                      <div style={{ display: 'flex', alignItems: 'center', gap: 8, marginTop: 4 }}>
                        <span className="badge badge-delivered" style={{ fontSize: 11, padding: '3px 8px' }}>
                           AI Category: {m.classifiedCategory}
                        </span>
                        {m.classificationConfidence && (
                          <span style={{ fontSize: 11, color: 'var(--text-secondary)' }}>
                            ({m.classificationConfidence.toFixed(0)}% confidence)
                          </span>
                        )}
                      </div>
                    )}
                  </div>
                ))}
              </div>
            )}

            <form onSubmit={handleSendMessage} style={{ display: 'flex', gap: 8 }}>
              <input
                type="text"
                placeholder="Type a message or inquiry (e.g. Where is my delivery?)..."
                value={newMessage}
                onChange={(e) => setNewMessage(e.target.value)}
                style={{ flex: 1, padding: '10px 12px', borderRadius: 6, border: '1px solid var(--border-color)', backgroundColor: 'var(--bg-secondary)', color: 'var(--text-primary)' }}
                disabled={sendingMsg}
              />
              <button className="btn btn-primary" type="submit" disabled={sendingMsg || !newMessage.trim()}>
                <FiSend /> {sendingMsg ? 'Sending...' : 'Send'}
              </button>
            </form>
          </div>
        </div>

        <div>
          <div className="card" style={{ marginBottom: 16 }}>
            <h3 style={{ marginBottom: 12 }}>Order Info</h3>
            <div style={{ fontSize: 14, lineHeight: 2 }}>
              <div><strong>Status:</strong> {order.status}</div>
              {order.customerAddress && <div><strong>Address:</strong> {order.customerAddress}</div>}
            </div>
          </div>

          {order.allocatedBranch && (
            <div className="card" style={{ marginBottom: 16 }}>
              <h3 style={{ marginBottom: 12 }}>Allocated Branch</h3>
              <div style={{ fontSize: 14, lineHeight: 2 }}>
                <div><strong>Name:</strong> {order.allocatedBranch.name}</div>
                <div><strong>Address:</strong> {order.allocatedBranch.address}</div>
              </div>
            </div>
          )}

          {canConfirmDelivery && (
            <button className="btn btn-primary" style={{ width: '100%', justifyContent: 'center', marginBottom: 12, padding: 12 }}
              onClick={handleConfirmDelivery} disabled={delivering}>
              {delivering ? 'Confirming...' : '✅ Confirm Order Received'}
            </button>
          )}

          {canCancel && (
            <button className="btn btn-danger" style={{ width: '100%', justifyContent: 'center' }}
              onClick={handleCancel} disabled={cancelling}>
              {cancelling ? 'Cancelling...' : 'Cancel Order'}
            </button>
          )}
        </div>
      </div>
    </>
  );
}
