package com.example.musicplayer.data

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.musicplayer.Global
import com.example.musicplayer.domain.MusicInfo
import com.example.musicplayer.domain.MusicListRepository
import org.yaml.snakeyaml.Yaml
import java.io.File

object MusicListRepositoryImpl : MusicListRepository {

    private const val MUSIC_FOLDER = "/Audio/Music/"
    private const val MUSIC_MARKDOWN_FOLDER = "/Text/Note/projects/music"

    private val musicList = mutableListOf<MusicInfo>()
    private val musicListLiveData = MutableLiveData<List<MusicInfo>>()

    private var rootFolder: String? = null


    override fun getMusicList(): LiveData<List<MusicInfo>> {
        return musicListLiveData
    }

    override fun getMusicInfo(musicInfoId: Long): MusicInfo {
        return musicList.find {
            it.id == musicInfoId
        } ?: throw RuntimeException("MusicInfo with id $musicInfoId not found!")
    }

    override fun analyzeMusicMarkdownFiles(context: Context) {
        val storage = context.getSharedPreferences(Global.SHARED_PREFERENCES_NAME, 0)
        rootFolder = storage.getString(Global.ROOT_FOLDER_KEY, "")
        if (rootFolder.isNullOrEmpty()) {
            return
        }

        val dir = File(rootFolder + MUSIC_MARKDOWN_FOLDER)
        dir.listFiles()?.forEach { file ->
            if (file.isFile) {
                val fileText = file.readText()
                val yamlText = fileText
                    .substringAfter("---")
                    .substringBefore("---")
                if (yamlText.isNotEmpty()) {
                    val yaml = Yaml().load<Map<String, Any>>(yamlText)
                    val extension = fileText
                        .substringAfter("![[")
                        .substringBefore("]]")
                        .substringAfterLast(".")
                    val music = MusicInfo(
                        id = -1,
                        title = (yaml[MusicInfo.TITLE_KEY] ?: MusicInfo.UNKNOWN_VALUE) as String,
                        album = MusicInfo.UNKNOWN_VALUE,
                        artist = (yaml[MusicInfo.ARTIST_KEY] ?: MusicInfo.UNKNOWN_VALUE) as String,
                        extension = extension,
                        text = fileText
                    )
                    musicList.add(music)
                }
            }
        }
        updateList()
    }


    private fun updateList() {
        musicListLiveData.value = musicList.toList()
    }
}
