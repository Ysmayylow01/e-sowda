const API_URL = 'http://localhost:5000/api';
let token = localStorage.getItem('adminToken');
let currentUser = null;

// Initialize app
document.addEventListener('DOMContentLoaded', () => {
    if (token) {
        checkAuth();
    } else {
        showLoginPage();
    }
    
    setupEventListeners();
});

function setupEventListeners() {
    // Login
    document.getElementById('loginForm').addEventListener('submit', handleLogin);
    
    // Logout
    document.getElementById('logoutBtn').addEventListener('click', handleLogout);
    
    // Navigation
    document.querySelectorAll('.nav-item').forEach(item => {
        item.addEventListener('click', (e) => {
            e.preventDefault();
            const page = e.currentTarget.dataset.page;
            navigateTo(page);
        });
    });
    
    // Add buttons
    document.getElementById('addProductBtn').addEventListener('click', () => openProductModal());
    document.getElementById('addCategoryBtn').addEventListener('click', () => openCategoryModal());
    
    // Forms
    document.getElementById('productForm').addEventListener('submit', handleProductSubmit);
    document.getElementById('categoryForm').addEventListener('submit', handleCategorySubmit);
    
    // Modal close buttons
    document.querySelectorAll('.close-btn').forEach(btn => {
        btn.addEventListener('click', (e) => {
            e.preventDefault();
            closeModals();
        });
    });
    
    // Close modal on background click
    document.querySelectorAll('.modal').forEach(modal => {
        modal.addEventListener('click', (e) => {
            if (e.target === modal) {
                closeModals();
            }
        });
    });
}

// Auth Functions
async function handleLogin(e) {
    e.preventDefault();
    
    const username = document.getElementById('loginUsername').value;
    const password = document.getElementById('loginPassword').value;
    
    try {
        const response = await fetch(`${API_URL}/auth/login`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ username, password })
        });
        
        const data = await response.json();
        
        if (response.ok) {
            if (data.user.is_admin) {
                token = data.token;
                localStorage.setItem('adminToken', token);
                currentUser = data.user;
                showDashboard();
                loadDashboardData();
            } else {
                showError('Bu hasap administrator hasaby däl!');
            }
        } else {
            showError(data.error || 'Giriş şowsuz boldy');
        }
    } catch (error) {
        console.error('Login error:', error);
        showError('Baglanyşyk ýalňyşlygy');
    }
}

async function checkAuth() {
    try {
        const response = await fetch(`${API_URL}/auth/me`, {
            headers: { 'Authorization': `Bearer ${token}` }
        });
        
        if (response.ok) {
            const user = await response.json();
            if (user.is_admin) {
                currentUser = user;
                showDashboard();
                loadDashboardData();
            } else {
                handleLogout();
            }
        } else {
            handleLogout();
        }
    } catch (error) {
        console.error('Auth check error:', error);
        handleLogout();
    }
}

function handleLogout() {
    token = null;
    currentUser = null;
    localStorage.removeItem('adminToken');
    showLoginPage();
}

function showLoginPage() {
    document.getElementById('loginPage').style.display = 'flex';
    document.getElementById('dashboard').style.display = 'none';
}

function showDashboard() {
    document.getElementById('loginPage').style.display = 'none';
    document.getElementById('dashboard').style.display = 'flex';
    document.getElementById('adminName').textContent = currentUser.username;
}

function showError(message) {
    const errorEl = document.getElementById('loginError');
    errorEl.textContent = message;
    errorEl.classList.add('show');
    setTimeout(() => errorEl.classList.remove('show'), 3000);
}

function showSuccess(message) {
    const successEl = document.createElement('div');
    successEl.className = 'success-message';
    successEl.textContent = message;
    document.body.appendChild(successEl);
    setTimeout(() => successEl.remove(), 3000);
}

// Navigation
function navigateTo(page) {
    // Update active nav item
    document.querySelectorAll('.nav-item').forEach(item => {
        item.classList.remove('active');
        if (item.dataset.page === page) {
            item.classList.add('active');
        }
    });
    
    // Hide all pages
    document.querySelectorAll('.content-page').forEach(p => {
        p.style.display = 'none';
    });
    
    // Show selected page
    const pageMap = {
        'overview': 'overviewPage',
        'products': 'productsPage',
        'categories': 'categoriesPage',
        'orders': 'ordersPage',
        'users': 'usersPage'
    };
    
    const pageId = pageMap[page];
    if (pageId) {
        document.getElementById(pageId).style.display = 'block';
        document.getElementById('pageTitle').textContent = getTitleForPage(page);
        loadPageData(page);
    }
}

function getTitleForPage(page) {
    const titles = {
        'overview': 'Umumy maglumat',
        'products': 'Harytlar',
        'categories': 'Kategoriýalar',
        'orders': 'Sargytlar',
        'users': 'Ulanyjylar'
    };
    return titles[page] || '';
}

// Data Loading Functions
async function loadDashboardData() {
    await Promise.all([
        loadOverviewData(),
        loadProducts(),
        loadCategories(),
        loadOrders(),
        loadUsers()
    ]);
}

async function loadPageData(page) {
    switch(page) {
        case 'overview':
            await loadOverviewData();
            break;
        case 'products':
            await loadProducts();
            break;
        case 'categories':
            await loadCategories();
            break;
        case 'orders':
            await loadOrders();
            break;
        case 'users':
            await loadUsers();
            break;
    }
}

async function loadOverviewData() {
    try {
        const response = await fetch(`${API_URL}/analytics/dashboard`, {
            headers: { 'Authorization': `Bearer ${token}` }
        });
        
        if (response.ok) {
            const data = await response.json();
            
            document.getElementById('totalUsers').textContent = data.total_users;
            document.getElementById('totalProducts').textContent = data.total_products;
            document.getElementById('totalOrders').textContent = data.total_orders;
            document.getElementById('totalRevenue').textContent = `${data.total_revenue.toFixed(2)} TMT`;
            
            // Top products
            const topProductsList = document.getElementById('topProductsList');
            topProductsList.innerHTML = data.top_products.map(product => `
                <div class="list-item">
                    <div class="list-item-info">
                        <h4>${product.name}</h4>
                        <p>${product.sales} satyldy</p>
                    </div>
                    <div class="list-item-value">${product.price} TMT</div>
                </div>
            `).join('');
            
            // Recent orders
            const recentOrdersList = document.getElementById('recentOrdersList');
            recentOrdersList.innerHTML = data.recent_orders.map(order => `
                <div class="list-item">
                    <div class="list-item-info">
                        <h4>Sargyt #${order.id}</h4>
                        <p>${order.user ? order.user.username : 'Näbelli'}</p>
                    </div>
                    <div class="list-item-value">${order.total_amount.toFixed(2)} TMT</div>
                </div>
            `).join('');
        }
    } catch (error) {
        console.error('Error loading overview:', error);
    }
}

async function loadProducts() {
    try {
        const response = await fetch(`${API_URL}/products?per_page=100`);
        
        if (response.ok) {
            const data = await response.json();
            const tbody = document.querySelector('#productsTable tbody');
            
            tbody.innerHTML = data.products.map(product => `
                <tr>
                    <td>${product.id}</td>
                    <td><img src="${product.image || 'https://via.placeholder.com/50'}" class="product-img" alt="${product.name}"></td>
                    <td>${product.name}</td>
                    <td>${product.category_name || '-'}</td>
                    <td>${product.discount_price || product.price} TMT</td>
                    <td>${product.stock}</td>
                    <td>
                        <button class="btn btn-info btn-sm" onclick="editProduct(${product.id})">
                            <i class="fas fa-edit"></i>
                        </button>
                        <button class="btn btn-danger btn-sm" onclick="deleteProduct(${product.id})">
                            <i class="fas fa-trash"></i>
                        </button>
                    </td>
                </tr>
            `).join('');
        }
    } catch (error) {
        console.error('Error loading products:', error);
    }
}

async function loadCategories() {
    try {
        const response = await fetch(`${API_URL}/categories`);
        
        if (response.ok) {
            const categories = await response.json();
            const grid = document.getElementById('categoriesGrid');
            
            grid.innerHTML = categories.map(category => `
                <div class="category-card">
                    ${category.image ? `<img src="${category.image}" alt="${category.name}">` : '<div style="height: 150px; background: var(--light); border-radius: 10px; margin-bottom: 15px;"></div>'}
                    <h3>${category.name_tm || category.name}</h3>
                    <p>${category.description || ''}</p>
                    <div class="category-actions">
                        <button class="btn btn-info btn-sm" onclick="editCategory(${category.id})">
                            <i class="fas fa-edit"></i> Düzet
                        </button>
                        <button class="btn btn-danger btn-sm" onclick="deleteCategory(${category.id})">
                            <i class="fas fa-trash"></i> Poz
                        </button>
                    </div>
                </div>
            `).join('');
            
            // Also update product form category dropdown
            const categorySelect = document.getElementById('productCategory');
            categorySelect.innerHTML = '<option value="">Kategoriýa saýlaň</option>' +
                categories.map(cat => `<option value="${cat.id}">${cat.name}</option>`).join('');
        }
    } catch (error) {
        console.error('Error loading categories:', error);
    }
}

async function loadOrders() {
    try {
        const response = await fetch(`${API_URL}/orders`, {
            headers: { 'Authorization': `Bearer ${token}` }
        });
        
        if (response.ok) {
            const orders = await response.json();
            const tbody = document.querySelector('#ordersTable tbody');
            
            tbody.innerHTML = orders.map(order => `
                <tr>
                    <td>${order.id}</td>
                    <td>${order.user ? order.user.username : 'Näbelli'}</td>
                    <td>${order.total_amount.toFixed(2)} TMT</td>
                    <td><span class="status-badge status-${order.status}">${getStatusText(order.status)}</span></td>
                    <td>${new Date(order.created_at).toLocaleDateString('tk-TM')}</td>
                    <td>
                        <button class="btn btn-info btn-sm" onclick="viewOrder(${order.id})">
                            <i class="fas fa-eye"></i>
                        </button>
                        <select class="btn btn-sm" onchange="updateOrderStatus(${order.id}, this.value)">
                            <option value="">Ýagdaý</option>
                            <option value="pending" ${order.status === 'pending' ? 'selected' : ''}>Garaşylýar</option>
                            <option value="processing" ${order.status === 'processing' ? 'selected' : ''}>Işlenýär</option>
                            <option value="shipped" ${order.status === 'shipped' ? 'selected' : ''}>Ugradyldy</option>
                            <option value="delivered" ${order.status === 'delivered' ? 'selected' : ''}>Gowşuryldy</option>
                            <option value="cancelled" ${order.status === 'cancelled' ? 'selected' : ''}>Ýatyryldy</option>
                        </select>
                    </td>
                </tr>
            `).join('');
        }
    } catch (error) {
        console.error('Error loading orders:', error);
    }
}

async function loadUsers() {
    try {
        const response = await fetch(`${API_URL}/users`, {
            headers: { 'Authorization': `Bearer ${token}` }
        });
        
        if (response.ok) {
            const users = await response.json();
            const tbody = document.querySelector('#usersTable tbody');
            
            tbody.innerHTML = users.map(user => `
                <tr>
                    <td>${user.id}</td>
                    <td>${user.username}</td>
                    <td>${user.email}</td>
                    <td>${user.full_name || '-'}</td>
                    <td>${user.phone || '-'}</td>
                    <td>${new Date(user.created_at).toLocaleDateString('tk-TM')}</td>
                </tr>
            `).join('');
        }
    } catch (error) {
        console.error('Error loading users:', error);
    }
}

function getStatusText(status) {
    const statusMap = {
        'pending': 'Garaşylýar',
        'processing': 'Işlenýär',
        'shipped': 'Ugradyldy',
        'delivered': 'Gowşuryldy',
        'cancelled': 'Ýatyryldy'
    };
    return statusMap[status] || status;
}

// Product Functions
function openProductModal(productId = null) {
    const modal = document.getElementById('productModal');
    const title = document.getElementById('productModalTitle');
    
    if (productId) {
        title.textContent = 'Harydy düzet';
        loadProductData(productId);
    } else {
        title.textContent = 'Täze haryt';
        document.getElementById('productForm').reset();
        document.getElementById('productId').value = '';
    }
    
    modal.classList.add('active');
}

async function loadProductData(productId) {
    try {
        const response = await fetch(`${API_URL}/products/${productId}`);
        
        if (response.ok) {
            const product = await response.json();
            
            document.getElementById('productId').value = product.id;
            document.getElementById('productName').value = product.name;
            document.getElementById('productNameTm').value = product.name_tm || '';
            document.getElementById('productDescription').value = product.description || '';
            document.getElementById('productPrice').value = product.price;
            document.getElementById('productDiscountPrice').value = product.discount_price || '';
            document.getElementById('productStock').value = product.stock;
            document.getElementById('productCategory').value = product.category_id;
            document.getElementById('productBrand').value = product.brand || '';
            document.getElementById('productImage').value = product.image || '';
            document.getElementById('productFeatured').checked = product.featured;
        }
    } catch (error) {
        console.error('Error loading product:', error);
    }
}

async function handleProductSubmit(e) {
    e.preventDefault();
    
    const productId = document.getElementById('productId').value;
    const productData = {
        name: document.getElementById('productName').value,
        name_tm: document.getElementById('productNameTm').value || '',
        description: document.getElementById('productDescription').value || '',
        price: parseFloat(document.getElementById('productPrice').value),
        discount_price: document.getElementById('productDiscountPrice').value ? 
            parseFloat(document.getElementById('productDiscountPrice').value) : null,
        stock: parseInt(document.getElementById('productStock').value),
        category_id: parseInt(document.getElementById('productCategory').value),
        brand: document.getElementById('productBrand').value || '',
        image: document.getElementById('productImage').value || '',
        featured: document.getElementById('productFeatured').checked
    };
    
    // Validate required fields
    if (!productData.name || productData.name.trim() === '') {
        alert('Haryt ady girizilmeli!');
        return;
    }
    if (isNaN(productData.price) || productData.price <= 0) {
        alert('Dogry baha girizilmeli!');
        return;
    }
    if (isNaN(productData.stock) || productData.stock < 0) {
        alert('Dogry mukdar girizilmeli!');
        return;
    }
    if (isNaN(productData.category_id)) {
        alert('Kategoriýa saýlanyň!');
        return;
    }
    
    try {
        const url = productId ? `${API_URL}/products/${productId}` : `${API_URL}/products`;
        const method = productId ? 'PUT' : 'POST';
        
        console.log('Sending request:', method, url, productData);
        
        const response = await fetch(url, {
            method,
            headers: {
                'Content-Type': 'application/json',
                'Authorization': `Bearer ${token}`
            },
            body: JSON.stringify(productData)
        });
        
        console.log('Response status:', response.status);
        
        if (response.ok) {
            showSuccess(productId ? 'Haryt täzelendi' : 'Haryt goşuldy');
            closeModals();
            loadProducts();
        } else {
            const data = await response.json();
            console.error('Error response:', data);
            alert(data.error || `Ýalňyşlyk ýüze çykdy (${response.status})`);
        }
    } catch (error) {
        console.error('Error saving product:', error);
        alert('Baglanyşyk ýalňyşlygy: ' + error.message);
    }
}

async function editProduct(productId) {
    openProductModal(productId);
}

async function deleteProduct(productId) {
    if (!confirm('Bu harydy pozmak isleýärsiňizmi?')) return;
    
    try {
        const response = await fetch(`${API_URL}/products/${productId}`, {
            method: 'DELETE',
            headers: { 'Authorization': `Bearer ${token}` }
        });
        
        if (response.ok) {
            showSuccess('Haryt pozuldy');
            loadProducts();
        } else {
            const data = await response.json();
            alert(data.error || 'Ýalňyşlyk ýüze çykdy');
        }
    } catch (error) {
        console.error('Error deleting product:', error);
    }
}

// Category Functions
function openCategoryModal(categoryId = null) {
    const modal = document.getElementById('categoryModal');
    const title = document.getElementById('categoryModalTitle');
    
    if (categoryId) {
        title.textContent = 'Kategoriýany düzet';
        loadCategoryData(categoryId);
    } else {
        title.textContent = 'Täze kategoriýa';
        document.getElementById('categoryForm').reset();
        document.getElementById('categoryId').value = '';
    }
    
    modal.classList.add('active');
}

async function loadCategoryData(categoryId) {
    try {
        const response = await fetch(`${API_URL}/categories/${categoryId}`);
        
        if (response.ok) {
            const category = await response.json();
            
            document.getElementById('categoryId').value = category.id;
            document.getElementById('categoryName').value = category.name;
            document.getElementById('categoryNameTm').value = category.name_tm || '';
            document.getElementById('categoryDescription').value = category.description || '';
            document.getElementById('categoryImage').value = category.image || '';
        }
    } catch (error) {
        console.error('Error loading category:', error);
    }
}

async function handleCategorySubmit(e) {
    e.preventDefault();
    
    const categoryId = document.getElementById('categoryId').value;
    const categoryData = {
        name: document.getElementById('categoryName').value,
        name_tm: document.getElementById('categoryNameTm').value || '',
        description: document.getElementById('categoryDescription').value || '',
        image: document.getElementById('categoryImage').value || ''
    };
    
    // Validate required fields
    if (!categoryData.name || categoryData.name.trim() === '') {
        alert('Kategoriýa ady girizilmeli!');
        return;
    }
    
    try {
        const url = categoryId ? `${API_URL}/categories/${categoryId}` : `${API_URL}/categories`;
        const method = categoryId ? 'PUT' : 'POST';
        
        console.log('Sending request:', method, url, categoryData);
        
        const response = await fetch(url, {
            method,
            headers: {
                'Content-Type': 'application/json',
                'Authorization': `Bearer ${token}`
            },
            body: JSON.stringify(categoryData)
        });
        
        console.log('Response status:', response.status);
        
        if (response.ok) {
            showSuccess(categoryId ? 'Kategoriýa täzelendi' : 'Kategoriýa goşuldy');
            closeModals();
            loadCategories();
        } else {
            const data = await response.json();
            console.error('Error response:', data);
            alert(data.error || `Ýalňyşlyk ýüze çykdy (${response.status})`);
        }
    } catch (error) {
        console.error('Error saving category:', error);
        alert('Baglanyşyk ýalňyşlygy: ' + error.message);
    }
}

async function editCategory(categoryId) {
    openCategoryModal(categoryId);
}

async function deleteCategory(categoryId) {
    if (!confirm('Bu kategoriýany pozmak isleýärsiňizmi?')) return;
    
    try {
        const response = await fetch(`${API_URL}/categories/${categoryId}`, {
            method: 'DELETE',
            headers: { 'Authorization': `Bearer ${token}` }
        });
        
        if (response.ok) {
            showSuccess('Kategoriýa pozuldy');
            loadCategories();
        } else {
            const data = await response.json();
            alert(data.error || 'Ýalňyşlyk ýüze çykdy');
        }
    } catch (error) {
        console.error('Error deleting category:', error);
    }
}

// Order Functions
async function viewOrder(orderId) {
    try {
        const response = await fetch(`${API_URL}/orders/${orderId}`, {
            headers: { 'Authorization': `Bearer ${token}` }
        });
        
        if (response.ok) {
            const order = await response.json();
            const modal = document.getElementById('orderModal');
            const detailsDiv = document.getElementById('orderDetails');
            
            detailsDiv.innerHTML = `
                <div style="padding: 25px;">
                    <h3>Sargyt #${order.id}</h3>
                    <p><strong>Ulanyjy:</strong> ${order.user ? order.user.username : 'Näbelli'}</p>
                    <p><strong>Salgy:</strong> ${order.shipping_address}</p>
                    <p><strong>Telefon:</strong> ${order.phone || '-'}</p>
                    <p><strong>Ýagdaý:</strong> <span class="status-badge status-${order.status}">${getStatusText(order.status)}</span></p>
                    <p><strong>Sene:</strong> ${new Date(order.created_at).toLocaleString('tk-TM')}</p>
                    
                    <h4 style="margin-top: 20px;">Harytlar:</h4>
                    <table style="width: 100%; margin-top: 10px;">
                        <thead>
                            <tr>
                                <th>Haryt</th>
                                <th>Mukdar</th>
                                <th>Baha</th>
                                <th>Jemi</th>
                            </tr>
                        </thead>
                        <tbody>
                            ${order.items.map(item => `
                                <tr>
                                    <td>${item.product ? item.product.name : 'Näbelli'}</td>
                                    <td>${item.quantity}</td>
                                    <td>${item.price} TMT</td>
                                    <td>${(item.price * item.quantity).toFixed(2)} TMT</td>
                                </tr>
                            `).join('')}
                        </tbody>
                    </table>
                    
                    <div style="margin-top: 20px; text-align: right;">
                        <h3>Jemi: ${order.total_amount.toFixed(2)} TMT</h3>
                    </div>
                </div>
            `;
            
            modal.classList.add('active');
        }
    } catch (error) {
        console.error('Error loading order:', error);
    }
}

async function updateOrderStatus(orderId, status) {
    if (!status) return;
    
    try {
        const response = await fetch(`${API_URL}/orders/${orderId}/status`, {
            method: 'PUT',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': `Bearer ${token}`
            },
            body: JSON.stringify({ status })
        });
        
        if (response.ok) {
            showSuccess('Sargyt ýagdaýy täzelendi');
            loadOrders();
        } else {
            const data = await response.json();
            alert(data.error || 'Ýalňyşlyk ýüze çykdy');
        }
    } catch (error) {
        console.error('Error updating order status:', error);
    }
}

// Modal Functions
function closeModals() {
    document.querySelectorAll('.modal').forEach(modal => {
        modal.classList.remove('active');
    });
}
