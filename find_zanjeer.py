import urllib.request, re, urllib.parse, json

query = "Zanjeer 1973 full movie Amitabh Bachchan"
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
                print(f"ID: {vid} | Dur: {dur} | Badges: {badges} | Owner: {owner}")
