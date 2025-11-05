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
            if (documentFile?.isDirectory == true && documentFile.exists()) {
                val files = documentFile.listFiles()
                
                for (file in files) {
                    if (file.isFile && file.name?.endsWith(".md") == true) {
                        val musicInfo = readMusicInfoFromFile(context, file.uri)
                        if (musicInfo != MusicInfo()) {
                            musicInfoList.add(musicInfo)
                        }
                    }
                }
            } else {
                Log.e("MarkdownMusicReader", "Directory does not exist or is not accessible: $directoryUri")
            }
        } catch (e: SecurityException) {
            Log.e("MarkdownMusicReader", "Permission denied accessing directory: $directoryUri", e)
        } catch (e: Exception) {
            Log.e("MarkdownMusicReader", "Error reading music info from directory: $directoryUri", e)
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
                createMusicInfoFromMarkdown(content)
            } ?: MusicInfo()
        } catch (e: SecurityException) {
            Log.e("MarkdownMusicReader", "Permission denied accessing file: $fileUri", e)
            MusicInfo()
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
            try {
                val documentFile = DocumentFile.fromTreeUri(context, notePathUri)
                if (documentFile?.exists() == true) {
                    readMusicInfoFromDirectory(context, notePathUri)
                } else {
                    Log.e("MarkdownMusicReader", "Directory does not exist or is not accessible: $notePathUri")
                    emptyList()
                }
            } catch (e: SecurityException) {
                Log.e("MarkdownMusicReader", "Permission denied accessing directory: $notePathUri", e)
                emptyList()
            } catch (e: Exception) {
                Log.e("MarkdownMusicReader", "Error accessing directory: $notePathUri", e)
                emptyList()
            }
        } else {
            emptyList()
        }
    }
    
    /**
     * Scans the notePath for markdown files with music info and provides incremental updates
     */
    suspend fun scanNotePathForMusicInfoIncremental(
        context: Context,
        onMusicInfoFound: suspend (MusicInfo) -> Unit
    ) {
        val sharedPreferences = context.getSharedPreferences("music_player_prefs", Context.MODE_PRIVATE)
        val notePathString = sharedPreferences.getString("note_path", null)
        Log.d("TAG", "Note Path String: $notePathString")

        if (!notePathString.isNullOrEmpty()) {
            val notePathUri = notePathString.toUri()
            Log.d("TAG", "Note Path Uri: ${notePathUri.path}")
            try {
                val documentFile = DocumentFile.fromTreeUri(context, notePathUri)
                Log.d("TAG", "Document file: ${documentFile?.name}")
                if (documentFile?.isDirectory == true && documentFile.exists()) {
                    val files = documentFile.listFiles()
                    
                    for (file in files) {
                        if (file.isFile && file.name?.endsWith(".md") == true) {
                            val musicInfo = readMusicInfoFromFile(context, file.uri)
                            if (musicInfo != MusicInfo()) {
                                onMusicInfoFound(musicInfo)
                            }
                        }
                    }
                } else {
                    Log.e("MarkdownMusicReader", "Directory does not exist or is not accessible: $notePathUri")
                }
            } catch (e: SecurityException) {
                Log.e("MarkdownMusicReader", "Permission denied accessing directory: $notePathUri", e)
            } catch (e: Exception) {
                Log.e("MarkdownMusicReader", "Error accessing directory: $notePathUri", e)
            }
        }
    }
}