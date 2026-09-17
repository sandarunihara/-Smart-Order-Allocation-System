import { useState, useEffect } from 'react';
import { adminAPI } from '../../api';
import { FiPlus, FiEdit2, FiTrash2 } from 'react-icons/fi';
import { SRI_LANKA_DISTRICTS } from '../../utils/districts';

export default function AdminBranches() {
  const [branches, setBranches] = useState([]);
  const [loading, setLoading] = useState(true);
  const [showModal, setShowModal] = useState(false);
  const [editing, setEditing] = useState(null);
  const [selectedDistrict, setSelectedDistrict] = useState('Colombo');
  const [form, setForm] = useState({
    name: '', address: '', latitude: 6.9271, longitude: 79.8612, maxWorkload: 50, isActive: true
  });
  const [error, setError] = useState('');

  useEffect(() => {
    loadBranches();
  }, []);

  const loadBranches = () => {
    adminAPI.getBranches().then((res) => setBranches(res.data)).finally(() => setLoading(false));
  };

  const handleDistrictChange = (e) => {
    const districtName = e.target.value;
    setSelectedDistrict(districtName);
    const found = SRI_LANKA_DISTRICTS.find((d) => d.name === districtName);
    if (found) {
      setForm((prev) => ({
        ...prev,
        latitude: found.lat,
        longitude: found.lng,
      }));
    }
  };

  const handleSave = async () => {
    setError('');
    if (!form.name || !form.name.trim()) {
      setError('Branch Name is required.');
      return;
    }
    if (!selectedDistrict) {
      setError('District selection is required.');
      return;
    }
    if (!form.address || !form.address.trim()) {
      setError('Street Address is required.');
      return;
    }
    if (!form.maxWorkload || form.maxWorkload <= 0) {
      setError('Max Workload must be at least 1.');
      return;
    }

    try {
      const fullAddress = form.address.toLowerCase().includes(selectedDistrict.toLowerCase())
        ? form.address.trim()
        : `${form.address.trim()}, ${selectedDistrict}`;

      const payload = { ...form, address: fullAddress };

      if (editing) {
        await adminAPI.updateBranch(editing, payload);
      } else {
        await adminAPI.createBranch(payload);
      }
      setShowModal(false);
      setEditing(null);
      loadBranches();
    } catch (err) {
      setError(err.response?.data?.error || 'Failed to save branch.');
    }
  };

  const handleEdit = (branch) => {
    setError('');
    setEditing(branch.id);
    let matchedDistrict = 'Colombo';

    if (branch.latitude && branch.longitude) {
      const match = SRI_LANKA_DISTRICTS.find(
        (d) => Math.abs(d.lat - branch.latitude) < 0.1 && Math.abs(d.lng - branch.longitude) < 0.1
      );
      if (match) matchedDistrict = match.name;
    }
    if (matchedDistrict === 'Colombo' && branch.address) {
      const match = SRI_LANKA_DISTRICTS.find((d) => branch.address.toLowerCase().includes(d.name.toLowerCase()));
      if (match) matchedDistrict = match.name;
    }

    setSelectedDistrict(matchedDistrict);
    setForm({
      name: branch.name || '',
      address: branch.address || '',
      latitude: branch.latitude || 6.9271,
      longitude: branch.longitude || 79.8612,
      maxWorkload: branch.maxWorkload || 50,
      isActive: branch.isActive ?? true
    });
    setShowModal(true);
  };

  const handleDelete = async (id) => {
    if (!confirm('Deactivate this branch?')) return;
    await adminAPI.deleteBranch(id);
    loadBranches();
  };

  const openNew = () => {
    setError('');
    setEditing(null);
    setSelectedDistrict('Colombo');
    const colombo = SRI_LANKA_DISTRICTS[0];
    setForm({
      name: '', address: '', latitude: colombo.lat, longitude: colombo.lng, maxWorkload: 50, isActive: true
    });
    setShowModal(true);
  };

  const getDistrictName = (branch) => {
    if (branch.latitude && branch.longitude) {
      const match = SRI_LANKA_DISTRICTS.find(
        (d) => Math.abs(d.lat - branch.latitude) < 0.1 && Math.abs(d.lng - branch.longitude) < 0.1
      );
      if (match) return `${match.name} District`;
    }
    if (branch.address) {
      const match = SRI_LANKA_DISTRICTS.find((d) => branch.address.toLowerCase().includes(d.name.toLowerCase()));
      if (match) return `${match.name} District`;
    }
    return 'N/A';
  };

  if (loading) return <div className="loading"><div className="spinner" />Loading...</div>;

  return (
    <>
      <div className="page-header" style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'start' }}>
        <div>
          <h2>Manage Branches</h2>
          <p>{branches.length} branches</p>
        </div>
        <button className="btn btn-primary" onClick={openNew}><FiPlus /> Add Branch</button>
      </div>

      <div className="card">
        <div className="table-container">
          <table>
            <thead>
              <tr><th>Name</th><th>Address</th><th>District</th><th>Workload</th><th>Status</th><th>Actions</th></tr>
            </thead>
            <tbody>
              {branches.map((b) => (
                <tr key={b.id}>
                  <td style={{ fontWeight: 600 }}>{b.name}</td>
                  <td>{b.address || '—'}</td>
                  <td>{getDistrictName(b)}</td>
                  <td>{b.currentWorkload ?? 0} / {b.maxWorkload}</td>
                  <td><span className={`badge ${b.isActive ? 'badge-delivered' : 'badge-cancelled'}`}>{b.isActive ? 'Active' : 'Inactive'}</span></td>
                  <td>
                    <div style={{ display: 'flex', gap: 4 }}>
                      <button className="btn btn-sm btn-outline" onClick={() => handleEdit(b)}><FiEdit2 /></button>
                      <button className="btn btn-sm btn-outline" onClick={() => handleDelete(b.id)}><FiTrash2 /></button>
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
            <h3>{editing ? 'Edit Branch' : 'Add Branch'}</h3>

            {error && <div className="alert alert-error" style={{ marginBottom: 12 }}>{error}</div>}

            <div className="form-group">
              <label>Branch Name <span style={{ color: 'var(--danger-color)' }}>*</span></label>
              <input
                value={form.name}
                onChange={(e) => setForm({ ...form, name: e.target.value })}
                placeholder="e.g. Matara City Branch"
                required
              />
            </div>

            <div className="form-group">
              <label>District <span style={{ color: 'var(--danger-color)' }}>*</span></label>
              <select
                value={selectedDistrict}
                onChange={handleDistrictChange}
                className="form-control"
                style={{ width: '100%', padding: '10px 12px', borderRadius: 6, border: '1px solid var(--border-color)', backgroundColor: 'var(--bg-secondary)', color: 'var(--text-primary)' }}
                required
              >
                {SRI_LANKA_DISTRICTS.map((d) => (
                  <option key={d.name} value={d.name}>{d.name} District</option>
                ))}
              </select>
            </div>

            <div className="form-group">
              <label>Street Address <span style={{ color: 'var(--danger-color)' }}>*</span></label>
              <input
                value={form.address}
                onChange={(e) => setForm({ ...form, address: e.target.value })}
                placeholder="e.g. 12 Beach Road, Matara"
                required
              />
            </div>

            <div className="form-group">
              <label>Max Workload <span style={{ color: 'var(--danger-color)' }}>*</span></label>
              <input
                type="number"
                min="1"
                value={form.maxWorkload}
                onChange={(e) => setForm({ ...form, maxWorkload: parseInt(e.target.value) || 1 })}
                required
              />
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

