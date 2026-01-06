package com.esowda.ui

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.esowda.R
import com.esowda.network.RetrofitClient
import com.esowda.utils.PrefsManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ProfileFragment : Fragment() {
    
    private lateinit var prefsManager: PrefsManager
    
    private lateinit var usernameText: TextView
    private lateinit var emailText: TextView
    private lateinit var fullNameText: TextView
    private lateinit var phoneText: TextView
    private lateinit var addressText: TextView
    
    private lateinit var editProfileButton: Button
    private lateinit var myOrdersButton: Button
    private lateinit var logoutButton: Button
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_profile, container, false)
        
        prefsManager = PrefsManager(requireContext())
        
        initViews(view)
        loadUserData()
        setupButtons()
        
        return view
    }
    
    private fun initViews(view: View) {
        usernameText = view.findViewById(R.id.usernameText)
        emailText = view.findViewById(R.id.emailText)
        fullNameText = view.findViewById(R.id.fullNameText)
        phoneText = view.findViewById(R.id.phoneText)
        addressText = view.findViewById(R.id.addressText)
        
        editProfileButton = view.findViewById(R.id.editProfileButton)
        myOrdersButton = view.findViewById(R.id.myOrdersButton)
        logoutButton = view.findViewById(R.id.logoutButton)
    }
    
    private fun loadUserData() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = RetrofitClient.apiService.getCurrentUser()
                
                withContext(Dispatchers.Main) {
                    if (response.isSuccessful) {
                        response.body()?.let { user ->
                            prefsManager.saveUser(user)
                            displayUserInfo(user)
                        }
                    } else {
                        // Display cached user data
                        prefsManager.getUser()?.let { user ->
                            displayUserInfo(user)
                        }
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    // Display cached user data
                    prefsManager.getUser()?.let { user ->
                        displayUserInfo(user)
                    }
                }
            }
        }
    }
    
    private fun displayUserInfo(user: com.esowda.models.User) {
        usernameText.text = user.username
        emailText.text = user.email
        fullNameText.text = user.fullName ?: "Girizilmedi"
        phoneText.text = user.phone ?: "Girizilmedi"
        addressText.text = user.address ?: "Girizilmedi"
    }
    
    private fun setupButtons() {
        editProfileButton.setOnClickListener {
            showEditProfileDialog()
        }
        
        myOrdersButton.setOnClickListener {
            startActivity(Intent(context, OrdersActivity::class.java))
        }
        
        logoutButton.setOnClickListener {
            showLogoutDialog()
        }
    }
    
    private fun showEditProfileDialog() {
        val user = prefsManager.getUser() ?: return
        
        val dialogView = layoutInflater.inflate(R.layout.dialog_edit_profile, null)
        
        val fullNameInput = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.fullNameInput)
        val phoneInput = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.phoneInput)
        val addressInput = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.addressInput)
        
        fullNameInput.setText(user.fullName)
        phoneInput.setText(user.phone)
        addressInput.setText(user.address)
        
        AlertDialog.Builder(requireContext())
            .setTitle("Profili düzet")
            .setView(dialogView)
            .setPositiveButton("Ýatda sakla") { _, _ ->
                val updatedData = mapOf(
                    "full_name" to (fullNameInput.text?.toString() ?: ""),
                    "phone" to (phoneInput.text?.toString() ?: ""),
                    "address" to (addressInput.text?.toString() ?: "")
                )
                updateProfile(user.id, updatedData)
            }
            .setNegativeButton("Ýatyrmak", null)
            .show()
    }
    
    private fun updateProfile(userId: Int, data: Map<String, String>) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = RetrofitClient.apiService.updateUser(userId, data)
                
                withContext(Dispatchers.Main) {
                    if (response.isSuccessful) {
                        Toast.makeText(
                            context,
                            "Profil üstünlikli täzelendi",
                            Toast.LENGTH_SHORT
                        ).show()
                        loadUserData()
                    } else {
                        Toast.makeText(
                            context,
                            "Täzelemek şowsuz boldy",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        context,
                        "Baglanyşyk ýalňyşlygy: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }
    
    private fun showLogoutDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("Çykmak")
            .setMessage("Hasapdan çykmak isleýärsiňizmi?")
            .setPositiveButton("Hawa") { _, _ ->
                logout()
            }
            .setNegativeButton("Ýok", null)
            .show()
    }
    
    private fun logout() {
        prefsManager.clearSession()
        RetrofitClient.setToken(null)
        
        val intent = Intent(context, AuthActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
    }
}
