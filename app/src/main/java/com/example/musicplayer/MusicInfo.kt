package com.example.musicplayer

import java.util.Date
import java.text.SimpleDateFormat
import java.util.Locale

data class MusicInfo(
    val created: Date = Date(),
    val aliases: List<String> = emptyList(),
    val cover: String = "",
    val creator: List<String> = emptyList(),
    val album: String = "",
    val year: Int = 0,
    val sourceFile: String = "",
    val trackNumber: Int = 0,
) {
    companion object {
        const val CREATED_KEY = "created"
        const val ALIASES_KEY = "aliases"
        const val COVER_KEY = "Cover"
        const val CREATOR_KEY = "creator"
        const val ALBUM_KEY = "Album"
        const val YEAR_KEY = "Year"
        const val SOURCE_FILE_KEY = "SourceFile"
        const val TRACK_NUMBER_KEY = "Index"
    }
}

/**
 * Parses YAML front matter from markdown content
 * Expected format:
 * ---
 * created: 2025-02-03 08:18:16
 * aliases:
 *   - Knew day
 * Cover: "[[cover name.jpg]]"
 * Album:
 *   - "[[album name]]"
 * creator:
 *   - "[[Artist Name]]"
 * Year: 2023
 * TrackNumber: 1
 * ---
 */
fun parseYamlFrontMatter(markdownContent: String): Map<String, String> {
    val yamlMap = mutableMapOf<String, String>()

    // Check if the content starts with YAML front matter
    if (markdownContent.startsWith("---")) {
        val lines = markdownContent.split("\n")
        var yamlEndIndex = -1

        // Find the end of YAML front matter (second occurrence of "---")
        for (i in 1 until lines.size) {
            if (lines[i].trim() == "---") {
                yamlEndIndex = i
                break
            }
        }

        if (yamlEndIndex > 0) {
            // Extract YAML content between the "---" markers
            val yamlLines = lines.slice(1 until yamlEndIndex)

            var i = 0
            while (i < yamlLines.size) {
                val line = yamlLines[i].trim()

                if (line.isNotEmpty() && line.contains(":")) {
                    val parts = line.split(":", limit = 2)
                    val key = parts[0].trim()
                    var value = if (parts.size > 1) parts[1].trim() else ""

                    // Handle multi-line values and arrays
                    if (value.isEmpty() || value == "-") {
                        // This could be an array or multi-line value
                        i++
                        val arrayValues = mutableListOf<String>()

                        // Collect array items
                        while (i < yamlLines.size && yamlLines[i].trim().startsWith("  -")) {
                            val item = yamlLines[i].substring(4).trim() // Remove "  - " prefix
                            val cleanedItem = item
                                .removePrefix("\"").removeSuffix("\"")
                                .removePrefix("[[").removeSuffix("]]")
                            arrayValues.add(cleanedItem)
                            i++
                        }
                        // For arrays, join the values with a special separator to handle in conversion
                        value = if (arrayValues.isNotEmpty())
                            arrayValues.joinToString("|||")
                            else ""
                        i-- // Adjust index since we incremented inside the loop
                    } else {
                        // Clean up regular values
                        value = value
                            .removePrefix("\"").removeSuffix("\"")
                            .removePrefix("[[").removeSuffix("]]")
                    }
                    yamlMap[key] = value
                }
                i++
            }
        }
    }

    return yamlMap
}

/**
 * Creates a MusicInfo object from markdown content with YAML front matter
 */
fun createMusicInfoFromMarkdown(markdownContent: String, sourceFilePath: String = ""): MusicInfo {
    val yamlData = parseYamlFrontMatter(markdownContent)

    return MusicInfo(
        album = yamlData[MusicInfo.ALBUM_KEY] ?: "",
        year = yamlData[MusicInfo.YEAR_KEY]?.toIntOrNull() ?: 0,
        sourceFile = if (yamlData[MusicInfo.SOURCE_FILE_KEY].isNullOrEmpty()) sourceFilePath else yamlData[MusicInfo.SOURCE_FILE_KEY]!!,
        trackNumber = yamlData[MusicInfo.TRACK_NUMBER_KEY]?.toIntOrNull() ?: 0,
        created = try {
            val createdStr = yamlData[MusicInfo.CREATED_KEY]
            if (!createdStr.isNullOrEmpty()) {
                SimpleDateFormat(
                    "yyyy-MM-dd HH:mm:ss",
                    Locale.getDefault()
                ).parse(createdStr)
            } else {
                Date()
            }
        } catch (_: Exception) {
            Date()
        },
        aliases = if (yamlData[MusicInfo.ALIASES_KEY].isNullOrEmpty()) {
            emptyList()
        } else {
            yamlData[MusicInfo.ALIASES_KEY]!!.split("|||") // Split back the joined array values
        },
        cover = yamlData[MusicInfo.COVER_KEY] ?: "",
        creator = if (yamlData[MusicInfo.CREATOR_KEY].isNullOrEmpty()) {
            emptyList()
        } else {
            yamlData[MusicInfo.CREATOR_KEY]!!.split("|||")
        }
    )
}