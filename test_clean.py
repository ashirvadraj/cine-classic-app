import urllib.request, re, urllib.parse, json

query = "Zanjeer full movie"
url = f"https://www.youtube.com/results?search_query={urllib.parse.quote(query)}&sp=EgIYAg%253D%253D"
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
            v = item.get('videoRenderer')
            if v:
                vid = v.get('videoId')
                title = v.get('title', {}).get('runs', [{}])[0].get('text', '')
                dur = v.get('lengthText', {}).get('simpleText', '')
                badges = [b.get('metadataBadgeRenderer', {}).get('label', '') for b in v.get('badges', [])]
                owner = v.get('ownerText', {}).get('runs', [{}])[0].get('text', '')
                
                # Check for paid
                is_paid = any('buy' in b.lower() or 'rent' in b.lower() or 'purchase' in b.lower() for b in badges)
                # Check for review/trailer
                is_review = any(k in title.lower() for k in ['review', 'trailer', 'teaser', 'fact', 'reaction', 'explained', 'analysis', 'scene', 'making'])
                
                print(f"ID: {vid} | Dur: {dur} | Paid: {is_paid} | Review: {is_review} | Title: {title.encode('ascii', 'replace').decode()[:50]}")
