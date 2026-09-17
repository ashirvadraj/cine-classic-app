import java.net.URL
import java.net.HttpURLConnection
import java.net.URLEncoder

fun searchYouTube(query: String) {
    val encoded = URLEncoder.encode("$query full movie", "UTF-8")
    val url = URL("https://www.youtube.com/results?search_query=$encoded")
    val conn = url.openConnection() as HttpURLConnection
    conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 10; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36")
    conn.connectTimeout = 10000
    conn.readTimeout = 10000
    val html = conn.inputStream.bufferedReader().use { it.readText() }
    
    val regex = Regex(""""videoId":"([a-zA-Z0-9_-]{11})".*?"title":\{"runs":\[\{"text":"(.*?)"\}\]\}""")
    val matches = regex.findAll(html).take(10).toList()
    println("Found ${matches.size} matches:")
    for (m in matches) {
        val vid = m.groupValues[1]
        val title = m.groupValues[2]
        println("ID: $vid | Title: $title")
    }
}
searchYouTube("Zanjeer")
