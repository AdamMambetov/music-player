package com.example.musicplayer.presentation

import android.content.Intent
import android.content.SharedPreferences
import android.database.Cursor
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.musicplayer.Global
import com.example.musicplayer.R
import com.example.musicplayer.databinding.FragmentPlayerBinding


class PlayerFragment : Fragment() {

    private lateinit var viewBinding: FragmentPlayerBinding
    private lateinit var mediaPlayer: MediaPlayer
    private lateinit var storage: SharedPreferences

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        viewBinding = FragmentPlayerBinding.inflate(inflater, container, false)
        return viewBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewBinding.btnMainPlay.setOnClickListener {
//            if (mediaPlayer.isPlaying){
//                mediaPlayer.stop()
//            } else {
//                mediaPlayer.setDataSource(this, musicUri)
//                mediaPlayer.start()
//            }
        }


        viewBinding.btnMainSearch.setOnClickListener {
            val rootFolder = storage.getString(Global.ROOT_FOLDER_KEY, "")
            if (rootFolder.isNullOrEmpty()) {
                pickRootFolder()
                return@setOnClickListener
            }
            parentFragmentManager
                .beginTransaction()
                .replace(R.id.fragment_container_view, SearchFragment.newInstance())
                .commit()

//            val dir = File(rootFolder + Global.MUSIC_MARKDOWN_FOLDER)
//            musics.clear()
//            dir.listFiles()?.forEach { file ->
//                if (file.isFile) {
//                    val music = Music.createFromFile(file)
//                    if (music != null) {
//                        musics.add(music)
//                    }
//                }
//            }
        }


        storage = context?.getSharedPreferences(Global.SHARED_PREFERENCES_NAME, 0)!!
        val rootFolder = storage.getString(Global.ROOT_FOLDER_KEY, "")
        if (rootFolder.isNullOrEmpty()) {
            pickRootFolder()
        }
        if (!isStoragePermissionGranted()) {
            requestStoragePermission()
        }


//        val uri1 = Uri.parse(rootFolder + Global.MUSIC_FOLDER + "Umineko no Naku Koro ni OST - 011 Кипа летних облаков.ogg")
//        mediaPlayer = MediaPlayer.create(this, uri1)
//        mediaPlayer.start()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        Log.d("TAG", "OnActivityResult")

        if (resultCode != AppCompatActivity.RESULT_OK) {
            Log.e("TAG", "$resultCode")
            return
        }

        when (requestCode) {
            Global.ROOT_FOLDER_CODE -> {
                Log.d("TAG", "root dir = " + data?.data?.path!!)
                var rootPath = if (data?.data?.path!!.contains("primary"))
                    Environment.getExternalStorageDirectory().path
                else Environment.getStorageDirectory().path + "/" + (data.data?.pathSegments?.get(1)
                    ?.substringBefore(':')
                    ?: "")
                rootPath += "/${data.data?.path!!.substringAfter(':')}"

                Log.d("TAG", rootPath)
                storage.edit()
                    .putString(Global.ROOT_FOLDER_KEY, rootPath)
                    .apply()
            }
        }
    }


    private fun pickRootFolder() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
        intent.addCategory(Intent.CATEGORY_DEFAULT)
        startActivityForResult(Intent.createChooser(intent, "Выбери корневую папку"),
            Global.ROOT_FOLDER_CODE
        )
    }

    private fun requestStoragePermission() = startActivity(
        Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
    )

    private fun isStoragePermissionGranted(): Boolean = Environment.isExternalStorageManager()

    private fun searchAllMusics() {
        Log.d("TAG", "Start analyze all musics")
        val uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        val cursor: Cursor? = null //contentResolver.query(uri, null, null, null)
        if (cursor == null) {
            Log.d("TAG", "cursor is null")
        } else if (!cursor.moveToFirst()) {
            Log.d("TAG", "no media")
        } else {
            val idColumn = cursor.getColumnIndex(MediaStore.Audio.Media._ID)
            val titleColumn = cursor.getColumnIndex(MediaStore.Audio.Media.TITLE)
            val artistColumn = cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST)
            val albumColumn = cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM)
            val yearColumn = cursor.getColumnIndex(MediaStore.Audio.Media.YEAR)
            val isMusicColumn = cursor.getColumnIndex(MediaStore.Audio.Media.IS_MUSIC)
            do {
                if (!cursor.isNull(isMusicColumn)) {
                    val id = cursor.getLong(idColumn)
                    val title = cursor.getString(titleColumn)
                    val artist = cursor.getString(artistColumn)
                    val album = cursor.getString(albumColumn)
                    val year = cursor.getInt(yearColumn)
                    Log.d("TAG", "id - [$id], title - [$title], artist - [$artist], " +
                            "album - [$album], year - [$year]")
                }
            } while (cursor.moveToNext())
            cursor.close()
        }
    }

    private fun findUriOfMusicId(id: Long): Uri {
        val uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        val selection = "${MediaStore.Audio.Media._ID} = ?"
        val selectionArgs = arrayOf("$id")
        val cursor: Cursor? = null // contentResolver.query(uri, null, selection, selectionArgs, null)
        if (cursor == null) {
            Log.d("TAG", "cursor is null")
        } else if (!cursor.moveToFirst()) {
            Log.d("TAG", "no media")
        } else {
            val volumeNameColumn = cursor.getColumnIndex(MediaStore.Audio.Media.VOLUME_NAME)
            val volumeName = cursor.getString(volumeNameColumn)
            cursor.close()

            return MediaStore.Audio.Media.getContentUri(volumeName, id)
            // or return ContentUris.withAppendedId(uri, id)
        }
        return Uri.EMPTY
    }


    companion object {
        @JvmStatic
        fun newInstance() = PlayerFragment()
    }

}