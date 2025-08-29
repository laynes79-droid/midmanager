package com.example.mediamanager

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MediaManagerTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    PermissionHandler()
                }
            }
        }
    }
}

@Composable
fun PermissionHandler() {
    val context = LocalContext.current

    val permissionsToRequest = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        arrayOf(
            Manifest.permission.READ_MEDIA_IMAGES,
            Manifest.permission.READ_MEDIA_VIDEO,
            Manifest.permission.READ_MEDIA_AUDIO
        )
    } else {
        arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
    }

    var hasPermissions by remember {
        mutableStateOf(
            permissionsToRequest.all {
                ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
            }
        )
    }

    val permissionsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissionsMap ->
        hasPermissions = permissionsMap.values.all { it }
    }

    if (hasPermissions) {
        MainScreen()
    } else {
        RequestPermissionScreen(
            onPermissionRequest = { permissionsLauncher.launch(permissionsToRequest) }
        )
    }
}

@Composable
fun RequestPermissionScreen(onPermissionRequest: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Permissão necessária para acessar todas as mídias.")
        Spacer(modifier = Modifier.height(8.dp))
        Button(onClick = onPermissionRequest) {
            Text("Conceder Permissões")
        }
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: MediaViewModel = viewModel()) {
    val context = LocalContext.current
    val imageItems by viewModel.imageItems
    val videoItems by viewModel.videoItems
    val audioItems by viewModel.audioItems
    val searchQuery by viewModel.searchQuery
    val sortOrder by viewModel.sortOrder

    LaunchedEffect(Unit) {
        viewModel.loadMedia(context)
    }

    val tabs = listOf("Imagens", "Vídeos", "Áudio")
    val pagerState = rememberPagerState(pageCount = { tabs.size })
    val coroutineScope = rememberCoroutineScope()

    Column {
        TextField(
            value = searchQuery,
            onValueChange = { viewModel.onSearchQueryChanged(it) },
            label = { Text("Buscar por nome...") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp)
        )

        SortControls(
            currentSortOrder = sortOrder,
            onSortOrderChanged = { viewModel.onSortOrderChanged(it) }
        )

        TabRow(selectedTabIndex = pagerState.currentPage) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = pagerState.currentPage == index,
                    onClick = {
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(index)
                        }
                    },
                    text = { Text(title) }
                )
            }
        }

        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            when (page) {
                0 -> MediaGrid(items = imageItems)
                1 -> MediaGrid(items = videoItems)
                2 -> MediaGrid(items = audioItems)
            }
        }
    }
}

@Composable
fun SortControls(currentSortOrder: SortOrder, onSortOrderChanged: (SortOrder) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        Button(
            onClick = { onSortOrderChanged(SortOrder.BY_DATE_DESC) },
            colors = if (currentSortOrder == SortOrder.BY_DATE_DESC) ButtonDefaults.buttonColors() else ButtonDefaults.outlinedButtonColors()
        ) {
            Text("Data")
        }
        Button(
            onClick = { onSortOrderChanged(SortOrder.BY_NAME_ASC) },
            colors = if (currentSortOrder == SortOrder.BY_NAME_ASC) ButtonDefaults.buttonColors() else ButtonDefaults.outlinedButtonColors()
        ) {
            Text("Nome")
        }
        Button(
            onClick = { onSortOrderChanged(SortOrder.BY_SIZE_DESC) },
            colors = if (currentSortOrder == SortOrder.BY_SIZE_DESC) ButtonDefaults.buttonColors() else ButtonDefaults.outlinedButtonColors()
        ) {
            Text("Tamanho")
        }
    }
}

@Composable
fun MediaGrid(items: List<MediaItem>) {
    if (items.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Nenhum item encontrado.")
        }
        return
    }
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 128.dp),
        contentPadding = PaddingValues(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(items) { item ->
            MediaGridItem(item = item)
        }
    }
}

@Composable
fun MediaGridItem(item: MediaItem) {
    Card(
        modifier = Modifier.aspectRatio(1f)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                when (item.type) {
                    MediaType.IMAGE -> AsyncImage(
                        model = item.uri,
                        contentDescription = item.name,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                    MediaType.VIDEO -> Icon(Icons.Default.Videocam, contentDescription = "Video", modifier = Modifier.size(48.dp))
                    MediaType.AUDIO -> Icon(Icons.Default.Audiotrack, contentDescription = "Audio", modifier = Modifier.size(48.dp))
                }
            }
            Text(
                text = item.name,
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                maxLines = 2,
                modifier = Modifier.padding(8.dp)
            )
        }
    }
}

@Composable
fun MediaManagerTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        content = content
    )
}
