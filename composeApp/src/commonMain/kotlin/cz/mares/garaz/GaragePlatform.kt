package cz.mares.garaz

expect object GaragePlatform {
    fun loadString(key: String, default: String): String
    fun saveString(key: String, value: String)
    fun currentDateString(): String
    fun copyToClipboard(text: String)
    suspend fun httpGet(url: String, headers: Map<String, String> = emptyMap()): Result<String>
}
