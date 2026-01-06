package com.esowda.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.esowda.MainActivity
import com.esowda.R
import com.esowda.models.AuthRequest
import com.esowda.models.RegisterRequest
import com.esowda.network.RetrofitClient
import com.esowda.utils.PrefsManager
import com.google.android.material.tabs.TabLayout
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AuthActivity : AppCompatActivity() {
    
    private lateinit var tabLayout: TabLayout
    private lateinit var loginContainer: View
    private lateinit var registerContainer: View
    private lateinit var progressBar: ProgressBar
    private lateinit var prefsManager: PrefsManager
    
    // Login fields
    private lateinit var loginUsername: TextInputEditText
    private lateinit var loginPassword: TextInputEditText
    private lateinit var loginButton: Button
    
    // Register fields
    private lateinit var registerUsername: TextInputEditText
    private lateinit var registerEmail: TextInputEditText
    private lateinit var registerPassword: TextInputEditText
    private lateinit var registerFullName: TextInputEditText
    private lateinit var registerPhone: TextInputEditText
    private lateinit var registerAddress: TextInputEditText
    private lateinit var registerButton: Button
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_auth)
        
        prefsManager = PrefsManager(this)
        
        // Check if already logged in
        if (prefsManager.isLoggedIn()) {
            navigateToMain()
            return
        }
        
        initViews()
        setupTabs()
        setupButtons()
    }
    
    private fun initViews() {
        tabLayout = findViewById(R.id.tabLayout)
        loginContainer = findViewById(R.id.loginContainer)
        registerContainer = findViewById(R.id.registerContainer)
        progressBar = findViewById(R.id.progressBar)
        
        // Login fields
        loginUsername = findViewById(R.id.loginUsername)
        loginPassword = findViewById(R.id.loginPassword)
        loginButton = findViewById(R.id.loginButton)
        
        // Register fields
        registerUsername = findViewById(R.id.registerUsername)
        registerEmail = findViewById(R.id.registerEmail)
        registerPassword = findViewById(R.id.registerPassword)
        registerFullName = findViewById(R.id.registerFullName)
        registerPhone = findViewById(R.id.registerPhone)
        registerAddress = findViewById(R.id.registerAddress)
        registerButton = findViewById(R.id.registerButton)
    }
    
    private fun setupTabs() {
        tabLayout.addTab(tabLayout.newTab().setText("Girmek"))
        tabLayout.addTab(tabLayout.newTab().setText("Hasaba almak"))
        
        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                when (tab?.position) {
                    0 -> {
                        loginContainer.visibility = View.VISIBLE
                        registerContainer.visibility = View.GONE
                    }
                    1 -> {
                        loginContainer.visibility = View.GONE
                        registerContainer.visibility = View.VISIBLE
                    }
                }
            }
            
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }
    
    private fun setupButtons() {
        loginButton.setOnClickListener {
            val username = loginUsername.text.toString().trim()
            val password = loginPassword.text.toString()
            
            if (username.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Ähli meýdançalary dolduryň", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            performLogin(username, password)
        }
        
        registerButton.setOnClickListener {
            val username = registerUsername.text.toString().trim()
            val email = registerEmail.text.toString().trim()
            val password = registerPassword.text.toString()
            val fullName = registerFullName.text.toString().trim()
            val phone = registerPhone.text.toString().trim()
            val address = registerAddress.text.toString().trim()
            
            if (username.isEmpty() || email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Zerur meýdançalary dolduryň", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                Toast.makeText(this, "Dogry email girizň", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            if (password.length < 6) {
                Toast.makeText(this, "Parol iň azyndan 6 harp bolmaly", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            performRegister(username, email, password, fullName, phone, address)
        }
    }
    
    private fun performLogin(username: String, password: String) {
        progressBar.visibility = View.VISIBLE
        loginButton.isEnabled = false
        
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val request = AuthRequest(username, password)
                val response = RetrofitClient.apiService.login(request)
                
                withContext(Dispatchers.Main) {
                    progressBar.visibility = View.GONE
                    loginButton.isEnabled = true
                    
                    if (response.isSuccessful) {
                        response.body()?.let { authResponse ->
                            prefsManager.saveToken(authResponse.token)
                            prefsManager.saveUser(authResponse.user)
                            prefsManager.setLoggedIn(true)
                            RetrofitClient.setToken(authResponse.token)
                            
                            Toast.makeText(
                                this@AuthActivity,
                                "Hoş geldiňiz!",
                                Toast.LENGTH_SHORT
                            ).show()
                            
                            navigateToMain()
                        }
                    } else {
                        Toast.makeText(
                            this@AuthActivity,
                            "Nädogry ulanyjy ady ýa-da parol",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    progressBar.visibility = View.GONE
                    loginButton.isEnabled = true
                    Toast.makeText(
                        this@AuthActivity,
                        "Baglanyşyk ýalňyşlygy: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }
    
    private fun performRegister(
        username: String,
        email: String,
        password: String,
        fullName: String?,
        phone: String?,
        address: String?
    ) {
        progressBar.visibility = View.VISIBLE
        registerButton.isEnabled = false
        
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val request = RegisterRequest(
                    username,
                    email,
                    password,
                    fullName,
                    phone,
                    address
                )
                val response = RetrofitClient.apiService.register(request)
                
                withContext(Dispatchers.Main) {
                    progressBar.visibility = View.GONE
                    registerButton.isEnabled = true
                    
                    if (response.isSuccessful) {
                        response.body()?.let { authResponse ->
                            prefsManager.saveToken(authResponse.token)
                            prefsManager.saveUser(authResponse.user)
                            prefsManager.setLoggedIn(true)
                            RetrofitClient.setToken(authResponse.token)
                            
                            Toast.makeText(
                                this@AuthActivity,
                                "Hasaba alyndy!",
                                Toast.LENGTH_SHORT
                            ).show()
                            
                            navigateToMain()
                        }
                    } else {
                        val errorBody = response.errorBody()?.string()
                        Toast.makeText(
                            this@AuthActivity,
                            "Hasaba almak şowsuz boldy",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    progressBar.visibility = View.GONE
                    registerButton.isEnabled = true
                    Toast.makeText(
                        this@AuthActivity,
                        "Baglanyşyk ýalňyşlygy: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }
    
    private fun navigateToMain() {
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}
