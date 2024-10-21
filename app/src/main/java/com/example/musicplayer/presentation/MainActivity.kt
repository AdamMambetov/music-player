package com.example.musicplayer.presentation

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.example.musicplayer.databinding.ActivityMainBinding
import com.example.musicplayer.domain.MusicInfo

class MainActivity : AppCompatActivity(), LifecycleOwner {

    private lateinit var viewBinding: ActivityMainBinding
    private lateinit var viewModel: MainViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewBinding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(viewBinding.root)

        supportFragmentManager
            .beginTransaction()
            .replace(viewBinding.fragmentContainerView.id, PlayerFragment.newInstance())
            .commit()

        viewModel = ViewModelProvider(this)[MainViewModel::class.java]
        viewModel.musicList.observe(this
        ) { value -> Log.d("TAG", value.toString()) }
        viewModel.analyzeMusicMarkdownFiles(this)
    }
}
