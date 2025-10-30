package com.example.musicplayer

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.documentfile.provider.DocumentFile
import java.io.BufferedReader
import java.io.InputStreamReader
import androidx.core.net.toUri

class MarkdownMusicReader {
    
    /**
     * Reads all markdown files from a given directory URI and extracts music info
     */
    fun readMusicInfoFromDirectory(context: Context, directoryUri: Uri): List<MusicInfo> {
        val musicInfoList = mutableListOf<MusicInfo>()
        
        try {
            val documentFile = DocumentFile.fromTreeUri(context, directoryUri)
            if (documentFile?.isDirectory == true) {
                val files = documentFile.listFiles()
                
                for (file in files) {
                    if (file.isFile && file.name?.endsWith(".md") == true) {
                        val musicInfo = readMusicInfoFromFile(context, file.uri)
                        if (musicInfo != MusicInfo()) {
                            musicInfoList.add(musicInfo)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("MarkdownMusicReader", "Error reading music info from directory", e)
        }
        
        return musicInfoList
    }
    
    /**
     * Reads a single markdown file and extracts music info from YAML front matter
     */
    fun readMusicInfoFromFile(context: Context, fileUri: Uri): MusicInfo {
        return try {
            context.contentResolver.openInputStream(fileUri)?.use { inputStream ->
                val content = BufferedReader(InputStreamReader(inputStream)).use { reader ->
                    reader.readText()
                }
                val documentFile = DocumentFile.fromSingleUri(context, fileUri)
                createMusicInfoFromMarkdown(content, documentFile?.name ?: fileUri.toString())
            } ?: MusicInfo()
        } catch (e: Exception) {
            Log.e("MarkdownMusicReader", "Error reading music info from file: $fileUri", e)
            MusicInfo()
        }
    }
    
    /**
     * Scans the notePath for markdown files with music info
     */
    fun scanNotePathForMusicInfo(context: Context): List<MusicInfo> {
        val sharedPreferences = context.getSharedPreferences("music_player_prefs", Context.MODE_PRIVATE)
        val notePathString = sharedPreferences.getString("note_path", null)
        
        return if (!notePathString.isNullOrEmpty()) {
            val notePathUri = notePathString.toUri()
            readMusicInfoFromDirectory(context, notePathUri)
        } else {
            emptyList()
        }
    }
}