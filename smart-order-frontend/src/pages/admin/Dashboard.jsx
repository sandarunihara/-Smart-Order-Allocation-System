import { useState, useEffect } from 'react';
import { adminAPI } from '../../api';
import { FiPackage, FiTruck, FiDollarSign, FiClock, FiCheckCircle, FiXCircle } from 'react-icons/fi';

export default function Dashboard() {
  const [stats, setStats] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    adminAPI.getDashboard().then((res) => setStats(res.data)).finally(() => setLoading(false));
  }, []);

  if (loading) return <div className="loading"><div className="spinner" />Loading dashboard...</div>;
  if (!stats) return null;

  return (
    <>
      <div className="page-header">
        <h2>Dashboard</h2>
        <p>Overview of your order management system</p>
      </div>

      <div className="stats-grid">
        <div className="stat-card">
          <div className="stat-icon primary"><FiPackage /></div>
          <div className="stat-label">Total Orders</div>
          <div className="stat-value">{stats.totalOrders}</div>
        </div>
        <div className="stat-card">
          <div className="stat-icon warning"><FiClock /></div>
          <div className="stat-label">Allocated</div>
          <div className="stat-value">{stats.allocatedOrders}</div>
        </div>
        <div className="stat-card">
          <div className="stat-icon info"><FiTruck /></div>
          <div className="stat-label">Preparing</div>
          <div className="stat-value">{stats.preparingOrders}</div>
        </div>
        <div className="stat-card">
          <div className="stat-icon success"><FiCheckCircle /></div>
          <div className="stat-label">Delivered</div>
          <div className="stat-value">{stats.deliveredOrders}</div>
        </div>
        <div className="stat-card">
          <div className="stat-icon danger"><FiXCircle /></div>
          <div className="stat-label">Cancelled</div>
          <div className="stat-value">{stats.cancelledOrders}</div>
        </div>
        <div className="stat-card">
          <div className="stat-icon success"><FiDollarSign /></div>
          <div className="stat-label">Total Revenue</div>
          <div className="stat-value" style={{ fontSize: 22 }}>
            Rs. {parseFloat(stats.totalRevenue || 0).toLocaleString()}
          </div>
        </div>
      </div>

      <div className="card">
        <h3 style={{ marginBottom: 16 }}>Branch Performance</h3>
        <div className="table-container">
          <table>
            <thead>
              <tr>
                <th>Branch</th>
                <th>Active Orders</th>
                <th>Workload</th>
                <th>Capacity</th>
                <th>Status</th>
              </tr>
            </thead>
            <tbody>
              {stats.branchStats?.map((b) => {
                const pct = b.maxWorkload > 0 ? Math.round((b.currentWorkload / b.maxWorkload) * 100) : 0;
                return (
                  <tr key={b.branchId}>
                    <td style={{ fontWeight: 600 }}>{b.branchName}</td>
                    <td>{b.activeOrders}</td>
                    <td>{b.currentWorkload} / {b.maxWorkload}</td>
                    <td>
                      <div style={{ background: 'var(--border-light)', borderRadius: 4, height: 8, width: 100 }}>
                        <div style={{
                          background: pct > 80 ? 'var(--danger)' : pct > 50 ? 'var(--warning)' : 'var(--success)',
                          borderRadius: 4, height: 8, width: `${Math.min(pct, 100)}%`,
                        }} />
                      </div>
                    </td>
                    <td>
                      <span className={`badge ${b.isActive ? 'badge-delivered' : 'badge-cancelled'}`}>
                        {b.isActive ? 'Active' : 'Inactive'}
                      </span>
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        </div>
      </div>
    </>
  );
}
