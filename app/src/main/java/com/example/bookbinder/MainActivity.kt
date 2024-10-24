package com.example.bookbinder

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import kotlinx.coroutines.*
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
// Slide 47:
interface GoogleBooksApi {
    @GET("volumes")
    suspend fun searchBooks(
        @Query("q") query: String,
        @Query("key") apiKey: String
    ): BookResponse
}

// Slide 50
object RetrofitInstance {
    private const val BASE_URL = "https://www.googleapis.com/books/v1/"
    const val API_KEY = "AIzaSyDX7T73Ry6CT75yla-oClTdDruDkEbeFn0"

    val api: GoogleBooksApi = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(OkHttpClient.Builder().build())
        .addConverterFactory(MoshiConverterFactory.create())
        .build()
        .create(GoogleBooksApi::class.java)
}

//Slide 56:
data class BookResponse(val items: List<BookItem>)
data class BookItem(val id: String, val volumeInfo: VolumeInfo)
data class VolumeInfo(
    val title: String,
    val authors: List<String>?,
    val imageLinks: ImageLinks?,
    val description: String?
)
data class ImageLinks(val smallThumbnail: String?)

//Slide 42:
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            BookSearchApp()
        }
    }
}

@Composable
fun BookSearchApp() {
    // Slide 65
    var query by remember { mutableStateOf("") }
    var books by remember { mutableStateOf<List<BookItem>>(emptyList()) }

    Column(Modifier.padding(16.dp)) {
        Spacer(modifier = Modifier.height(8.dp))
        TextField(
            value = query,
            onValueChange = { query = it },
            label = { Text("Search Books") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        Button(
            onClick = {
                if (query.isNotBlank()) {
                    searchBooks(query) { books = it }
                } else {
                    println("Query is blank. Please enter a valid search term.")
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Search")
        }
        Spacer(modifier = Modifier.height(8.dp))
        BookList(books)
    }
}

fun searchBooks(query: String, onResult: (List<BookItem>) -> Unit) {
    // Slide 25
    CoroutineScope(Dispatchers.IO).launch {
        try {
            println("Searching for books with query: $query")
            val response = RetrofitInstance.api.searchBooks(query, RetrofitInstance.API_KEY)
            println("Received response: ${response.items.size} items")
            withContext(Dispatchers.Main) {
                onResult(response.items)
            }
        } catch (e: Exception) {
            println("Error fetching books: ${e.message}")
            e.printStackTrace()
        }
    }
}

@Composable
fun BookList(books: List<BookItem>) {
    // Slide 42:
    LazyColumn {
        items(books) { book ->
            BookItemView(book)
        }
    }
}

@Composable
fun BookItemView(book: BookItem) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { }
            .padding(8.dp)
    ) {
        book.volumeInfo.imageLinks?.smallThumbnail?.let { url ->
            Image(
                painter = rememberAsyncImagePainter(url),
                contentDescription = "Book Cover",
                modifier = Modifier.size(80.dp)
            )
        }
        Spacer(Modifier.width(8.dp))
        Column {
            Text(book.volumeInfo.title, style = MaterialTheme.typography.titleLarge)
            Text(book.volumeInfo.authors?.joinToString(", ") ?: "Unknown Author")
        }
    }
}
