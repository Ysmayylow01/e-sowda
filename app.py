from flask import jsonify, request, send_from_directory, render_template
from flask_jwt_extended import create_access_token, jwt_required, get_jwt_identity
from flask_cors import CORS
from werkzeug.utils import secure_filename
from config import app, db, bcrypt, User, Category, Product, CartItem, Order, OrderItem
import os
from datetime import datetime
import json

# ==================== CORS ====================
CORS(app, resources={r"/api/*": {"origins": "*"}})

ALLOWED_EXTENSIONS = {'png', 'jpg', 'jpeg', 'gif', 'webp'}

def allowed_file(filename):
    return '.' in filename and filename.rsplit('.', 1)[1].lower() in ALLOWED_EXTENSIONS

def sanitize_str(value):
    """Basic XSS sanitize: escape HTML special chars"""
    if not isinstance(value, str):
        return value
    return (value
        .replace('&', '&amp;')
        .replace('<', '&lt;')
        .replace('>', '&gt;')
        .replace('"', '&quot;')
        .replace("'", '&#x27;'))

# ==================== ADMIN PANEL ====================

@app.route('/admin')
def dashboard():
    return render_template('index.html')

# ==================== AUTH ROUTES ====================

@app.route('/api/auth/register', methods=['POST'])
def register():
    try:
        data = request.get_json()

        if User.query.filter_by(username=data['username']).first():
            return jsonify({'error': 'Ulanyjy ady eyyam alyndy'}), 400

        if User.query.filter_by(email=data['email']).first():
            return jsonify({'error': 'Email eyyam ulanylypdyr'}), 400

        hashed_password = bcrypt.generate_password_hash(data['password']).decode('utf-8')

        user = User(
            username=sanitize_str(data['username']),
            email=sanitize_str(data['email']),
            password=hashed_password,
            full_name=sanitize_str(data.get('full_name', '')),
            phone=data.get('phone'),
            address=data.get('address')
        )

        db.session.add(user)
        db.session.commit()

        access_token = create_access_token(identity=str(user.id))

        return jsonify({
            'message': 'Hasaba alyndy!',
            'token': access_token,
            'user': user.to_dict()
        }), 201
    except Exception as e:
        db.session.rollback()
        return jsonify({'error': str(e)}), 500

@app.route('/api/auth/login', methods=['POST'])
def login():
    try:
        data = request.get_json()

        user = User.query.filter_by(username=data['username']).first()

        if user and bcrypt.check_password_hash(user.password, data['password']):
            access_token = create_access_token(identity=str(user.id))
            return jsonify({
                'message': 'Giris ustunlikli!',
                'token': access_token,
                'user': user.to_dict()
            }), 200
        else:
            return jsonify({'error': 'Nadogry ulanyjy ady ya-da parol'}), 401
    except Exception as e:
        return jsonify({'error': str(e)}), 500

@app.route('/api/auth/me', methods=['GET'])
@jwt_required()
def get_current_user():
    try:
        user_id = int(get_jwt_identity())
        user = User.query.get(user_id)

        if not user:
            return jsonify({'error': 'Ulanyjy tapylmady'}), 404

        return jsonify(user.to_dict()), 200
    except Exception as e:
        return jsonify({'error': str(e)}), 500

# ==================== USER ROUTES ====================

@app.route('/api/users', methods=['GET'])
@jwt_required()
def get_users():
    try:
        user_id = int(get_jwt_identity())
        user = User.query.get(user_id)

        if not user or not user.is_admin:
            return jsonify({'error': 'Rugsat yok'}), 403

        users = User.query.all()
        return jsonify([u.to_dict() for u in users]), 200
    except Exception as e:
        return jsonify({'error': str(e)}), 500

@app.route('/api/users/<int:target_user_id>', methods=['PUT'])
@jwt_required()
def update_user(target_user_id):
    try:
        current_user_id = int(get_jwt_identity())
        user = User.query.get(target_user_id)

        if not user:
            return jsonify({'error': 'Ulanyjy tapylmady'}), 404

        if current_user_id != target_user_id:
            current_user = User.query.get(current_user_id)
            if not current_user or not current_user.is_admin:
                return jsonify({'error': 'Rugsat yok'}), 403

        data = request.get_json()

        if 'full_name' in data:
            user.full_name = sanitize_str(data['full_name'])
        if 'phone' in data:
            user.phone = data['phone']
        if 'address' in data:
            user.address = data['address']
        if 'email' in data:
            user.email = sanitize_str(data['email'])

        db.session.commit()

        return jsonify({
            'message': 'Maglumat ustunlikli tazelendi',
            'user': user.to_dict()
        }), 200
    except Exception as e:
        db.session.rollback()
        return jsonify({'error': str(e)}), 500

# ==================== CATEGORY ROUTES ====================

@app.route('/api/categories', methods=['GET'])
def get_categories():
    try:
        categories = Category.query.all()
        return jsonify([cat.to_dict() for cat in categories]), 200
    except Exception as e:
        return jsonify({'error': str(e)}), 500

@app.route('/api/categories/<int:category_id>', methods=['GET'])
def get_category(category_id):
    try:
        category = Category.query.get(category_id)
        if not category:
            return jsonify({'error': 'Kategoriya tapylmady'}), 404
        return jsonify(category.to_dict()), 200
    except Exception as e:
        return jsonify({'error': str(e)}), 500

@app.route('/api/categories', methods=['POST'])
@jwt_required()
def create_category():
    try:
        user_id = int(get_jwt_identity())
        user = User.query.get(user_id)

        if not user or not user.is_admin:
            return jsonify({'error': 'Rugsat yok'}), 403

        data = request.get_json()

        if not data.get('name', '').strip():
            return jsonify({'error': 'Kategoriya ady hokman girizilmeli'}), 400

        category = Category(
            name=sanitize_str(data['name']),
            name_tm=sanitize_str(data.get('name_tm', '')),
            description=sanitize_str(data.get('description', '')),
            image=data.get('image')
        )

        db.session.add(category)
        db.session.commit()

        return jsonify({
            'message': 'Kategoriya gosuldy',
            'category': category.to_dict()
        }), 201
    except Exception as e:
        db.session.rollback()
        return jsonify({'error': str(e)}), 500

@app.route('/api/categories/<int:category_id>', methods=['PUT'])
@jwt_required()
def update_category(category_id):
    try:
        user_id = int(get_jwt_identity())
        user = User.query.get(user_id)

        if not user or not user.is_admin:
            return jsonify({'error': 'Rugsat yok'}), 403

        category = Category.query.get(category_id)
        if not category:
            return jsonify({'error': 'Kategoriya tapylmady'}), 404

        data = request.get_json()

        if 'name' in data:
            if not data['name'] or not data['name'].strip():
                return jsonify({'error': 'Kategoriya ady hokman girizilmeli'}), 400
            category.name = sanitize_str(data['name'])

        if 'name_tm' in data:
            category.name_tm = sanitize_str(data['name_tm']) if data['name_tm'] else None
        if 'description' in data:
            category.description = sanitize_str(data['description']) if data['description'] else None
        if 'image' in data:
            category.image = data['image'] if data['image'] else None

        db.session.commit()

        return jsonify({
            'message': 'Kategoriya tazelendi',
            'category': category.to_dict()
        }), 200
    except Exception as e:
        db.session.rollback()
        return jsonify({'error': str(e)}), 500

@app.route('/api/categories/<int:category_id>', methods=['DELETE'])
@jwt_required()
def delete_category(category_id):
    try:
        user_id = int(get_jwt_identity())
        user = User.query.get(user_id)

        if not user or not user.is_admin:
            return jsonify({'error': 'Rugsat yok'}), 403

        category = Category.query.get(category_id)
        if not category:
            return jsonify({'error': 'Kategoriya tapylmady'}), 404

        db.session.delete(category)
        db.session.commit()

        return jsonify({'message': 'Kategoriya pozuldy'}), 200
    except Exception as e:
        db.session.rollback()
        return jsonify({'error': str(e)}), 500

# ==================== PRODUCT ROUTES ====================

@app.route('/api/products', methods=['GET'])
def get_products():
    try:
        page = request.args.get('page', 1, type=int)
        per_page = request.args.get('per_page', 20, type=int)
        category_id = request.args.get('category_id', type=int)
        search = request.args.get('search', '')
        sort_by = request.args.get('sort_by', 'created_at')
        featured = request.args.get('featured', type=bool)

        query = Product.query

        if category_id:
            query = query.filter_by(category_id=category_id)

        if search:
            query = query.filter(
                (Product.name.ilike(f'%{search}%')) |
                (Product.name_tm.ilike(f'%{search}%')) |
                (Product.description.ilike(f'%{search}%'))
            )

        if featured:
            query = query.filter_by(featured=True)

        if sort_by == 'price_asc':
            query = query.order_by(Product.price.asc())
        elif sort_by == 'price_desc':
            query = query.order_by(Product.price.desc())
        elif sort_by == 'rating':
            query = query.order_by(Product.rating.desc())
        elif sort_by == 'sales':
            query = query.order_by(Product.sales.desc())
        else:
            query = query.order_by(Product.created_at.desc())

        pagination = query.paginate(page=page, per_page=per_page, error_out=False)

        return jsonify({
            'products': [product.to_dict() for product in pagination.items],
            'total': pagination.total,
            'pages': pagination.pages,
            'current_page': page
        }), 200
    except Exception as e:
        return jsonify({'error': str(e)}), 500

@app.route('/api/products/<int:product_id>', methods=['GET'])
def get_product(product_id):
    try:
        product = Product.query.get(product_id)
        if not product:
            return jsonify({'error': 'Haryt tapylmady'}), 404

        product.views += 1
        db.session.commit()

        return jsonify(product.to_dict()), 200
    except Exception as e:
        return jsonify({'error': str(e)}), 500

@app.route('/api/products', methods=['POST'])
@jwt_required()
def create_product():
    try:
        user_id = int(get_jwt_identity())
        user = User.query.get(user_id)

        if not user or not user.is_admin:
            return jsonify({'error': 'Rugsat yok'}), 403

        data = request.get_json()

        if not data.get('name', '').strip():
            return jsonify({'error': 'Haryt ady hokman girizilmeli'}), 400
        if not data.get('price') or float(data['price']) <= 0:
            return jsonify({'error': 'Dogry baha girizilmeli'}), 400
        if not data.get('category_id'):
            return jsonify({'error': 'Kategoriya sayylanmaly'}), 400

        product = Product(
            name=sanitize_str(data['name']),
            name_tm=sanitize_str(data.get('name_tm', '')),
            description=sanitize_str(data.get('description', '')),
            price=float(data['price']),
            discount_price=float(data['discount_price']) if data.get('discount_price') else None,
            stock=int(data.get('stock', 0)),
            image=data.get('image'),
            images=data.get('images'),
            category_id=int(data['category_id']),
            brand=sanitize_str(data.get('brand', '')),
            featured=bool(data.get('featured', False))
        )

        db.session.add(product)
        db.session.commit()

        return jsonify({
            'message': 'Haryt gosuldy',
            'product': product.to_dict()
        }), 201
    except Exception as e:
        db.session.rollback()
        return jsonify({'error': str(e)}), 500

@app.route('/api/products/<int:product_id>', methods=['PUT'])
@jwt_required()
def update_product(product_id):
    try:
        user_id = int(get_jwt_identity())
        user = User.query.get(user_id)

        if not user or not user.is_admin:
            return jsonify({'error': 'Rugsat yok'}), 403

        product = Product.query.get(product_id)
        if not product:
            return jsonify({'error': 'Haryt tapylmady'}), 404

        data = request.get_json()

        if 'name' in data and data['name']:
            product.name = sanitize_str(data['name'])
        if 'name_tm' in data:
            product.name_tm = sanitize_str(data['name_tm']) if data['name_tm'] else None
        if 'description' in data:
            product.description = sanitize_str(data['description']) if data['description'] else None
        if 'price' in data and data['price'] is not None:
            product.price = float(data['price'])
        if 'discount_price' in data:
            product.discount_price = float(data['discount_price']) if data['discount_price'] else None
        if 'stock' in data and data['stock'] is not None:
            product.stock = int(data['stock'])
        if 'image' in data:
            product.image = data['image'] if data['image'] else None
        if 'images' in data:
            product.images = data['images'] if data['images'] else None
        if 'category_id' in data and data['category_id']:
            product.category_id = int(data['category_id'])
        if 'brand' in data:
            product.brand = sanitize_str(data['brand']) if data['brand'] else None
        if 'featured' in data:
            product.featured = bool(data['featured'])

        db.session.commit()

        return jsonify({
            'message': 'Haryt tazelendi',
            'product': product.to_dict()
        }), 200
    except Exception as e:
        db.session.rollback()
        return jsonify({'error': str(e)}), 500

@app.route('/api/products/<int:product_id>', methods=['DELETE'])
@jwt_required()
def delete_product(product_id):
    try:
        user_id = int(get_jwt_identity())
        user = User.query.get(user_id)

        if not user or not user.is_admin:
            return jsonify({'error': 'Rugsat yok'}), 403

        product = Product.query.get(product_id)
        if not product:
            return jsonify({'error': 'Haryt tapylmady'}), 404

        db.session.delete(product)
        db.session.commit()

        return jsonify({'message': 'Haryt pozuldy'}), 200
    except Exception as e:
        db.session.rollback()
        return jsonify({'error': str(e)}), 500

# ==================== CART ROUTES ====================

@app.route('/api/cart', methods=['GET'])
@jwt_required()
def get_cart():
    try:
        user_id = int(get_jwt_identity())
        cart_items = CartItem.query.filter_by(user_id=user_id).all()

        total = sum(
            (item.product.discount_price or item.product.price) * item.quantity
            for item in cart_items if item.product
        )

        return jsonify({
            'items': [item.to_dict() for item in cart_items],
            'total': total,
            'count': len(cart_items)
        }), 200
    except Exception as e:
        return jsonify({'error': str(e)}), 500

@app.route('/api/cart', methods=['POST'])
@jwt_required()
def add_to_cart():
    try:
        user_id = int(get_jwt_identity())
        data = request.get_json()

        product = Product.query.get(data['product_id'])
        if not product:
            return jsonify({'error': 'Haryt tapylmady'}), 404

        qty = data.get('quantity', 1)
        if product.stock < qty:
            return jsonify({'error': 'Yeterlik haryt yok'}), 400

        cart_item = CartItem.query.filter_by(
            user_id=user_id,
            product_id=data['product_id']
        ).first()

        if cart_item:
            cart_item.quantity += qty
        else:
            cart_item = CartItem(
                user_id=user_id,
                product_id=data['product_id'],
                quantity=qty
            )
            db.session.add(cart_item)

        db.session.commit()

        return jsonify({
            'message': 'Sebete gosuldy',
            'item': cart_item.to_dict()
        }), 201
    except Exception as e:
        db.session.rollback()
        return jsonify({'error': str(e)}), 500

@app.route('/api/cart/<int:item_id>', methods=['PUT'])
@jwt_required()
def update_cart_item(item_id):
    try:
        user_id = int(get_jwt_identity())
        cart_item = CartItem.query.filter_by(id=item_id, user_id=user_id).first()

        if not cart_item:
            return jsonify({'error': 'Sebet elementi tapylmady'}), 404

        data = request.get_json()
        quantity = data.get('quantity', 1)

        if quantity <= 0:
            db.session.delete(cart_item)
        else:
            if cart_item.product.stock < quantity:
                return jsonify({'error': 'Yeterlik haryt yok'}), 400
            cart_item.quantity = quantity

        db.session.commit()

        return jsonify({
            'message': 'Sebet tazelendi',
            'item': cart_item.to_dict() if quantity > 0 else None
        }), 200
    except Exception as e:
        db.session.rollback()
        return jsonify({'error': str(e)}), 500

@app.route('/api/cart/<int:item_id>', methods=['DELETE'])
@jwt_required()
def remove_from_cart(item_id):
    try:
        user_id = int(get_jwt_identity())
        cart_item = CartItem.query.filter_by(id=item_id, user_id=user_id).first()

        if not cart_item:
            return jsonify({'error': 'Sebet elementi tapylmady'}), 404

        db.session.delete(cart_item)
        db.session.commit()

        return jsonify({'message': 'Sebetden ayyryldy'}), 200
    except Exception as e:
        db.session.rollback()
        return jsonify({'error': str(e)}), 500

@app.route('/api/cart/clear', methods=['DELETE'])
@jwt_required()
def clear_cart():
    try:
        user_id = int(get_jwt_identity())
        CartItem.query.filter_by(user_id=user_id).delete()
        db.session.commit()

        return jsonify({'message': 'Sebet arassalandy'}), 200
    except Exception as e:
        db.session.rollback()
        return jsonify({'error': str(e)}), 500

# ==================== ORDER ROUTES ====================

@app.route('/api/orders', methods=['GET'])
@jwt_required()
def get_orders():
    try:
        user_id = int(get_jwt_identity())
        user = User.query.get(user_id)

        if user.is_admin:
            orders = Order.query.order_by(Order.created_at.desc()).all()
        else:
            orders = Order.query.filter_by(user_id=user_id).order_by(Order.created_at.desc()).all()

        return jsonify([order.to_dict() for order in orders]), 200
    except Exception as e:
        return jsonify({'error': str(e)}), 500

@app.route('/api/orders/<int:order_id>', methods=['GET'])
@jwt_required()
def get_order(order_id):
    try:
        user_id = int(get_jwt_identity())
        user = User.query.get(user_id)

        order = Order.query.get(order_id)
        if not order:
            return jsonify({'error': 'Sargyt tapylmady'}), 404

        if not user.is_admin and order.user_id != user_id:
            return jsonify({'error': 'Rugsat yok'}), 403

        return jsonify(order.to_dict()), 200
    except Exception as e:
        return jsonify({'error': str(e)}), 500

@app.route('/api/orders', methods=['POST'])
@jwt_required()
def create_order():
    try:
        user_id = int(get_jwt_identity())
        data = request.get_json()

        cart_items = CartItem.query.filter_by(user_id=user_id).all()

        if not cart_items:
            return jsonify({'error': 'Sebet bos'}), 400

        total = 0
        order_items = []

        for cart_item in cart_items:
            product = cart_item.product

            if product.stock < cart_item.quantity:
                return jsonify({'error': f'{product.name} - Yeterlik haryt yok'}), 400

            price = product.discount_price or product.price
            total += price * cart_item.quantity

            order_items.append({
                'product_id': product.id,
                'quantity': cart_item.quantity,
                'price': price
            })

            product.stock -= cart_item.quantity
            product.sales += cart_item.quantity

        order = Order(
            user_id=user_id,
            total_amount=total,
            shipping_address=data['shipping_address'],
            phone=data.get('phone'),
            notes=data.get('notes'),
            status='pending'
        )

        db.session.add(order)
        db.session.flush()

        for item_data in order_items:
            order_item = OrderItem(
                order_id=order.id,
                product_id=item_data['product_id'],
                quantity=item_data['quantity'],
                price=item_data['price']
            )
            db.session.add(order_item)

        CartItem.query.filter_by(user_id=user_id).delete()

        db.session.commit()

        return jsonify({
            'message': 'Sargyt ustunlikli yerine yetirildi',
            'order': order.to_dict()
        }), 201
    except Exception as e:
        db.session.rollback()
        return jsonify({'error': str(e)}), 500

@app.route('/api/orders/<int:order_id>/status', methods=['PUT'])
@jwt_required()
def update_order_status(order_id):
    try:
        user_id = int(get_jwt_identity())
        user = User.query.get(user_id)

        if not user or not user.is_admin:
            return jsonify({'error': 'Rugsat yok'}), 403

        order = Order.query.get(order_id)
        if not order:
            return jsonify({'error': 'Sargyt tapylmady'}), 404

        data = request.get_json()
        valid_statuses = ['pending', 'processing', 'shipped', 'delivered', 'cancelled']
        if data.get('status') not in valid_statuses:
            return jsonify({'error': 'Nadogry yagday'}), 400

        order.status = data['status']
        order.updated_at = datetime.utcnow()

        db.session.commit()

        return jsonify({
            'message': 'Sargyt yagdayy tazelendi',
            'order': order.to_dict()
        }), 200
    except Exception as e:
        db.session.rollback()
        return jsonify({'error': str(e)}), 500

# ==================== RECOMMENDATIONS ====================

@app.route('/api/recommendations/<int:product_id>', methods=['GET'])
def get_recommendations(product_id):
    try:
        product = Product.query.get(product_id)
        if not product:
            return jsonify({'error': 'Haryt tapylmady'}), 404

        similar_products = Product.query.filter(
            Product.category_id == product.category_id,
            Product.id != product_id
        ).limit(10).all()

        recommendations = sorted(
            similar_products,
            key=lambda p: (
                abs(p.price - product.price),
                -p.rating,
                -p.sales
            )
        )[:5]

        return jsonify([p.to_dict() for p in recommendations]), 200
    except Exception as e:
        return jsonify({'error': str(e)}), 500

@app.route('/api/recommendations/user', methods=['GET'])
@jwt_required()
def get_user_recommendations():
    try:
        user_id = int(get_jwt_identity())
        orders = Order.query.filter_by(user_id=user_id).all()

        if not orders:
            popular = Product.query.order_by(Product.sales.desc()).limit(10).all()
            return jsonify([p.to_dict() for p in popular]), 200

        purchased_categories = set()
        purchased_product_ids = set()

        for order in orders:
            for item in order.items:
                if item.product:
                    purchased_categories.add(item.product.category_id)
                purchased_product_ids.add(item.product_id)

        recommendations = Product.query.filter(
            Product.category_id.in_(purchased_categories),
            ~Product.id.in_(purchased_product_ids)
        ).order_by(Product.rating.desc(), Product.sales.desc()).limit(10).all()

        return jsonify([p.to_dict() for p in recommendations]), 200
    except Exception as e:
        return jsonify({'error': str(e)}), 500

# ==================== ANALYTICS ====================

@app.route('/api/analytics/dashboard', methods=['GET'])
@jwt_required()
def get_dashboard_analytics():
    try:
        user_id = int(get_jwt_identity())
        user = User.query.get(user_id)

        if not user or not user.is_admin:
            return jsonify({'error': 'Rugsat yok'}), 403

        total_users = User.query.count()
        total_products = Product.query.count()
        total_orders = Order.query.count()
        total_revenue = db.session.query(db.func.sum(Order.total_amount)).scalar() or 0

        pending_orders = Order.query.filter_by(status='pending').count()
        low_stock_products = Product.query.filter(Product.stock < 10).count()

        recent_orders = Order.query.order_by(Order.created_at.desc()).limit(10).all()
        top_products = Product.query.order_by(Product.sales.desc()).limit(5).all()

        return jsonify({
            'total_users': total_users,
            'total_products': total_products,
            'total_orders': total_orders,
            'total_revenue': float(total_revenue),
            'pending_orders': pending_orders,
            'low_stock_products': low_stock_products,
            'recent_orders': [order.to_dict() for order in recent_orders],
            'top_products': [p.to_dict() for p in top_products]
        }), 200
    except Exception as e:
        return jsonify({'error': str(e)}), 500

# ==================== FILE UPLOAD ====================

@app.route('/api/upload', methods=['POST'])
@jwt_required()
def upload_file():
    try:
        # FIX: Admin barlagy gosuyldy
        user_id = int(get_jwt_identity())
        user = User.query.get(user_id)
        if not user or not user.is_admin:
            return jsonify({'error': 'Rugsat yok'}), 403

        if 'file' not in request.files:
            return jsonify({'error': 'Fayl tapylmady'}), 400

        file = request.files['file']

        if file.filename == '':
            return jsonify({'error': 'Fayl sayylanmady'}), 400

        if file and allowed_file(file.filename):
            filename = secure_filename(file.filename)
            timestamp = datetime.now().strftime('%Y%m%d_%H%M%S_')
            filename = timestamp + filename

            filepath = os.path.join(app.config['UPLOAD_FOLDER'], filename)
            file.save(filepath)

            file_url = f'/static/uploads/{filename}'

            return jsonify({
                'message': 'Fayl ustunlikli yuklendi',
                'url': file_url
            }), 200
        else:
            return jsonify({'error': 'Rugsat edilmedik fayl gornusy'}), 400
    except Exception as e:
        return jsonify({'error': str(e)}), 500

@app.route('/static/uploads/<filename>')
def uploaded_file(filename):
    return send_from_directory(app.config['UPLOAD_FOLDER'], filename)

# ==================== DATABASE INIT ====================

def init_db():
    with app.app_context():
        db.create_all()

        admin = User.query.filter_by(username='admin').first()
        if not admin:
            hashed_password = bcrypt.generate_password_hash('admin123').decode('utf-8')
            admin = User(
                username='admin',
                email='admin@esowda.tm',
                password=hashed_password,
                full_name='Administrator',
                is_admin=True
            )
            db.session.add(admin)

        if Category.query.count() == 0:
            categories = [
                Category(name='Electronics', name_tm='Elektronika', description='Elektronik enjamlar'),
                Category(name='Fashion', name_tm='Moda', description='Egin-esik we moda'),
                Category(name='Home & Garden', name_tm='Oy we bag', description='Oy ucin harytlar'),
                Category(name='Sports', name_tm='Sport', description='Sport harytlary'),
                Category(name='Books', name_tm='Kitaplar', description='Kitaplar we okuw materiallary'),
            ]
            for cat in categories:
                db.session.add(cat)

        if Product.query.count() == 0:
            db.session.flush()
            electronics = Category.query.filter_by(name='Electronics').first()
            fashion = Category.query.filter_by(name='Fashion').first()

            if electronics:
                products = [
                    Product(
                        name='Smartphone Pro Max', name_tm='Smartfon Pro Max',
                        description='In taze tehnologiya bilen enjamlasdyrylan smartfon',
                        price=2999.99, discount_price=2499.99, stock=50,
                        category_id=electronics.id, brand='TechBrand', rating=4.8, featured=True
                    ),
                    Product(
                        name='Laptop Ultra', name_tm='Noutbuk Ultra',
                        description='Gycli prosessor we yokary ondurjilik',
                        price=4999.99, stock=30,
                        category_id=electronics.id, brand='CompuBrand', rating=4.7, featured=True
                    ),
                ]
                for p in products:
                    db.session.add(p)

            if fashion:
                db.session.add(Product(
                    name='Summer T-Shirt', name_tm='Tomusky futbolka',
                    description='Rahat we stilli futbolka',
                    price=29.99, discount_price=19.99, stock=100,
                    category_id=fashion.id, brand='FashionBrand', rating=4.5, featured=False
                ))

        db.session.commit()
        print("Maglumat bazasy ustunlikli basladyldy!")

# ==================== MAIN ====================

if __name__ == '__main__':
    init_db()
    # FIX: debug=False, production ucin Gunicorn ulanmaly
    app.run(debug=False, host='0.0.0.0', port=5030)