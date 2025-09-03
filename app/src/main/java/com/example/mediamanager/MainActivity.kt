package com.example.mediamanager

import android.Manifest
import android.app.Application
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayCircleOutline
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.MediaItem as Media3MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import androidx.navigation.NavController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import coil.compose.AsyncImage
import kotlinx.coroutines.launch
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

// --- Theme Definition ---
private val DarkColorPalette = darkColorScheme(
    primary = Color(0xFF03DAC5),
    background = Color(0xFF121212),
    surface = Color(0xFF1E1E1E),
    onPrimary = Color(0xFF000000),
    onBackground = Color(0xFFE0E0E0),
    onSurface = Color(0xFFE0E0E0)
)

@Composable
fun MediaManagerTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorPalette,
        typography = Typography(),
        content = content
    )
}


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MediaManagerTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavigation()
                }
            }
        }
    }
}

// --- Navigation ---
@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val context = LocalContext.current
    val factory = MediaViewModelFactory(context.applicationContext as Application)
    val viewModel: MediaViewModel = viewModel(factory = factory)

    NavHost(navController = navController, startDestination = "permission") {
        composable("permission") {
            PermissionHandler(onPermissionsGranted = {
                navController.navigate("main") { popUpTo("permission") { inclusive = true } }
            })
        }
        composable("main") {
            MainScreen(navController = navController, viewModel = viewModel)
        }
        composable(
            "detail/{mediaType}/{mediaUri}",
            arguments = listOf(
                navArgument("mediaType") { type = NavType.StringType },
                navArgument("mediaUri") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val mediaUri = backStackEntry.arguments?.getString("mediaUri")
            val mediaTypeString = backStackEntry.arguments?.getString("mediaType")
            val decodedUri = Uri.parse(mediaUri)
            val mediaType = MediaType.valueOf(mediaTypeString!!)
            DetailScreen(
                uri = decodedUri,
                type = mediaType,
                navController = navController,
                viewModel = viewModel
            )
        }
    }
}

// --- Screens ---
@Composable
fun PermissionHandler(onPermissionsGranted: () -> Unit) {
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
        mutableStateOf(permissionsToRequest.all { ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED })
    }

    val permissionsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissionsMap ->
        if (permissionsMap.values.all { it }) {
            onPermissionsGranted()
        }
    }

    if (hasPermissions) {
        onPermissionsGranted()
    } else {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Permissão necessária para acessar todas as mídias.")
            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = { permissionsLauncher.launch(permissionsToRequest) }) {
                Text("Conceder Permissões")
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun MainScreen(navController: NavController, viewModel: MediaViewModel) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val coroutineScope = rememberCoroutineScope()

    val searchQuery by viewModel.searchQuery.collectAsState()
    val sortOrder by viewModel.sortOrder.collectAsState()
    val allTags by viewModel.allTags.collectAsState()
    val selectedTag by viewModel.tagFilter.collectAsState()

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Filtros e Ordenação", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(bottom = 16.dp))

                    TextField(
                        value = searchQuery,
                        onValueChange = { viewModel.onSearchQueryChanged(it) },
                        label = { Text("Buscar por nome...") },
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                    )

                    Text("Ordenar por", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 16.dp, bottom = 8.dp))
                    SortControls(
                        currentSortOrder = sortOrder,
                        onSortOrderChanged = { viewModel.onSortOrderChanged(it) }
                    )

                    Text("Filtrar por Tag", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 16.dp, bottom = 8.dp))
                    TagFilterControls(
                        allTags = allTags,
                        selectedTag = selectedTag,
                        onTagSelected = { viewModel.onTagFilterChanged(it) }
                    )
                }
            }
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Media Manager") },
                    navigationIcon = {
                        IconButton(onClick = { coroutineScope.launch { drawerState.open() } }) {
                            Icon(Icons.Filled.Menu, contentDescription = "Abrir menu")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        titleContentColor = MaterialTheme.colorScheme.onSurface,
                        navigationIconContentColor = MaterialTheme.colorScheme.onSurface
                    )
                )
            }
        ) { paddingValues ->
            Column(modifier = Modifier.padding(paddingValues)) {
                val tabs = listOf("Imagens", "Vídeos", "Áudio")
                val pagerState = rememberPagerState(pageCount = { tabs.size })

                TabRow(selectedTabIndex = pagerState.currentPage) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = pagerState.currentPage == index,
                            onClick = { coroutineScope.launch { pagerState.animateScrollToPage(index) } },
                            text = { Text(title) }
                        )
                    }
                }
                HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
                    when (page) {
                        0 -> {
                            val imageItems by viewModel.imageItems.collectAsState()
                            MediaGrid(items = imageItems, navController = navController)
                        }
                        1 -> {
                            val videoItems by viewModel.videoItems.collectAsState()
                            MediaGrid(items = videoItems, navController = navController)
                        }
                        2 -> {
                            val audioItems by viewModel.audioItems.collectAsState()
                            MediaGrid(items = audioItems, navController = navController)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun DetailScreen(uri: Uri, type: MediaType, navController: NavController, viewModel: MediaViewModel) {
    val context = LocalContext.current
    val imageItems by viewModel.imageItems.collectAsState()
    val videoItems by viewModel.videoItems.collectAsState()
    val audioItems by viewModel.audioItems.collectAsState()
    val allItems = imageItems + videoItems + audioItems
    val item = allItems.find { it.uri == uri }
    var newTag by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(item?.name ?: "Visualizador") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                },
                actions = {
                    var showMenu by remember { mutableStateOf(false) }
                    IconButton(onClick = {
                        viewModel.deleteMediaItem(uri)
                        navController.navigateUp()
                    }) {
                        Icon(Icons.Filled.Delete, contentDescription = "Deletar")
                    }
                    IconButton(onClick = { showMenu = !showMenu }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Mais opções")
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Duplicar") },
                            onClick = {
                                item?.let { viewModel.duplicateMediaItem(it) }
                                showMenu = false
                            }
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues).fillMaxSize()) {
            Box(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                when (type) {
                    MediaType.IMAGE -> AsyncImage(model = uri, contentDescription = "Full screen image", modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
                    MediaType.VIDEO, MediaType.AUDIO -> {
                        val exoPlayer = remember {
                            ExoPlayer.Builder(context).build().apply {
                                val mediaItem = Media3MediaItem.fromUri(uri)
                                setMediaItem(mediaItem)
                                prepare()
                                playWhenReady = true
                            }
                        }
                        DisposableEffect(Unit) {
                            onDispose { exoPlayer.release() }
                        }
                        AndroidView(factory = { PlayerView(it).apply { player = exoPlayer } }, modifier = Modifier.fillMaxSize())
                    }
                }
            }
            if (item != null) {
                Column(modifier = Modifier.padding(16.dp)) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        item.tags.forEach { tag ->
                            AssistChip(onClick = {}, label = { Text(tag) })
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = newTag,
                            onValueChange = { newTag = it },
                            label = { Text("Adicionar tag") },
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(onClick = {
                            if (newTag.isNotBlank()) {
                                viewModel.addTagToMediaItem(item.uri.toString(), newTag.trim())
                                newTag = ""
                            }
                        }) {
                            Text("Add")
                        }
                    }
                }
            }
        }
    }
}


// --- Reusable Components ---
@Composable
fun SortControls(currentSortOrder: SortOrder, onSortOrderChanged: (SortOrder) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        Button(
            onClick = { onSortOrderChanged(SortOrder.BY_DATE_DESC) },
            colors = if (currentSortOrder == SortOrder.BY_DATE_DESC) ButtonDefaults.buttonColors() else ButtonDefaults.outlinedButtonColors()
        ) { Text("Data") }
        Button(
            onClick = { onSortOrderChanged(SortOrder.BY_NAME_ASC) },
            colors = if (currentSortOrder == SortOrder.BY_NAME_ASC) ButtonDefaults.buttonColors() else ButtonDefaults.outlinedButtonColors()
        ) { Text("Nome") }
        Button(
            onClick = { onSortOrderChanged(SortOrder.BY_SIZE_DESC) },
            colors = if (currentSortOrder == SortOrder.BY_SIZE_DESC) ButtonDefaults.buttonColors() else ButtonDefaults.outlinedButtonColors()
        ) { Text("Tamanho") }
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun TagFilterControls(allTags: List<String>, selectedTag: String?, onTagSelected: (String?) -> Unit) {
    FlowRow(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        allTags.forEach { tag ->
            FilterChip(
                selected = tag == selectedTag,
                onClick = {
                    val newSelection = if (tag == selectedTag) null else tag
                    onTagSelected(newSelection)
                },
                label = { Text(tag) }
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MediaGrid(items: List<MediaItem>, navController: NavController) {
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
        items(items, key = { it.uri }) { item ->
            MediaGridItem(
                item = item,
                navController = navController,
                modifier = Modifier.animateItemPlacement()
            )
        }
    }
}

@Composable
fun MediaGridItem(item: MediaItem, navController: NavController, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier
            .aspectRatio(1f)
            .clickable {
                val encodedUri = URLEncoder.encode(item.uri.toString(), StandardCharsets.UTF_8.name())
                navController.navigate("detail/${item.type.name}/$encodedUri")
            }
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
                val imageModifier = Modifier.fillMaxSize()
                when (item.type) {
                    MediaType.IMAGE -> AsyncImage(model = item.uri, contentDescription = item.name, modifier = imageModifier, contentScale = ContentScale.Crop)
                    MediaType.VIDEO -> AsyncImage(model = item.uri, contentDescription = item.name, modifier = imageModifier, contentScale = ContentScale.Crop)
                    MediaType.AUDIO -> Icon(Icons.Default.Audiotrack, contentDescription = "Audio", modifier = Modifier.size(48.dp))
                }
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f)),
                                startY = 200f * 0.7f,
                                endY = 200f
                            )
                        )
                )
                if (item.type == MediaType.VIDEO) {
                    Icon(
                        imageVector = Icons.Filled.PlayCircleOutline,
                        contentDescription = "Play Video",
                        modifier = Modifier.size(48.dp),
                        tint = Color.White.copy(alpha = 0.9f)
                    )
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
