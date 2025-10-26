import requests

SPOTIFY_TOKEN = "TU_ACCESS_TOKEN"

def get_top10_playlist(playlist_id="37i9dQZF1DXcBWIGoYBM5M"):
    url = f"https://api.spotify.com/v1/playlists/{playlist_id}/tracks"
    headers = {"Authorization": f"Bearer {SPOTIFY_TOKEN}"}
    resp = requests.get(url, headers=headers)
    if resp.status_code != 200:
        return []
    data = resp.json()['items'][:10]
    top10 = []
    for idx, item in enumerate(data):
        track = item['track']
        top10.append({
            "rank": idx + 1,
            "name": track['name'],
            "artist": track['artists'][0]['name'],
            "preview_url": track['preview_url'],
            "spotify_url": track['external_urls']['spotify']
        })
    return top10
