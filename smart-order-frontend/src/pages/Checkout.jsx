import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useCart } from '../context/CartContext';
import { orderAPI } from '../api';
import { FiTrash2, FiPlus, FiMinus, FiMapPin } from 'react-icons/fi';
import { SRI_LANKA_DISTRICTS } from '../utils/districts';

export default function Checkout() {
  const { items, updateQuantity, removeItem, clearCart, total } = useCart();
  const [message, setMessage] = useState('');
  const [address, setAddress] = useState('');
  const [selectedDistrict, setSelectedDistrict] = useState('Colombo');
  const [lat, setLat] = useState(6.9271);
  const [lng, setLng] = useState(79.8612);
  const [locStatus, setLocStatus] = useState('Defaulted to Colombo');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const navigate = useNavigate();

  const handleDistrictChange = (e) => {
    const districtName = e.target.value;
    setSelectedDistrict(districtName);
    const found = SRI_LANKA_DISTRICTS.find((d) => d.name === districtName);
    if (found) {
      setLat(found.lat);
      setLng(found.lng);
      setLocStatus(`Coordinates set to ${found.name} District center (${found.lat.toFixed(4)}, ${found.lng.toFixed(4)})`);
    }
  };

  const handleDetectLocation = () => {
    if (!navigator.geolocation) {
      setError('Geolocation is not supported by your browser.');
      return;
    }
    setLocStatus('Detecting GPS location...');
    navigator.geolocation.getCurrentPosition(
      (pos) => {
        const detectedLat = pos.coords.latitude;
        const detectedLng = pos.coords.longitude;
        setLat(detectedLat);
        setLng(detectedLng);
        setLocStatus(`GPS location detected (${detectedLat.toFixed(4)}, ${detectedLng.toFixed(4)})`);
      },
      (err) => {
        setLocStatus('Could not access GPS location. Using district coordinates.');
      }
    );
  };

  const handlePlaceOrder = async () => {
    if (items.length === 0) return;
    if (!selectedDistrict) {
      setError('Please select a district for delivery.');
      return;
    }
    if (!address || !address.trim()) {
      setError('Please enter your Street / Delivery Address before placing your order.');
      return;
    }
    setLoading(true);
    setError('');
    try {
      const fullAddress = `${address.trim()}, ${selectedDistrict}`;
      const orderData = {
        items: items.map((i) => ({ productId: i.product.id, quantity: i.quantity })),
        customerMessage: message || null,
        customerLatitude: lat,
        customerLongitude: lng,
        customerAddress: fullAddress,
      };
      const res = await orderAPI.create(orderData);
      clearCart();
      navigate(`/orders/${res.data.id}`);
    } catch (err) {
      setError(err.response?.data?.error || 'Failed to place order');
    } finally {
      setLoading(false);
    }
  };

  if (items.length === 0) {
    return (
      <div className="empty-state">
        <h3>Your cart is empty</h3>
        <p>Browse products and add items to your cart.</p>
        <button className="btn btn-primary" style={{ marginTop: 16 }} onClick={() => navigate('/products')}>
          Browse Products
        </button>
      </div>
    );
  }

  return (
    <>
      <div className="page-header">
        <h2>Checkout</h2>
        <p>Review your order and place it</p>
      </div>

      {error && <div className="alert alert-error">{error}</div>}

      <div className="order-detail-grid">
        <div className="card">
          <h3 style={{ marginBottom: 16 }}>Order Items</h3>
          {items.map((item) => (
            <div key={item.product.id} className="cart-item">
              <div>
                <div style={{ fontWeight: 600 }}>{item.product.name}</div>
                <div style={{ fontSize: 13, color: 'var(--text-secondary)' }}>
                  Rs. {parseFloat(item.product.price).toLocaleString()} each
                </div>
              </div>
              <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
                <div className="qty-control">
                  <button onClick={() => updateQuantity(item.product.id, item.quantity - 1)}><FiMinus /></button>
                  <span>{item.quantity}</span>
                  <button onClick={() => updateQuantity(item.product.id, item.quantity + 1)}><FiPlus /></button>
                </div>
                <div style={{ fontWeight: 600, minWidth: 80, textAlign: 'right' }}>
                  Rs. {(parseFloat(item.product.price) * item.quantity).toLocaleString()}
                </div>
                <button className="btn btn-sm btn-outline" onClick={() => removeItem(item.product.id)}>
                  <FiTrash2 />
                </button>
              </div>
            </div>
          ))}
          <div className="cart-total">
            <span>Total</span>
            <span>Rs. {total.toLocaleString()}</span>
          </div>
        </div>

        <div>
          <div className="card" style={{ marginBottom: 16 }}>
            <h3 style={{ marginBottom: 16 }}>Delivery Details</h3>

            <div className="form-group">
              <label>District <span style={{ color: 'var(--danger-color)' }}>*</span></label>
              <select value={selectedDistrict} onChange={handleDistrictChange} className="form-control" style={{ width: '100%', padding: '10px 12px', borderRadius: 6, border: '1px solid var(--border-color)', backgroundColor: 'var(--bg-secondary)', color: 'var(--text-primary)' }}>
                {SRI_LANKA_DISTRICTS.map((d) => (
                  <option key={d.name} value={d.name}>{d.name} District</option>
                ))}
              </select>
            </div>

            <div className="form-group">
              <label>Street / Delivery Address <span style={{ color: 'var(--danger-color)' }}>*</span></label>
              <input type="text" placeholder="e.g. 42 Galle Road, Colombo 03" value={address}
                required
                onChange={(e) => setAddress(e.target.value)} />
            </div>


          </div>

          <div className="card" style={{ marginBottom: 16 }}>
            <h3 style={{ marginBottom: 16 }}>Order Note (Optional)</h3>
            <div className="form-group">
              <textarea rows={3} placeholder="Add any message or note for your order..."
                value={message} onChange={(e) => setMessage(e.target.value)} />
            </div>
            <p style={{ fontSize: 12, color: 'var(--text-secondary)' }}>
              Your message will be automatically classified by our AI system.
            </p>
          </div>

          <button className="btn btn-primary" style={{ width: '100%', justifyContent: 'center', padding: 14 }}
            onClick={handlePlaceOrder} disabled={loading}>
            {loading ? 'Placing Order...' : `Place Order — Rs. ${total.toLocaleString()}`}
          </button>
        </div>
      </div>
    </>
  );
}

