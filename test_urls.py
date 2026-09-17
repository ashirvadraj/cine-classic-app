import urllib.request, json, urllib.parse

with open('C:/Users/ASHIR/.gemini/antigravity/scratch/cine-classic-app/app/src/main/assets/movies.json', 'r', encoding='utf-8') as f:
    movies = json.load(f)

for m in movies:
    url = m['videoUrl']
    req = urllib.request.Request(url, headers={'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64)'})
    try:
        with urllib.request.urlopen(req, timeout=6) as r:
            print(f"OK: {m['title']} -> {r.status}")
    except Exception as e:
        print(f"FAIL: {m['title']} -> {e}")
