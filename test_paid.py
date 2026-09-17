import urllib.request, re, urllib.parse, json

query = "Zanjeer movie"
url = f"https://www.youtube.com/results?search_query={urllib.parse.quote(query)}"
headers = {
    "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
    "Accept-Language": "en-US,en;q=0.9"
}
req = urllib.request.Request(url, headers=headers)
with urllib.request.urlopen(req, timeout=10) as resp:
    html = resp.read().decode('utf-8', errors='ignore')

match = re.search(r'var ytInitialData = ({.*?});</script>', html)
if match:
    data = json.loads(match.group(1))
    contents = data['contents']['twoColumnSearchResultsRenderer']['primaryContents']['sectionListRenderer']['contents']
    for section in contents:
        items = section.get('itemSectionRenderer', {}).get('contents', [])
        for item in items:
            # Check movieRenderer or compactMovieRenderer
            m = item.get('movieRenderer') or item.get('compactMovieRenderer')
            if m:
                print("FOUND PAID MOVIE RENDERER:", m.get('videoId'), m.get('title', {}).get('simpleText'))
            v = item.get('videoRenderer')
            if v:
                badges = [b.get('metadataBadgeRenderer', {}).get('label', '') for b in v.get('badges', [])]
                if any('buy' in b.lower() or 'rent' in b.lower() or 'purchase' in b.lower() for b in badges):
                    print("FOUND PAID VIDEO:", v.get('videoId'), badges)
