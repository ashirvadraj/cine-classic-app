import urllib.request, json

ids = [
    ('Awara', 'V88h_NvPIKU'),
    ('Mother India', '5J-TwuoKxVs'),
    ('Do Bigha Zamin', '_a5cZ6OkgmA'),
    ('Pyaasa', 'WJK45r-j6TU'),
    ('Devdas', '2r9QHR2L3_8'),
    ('Shree 420', 'qu8ShpSlFX0'),
    ('Night of the Living Dead', 'H91BxkBXttE'),
    ('Charade', 'VhGNhwOtbVs'),
    ('His Girl Friday', 'BBa_OHGmOPs'),
    ('The Stranger', 'Q-iglYhLl-8'),
    ('A Star Is Born', 'mAVEaMMQffA'),
    ('Gullivers Travels', 'rehNT9wIjUg')
]

for title, vid in ids:
    try:
        url = f'https://www.youtube.com/oembed?url=https://www.youtube.com/watch?v={vid}&format=json'
        with urllib.request.urlopen(url) as r:
            d = json.loads(r.read().decode())
            print(f"{title}: {d.get('title')}")
    except Exception as e:
        print(f"{title}: {e}")
