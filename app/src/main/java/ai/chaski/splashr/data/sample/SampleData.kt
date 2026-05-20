package ai.chaski.splashr.data.sample

import ai.chaski.splashr.data.model.Orientation
import ai.chaski.splashr.data.model.Photo
import ai.chaski.splashr.data.model.Photographer
import ai.chaski.splashr.data.model.Topic

/**
 * Built-in sample catalogue. This is what makes Splashr run with zero setup —
 * no API keys, no accounts. The [ai.chaski.splashr.data.repository.PhotoRepository]
 * abstraction means this can later be swapped for a real Unsplash/Pexels backend.
 *
 * Images are served by picsum.photos using stable seeds, so every photo resolves
 * to a consistent, real image without authentication.
 */
object SampleData {

    val photographers: List<Photographer> = listOf(
        Photographer(
            "pg-alex", "Alex Rivera", "alexrivera",
            "Travel photographer chasing soft light across coastlines and old towns.",
            avatar("alexrivera"), "Lisbon, Portugal", 248,
        ),
        Photographer(
            "pg-maya", "Maya Chen", "mayachen",
            "Documenting the texture and rhythm of cities after dark.",
            avatar("mayachen"), "Tokyo, Japan", 412,
        ),
        Photographer(
            "pg-diego", "Diego Santos", "diegosantos",
            "Wilderness and wildlife from the far south of the Americas.",
            avatar("diegosantos"), "Patagonia, Chile", 305,
        ),
        Photographer(
            "pg-nora", "Nora Adebayo", "noraadebayo",
            "Portrait and people photography centered on everyday joy.",
            avatar("noraadebayo"), "Lagos, Nigeria", 189,
        ),
        Photographer(
            "pg-liam", "Liam O'Connor", "liamoconnor",
            "Ocean, weather, and the moods of the Atlantic coast.",
            avatar("liamoconnor"), "Galway, Ireland", 276,
        ),
        Photographer(
            "pg-sofia", "Sofia Rossi", "sofiarossi",
            "Food, still life, and abstract studies in colour.",
            avatar("sofiarossi"), "Bologna, Italy", 154,
        ),
        Photographer(
            "pg-kenji", "Kenji Tanaka", "kenjitanaka",
            "Minimal architecture and quiet technology close-ups.",
            avatar("kenjitanaka"), "Osaka, Japan", 331,
        ),
        Photographer(
            "pg-ava", "Ava Thompson", "avathompson",
            "Animals and still nature scenes from the Pacific Northwest.",
            avatar("avathompson"), "Vancouver, Canada", 222,
        ),
    )

    val photos: List<Photo> = listOf(
        // Nature
        photo("ph-01", "Morning Fog Over Pines", "Soft fog drifting through evergreen pines at first light.", "pg-ava", "Nature", "green", listOf("forest", "fog", "trees", "calm"), Orientation.PORTRAIT),
        photo("ph-02", "Golden Hour Meadow", "Tall grass glowing in the last warm light of the day.", "pg-diego", "Nature", "warm", listOf("meadow", "golden hour", "grass", "sunset"), Orientation.LANDSCAPE),
        photo("ph-03", "Alpine Lake Mirror", "A still mountain lake reflecting the ridgeline above it.", "pg-diego", "Nature", "blue", listOf("lake", "mountains", "reflection", "water"), Orientation.LANDSCAPE),
        photo("ph-04", "Wildflowers in Bloom", "A close cluster of wildflowers after spring rain.", "pg-ava", "Nature", "warm", listOf("flowers", "spring", "bloom", "macro"), Orientation.SQUARE),
        // City
        photo("ph-05", "Neon Crossing", "A busy crosswalk lit by signs and storefront neon.", "pg-maya", "City", "dark", listOf("urban", "neon", "night", "street"), Orientation.PORTRAIT),
        photo("ph-06", "Rainy Downtown", "Reflections on wet asphalt during an evening downpour.", "pg-maya", "City", "dark", listOf("rain", "downtown", "reflection", "moody"), Orientation.LANDSCAPE),
        photo("ph-07", "Rooftop Skyline", "The city skyline seen from a rooftop at blue hour.", "pg-maya", "City", "blue", listOf("skyline", "rooftop", "blue hour", "buildings"), Orientation.LANDSCAPE),
        photo("ph-08", "Night Market Glow", "Lanterns and stalls crowding a narrow night market.", "pg-maya", "City", "warm", listOf("market", "lanterns", "night", "food"), Orientation.PORTRAIT),
        // Travel
        photo("ph-09", "Old Town Stairs", "Worn stone stairs winding up through an old quarter.", "pg-alex", "Travel", "warm", listOf("stairs", "old town", "europe", "architecture"), Orientation.PORTRAIT),
        photo("ph-10", "Coastal Village", "A pastel village stacked above a quiet harbour.", "pg-alex", "Travel", "blue", listOf("village", "coast", "harbour", "houses"), Orientation.LANDSCAPE),
        photo("ph-11", "Desert Highway", "An empty road cutting straight through red desert.", "pg-alex", "Travel", "orange", listOf("desert", "road", "horizon", "roadtrip"), Orientation.LANDSCAPE),
        photo("ph-12", "Tiled Courtyard", "Intricate patterned tiles in a shaded courtyard.", "pg-alex", "Travel", "teal", listOf("tiles", "pattern", "courtyard", "culture"), Orientation.SQUARE),
        // Architecture
        photo("ph-13", "Concrete Curves", "Sweeping curves of a modern concrete facade.", "pg-kenji", "Architecture", "monochrome", listOf("concrete", "curves", "modern", "minimal"), Orientation.PORTRAIT),
        photo("ph-14", "Glass Atrium", "Light pouring through a vast glass-roofed atrium.", "pg-kenji", "Architecture", "white", listOf("glass", "atrium", "light", "interior"), Orientation.LANDSCAPE),
        photo("ph-15", "Spiral Staircase", "A spiral staircase viewed straight down its centre.", "pg-kenji", "Architecture", "neutral", listOf("staircase", "spiral", "geometry", "symmetry"), Orientation.SQUARE),
        photo("ph-16", "Brutalist Facade", "Repeating concrete forms of a brutalist building.", "pg-kenji", "Architecture", "monochrome", listOf("brutalism", "facade", "pattern", "concrete"), Orientation.PORTRAIT),
        // Animals
        photo("ph-17", "Red Fox in Snow", "A red fox pausing alert in fresh snow.", "pg-ava", "Animals", "white", listOf("fox", "snow", "wildlife", "winter"), Orientation.LANDSCAPE),
        photo("ph-18", "Heron at Dawn", "A heron standing still in shallow dawn water.", "pg-ava", "Animals", "blue", listOf("heron", "bird", "water", "dawn"), Orientation.PORTRAIT),
        photo("ph-19", "Guanaco Herd", "A small herd of guanaco grazing on open steppe.", "pg-diego", "Animals", "warm", listOf("guanaco", "herd", "patagonia", "wildlife"), Orientation.LANDSCAPE),
        photo("ph-20", "Curious Deer", "A young deer looking back from the forest edge.", "pg-ava", "Animals", "green", listOf("deer", "forest", "wildlife", "calm"), Orientation.SQUARE),
        // Food
        photo("ph-21", "Fresh Pasta Board", "Hand-cut pasta resting on a floured wooden board.", "pg-sofia", "Food", "warm", listOf("pasta", "cooking", "italian", "handmade"), Orientation.LANDSCAPE),
        photo("ph-22", "Citrus Still Life", "Halved citrus arranged against a bright backdrop.", "pg-sofia", "Food", "orange", listOf("citrus", "fruit", "still life", "colour"), Orientation.SQUARE),
        photo("ph-23", "Morning Espresso", "A single espresso on a dark cafe counter.", "pg-sofia", "Food", "dark", listOf("coffee", "espresso", "cafe", "morning"), Orientation.PORTRAIT),
        photo("ph-24", "Market Vegetables", "Crates of fresh vegetables at a farmers market.", "pg-sofia", "Food", "green", listOf("vegetables", "market", "fresh", "produce"), Orientation.LANDSCAPE),
        // Technology
        photo("ph-25", "Circuit Macro", "An extreme close-up of a circuit board's traces.", "pg-kenji", "Technology", "blue", listOf("circuit", "macro", "electronics", "detail"), Orientation.LANDSCAPE),
        photo("ph-26", "Workspace Setup", "A clean desk with a laptop and notebook in daylight.", "pg-kenji", "Technology", "neutral", listOf("desk", "workspace", "laptop", "minimal"), Orientation.LANDSCAPE),
        photo("ph-27", "Server Lights", "Rows of status lights glowing in a dark server room.", "pg-kenji", "Technology", "dark", listOf("server", "lights", "data", "network"), Orientation.PORTRAIT),
        photo("ph-28", "Drone Over Fields", "A drone hovering above a patchwork of green fields.", "pg-kenji", "Technology", "green", listOf("drone", "aerial", "fields", "flight"), Orientation.LANDSCAPE),
        // People
        photo("ph-29", "Street Portrait", "A candid portrait caught in warm afternoon light.", "pg-nora", "People", "warm", listOf("portrait", "candid", "street", "people"), Orientation.PORTRAIT),
        photo("ph-30", "Laughter in Light", "Two friends laughing in a sunlit doorway.", "pg-nora", "People", "warm", listOf("friends", "joy", "candid", "light"), Orientation.SQUARE),
        photo("ph-31", "Quiet Reading", "Someone reading by a window on a grey afternoon.", "pg-nora", "People", "neutral", listOf("reading", "quiet", "indoors", "calm"), Orientation.PORTRAIT),
        photo("ph-32", "Festival Colours", "Dancers mid-step in bright festival dress.", "pg-nora", "People", "orange", listOf("festival", "dance", "colour", "culture"), Orientation.LANDSCAPE),
        // Abstract
        photo("ph-33", "Colour Field Study", "Soft blocks of colour blending into one another.", "pg-sofia", "Abstract", "teal", listOf("colour", "abstract", "study", "gradient"), Orientation.SQUARE),
        photo("ph-34", "Light and Shadow", "Hard diagonal shadows across a plain wall.", "pg-maya", "Abstract", "monochrome", listOf("shadow", "light", "lines", "minimal"), Orientation.PORTRAIT),
        photo("ph-35", "Paint in Motion", "Streaks of red paint frozen mid-swirl.", "pg-sofia", "Abstract", "red", listOf("paint", "motion", "texture", "colour"), Orientation.LANDSCAPE),
        photo("ph-36", "Geometric Calm", "A balanced arrangement of simple geometric shapes.", "pg-sofia", "Abstract", "blue", listOf("geometry", "shapes", "balance", "calm"), Orientation.SQUARE),
        // Ocean
        photo("ph-37", "Atlantic Swell", "A long ocean swell rolling under an open sky.", "pg-liam", "Ocean", "blue", listOf("ocean", "waves", "swell", "sea"), Orientation.LANDSCAPE),
        photo("ph-38", "Tide Pool Detail", "Anemones and stones in a clear coastal tide pool.", "pg-liam", "Ocean", "teal", listOf("tide pool", "coast", "detail", "marine"), Orientation.SQUARE),
        photo("ph-39", "Lighthouse at Dusk", "A lighthouse beam cutting through deep blue dusk.", "pg-liam", "Ocean", "dark", listOf("lighthouse", "dusk", "coast", "moody"), Orientation.PORTRAIT),
        photo("ph-40", "Surf and Mist", "Spray and mist hanging over breaking surf.", "pg-liam", "Ocean", "white", listOf("surf", "mist", "waves", "spray"), Orientation.LANDSCAPE),
    )

    val topics: List<Topic> = listOf(
        topicOf("Nature", "Forests, mountains, and quiet wild places."),
        topicOf("City", "Streets, skylines, and life after dark."),
        topicOf("Travel", "Old towns, far roads, and places worth the trip."),
        topicOf("Architecture", "Form, light, and structure by design."),
        topicOf("Animals", "Wildlife and creatures, close and far."),
        topicOf("Food", "Cooking, produce, and still life on the table."),
        topicOf("Technology", "Hardware, workspaces, and the built digital world."),
        topicOf("People", "Portraits and candid moments of everyday life."),
        topicOf("Abstract", "Colour, shape, and texture without a subject."),
        topicOf("Ocean", "Coastlines, waves, and the moods of the sea."),
    )

    /** A curated subset shown in the Home "Featured" rail. */
    val featuredPhotos: List<Photo> = photos.filterIndexed { index, _ -> index % 4 == 0 }

    /** The remaining photos, shown in the Home "Trending now" grid. */
    val trendingPhotos: List<Photo> = photos.filterIndexed { index, _ -> index % 4 != 0 }

    /** All colour labels in the catalogue — used to build the search colour filter. */
    val colors: List<String> = photos.map { it.dominantColor }.distinct().sorted()

    private fun avatar(username: String) =
        "https://picsum.photos/seed/splashr-pg-$username/300/300"

    private fun photo(
        id: String,
        title: String,
        description: String,
        photographerId: String,
        topic: String,
        color: String,
        tags: List<String>,
        orientation: Orientation,
    ): Photo {
        val (w, h) = when (orientation) {
            Orientation.LANDSCAPE -> 1200 to 800
            Orientation.PORTRAIT -> 800 to 1200
            else -> 1080 to 1080
        }
        val photographer = photographers.first { it.id == photographerId }
        return Photo(
            id = id,
            title = title,
            description = description,
            imageUrl = "https://picsum.photos/seed/splashr-$id/$w/$h",
            photographerId = photographer.id,
            photographerName = photographer.name,
            tags = tags,
            topic = topic,
            width = w,
            height = h,
            dominantColor = color,
        )
    }

    private fun topicOf(name: String, description: String): Topic = Topic(
        id = name.lowercase(),
        title = name,
        description = description,
        coverImageUrl = "https://picsum.photos/seed/splashr-topic-${name.lowercase()}/600/400",
        photoCount = photos.count { it.topic == name },
    )
}
