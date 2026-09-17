import urllib.request, re, urllib.parse

query = "Zanjeer full movie"
url = "https://www.youtube.com/results?search_query=" + urllib.parse.quote(query)
headers = {
    "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
    "Accept-Language": "en-US,en;q=0.9"
}
req = urllib.request.Request(url, headers=headers)
with urllib.request.urlopen(req, timeout=10) as resp:
    html = resp.read().decode('utf-8', errors='ignore')

matches = re.findall(r'"videoId":"([a-zA-Z0-9_-]{11})".*?"title":\{"runs":\[\{"text":"(.*?)"\}\]', html)
print(f"Total raw matches: {len(matches)}")
seen = set()
for vid, title in matches:
    if vid not in seen:
        seen.add(vid)
        print(f"ID: {vid} | Title: {title.encode('ascii', 'replace').decode()}")
