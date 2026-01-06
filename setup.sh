#!/bin/bash

# E-söwda Platform Setup Script
# This script helps you quickly set up and run the E-söwda e-commerce platform

echo "╔══════════════════════════════════════════════════════════╗"
echo "║        E-söwda Platform - Setup & Deployment            ║"
echo "╚══════════════════════════════════════════════════════════╝"
echo ""

# Check Python installation
if ! command -v python3 &> /dev/null; then
    echo "❌ Python 3 is not installed. Please install Python 3.8 or higher."
    exit 1
fi

echo "✓ Python 3 detected: $(python3 --version)"
echo ""

# Menu
echo "Select an option:"
echo "1. Setup Backend (Flask API)"
echo "2. Run Backend Server"
echo "3. Open Admin Panel"
echo "4. Setup Android Project"
echo "5. Complete Setup (Backend + Admin Panel)"
echo "6. Exit"
echo ""

read -p "Enter your choice [1-6]: " choice

case $choice in
    1)
        echo ""
        echo "🔧 Setting up Backend..."
        cd backend
        
        # Install dependencies
        echo "📦 Installing Python dependencies..."
        pip3 install -r requirements.txt --break-system-packages
        
        echo ""
        echo "✅ Backend setup complete!"
        echo "Run option 2 to start the server."
        ;;
        
    2)
        echo ""
        echo "🚀 Starting Flask Backend Server..."
        cd backend
        
        if [ ! -f "requirements.txt" ]; then
            echo "❌ Backend not set up. Please run option 1 first."
            exit 1
        fi
        
        echo ""
        echo "Server starting on http://localhost:5000"
        echo "Press Ctrl+C to stop the server"
        echo ""
        python3 app.py
        ;;
        
    3)
        echo ""
        echo "🌐 Opening Admin Panel..."
        cd admin-panel
        
        # Try to open with default browser
        if command -v xdg-open &> /dev/null; then
            xdg-open index.html
        elif command -v open &> /dev/null; then
            open index.html
        else
            echo "Starting local server on port 8080..."
            python3 -m http.server 8080
        fi
        ;;
        
    4)
        echo ""
        echo "📱 Android Project Setup Instructions:"
        echo ""
        echo "1. Open Android Studio"
        echo "2. Select 'Open an existing project'"
        echo "3. Navigate to: $(pwd)/android-app"
        echo "4. Wait for Gradle sync to complete"
        echo "5. Update API URL in ApiService.kt if needed"
        echo "6. Run on emulator or device"
        echo ""
        echo "Note: Make sure Backend is running before testing the app!"
        ;;
        
    5)
        echo ""
        echo "🔧 Complete Setup Starting..."
        echo ""
        
        # Setup backend
        echo "Step 1/2: Setting up Backend..."
        cd backend
        pip3 install -r requirements.txt --break-system-packages
        
        # Initialize database
        echo ""
        echo "Initializing database..."
        python3 -c "from app import init_db; init_db()"
        
        cd ..
        
        echo ""
        echo "✅ Complete setup finished!"
        echo ""
        echo "To start using E-söwda:"
        echo "1. Run: ./setup.sh and select option 2 (Start Backend)"
        echo "2. Open Admin Panel: http://localhost:8080"
        echo "3. Login with: admin / admin123"
        echo "4. Import Android project in Android Studio"
        echo ""
        ;;
        
    6)
        echo "Exiting..."
        exit 0
        ;;
        
    *)
        echo "❌ Invalid option. Please run the script again."
        exit 1
        ;;
esac

echo ""
echo "═══════════════════════════════════════════════════════════"
echo "Thank you for using E-söwda Platform!"
echo "═══════════════════════════════════════════════════════════"
