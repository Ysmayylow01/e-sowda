// ==================== CONFIG ====================
// FIX: API_URL relative path - HTTPS/HTTP garysyk meselesini cozyer
// Eger backend ayry serverde bolsa: 'http://85.198.97.249:5000/api' gayrat et
const API_URL = window.location.origin.includes('localhost') || window.location.origin.includes('127.0.0.1')
    ? 'http://localhost:5000/api'
    : '/api';

let token = localStorage.getItem('adminToken');
let currentUser = null;

// ==================== XSS GORAGY ====================
// FIX: innerHTML-de gon ulanylmaz, bu funksiyon HTML char-lary escape edyar
function escHtml(str) {
    if (str == null) return '';
    return String(str)
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;')
        .replace(/'/g, '&#x27;');
}

// ==================== INIT ====================
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

    // Modal close buttons — FIX: type="button" HTML-de gosulan, JS hem dogry isleyar
    document.querySelectorAll('.close-btn').forEach(btn => {
        btn.addEventListener('click', (e) => {
            e.preventDefault();
            e.stopPropagation();
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

    // Escape key bilen modal yapmak
    document.addEventListener('keydown', (e) => {
        if (e.key === 'Escape') {
            closeModals();
        }
    });
}

// ==================== AUTH ====================

async function handleLogin(e) {
    e.preventDefault();

    const username = document.getElementById('loginUsername').value.trim();
    const password = document.getElementById('loginPassword').value;

    if (!username || !password) {
        showError('Ulanyjy ady we paroly doluduryň!');
        return;
    }

    const submitBtn = e.target.querySelector('button[type="submit"]');
    submitBtn.disabled = true;
    submitBtn.innerHTML = '<i class="fas fa-spinner fa-spin"></i> Girýär...';

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
        showError('Baglanyşyk ýalňyşlygy. Server işleýändigini barlaň.');
    } finally {
        submitBtn.disabled = false;
        submitBtn.innerHTML = '<i class="fas fa-sign-in-alt"></i> Girmek';
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
    document.getElementById('loginError').classList.remove('show');
}

function showDashboard() {
    document.getElementById('loginPage').style.display = 'none';
    document.getElementById('dashboard').style.display = 'flex';
    document.getElementById('adminName').textContent = escHtml(currentUser.username);
}

function showError(message) {
    const errorEl = document.getElementById('loginError');
    errorEl.textContent = message;
    errorEl.classList.add('show');
    setTimeout(() => errorEl.classList.remove('show'), 4000);
}

function showSuccess(message) {
    // Onki success-leri poz
    document.querySelectorAll('.success-message').forEach(el => el.remove());

    const successEl = document.createElement('div');
    successEl.className = 'success-message';
    successEl.textContent = message;
    document.body.appendChild(successEl);

    setTimeout(() => {
        successEl.style.opacity = '0';
        setTimeout(() => successEl.remove(), 300);
    }, 2700);
}

// ==================== NAVIGATION ====================

function navigateTo(page) {
    document.querySelectorAll('.nav-item').forEach(item => {
        item.classList.remove('active');
        if (item.dataset.page === page) {
            item.classList.add('active');
        }
    });

    document.querySelectorAll('.content-page').forEach(p => {
        p.style.display = 'none';
    });

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

// ==================== DATA LOADING ====================

async function loadDashboardData() {
    // FIX: overview ilki yuklemeli, galanlary yzyna
    await loadOverviewData();
    loadProducts();
    loadCategories();
    loadOrders();
    loadUsers();
}

async function loadPageData(page) {
    switch (page) {
        case 'overview':   await loadOverviewData(); break;
        case 'products':   await loadProducts();     break;
        case 'categories': await loadCategories();   break;
        case 'orders':     await loadOrders();       break;
        case 'users':      await loadUsers();        break;
    }
}

async function loadOverviewData() {
    try {
        const response = await fetch(`${API_URL}/analytics/dashboard`, {
            headers: { 'Authorization': `Bearer ${token}` }
        });

        if (response.ok) {
            const data = await response.json();

            document.getElementById('totalUsers').textContent    = data.total_users    ?? 0;
            document.getElementById('totalProducts').textContent = data.total_products ?? 0;
            document.getElementById('totalOrders').textContent   = data.total_orders   ?? 0;
            document.getElementById('totalRevenue').textContent  = `${(data.total_revenue || 0).toFixed(2)} TMT`;

            // Top products — FIX: escHtml bilen XSS goragy
            const topProductsList = document.getElementById('topProductsList');
            if (data.top_products && data.top_products.length) {
                topProductsList.innerHTML = data.top_products.map(product => `
                    <div class="list-item">
                        <div class="list-item-info">
                            <h4>${escHtml(product.name)}</h4>
                            <p>${escHtml(String(product.sales ?? 0))} satyldy</p>
                        </div>
                        <div class="list-item-value">${escHtml(String(product.price ?? 0))} TMT</div>
                    </div>
                `).join('');
            } else {
                topProductsList.innerHTML = '<p class="empty-text">Haryt ýok</p>';
            }

            // Recent orders — FIX: escHtml bilen XSS goragy
            const recentOrdersList = document.getElementById('recentOrdersList');
            if (data.recent_orders && data.recent_orders.length) {
                recentOrdersList.innerHTML = data.recent_orders.map(order => `
                    <div class="list-item">
                        <div class="list-item-info">
                            <h4>Sargyt #${escHtml(String(order.id))}</h4>
                            <p>${escHtml(order.user ? order.user.username : 'Näbelli')}</p>
                        </div>
                        <div class="list-item-value">${(order.total_amount || 0).toFixed(2)} TMT</div>
                    </div>
                `).join('');
            } else {
                recentOrdersList.innerHTML = '<p class="empty-text">Sargyt ýok</p>';
            }
        } else if (response.status === 401) {
            handleLogout();
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

            if (data.products && data.products.length) {
                tbody.innerHTML = data.products.map(product => `
                    <tr>
                        <td>${escHtml(String(product.id))}</td>
                        <td>
                            <img src="${escHtml(product.image || 'https://via.placeholder.com/50')}"
                                 class="product-img"
                                 alt="${escHtml(product.name)}"
                                 onerror="this.src='https://via.placeholder.com/50'">
                        </td>
                        <td>${escHtml(product.name)}</td>
                        <td>${escHtml(product.category_name || '-')}</td>
                        <td>${escHtml(String(product.discount_price || product.price))} TMT</td>
                        <td>${escHtml(String(product.stock))}</td>
                        <td>
                            <button type="button" class="btn btn-info btn-sm" onclick="editProduct(${product.id})" title="Düzet">
                                <i class="fas fa-edit"></i>
                            </button>
                            <button type="button" class="btn btn-danger btn-sm" onclick="deleteProduct(${product.id})" title="Poz">
                                <i class="fas fa-trash"></i>
                            </button>
                        </td>
                    </tr>
                `).join('');
            } else {
                tbody.innerHTML = '<tr><td colspan="7" style="text-align:center;padding:20px;">Haryt ýok</td></tr>';
            }
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

            if (categories.length) {
                grid.innerHTML = categories.map(category => `
                    <div class="category-card">
                        ${category.image
                            ? `<img src="${escHtml(category.image)}" alt="${escHtml(category.name_tm || category.name)}" onerror="this.style.display='none'">`
                            : '<div style="height:150px;background:var(--light);border-radius:10px;margin-bottom:15px;"></div>'
                        }
                        <h3>${escHtml(category.name_tm || category.name)}</h3>
                        <p>${escHtml(category.description || '')}</p>
                        <div class="category-actions">
                            <button type="button" class="btn btn-info btn-sm" onclick="editCategory(${category.id})">
                                <i class="fas fa-edit"></i> Düzet
                            </button>
                            <button type="button" class="btn btn-danger btn-sm" onclick="deleteCategory(${category.id})">
                                <i class="fas fa-trash"></i> Poz
                            </button>
                        </div>
                    </div>
                `).join('');
            } else {
                grid.innerHTML = '<p class="empty-text">Kategoriýa ýok</p>';
            }

            // Product form category dropdown update
            const categorySelect = document.getElementById('productCategory');
            categorySelect.innerHTML = '<option value="">Kategoriýa saýlaň</option>' +
                categories.map(cat => `<option value="${cat.id}">${escHtml(cat.name)}</option>`).join('');
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

            if (orders.length) {
                tbody.innerHTML = orders.map(order => `
                    <tr>
                        <td>${escHtml(String(order.id))}</td>
                        <td>${escHtml(order.user ? order.user.username : 'Näbelli')}</td>
                        <td>${(order.total_amount || 0).toFixed(2)} TMT</td>
                        <td><span class="status-badge status-${escHtml(order.status)}">${escHtml(getStatusText(order.status))}</span></td>
                        <td>${new Date(order.created_at).toLocaleDateString('tk-TM')}</td>
                        <td>
                            <button type="button" class="btn btn-info btn-sm" onclick="viewOrder(${order.id})" title="Görkez">
                                <i class="fas fa-eye"></i>
                            </button>
                            <select class="btn btn-sm status-select" onchange="updateOrderStatus(${order.id}, this.value)" title="Ýagdaý üýtget">
                                <option value="">Ýagdaý</option>
                                <option value="pending"    ${order.status === 'pending'    ? 'selected' : ''}>Garaşylýar</option>
                                <option value="processing" ${order.status === 'processing' ? 'selected' : ''}>Işlenýär</option>
                                <option value="shipped"    ${order.status === 'shipped'    ? 'selected' : ''}>Ugradyldy</option>
                                <option value="delivered"  ${order.status === 'delivered'  ? 'selected' : ''}>Gowşuryldy</option>
                                <option value="cancelled"  ${order.status === 'cancelled'  ? 'selected' : ''}>Ýatyryldy</option>
                            </select>
                        </td>
                    </tr>
                `).join('');
            } else {
                tbody.innerHTML = '<tr><td colspan="6" style="text-align:center;padding:20px;">Sargyt ýok</td></tr>';
            }
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

            if (users.length) {
                tbody.innerHTML = users.map(user => `
                    <tr>
                        <td>${escHtml(String(user.id))}</td>
                        <td>${escHtml(user.username)}</td>
                        <td>${escHtml(user.email)}</td>
                        <td>${escHtml(user.full_name || '-')}</td>
                        <td>${escHtml(user.phone || '-')}</td>
                        <td>${new Date(user.created_at).toLocaleDateString('tk-TM')}</td>
                    </tr>
                `).join('');
            } else {
                tbody.innerHTML = '<tr><td colspan="6" style="text-align:center;padding:20px;">Ulanyjy ýok</td></tr>';
            }
        }
    } catch (error) {
        console.error('Error loading users:', error);
    }
}

function getStatusText(status) {
    const statusMap = {
        'pending':    'Garaşylýar',
        'processing': 'Işlenýär',
        'shipped':    'Ugradyldy',
        'delivered':  'Gowşuryldy',
        'cancelled':  'Ýatyryldy'
    };
    return statusMap[status] || status;
}

// ==================== PRODUCT CRUD ====================

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

            document.getElementById('productId').value           = product.id;
            document.getElementById('productName').value         = product.name          || '';
            document.getElementById('productNameTm').value       = product.name_tm       || '';
            document.getElementById('productDescription').value  = product.description   || '';
            document.getElementById('productPrice').value        = product.price         || '';
            document.getElementById('productDiscountPrice').value = product.discount_price || '';
            document.getElementById('productStock').value        = product.stock         ?? 0;
            document.getElementById('productCategory').value     = product.category_id   || '';
            document.getElementById('productBrand').value        = product.brand         || '';
            document.getElementById('productImage').value        = product.image         || '';
            document.getElementById('productFeatured').checked   = !!product.featured;
        }
    } catch (error) {
        console.error('Error loading product:', error);
        alert('Haryt maglumatlaryny ýüklemekde ýalňyşlyk');
    }
}

async function handleProductSubmit(e) {
    e.preventDefault();

    const productId = document.getElementById('productId').value;

    const name        = document.getElementById('productName').value.trim();
    const price       = parseFloat(document.getElementById('productPrice').value);
    const stock       = parseInt(document.getElementById('productStock').value);
    const category_id = parseInt(document.getElementById('productCategory').value);

    // Client-side validation
    if (!name) {
        alert('Haryt ady girizilmeli!');
        return;
    }
    if (isNaN(price) || price <= 0) {
        alert('Dogry baha girizilmeli!');
        return;
    }
    if (isNaN(stock) || stock < 0) {
        alert('Dogry mukdar girizilmeli!');
        return;
    }
    if (isNaN(category_id)) {
        alert('Kategoriýa saýlanyň!');
        return;
    }

    const discountVal = document.getElementById('productDiscountPrice').value;

    const productData = {
        name,
        name_tm:        document.getElementById('productNameTm').value.trim()    || '',
        description:    document.getElementById('productDescription').value.trim() || '',
        price,
        discount_price: discountVal ? parseFloat(discountVal) : null,
        stock,
        category_id,
        brand:          document.getElementById('productBrand').value.trim() || '',
        image:          document.getElementById('productImage').value.trim() || '',
        featured:       document.getElementById('productFeatured').checked
    };

    const submitBtn = document.querySelector('#productForm button[type="submit"]');
    submitBtn.disabled = true;
    submitBtn.textContent = 'Ýatda saklanýar...';

    try {
        const url    = productId ? `${API_URL}/products/${productId}` : `${API_URL}/products`;
        const method = productId ? 'PUT' : 'POST';

        const response = await fetch(url, {
            method,
            headers: {
                'Content-Type': 'application/json',
                'Authorization': `Bearer ${token}`
            },
            body: JSON.stringify(productData)
        });

        if (response.ok) {
            showSuccess(productId ? 'Haryt täzelendi' : 'Haryt goşuldy');
            closeModals();
            loadProducts();
        } else {
            const data = await response.json();
            alert(data.error || `Ýalňyşlyk ýüze çykdy (${response.status})`);
        }
    } catch (error) {
        console.error('Error saving product:', error);
        alert('Baglanyşyk ýalňyşlygy: ' + error.message);
    } finally {
        submitBtn.disabled = false;
        submitBtn.textContent = 'Ýatda sakla';
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
        alert('Baglanyşyk ýalňyşlygy');
    }
}

// ==================== CATEGORY CRUD ====================

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

            document.getElementById('categoryId').value          = category.id;
            document.getElementById('categoryName').value        = category.name        || '';
            document.getElementById('categoryNameTm').value      = category.name_tm     || '';
            document.getElementById('categoryDescription').value = category.description || '';
            document.getElementById('categoryImage').value       = category.image       || '';
        }
    } catch (error) {
        console.error('Error loading category:', error);
        alert('Kategoriýa maglumatlaryny ýüklemekde ýalňyşlyk');
    }
}

async function handleCategorySubmit(e) {
    e.preventDefault();

    const categoryId = document.getElementById('categoryId').value;
    const name = document.getElementById('categoryName').value.trim();

    if (!name) {
        alert('Kategoriýa ady girizilmeli!');
        return;
    }

    const categoryData = {
        name,
        name_tm:     document.getElementById('categoryNameTm').value.trim()      || '',
        description: document.getElementById('categoryDescription').value.trim() || '',
        image:       document.getElementById('categoryImage').value.trim()       || ''
    };

    const submitBtn = document.querySelector('#categoryForm button[type="submit"]');
    submitBtn.disabled = true;
    submitBtn.textContent = 'Ýatda saklanýar...';

    try {
        const url    = categoryId ? `${API_URL}/categories/${categoryId}` : `${API_URL}/categories`;
        const method = categoryId ? 'PUT' : 'POST';

        const response = await fetch(url, {
            method,
            headers: {
                'Content-Type': 'application/json',
                'Authorization': `Bearer ${token}`
            },
            body: JSON.stringify(categoryData)
        });

        if (response.ok) {
            showSuccess(categoryId ? 'Kategoriýa täzelendi' : 'Kategoriýa goşuldy');
            closeModals();
            loadCategories();
        } else {
            const data = await response.json();
            alert(data.error || `Ýalňyşlyk ýüze çykdy (${response.status})`);
        }
    } catch (error) {
        console.error('Error saving category:', error);
        alert('Baglanyşyk ýalňyşlygy: ' + error.message);
    } finally {
        submitBtn.disabled = false;
        submitBtn.textContent = 'Ýatda sakla';
    }
}

function editCategory(categoryId) {
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
        alert('Baglanyşyk ýalňyşlygy');
    }
}

// ==================== ORDER FUNCTIONS ====================

async function viewOrder(orderId) {
    try {
        const response = await fetch(`${API_URL}/orders/${orderId}`, {
            headers: { 'Authorization': `Bearer ${token}` }
        });

        if (response.ok) {
            const order = await response.json();
            const modal      = document.getElementById('orderModal');
            const detailsDiv = document.getElementById('orderDetails');

            const itemsHtml = (order.items || []).map(item => `
                <tr>
                    <td>${escHtml(item.product ? item.product.name : 'Näbelli')}</td>
                    <td>${escHtml(String(item.quantity))}</td>
                    <td>${escHtml(String(item.price))} TMT</td>
                    <td>${((item.price || 0) * (item.quantity || 0)).toFixed(2)} TMT</td>
                </tr>
            `).join('');

            detailsDiv.innerHTML = `
                <div style="padding:25px;">
                    <h3>Sargyt #${escHtml(String(order.id))}</h3>
                    <p><strong>Ulanyjy:</strong> ${escHtml(order.user ? order.user.username : 'Näbelli')}</p>
                    <p><strong>Salgy:</strong> ${escHtml(order.shipping_address || '-')}</p>
                    <p><strong>Telefon:</strong> ${escHtml(order.phone || '-')}</p>
                    <p><strong>Ýagdaý:</strong>
                        <span class="status-badge status-${escHtml(order.status)}">${escHtml(getStatusText(order.status))}</span>
                    </p>
                    <p><strong>Sene:</strong> ${new Date(order.created_at).toLocaleString('tk-TM')}</p>

                    <h4 style="margin-top:20px;">Harytlar:</h4>
                    <table style="width:100%;margin-top:10px;border-collapse:collapse;">
                        <thead>
                            <tr>
                                <th style="text-align:left;padding:8px;border-bottom:1px solid #ddd;">Haryt</th>
                                <th style="text-align:left;padding:8px;border-bottom:1px solid #ddd;">Mukdar</th>
                                <th style="text-align:left;padding:8px;border-bottom:1px solid #ddd;">Baha</th>
                                <th style="text-align:left;padding:8px;border-bottom:1px solid #ddd;">Jemi</th>
                            </tr>
                        </thead>
                        <tbody>${itemsHtml}</tbody>
                    </table>

                    <div style="margin-top:20px;text-align:right;">
                        <h3>Jemi: ${(order.total_amount || 0).toFixed(2)} TMT</h3>
                    </div>
                </div>
            `;

            modal.classList.add('active');
        } else {
            alert('Sargyt maglumatlaryny ýüklemekde ýalňyşlyk');
        }
    } catch (error) {
        console.error('Error loading order:', error);
        alert('Baglanyşyk ýalňyşlygy');
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
            loadOrders(); // select-i reset et
        }
    } catch (error) {
        console.error('Error updating order status:', error);
        alert('Baglanyşyk ýalňyşlygy');
    }
}

// ==================== MODAL ====================

function closeModals() {
    document.querySelectorAll('.modal').forEach(modal => {
        modal.classList.remove('active');
    });
}