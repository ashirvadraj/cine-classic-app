import urllib.request, re, urllib.parse, json

def search_clean_movies(query):
    encoded = urllib.parse.quote(f"{query} full movie -trailer -review")
    url = f"https://www.youtube.com/results?search_query={encoded}&sp=EgIYAg%253D%253D"
    headers = {
        "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
        "Accept-Language": "en-US,en;q=0.9"
    }
    req = urllib.request.Request(url, headers=headers)
    try:
        with urllib.request.urlopen(req, timeout=8) as resp:
            html = resp.read().decode('utf-8', errors='ignore')
            match = re.search(r'var ytInitialData = ({.*?});</script>', html)
            if not match:
                return []
            data = json.loads(match.group(1))
            contents = data['contents']['twoColumnSearchResultsRenderer']['primaryContents']['sectionListRenderer']['contents']
            movies = []
            seen = set()
            blacklist = [
                'review', 'trailer', 'teaser', 'fact', 'facts', 'reaction', 
                'explained', 'explanation', 'analysis', 'roast', 'scene', 
                'making of', 'behind the scene', 'status', 'spoiler', 
                'preview', 'interview', 'tribute', 'parody', 'jukebox',
                'audio song', 'video song', 'full songs', 'box office'
            ]
            for section in contents:
                items = section.get('itemSectionRenderer', {}).get('contents', [])
                for item in items:
                    v = item.get('videoRenderer')
                    if not v:
                        continue
                    vid = v.get('videoId')
                    title = v.get('title', {}).get('runs', [{}])[0].get('text', '')
                    dur = v.get('lengthText', {}).get('simpleText', '')
                    badges = [b.get('metadataBadgeRenderer', {}).get('label', '') for b in v.get('badges', [])]
                    owner = v.get('ownerText', {}).get('runs', [{}])[0].get('text', '')

                    # 1. Skip paid / rental videos
                    if any(k in b.lower() for b in badges for k in ['buy', 'rent', 'purchase', 'paid']):
                        continue
                    if 'youtube movies' in owner.lower():
                        continue

                    # 2. Skip reviews, facts, trailers
                    title_lower = title.lower()
                    if any(b in title_lower for b in blacklist):
                        continue

                    # 3. Check duration: must be >= 45 minutes or 1+ hours
                    # Formats: H:MM:SS or MM:SS
                    parts = dur.split(':')
                    if len(parts) == 3:
                        # At least 1 hour
                        total_mins = int(parts[0]) * 60 + int(parts[1])
                    elif len(parts) == 2:
                        total_mins = int(parts[0])
                    else:
                        total_mins = 0

                    if total_mins < 45:
                        continue

                    if vid not in seen:
                        seen.add(vid)
                        movies.append({
                            "id": vid,
                            "title": title,
                            "dur": dur,
                            "channel": owner
                        })
            return movies
    except Exception as e:
        print(f"Error {query}: {e}")
        return []

for test_q in ["Zanjeer", "Don 1978", "Casablanca", "Sholay"]:
    res = search_clean_movies(test_q)
    print(f"\n--- Results for '{test_q}' ({len(res)} found) ---")
    for m in res[:3]:
        print(f"[{m['dur']}] {m['title'][:55]} | Channel: {m['channel']}")
