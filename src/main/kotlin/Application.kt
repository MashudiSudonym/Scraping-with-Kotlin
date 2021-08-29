import com.github.doyaaaaaken.kotlincsv.dsl.csvWriter
import it.skrape.core.htmlDocument
import it.skrape.fetcher.BrowserFetcher
import it.skrape.fetcher.extractIt
import it.skrape.fetcher.skrape
import it.skrape.selects.and
import it.skrape.selects.eachText
import it.skrape.selects.html5.a
import it.skrape.selects.html5.div
import it.skrape.selects.html5.p
import it.skrape.selects.html5.span
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

@Serializable
data class MyExtractedData(
    var httpMessage: String = "",
    var userName: String = "",
    var repositoryName: List<String> = emptyList(),
    var theThirdRepositoriesName: String = "",
    var firstThreeHrefs: List<String> = emptyList(),
    var overviewLink: String = "",
    var firstThreeImageSources: List<String> = emptyList(),
    var title: String = "",
    var starCount: String = "",
)

fun main() {
    val extracted = skrape(BrowserFetcher) {
        request {
            url = "https://github.com/skrapeit"
        }

        extractIt<MyExtractedData> { it ->
            it.httpMessage = status { message }
            htmlDocument {
                relaxed = true

                // To query a certain element you can invoke plain CSS-selector queries as string and
                // directly use the picked element inside the lambde function to do your stuff
                // https://docs.skrape.it/docs/dsl/parsing-html#building-css-selectors
                it.userName = ".h-card .p-nickname" { findFirst { text } }
                // You can pass additional css selector query to an element
                // that will be automatically merged to a valid selector.
                val repositories = span(".repo") { findAll { this } }
                // The findAll {} is returning a List<DocElement>,
                // means you can use all of Kotlins collection functions to do stuff with your selected elements
                it.repositoryName = repositories.filter { it.text.contains("skrape") }.eachText
                it.theThirdRepositoriesName = span(".repo") {
                    // On a defined selector, you can invoke an integer as a lambda function
                    // that will act as a shorthand to pick the nth occurrence of an element.
                    2 { text }
                }
                // We are using one of the convenient shorthand functions (eachImage)
                // to get all images (will return a map of alt-text to src) here.
                // Additionally there are more shorthands to parse all links, all hrefs,
                // all source attribute values and more.
                it.firstThreeImageSources = findAll { eachImage.map { image -> image.value } }.take(3)
                // Another shorthand example that is gettingthe values of all href attributes
                // that are contained in the HTML document and stores the first 3 items.
                it.firstThreeHrefs = findAll { eachHref }.take(3)
                // Because of you can just use any valid Koltin code inside the DSL and
                // the eachLink function will find each link as map of text to url you can get a specific
                // href by its text by kotlins usual map access.
                it.overviewLink = findAll { eachLink["Overview"] ?: "not found" }
                // Here we are using some more
                // handy helpers like titleText (shorthand for title { findFirst { text } } )
                // but this time we are operating top level on the parsed HTML document.
                it.title = titleText

                // In the next few lines of code we are doing a complex and nested search for of element.
                // The example will select an equivalent of the following CSS selector:
                // div.pinned-item-list-item p:nth-of-type(2)
                // a.pinned-item-meta.muted-link[href='/skrapeit/skrape.it/stargazers'],
                // but in a much more readable way than using a plain CSS-selector query
                it.starCount = div {
                    // As you will see from here and the next marker you can nest a html element
                    // query to keep complex selections comprehensible and readable.
                    withClass = "pinned-item-list-item"
                    findFirst {
                        // element selection are nesting, so we will search for the second p-tag inside
                        // the parent div.pinned-item-list-item only. This is really powerful behaviour
                        p {
                            findSecond {
                                a {
                                    // You can define properties of an element without
                                    // having deep knowledge about CSS-selector query syntax
                                    // by just setting properties without the foo to keep things simple and readable.
                                    withClass = "pinned-item-meta" and "Link--muted"
                                    withAttribute = "href" to "/skrapeit/skrape.it/stargazers"

                                    findFirst { ownText }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    println(extracted)

    // save to csv
    csvWriter {
        delimiter = '\t'
        lineTerminator = "\n"
    }.open("test.csv") {
        writeRow(
            listOf("HTTP Message", extracted.httpMessage)
        )
        writeRow(
            listOf("User Name", extracted.userName)
        )
        extracted.repositoryName.forEach { data ->
            writeRow(
                listOf(
                    "Repository Name",
                    data,
                )
            )
        }
        writeRow(
            listOf(
                "The Third Repositories Name",
                extracted.theThirdRepositoriesName
            )
        )
        extracted.firstThreeHrefs.forEach { data ->
            writeRow(
                listOf(
                    "First Three Hrefs",
                    data,
                )
            )
        }
        writeRow(listOf("Overview Link", extracted.overviewLink))
        extracted.firstThreeImageSources.forEach { data ->
            writeRow(
                listOf(
                    "First Three Image Sources",
                    data,
                )
            )
        }
        writeRow(
            listOf("Title", extracted.title)
        )
        writeRow(
            listOf("Star Count", extracted.starCount)
        )
    }

    // save to json file
    val format = Json { prettyPrint = true }
    val file = File("test.json")

    file.writeText(format.encodeToString(extracted))
}