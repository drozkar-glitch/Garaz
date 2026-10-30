package cz.mares.garaz

expect object GaragePlatform {
    fun loadString(key: String, default: String): String
    fun saveString(key: String, value: String)
    fun currentDateString(): String
    fun currentTimeMillis(): Long
    fun copyToClipboard(text: String)
    fun selectBackupFileForSave(suggestedName: String, initialContent: String, onResult: (String?) -> Unit)
    fun selectBackupFileForOpen(onResult: (uri: String?, content: String?) -> Unit)
    fun writeBackupFileUri(uriString: String, content: String): Boolean
    fun readBackupFileUri(uriString: String): String?
    suspend fun httpGet(url: String, headers: Map<String, String> = emptyMap()): Result<String>
}
