package com.kwikpass

import android.app.Application
import android.os.Bundle
import android.widget.Button
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.gk.kwikpass.LoginActivity
import com.gk.kwikpass.initializer.kwikpassInitializer

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        val loginButton: Button = findViewById(R.id.btnLogin)

        kwikpassInitializer.initialize(applicationContext, "abc", "sandbox", true)

        loginButton.setOnClickListener {
            val loginIntent = LoginActivity.getIntent(
                context = this,
                title = "Welcome Back",
                submitButtonText = "Sign-In"
            )
            startActivity(loginIntent)
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }
}
