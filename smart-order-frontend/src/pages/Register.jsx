import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { authAPI } from '../api';
import { FiMapPin } from 'react-icons/fi';
import { SRI_LANKA_DISTRICTS } from '../utils/districts';

export default function Register() {
  const [form, setForm] = useState({
    name: '', email: '', password: '', phone: '', address: '',
    latitude: 6.9271, longitude: 79.8612,
  });
  const [selectedDistrict, setSelectedDistrict] = useState('Colombo');
  const [locStatus, setLocStatus] = useState('Defaulted to Colombo');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);
  const { login } = useAuth();
  const navigate = useNavigate();

  const handleDistrictChange = (e) => {
    const districtName = e.target.value;
    setSelectedDistrict(districtName);
    const found = SRI_LANKA_DISTRICTS.find((d) => d.name === districtName);
    if (found) {
      setForm((prev) => ({ ...prev, latitude: found.lat, longitude: found.lng }));
      setLocStatus(`Coordinates set to ${found.name} District center`);
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
        setForm((prev) => ({
          ...prev,
          latitude: pos.coords.latitude,
          longitude: pos.coords.longitude,
        }));
        setLocStatus(`GPS detected (${pos.coords.latitude.toFixed(4)}, ${pos.coords.longitude.toFixed(4)})`);
      },
      () => {
        setLocStatus('Could not access GPS. Using district coordinates.');
      }
    );
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    setLoading(true);
    try {
      const fullAddress = form.address ? `${form.address}, ${selectedDistrict}` : `${selectedDistrict} District`;
      const res = await authAPI.register({ ...form, address: fullAddress });
      login(res.data);
      navigate('/products');
    } catch (err) {
      setError(err.response?.data?.error || err.response?.data?.fieldErrors
        ? Object.values(err.response.data.fieldErrors).join(', ')
        : 'Registration failed.');
    } finally {
      setLoading(false);
    }
  };

  const update = (field) => (e) => setForm({ ...form, [field]: e.target.value });

  return (
    <div className="auth-container">
      <div className="auth-card">
        <h2>Create Account</h2>
        <p>Join Smart Order to start shopping</p>
        {error && <div className="alert alert-error">{error}</div>}
        <form onSubmit={handleSubmit}>
          <div className="form-group">
            <label htmlFor="name">Full Name</label>
            <input id="name" type="text" placeholder="John Perera" required
              value={form.name} onChange={update('name')} />
          </div>
          <div className="form-group">
            <label htmlFor="reg-email">Email Address</label>
            <input id="reg-email" type="email" placeholder="you@example.com" required
              value={form.email} onChange={update('email')} />
          </div>
          <div className="form-group">
            <label htmlFor="reg-password">Password</label>
            <input id="reg-password" type="password" placeholder="Min 6 characters" required minLength={6}
              value={form.password} onChange={update('password')} />
          </div>
          <div className="form-group">
            <label htmlFor="phone">Phone Number</label>
            <input id="phone" type="text" placeholder="077 123 4567"
              value={form.phone} onChange={update('phone')} />
          </div>
          <div className="form-group">
            <label htmlFor="district">District <span style={{ color: 'var(--danger-color)' }}>*</span></label>
            <select id="district" value={selectedDistrict} onChange={handleDistrictChange} className="form-control" style={{ width: '100%', padding: '10px 12px', borderRadius: 6, border: '1px solid var(--border-color)', backgroundColor: 'var(--bg-secondary)', color: 'var(--text-primary)' }}>
              {SRI_LANKA_DISTRICTS.map((d) => (
                <option key={d.name} value={d.name}>{d.name} District</option>
              ))}
            </select>
          </div>
          <div className="form-group">
            <label htmlFor="address">Street / Delivery Address</label>
            <input id="address" type="text" placeholder="e.g. 42 Galle Road, Colombo 03"
              value={form.address} onChange={update('address')} />
          </div>



          <button className="btn btn-primary" type="submit" disabled={loading}>
            {loading ? 'Creating Account...' : 'Create Account'}
          </button>
        </form>
        <div className="auth-footer">
          Already have an account? <Link to="/login">Sign in</Link>
        </div>
      </div>
    </div>
  );
}

