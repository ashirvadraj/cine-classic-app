import urllib.request, re, json, urllib.parse

query = "Zanjeer 1973 full movie"
url = "https://www.youtube.com/results?search_query=" + urllib.parse.quote(query)
headers = {
    "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
}
req = urllib.request.Request(url, headers=headers)
try:
    with urllib.request.urlopen(req, timeout=10) as resp:
        html = resp.read().decode('utf-8', errors='ignore')
        # Find ytInitialData
        match = re.search(r'var ytInitialData = ({.*?});</script>', html)
        if match:
            data = json.loads(match.group(1))
            contents = data['contents']['twoColumnSearchResultsRenderer']['primaryContents']['sectionListRenderer']['contents']
            videos = []
            for section in contents:
                items = section.get('itemSectionRenderer', {}).get('contents', [])
                for item in items:
                    v = item.get('videoRenderer')
                    if v:
                        vid = v.get('videoId')
                        title = v.get('title', {}).get('runs', [{}])[0].get('text')
                        channel = v.get('ownerText', {}).get('runs', [{}])[0].get('text')
                        duration = v.get('lengthText', {}).get('simpleText')
                        thumb = v.get('thumbnail', {}).get('thumbnails', [{}])[-1].get('url')
                        videos.append({"id": vid, "title": title, "channel": channel, "duration": duration, "thumb": thumb})
            print("Found", len(videos), "videos:")
            for v in videos[:5]:
                print(v)
        else:
            print("ytInitialData not found")
except Exception as e:
    print("Error:", e)
