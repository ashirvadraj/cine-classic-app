package com.cineclassic.app.data

data class CinemaTrivia(
    val id: String,
    val movieTitle: String,
    val category: String,
    val fact: String,
    val relatedMovieId: String? = null,
    val badge: String = "DID YOU KNOW?"
)

object CinemaTriviaProvider {

    private val triviaList = listOf(
        CinemaTrivia(
            id = "t_zanjeer_1973",
            movieTitle = "Zanjeer (1973)",
            category = "Classic Bollywood",
            fact = "The script of Zanjeer was rejected by top stars of the era including Dharmendra, Dev Anand, and Raaj Kumar because the hero had no songs and smiled only once. Director Prakash Mehra took a gamble by casting Amitabh Bachchan, birthing the legendary 'Angry Young Man' persona that transformed Indian cinema.",
            relatedMovieId = "hindi_zanjeer_1973",
            badge = "TURNING POINT OF CINEMA"
        ),
        CinemaTrivia(
            id = "t_sholay",
            movieTitle = "Sholay (1975)",
            category = "Classic Bollywood",
            fact = "Amjad Khan was almost dropped from playing Gabbar Singh because writer Javed Akhtar felt his voice was too soft. Khan practiced tirelessly, drawing inspiration from real Chambal dacoits, and delivered one of the most iconic villain performances in cinema history.",
            relatedMovieId = null,
            badge = "BEHIND THE SCENES"
        ),
        CinemaTrivia(
            id = "t_mughal_e_azam",
            movieTitle = "Mughal-e-Azam (1960)",
            category = "Golden Era Classic",
            fact = "Director K. Asif spent over two years and an unprecedented fortune building the Sheesh Mahal (Palace of Mirrors) set. He imported Belgian glass and recruited specialized craftsmen from Firozabad to produce the legendary reflections in 'Pyar Kiya To Darna Kya'.",
            relatedMovieId = null,
            badge = "EPIC SCALE"
        ),
        CinemaTrivia(
            id = "t_mother_india",
            movieTitle = "Mother India (1957)",
            category = "Academy Award Nominee",
            fact = "Mother India was India's first ever submission to the Academy Awards for Best Foreign Language Film. In the final voting round, it lost the Oscar by just a single vote to Federico Fellini's 'Nights of Cabiria'.",
            relatedMovieId = "hindi_02",
            badge = "OSCAR RECORD"
        ),
        CinemaTrivia(
            id = "t_pyaasa",
            movieTitle = "Pyaasa (1957)",
            category = "Golden Era Classic",
            fact = "Guru Dutt originally offered the lead role of the poet Vijay to Dilip Kumar. When Dilip Kumar declined fearing it was too close to his Devdas character, Guru Dutt played the protagonist himself, creating what Time Magazine ranked among the 100 greatest films of all time.",
            relatedMovieId = "hindi_04",
            badge = "TIME 100 MASTERPIECE"
        ),
        CinemaTrivia(
            id = "t_awara",
            movieTitle = "Awara (1951)",
            category = "Global Phenomenon",
            fact = "Raj Kapoor's 'Awara' became an international sensation across the Soviet Union, China, Turkey, and the Middle East. Chairman Mao Zedong was reportedly a huge fan of the film, and the title track 'Awara Hoon' was sung by fans across streets in Moscow.",
            relatedMovieId = "hindi_01",
            badge = "GLOBAL SENSATION"
        ),
        CinemaTrivia(
            id = "t_ddlj",
            movieTitle = "Dilwale Dulhania Le Jayenge (1995)",
            category = "Romance Classic",
            fact = "DDLJ holds the world record for the longest-running theatrical film in cinema history, screening continuously at Mumbai's Maratha Mandir theatre for over 28 years (more than 1,400 weeks) without interruption.",
            relatedMovieId = "hindi_ddlj_1995",
            badge = "WORLD RECORD"
        ),
        CinemaTrivia(
            id = "t_anand",
            movieTitle = "Anand (1971)",
            category = "Emotional Masterpiece",
            fact = "Director Hrishikesh Mukherjee wrote Anand inspired by his real-life anxiety of losing his dearest friend Raj Kapoor. The dialogue 'Babumoshai, zindagi badi honi chahiye, lambi nahi' remains one of the most quoted philosophical lines in film history.",
            relatedMovieId = null,
            badge = "CINEMA PHILOSOPHY"
        ),
        CinemaTrivia(
            id = "t_deewaar",
            movieTitle = "Deewaar (1975)",
            category = "Salim-Javed Masterpiece",
            fact = "The unforgettable confrontation scene between brothers Vijay and Ravi under the bridge ('Mere paas maa hai') was written in one inspired sitting by Salim-Javed. It cemented Amitabh Bachchan and Shashi Kapoor as the ultimate cinematic duo.",
            relatedMovieId = null,
            badge = "TIMELESS DIALOGUE"
        ),
        CinemaTrivia(
            id = "t_pakeezah",
            movieTitle = "Pakeezah (1972)",
            category = "Cult Classic",
            fact = "Director Kamal Amrohi took 14 years to complete Pakeezah. It became legendary actress Meena Kumari's swan song, celebrated for its haunting classical soundtrack and mesmerizing ghazals.",
            relatedMovieId = null,
            badge = "14 YEARS IN THE MAKING"
        ),
        CinemaTrivia(
            id = "t_don_1978",
            movieTitle = "Don (1978)",
            category = "Action Classic",
            fact = "The immortal song 'Khaike Paan Banaraswala' was not in the original movie! It was composed for a Dev Anand film, but when Dev Anand dropped it, director Chandra Barot added it to Don at the last minute to lighten the intense second half.",
            relatedMovieId = null,
            badge = "LAST MINUTE HIT"
        ),
        CinemaTrivia(
            id = "t_gol_maal",
            movieTitle = "Gol Maal (1979)",
            category = "Classic Comedy",
            fact = "Hrishikesh Mukherjee's classic comedy required Amol Palekar to shave his real mustache for the fake double identity of Ramprasad and Lakshmanprasad. Utpal Dutt won Best Comedian for his obsession with traditional values and mustaches.",
            relatedMovieId = null,
            badge = "COMEDY BENCHMARK"
        ),
        CinemaTrivia(
            id = "t_charade",
            movieTitle = "Charade (1963)",
            category = "Vintage Hollywood",
            fact = "Often celebrated as 'the best Hitchcock movie that Hitchcock never made', Cary Grant was self-conscious about the 25-year age gap between him and Audrey Hepburn. Screenwriter Peter Stone solved this by rewriting the banter so Audrey's character pursued him instead.",
            relatedMovieId = "eng_02",
            badge = "VINTAGE HOLLYWOOD"
        ),
        CinemaTrivia(
            id = "t_night_of_living_dead",
            movieTitle = "Night of the Living Dead (1968)",
            category = "Horror Landmark",
            fact = "George A. Romero created the modern zombie genre on a $114,000 budget using chocolate syrup for blood. Due to a clerical error where the distributor omitted the copyright symbol when renaming the film, it immediately entered the public domain upon release!",
            relatedMovieId = "eng_01",
            badge = "CULT REVOLUTION"
        ),
        CinemaTrivia(
            id = "t_his_girl_friday",
            movieTitle = "His Girl Friday (1940)",
            category = "Screwball Comedy",
            fact = "Director Howard Hawks pioneered overlapping dialogue in cinema with this film. The cast spoke at a record-breaking 240 words per minute—nearly double normal conversation speed—to achieve its breathless, razor-sharp comic tempo.",
            relatedMovieId = "eng_03",
            badge = "RECORD TEMPO"
        ),
        CinemaTrivia(
            id = "t_a_star_is_born",
            movieTitle = "A Star Is Born (1937)",
            category = "Technicolor Pioneer",
            fact = "The original 1937 drama starring Janet Gaynor and Fredric March was the very first color film nominated for Best Picture. It created a timeless Hollywood archetype that has been officially remade in 1954, 1976, and 2018.",
            relatedMovieId = "eng_05",
            badge = "ORIGINAL CLASSIC"
        ),
        CinemaTrivia(
            id = "t_veer_zaara",
            movieTitle = "Veer-Zaara (2004)",
            category = "Musical Masterpiece",
            fact = "The soundtrack for Veer-Zaara was built around unreleased melodies composed by the legendary Madan Mohan, who had passed away 29 years earlier in 1975. Lata Mangeshkar recorded the vocals at age 75 with deep emotional reverence for her mentor.",
            relatedMovieId = "hindi_veer_zaara_2004",
            badge = "UNRELEASED MELODIES"
        ),
        CinemaTrivia(
            id = "t_do_bigha_zamin",
            movieTitle = "Do Bigha Zamin (1953)",
            category = "Neorealist Landmark",
            fact = "Inspired by Vittorio De Sica's 'Bicycle Thieves', director Bimal Roy insisted on complete realism. Star Balraj Sahni spent weeks training with real hand-rickshaw pullers on Kolkata's blistering cobblestone streets to prepare for the role.",
            relatedMovieId = "hindi_03",
            badge = "NEOREALIST MILESTONE"
        ),
        CinemaTrivia(
            id = "t_shree_420",
            movieTitle = "Shree 420 (1955)",
            category = "Golden Era Classic",
            fact = "The timeless black umbrella sequence in 'Pyar Hua Iqrar Hua' took 10 consecutive night shoots under artificial rain. It became one of the most recognizable and enduring visual symbols of romance in world cinema.",
            relatedMovieId = "hindi_06",
            badge = "ICONIC VISUAL"
        ),
        CinemaTrivia(
            id = "t_the_stranger",
            movieTitle = "The Stranger (1946)",
            category = "Film Noir",
            fact = "Directed by and starring Orson Welles, this intense noir thriller was Welles's only commercial box office hit upon initial release. It was also one of the first Hollywood films to show documentary footage of WW2 concentration camps.",
            relatedMovieId = "eng_04",
            badge = "ORSON WELLES CLASSIC"
        )
    )

    fun getAllTrivia(): List<CinemaTrivia> = triviaList

    fun getRandomTrivia(excludeId: String? = null): CinemaTrivia {
        val candidates = if (excludeId != null && triviaList.size > 1) {
            triviaList.filter { it.id != excludeId }
        } else {
            triviaList
        }
        return candidates.random()
    }

    fun getTriviaForMovie(movieId: String): CinemaTrivia? {
        return triviaList.find { it.relatedMovieId == movieId }
    }

    fun getTriviaForTitle(title: String): CinemaTrivia? {
        val cleanTitle = title.lowercase()
        return triviaList.find { cleanTitle.contains(it.movieTitle.lowercase().take(6)) }
    }
}
