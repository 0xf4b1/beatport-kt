import com.tiefensuche.beatport.api.BeatportApi
import com.tiefensuche.beatport.api.Playlist
import com.tiefensuche.beatport.api.Track
import kotlin.test.Test

class Test {

    val api = BeatportApi(BeatportApi.Session("clientId", "redirectUri", null))

    init {
        api.session.accessToken = "accessToken"
        api.session.refreshToken = "refreshToken"
    }

    private fun printTracks(tracks: List<Track>) {
        tracks.forEach { println("id: ${it.id}, artist: ${it.artist}, title: ${it.title}, duration: ${it.duration}, artwork: ${it.artwork}, url: ${it.url}") }
    }

    private fun printPlaylists(playlists: List<Playlist>) {
        playlists.forEach { println("uuid: ${it.uuid}, title: ${it.title}") }
    }

    @Test
    fun testGetGenres() {
        val genres = api.getGenres(false)
        assert(genres.isNotEmpty())
        for (genre in genres) {
            println(genre.name)
        }
    }

    @Test
    fun testGetTracksForGenre() {
        val genres = api.getGenres(false)
        assert(genres.isNotEmpty())
        val tracks = api.getTracks(genres.first().id.toString(), false)
        assert(tracks.isNotEmpty())
        printTracks(tracks)
    }

    @Test
    fun testGetTop100ForGenre() {
        val genres = api.getGenres(false)
        assert(genres.isNotEmpty())
        val tracks = api.getTop100(genres.first().id.toString(), false)
        assert(tracks.isNotEmpty())
        printTracks(tracks)
    }

    @Test
    fun getCuratedPlaylistsForGenre() {
        val genres = api.getGenres(false)
        assert(genres.isNotEmpty())
        val playlists = api.getCuratedPlaylists(genres.first().id.toString(), false)
        assert(playlists.isNotEmpty())
        printPlaylists(playlists)
    }

    @Test
    fun getCuratedPlaylist() {
        val genres = api.getGenres(false)
        assert(genres.isNotEmpty())
        val playlists = api.getCuratedPlaylists(genres.first().id.toString(), false)
        assert(playlists.isNotEmpty())
        val tracks = api.getCuratedPlaylist(playlists.first().uuid.toString(), false)
        assert(tracks.isNotEmpty())
        printTracks(tracks)
    }

    @Test
    fun getUserPlaylists() {
        val playlists = api.getMyPlaylists(false)
        assert(playlists.isNotEmpty())
        printPlaylists(playlists)
    }

    @Test
    fun getUserPlaylist() {
        val playlists = api.getMyPlaylists(false)
        assert(playlists.isNotEmpty())
        val tracks = api.getPlaylist(playlists.first().uuid.toString(), false)
        assert(tracks.isNotEmpty())
        printTracks(tracks)
    }

    @Test
    fun testQuery() {
        val tracks = api.query("Solee", false)
        assert(tracks.isNotEmpty())
        printTracks(tracks)
    }
}