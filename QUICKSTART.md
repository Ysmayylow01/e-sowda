# E-söwda - Quick Start Guide

## 🚀 Fast Setup (5 minutes)

### Step 1: Backend Setup
```bash
cd esowda-platform/backend
pip install -r requirements.txt --break-system-packages
python app.py
```

Backend will run on: **http://localhost:5000**

### Step 2: Admin Panel
Open `admin-panel/index.html` in your browser or run:
```bash
cd admin-panel
python -m http.server 8080
```

Then visit: **http://localhost:8080**

**Login:** admin / admin123

### Step 3: Android App
1. Open Android Studio
2. Import project from `android-app` folder
3. Wait for Gradle sync
4. Update `BASE_URL` in `ApiService.kt` if needed
5. Run on emulator or device

---

## 📱 Testing the Platform

### Test Backend API
```bash
# Test login
curl -X POST http://localhost:5000/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}'

# Get products
curl http://localhost:5000/api/products
```

### Test Admin Panel
1. Login with admin credentials
2. Create categories (Electronics, Fashion, etc.)
3. Add products with images
4. View dashboard statistics

### Test Android App
1. Run app on emulator
2. Register new user or login
3. Browse products and categories
4. Add items to cart
5. Place an order
6. View order history

---

## 🎯 Key Features Checklist

### Backend ✅
- [x] User authentication (JWT)
- [x] Product CRUD operations
- [x] Category management
- [x] Shopping cart
- [x] Order processing
- [x] Recommendation system
- [x] Analytics dashboard

### Admin Panel ✅
- [x] Modern responsive UI
- [x] Product management
- [x] Category management
- [x] Order tracking
- [x] User management
- [x] Dashboard statistics

### Android App ✅
- [x] Material Design UI
- [x] Bottom navigation
- [x] Product browsing
- [x] Search functionality
- [x] Shopping cart
- [x] User profile
- [x] Order history
- [x] Product recommendations

---

## 🔧 Configuration

### Change Backend Port
Edit `backend/app.py`:
```python
app.run(debug=True, host='0.0.0.0', port=5001)  # Change 5000 to 5001
```

### Update API URL in Admin Panel
Edit `admin-panel/script.js`:
```javascript
const API_URL = 'http://your-server:5000/api';
```

### Update API URL in Android
Edit `android-app/app/src/main/java/com/esowda/network/ApiService.kt`:
```kotlin
private const val BASE_URL = "http://your-server:5000/api/"
```

---

## 📊 Sample Data

The backend automatically creates:
- **Admin user**: admin / admin123
- **Sample categories**: Electronics, Fashion, Home & Garden, Sports, Books
- **Sample products**: Smartphones, Laptops, T-Shirts

---

## 🛠️ Troubleshooting

### Backend won't start
```bash
# Check if port is in use
lsof -i :5000

# Kill process if needed
kill -9 <PID>
```

### Admin Panel can't connect
1. Make sure backend is running
2. Check browser console for errors
3. Verify API_URL in script.js

### Android app network error
1. For emulator: Use `10.0.2.2` instead of `localhost`
2. Check backend is accessible
3. Verify internet permission in AndroidManifest.xml

---

## 📝 Default Credentials

**Admin Account:**
- Username: `admin`
- Password: `admin123`

**Test User (Create via app):**
- Use registration form in Android app or Admin Panel

---

## 🎨 Customization

### Change Color Scheme (Admin Panel)
Edit `admin-panel/styles.css`:
```css
:root {
    --primary-color: #6366f1;  /* Change this */
    --secondary-color: #8b5cf6;
}
```

### Change App Colors (Android)
Edit `android-app/app/src/main/res/values/colors.xml`:
```xml
<color name="colorPrimary">#6366F1</color>
```

---

## 🚢 Deployment

### Backend (Production)
```bash
pip install gunicorn
gunicorn -w 4 -b 0.0.0.0:5000 app:app
```

### Admin Panel
Upload to any static hosting:
- GitHub Pages
- Netlify
- Vercel
- AWS S3

### Android App
1. Build signed APK
2. Upload to Google Play Store
3. Or distribute APK directly

---

## 📞 Support

For issues or questions:
1. Check the full README.md
2. Review API documentation
3. Test with sample data first

---

## ✨ Next Steps

1. **Customize the design** to match your brand
2. **Add more products** and categories
3. **Configure payment gateway** (optional)
4. **Set up email notifications** (optional)
5. **Deploy to production** servers
6. **Publish app** to Google Play Store

---

**Congratulations! Your E-söwda platform is ready! 🎉**
