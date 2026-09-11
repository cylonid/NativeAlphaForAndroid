package com.cylonid.nativealpha.activities

import android.os.Bundle
import android.view.LayoutInflater
import androidx.activity.addCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.viewbinding.ViewBinding
import com.cylonid.nativealpha.databinding.ActivityToolbarBaseBinding
import com.cylonid.nativealpha.util.WindowInsetsUtils

abstract class ToolbarBaseActivity<VB : ViewBinding> : AppCompatActivity() {

    private lateinit var _binding: VB
    protected val binding get() = _binding

    private var onNavigationClickListener: (() -> Unit)? = null

    abstract fun inflateBinding(layoutInflater: LayoutInflater): VB

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val baseBinding = ActivityToolbarBaseBinding.inflate(layoutInflater)
        setContentView(baseBinding.root)
        WindowInsetsUtils.applyAsPadding(this, false)

        _binding = inflateBinding(layoutInflater)
        baseBinding.activityContent.addView(_binding.root)

        val toolbar = baseBinding.toolbar.topAppBar
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        onBackPressedDispatcher.addCallback(this) {
            finish()
        }

        toolbar.setNavigationOnClickListener {
            onNavigationClickListener?.invoke() ?: onBackPressedDispatcher.onBackPressed()
        }
    }

    fun setToolbarTitle(title: String) {
        supportActionBar?.title = title
    }

    fun setNavigationClickListener(listener: () -> Unit) {
        onNavigationClickListener = listener
    }
}