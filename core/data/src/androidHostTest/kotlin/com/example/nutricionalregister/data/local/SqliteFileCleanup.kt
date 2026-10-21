package com.example.nutricionalregister.data.local

import java.io.File

/**
 * Removes the SQLite sidecar files a persistent test database leaves behind.
 *
 * The sidecars are created next to the database file and would otherwise
 * accumulate in the temporary directory between test runs.
 */
internal object SqliteFileCleanup {
    fun delete(file: File) {
        File("${file.path}-journal").delete()
        File("${file.path}-shm").delete()
        File("${file.path}-wal").delete()
    }
}
