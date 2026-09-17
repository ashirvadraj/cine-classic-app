import urllib.request, re, urllib.parse, json

def find_verified_movie(title_query):
    url = f"https://www.youtube.com/results?search_query={urllib.parse.quote(title_query)}&sp=EgIYAg%253D%253D"
    headers = {
        "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36",
        "Accept-Language": "en-US,en;q=0.9"
    }
    req = urllib.request.Request(url, headers=headers)
    with urllib.request.urlopen(req, timeout=10) as resp:
        html = resp.read().decode('utf-8', errors='ignore')
    
    match = re.search(r'var ytInitialData = ({.*?});</script>', html)
    if not match:
        return None
    data = json.loads(match.group(1))
    contents = data['contents']['twoColumnSearchResultsRenderer']['primaryContents']['sectionListRenderer']['contents']
    for section in contents:
        items = section.get('itemSectionRenderer', {}).get('contents', [])
        for item in items:
            v = item.get('videoRenderer')
            if v:
                vid = v.get('videoId')
                title = v.get('title', {}).get('runs', [{}])[0].get('text', '')
                dur = v.get('lengthText', {}).get('simpleText', '')
                badges = [b.get('metadataBadgeRenderer', {}).get('label', '') for b in v.get('badges', [])]
                owner = v.get('ownerText', {}).get('runs', [{}])[0].get('text', '')
                
                # Check badges for paid
                if any('buy' in b.lower() or 'rent' in b.lower() or 'paid' in b.lower() for b in badges):
                    continue
                if 'youtube movies' in owner.lower():
                    continue

                # Check duration > 1 hr
                parts = dur.split(':')
                if len(parts) >= 3 or (len(parts) == 2 and int(parts[0]) >= 60):
                    # Verify oembed works
                    try:
                        oembed = f"https://www.youtube.com/oembed?url=https://www.youtube.com/watch?v={vid}&format=json"
                        with urllib.request.urlopen(oembed, timeout=3) as o_resp:
                            if o_resp.status == 200:
                                return {"id": vid, "title": title, "dur": dur, "channel": owner}
                    except:
                        pass
    return None

targets = [
    "Mother India full movie hindi",
    "Pyaasa 1957 full movie guru dutt",
    "Devdas 1955 full movie dilip kumar",
    "Zanjeer full movie hindi"
]

for t in targets:
    res = find_verified_movie(t)
    print(t, "->", res)
