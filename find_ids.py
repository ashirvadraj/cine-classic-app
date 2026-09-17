import urllib.request, re, urllib.parse, json

queries = [
    ("Awara 1951 full movie hindi", "Awara"),
    ("Mother India 1957 full movie hindi", "Mother India"),
    ("Do Bigha Zamin 1953 full movie", "Do Bigha Zamin"),
    ("Pyaasa 1957 full movie hindi", "Pyaasa"),
    ("Devdas 1955 full movie hindi", "Devdas"),
    ("Shree 420 1955 full movie hindi", "Shree 420"),
    ("Zanjeer 1973 full movie hindi", "Zanjeer"),
    ("Night of the Living Dead 1968 full movie", "Night of the Living Dead"),
    ("Charade 1963 full movie cary grant", "Charade"),
    ("His Girl Friday 1940 full movie", "His Girl Friday"),
    ("The Stranger 1946 orson welles full movie", "The Stranger"),
    ("A Star Is Born 1937 full movie", "A Star Is Born"),
    ("Gulliver's Travels 1939 full movie", "Gullivers Travels")
]

results = {}
headers = {
    "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
    "Accept-Language": "en-US,en;q=0.9"
}

for q, key in queries:
    url = "https://www.youtube.com/results?search_query=" + urllib.parse.quote(q)
    req = urllib.request.Request(url, headers=headers)
    try:
        with urllib.request.urlopen(req, timeout=6) as resp:
            html = resp.read().decode('utf-8', errors='ignore')
            matches = re.findall(r'"videoId":"([a-zA-Z0-9_-]{11})".*?"title":\{"runs":\[\{"text":"(.*?)"\}\]', html)
            for vid, title in matches:
                # filter out non-movie items
                if len(vid) == 11:
                    results[key] = vid
                    print(f"{key} -> {vid} ({title[:30]})")
                    break
    except Exception as e:
        print(f"Failed {key}: {e}")

print(json.dumps(results, indent=2))
