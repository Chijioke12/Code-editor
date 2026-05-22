package com.example

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.NoteAdd
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextButton
import androidx.compose.material3.Button
import androidx.compose.foundation.lazy.LazyColumn
import androidx.documentfile.provider.DocumentFile
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.LaunchedEffect
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.rgb(43, 43, 43))
        )
        setContent {
            MyApplicationTheme {
                val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
                val scope = rememberCoroutineScope()
                
                var workspaceUri by remember { mutableStateOf<Uri?>(null) }
                var workspaceRoot by remember { mutableStateOf<DocumentFile?>(null) }
                var currentDirectory by remember { mutableStateOf<DocumentFile?>(null) }
                var workspaceFiles by remember { mutableStateOf<List<DocumentFile>>(emptyList()) }
                
                var showNewFileDialog by remember { mutableStateOf(false) }
                var newFileName by remember { mutableStateOf("") }
                var showNewFolderDialog by remember { mutableStateOf(false) }
                var newFolderName by remember { mutableStateOf("") }
                var showHtmlPreview by remember { mutableStateOf(false) }

                var codeText by remember { mutableStateOf(TextFieldValue(initialCode)) }
                var currentUriFile by remember { mutableStateOf<Uri?>(null) }
                var currentFileName by remember { mutableStateOf<String?>(null) }
                val context = LocalContext.current
                
                // Prompt user to open folder on startup
                LaunchedEffect(Unit) {
                    if (workspaceUri == null) {
                        scope.launch {
                            drawerState.open()
                        }
                    }
                }
                
                val undoStack = remember { mutableStateListOf<TextFieldValue>() }
                val redoStack = remember { mutableStateListOf<TextFieldValue>() }

                LaunchedEffect(currentDirectory) {
                    if (currentDirectory != null) {
                        currentDirectory?.let { dir ->
                            workspaceFiles = withContext(Dispatchers.IO) {
                                dir.listFiles().sortedBy { !it.isDirectory }.toList()
                            }
                        }
                    } else {
                        workspaceFiles = emptyList()
                    }
                }
                
                val refreshFiles = {
                    scope.launch {
                        currentDirectory?.let { dir ->
                            workspaceFiles = withContext(Dispatchers.IO) {
                                dir.listFiles().sortedBy { !it.isDirectory }.toList()
                            }
                        }
                    }
                }

                val openTreeLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.OpenDocumentTree()
                ) { uri: Uri? ->
                    uri?.let {
                        context.contentResolver.takePersistableUriPermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                        workspaceUri = it
                        val root = DocumentFile.fromTreeUri(context, it)
                        workspaceRoot = root
                        currentDirectory = root
                    }
                }

                val openDocumentLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.OpenDocument()
                ) { uri: Uri? ->
                    uri?.let {
                        currentUriFile = it
                        currentFileName = DocumentFile.fromSingleUri(context, it)?.name
                        try {
                            context.contentResolver.openInputStream(it)?.use { inputStream ->
                                val text = inputStream.bufferedReader().use { reader -> reader.readText() }
                                codeText = TextFieldValue(text)
                                undoStack.clear()
                                redoStack.clear()
                            }
                        } catch (e: Exception) {
                            Toast.makeText(context, "Error opening file", Toast.LENGTH_SHORT).show()
                        }
                    }
                }

                val createDocumentLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.CreateDocument("*/*")
                ) { uri: Uri? ->
                    uri?.let {
                        currentUriFile = it
                        currentFileName = DocumentFile.fromSingleUri(context, it)?.name
                        try {
                            context.contentResolver.openOutputStream(it)?.use { outputStream ->
                                outputStream.bufferedWriter().use { writer ->
                                    writer.write(codeText.text)
                                }
                            }
                            Toast.makeText(context, "File saved", Toast.LENGTH_SHORT).show()
                        } catch (e: Exception) {
                            Toast.makeText(context, "Error saving file", Toast.LENGTH_SHORT).show()
                        }
                    }
                }

                val onOpenClick = { openDocumentLauncher.launch(arrayOf("*/*")) }
                val onSaveClick = {
                    if (currentUriFile != null) {
                        try {
                            context.contentResolver.openOutputStream(currentUriFile!!)?.use { outputStream ->
                                outputStream.bufferedWriter().use { writer ->
                                    writer.write(codeText.text)
                                }
                            }
                            Toast.makeText(context, "File saved", Toast.LENGTH_SHORT).show()
                        } catch (e: Exception) {
                            createDocumentLauncher.launch(currentFileName ?: "code.txt")
                        }
                    } else {
                        createDocumentLauncher.launch("code.txt")
                    }
                }

                val onUndoClick = {
                    if (undoStack.isNotEmpty()) {
                        val prevState = undoStack.removeLast()
                        redoStack.add(codeText)
                        codeText = prevState
                    }
                }

                val onRedoClick = {
                    if (redoStack.isNotEmpty()) {
                        val nextState = redoStack.removeLast()
                        undoStack.add(codeText)
                        codeText = nextState
                    }
                }
                
                val onLeftClick = {
                    val selection = codeText.selection
                    if (selection.start > 0) {
                        val newPos = selection.start - 1
                        codeText = codeText.copy(selection = TextRange(newPos, newPos))
                    }
                }
                
                val onRightClick = {
                    val selection = codeText.selection
                    if (selection.end < codeText.text.length) {
                        val newPos = selection.end + 1
                        codeText = codeText.copy(selection = TextRange(newPos, newPos))
                    }
                }

                val keywords = listOf(
                    // Kotlin / Java
                    "package", "import", "class", "interface", "fun", "val", "var", 
                    "if", "else", "while", "for", "return", "true", "false", "null", 
                    "is", "in", "object", "companion", "override", "public", "private", 
                    "protected", "internal", "String", "Int", "Boolean", "println",
                    // JavaScript / TypeScript
                    "function", "const", "let", "await", "async", "typeof", "export", 
                    "default", "try", "catch", "finally", "document", "window",
                    // Web / HTML / CSS
                    "div", "span", "html", "body", "head", "script", "style", "link", 
                    "meta", "button", "input", "color", "background", "margin", "padding", 
                    "display", "flex", "grid", "border", "width", "height",
                    // Python
                    "def", "elif", "print", "from"
                )
                
                val currentWord = remember(codeText) {
                    val text = codeText.text
                    val cursorPosition = codeText.selection.end
                    if (cursorPosition >= 0 && cursorPosition <= text.length) {
                        val textBeforeCursor = text.substring(0, cursorPosition)
                        val words = textBeforeCursor.split(Regex("[^a-zA-Z0-9_]"))
                        words.lastOrNull()?.takeIf { it.isNotEmpty() }
                    } else null
                }
                
                val suggestions = remember(currentWord) {
                    if (currentWord != null && currentWord.length >= 1) {
                        keywords.filter { it.startsWith(currentWord, ignoreCase = true) && it != currentWord }.take(10)
                    } else emptyList()
                }

                if (showNewFileDialog) {
                    AlertDialog(
                        onDismissRequest = { showNewFileDialog = false },
                        title = { Text("New File") },
                        text = {
                            OutlinedTextField(
                                value = newFileName,
                                onValueChange = { newFileName = it },
                                label = { Text("File name") },
                                singleLine = true
                            )
                        },
                        confirmButton = {
                            TextButton(onClick = {
                                if (newFileName.isNotBlank()) {
                                    scope.launch {
                                        withContext(Dispatchers.IO) {
                                            currentDirectory?.createFile("*/*", newFileName)
                                        }
                                        showNewFileDialog = false
                                        newFileName = ""
                                        refreshFiles()
                                    }
                                }
                            }) { Text("Create") }
                        },
                        dismissButton = {
                            TextButton(onClick = { showNewFileDialog = false }) { Text("Cancel") }
                        }
                    )
                }

                if (showNewFolderDialog) {
                    AlertDialog(
                        onDismissRequest = { showNewFolderDialog = false },
                        title = { Text("New Folder") },
                        text = {
                            OutlinedTextField(
                                value = newFolderName,
                                onValueChange = { newFolderName = it },
                                label = { Text("Folder name") },
                                singleLine = true
                            )
                        },
                        confirmButton = {
                            TextButton(onClick = {
                                if (newFolderName.isNotBlank()) {
                                    scope.launch {
                                        withContext(Dispatchers.IO) {
                                            currentDirectory?.createDirectory(newFolderName)
                                        }
                                        showNewFolderDialog = false
                                        newFolderName = ""
                                        refreshFiles()
                                    }
                                }
                            }) { Text("Create") }
                        },
                        dismissButton = {
                            TextButton(onClick = { showNewFolderDialog = false }) { Text("Cancel") }
                        }
                    )
                }

                ModalNavigationDrawer(
                    drawerState = drawerState,
                    drawerContent = {
                        ModalDrawerSheet(
                            modifier = Modifier.width(300.dp),
                            drawerContainerColor = Color(0xFF2B2B2B),
                            drawerContentColor = Color(0xFFA9B7C6)
                        ) {
                            Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                                Text("Workspace", style = MaterialTheme.typography.titleMedium, color = Color.White)
                                Spacer(modifier = Modifier.height(16.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Button(onClick = { openTreeLauncher.launch(null) }) {
                                        Text("Open Folder")
                                    }
                                    if (currentDirectory != null) {
                                        Row {
                                            IconButton(onClick = { showNewFileDialog = true }) {
                                                Icon(Icons.Default.NoteAdd, contentDescription = "New File", tint = Color.White)
                                            }
                                            IconButton(onClick = { showNewFolderDialog = true }) {
                                                Icon(Icons.Default.CreateNewFolder, contentDescription = "New Folder", tint = Color.White)
                                            }
                                        }
                                    }
                                }
                                
                                Spacer(modifier = Modifier.height(16.dp))
                                
                                if (currentDirectory != null && workspaceRoot != null) {
                                    if (currentDirectory?.uri != workspaceRoot?.uri) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth().clickable {
                                                currentDirectory = currentDirectory?.parentFile
                                            }.padding(vertical = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color(0xFFA9B7C6))
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("..", color = Color(0xFFA9B7C6))
                                        }
                                    }
                                    
                                    LazyColumn {
                                        items(workspaceFiles) { file ->
                                            Row(
                                                modifier = Modifier.fillMaxWidth().clickable {
                                                    if (file.isDirectory) {
                                                        currentDirectory = file
                                                    } else {
                                                        try {
                                                            context.contentResolver.openInputStream(file.uri)?.use { inputStream ->
                                                                val text = inputStream.bufferedReader().use { reader -> reader.readText() }
                                                                codeText = TextFieldValue(text)
                                                                currentUriFile = file.uri
                                                                currentFileName = file.name
                                                                undoStack.clear()
                                                                redoStack.clear()
                                                            }
                                                            scope.launch { drawerState.close() }
                                                        } catch (e: Exception) {
                                                            Toast.makeText(context, "Error opening file", Toast.LENGTH_SHORT).show()
                                                        }
                                                    }
                                                }.padding(vertical = 8.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    imageVector = if (file.isDirectory) Icons.Default.Folder else Icons.Default.InsertDriveFile,
                                                    contentDescription = "File Icon",
                                                    tint = if (file.isDirectory) Color(0xFF6A8759) else Color(0xFFA9B7C6)
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(file.name ?: "Unknown", color = Color(0xFFA9B7C6))
                                            }
                                        }
                                    }
                                } else {
                                    Text("No workspace selected.", color = Color(0xFF606366))
                                }
                            }
                        }
                    }
                ) {
                    Scaffold(
                        modifier = Modifier.fillMaxSize().imePadding(),
                        topBar = { 
                            CodeEditorTopBar(
                                title = currentFileName ?: "Code Editor",
                                onMenuClick = { scope.launch { drawerState.open() } },
                                onOpenClick = onOpenClick, 
                                onSaveClick = onSaveClick
                            ) 
                        },
                        bottomBar = {
                        Column {
                            if (suggestions.isNotEmpty()) {
                                LazyRow(
                                    modifier = Modifier.fillMaxWidth().background(Color(0xFF313335)).padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp)
                                ) {
                                    items(suggestions) { suggestion ->
                                        Surface(
                                            color = Color(0xFF4C5052),
                                            shape = RoundedCornerShape(4.dp),
                                            modifier = Modifier.clickable {
                                                val text = codeText.text
                                                val cursorPosition = codeText.selection.end
                                                val textBeforeCursor = text.substring(0, cursorPosition)
                                                val words = textBeforeCursor.split(Regex("[^a-zA-Z0-9_]"))
                                                val lastWord = words.lastOrNull() ?: ""
                                                val startReplacementIndex = cursorPosition - lastWord.length
                                                
                                                val newText = text.substring(0, startReplacementIndex) + suggestion + text.substring(codeText.selection.end)
                                                val newCursorPos = startReplacementIndex + suggestion.length
                                                
                                                codeText = TextFieldValue(newText, selection = TextRange(newCursorPos))
                                            }
                                        ) {
                                            Text(
                                                text = suggestion,
                                                color = Color(0xFFA9B7C6),
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                                fontFamily = FontFamily.Monospace,
                                                fontSize = 14.sp
                                            )
                                        }
                                    }
                                }
                            }
                            CodeEditorBottomBar(
                                onFolderClick = onOpenClick,
                                onUndoClick = onUndoClick,
                                onRedoClick = onRedoClick,
                                onSearchClick = { Toast.makeText(context, "Search functionality to be added", Toast.LENGTH_SHORT).show() },
                                onLeftClick = onLeftClick,
                                onRightClick = onRightClick,
                                onSaveClick = onSaveClick,
                                onPlayClick = { showHtmlPreview = !showHtmlPreview },
                                onCloseClick = { 
                                    currentUriFile = null
                                    currentFileName = null
                                    codeText = TextFieldValue("")
                                    undoStack.clear()
                                    redoStack.clear()
                                }
                            )
                        }
                    }
                ) { innerPadding ->
                    Box(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
                        if (showHtmlPreview) {
                            AndroidView(
                                factory = { ctx ->
                                    WebView(ctx).apply {
                                        settings.javaScriptEnabled = true
                                        webViewClient = WebViewClient()
                                    }
                                },
                                update = { webView ->
                                    webView.loadDataWithBaseURL(null, codeText.text, "text/html", "UTF-8", null)
                                },
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            CodeEditorUI(
                                codeText = codeText,
                                onCodeChange = { newValue -> 
                                    if (newValue.text != codeText.text) {
                                        undoStack.add(codeText)
                                        if (undoStack.size > 100) undoStack.removeFirst()
                                        redoStack.clear()
                                    }
                                    codeText = newValue
                                }
                            )
                        }
                    }
                }
                } // End ModalNavigationDrawer
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CodeEditorTopBar(title: String, onMenuClick: () -> Unit, onOpenClick: () -> Unit, onSaveClick: () -> Unit) {
    TopAppBar(
        title = { Text(title, fontFamily = FontFamily.Monospace) },
        navigationIcon = {
            IconButton(onClick = onMenuClick) {
                Icon(Icons.Default.Menu, contentDescription = "Menu", tint = Color(0xFFA9B7C6))
            }
        },
        actions = {
            IconButton(onClick = onOpenClick) {
                Icon(Icons.Default.FolderOpen, contentDescription = "Open File", tint = Color(0xFFA9B7C6))
            }
            IconButton(onClick = onSaveClick) {
                Icon(Icons.Default.Save, contentDescription = "Save File", tint = Color(0xFFA9B7C6))
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color(0xFF2B2B2B),
            titleContentColor = Color(0xFFA9B7C6)
        )
    )
}

@Composable
fun CodeEditorBottomBar(
    onFolderClick: () -> Unit,
    onUndoClick: () -> Unit,
    onRedoClick: () -> Unit,
    onSearchClick: () -> Unit,
    onLeftClick: () -> Unit,
    onRightClick: () -> Unit,
    onSaveClick: () -> Unit,
    onPlayClick: () -> Unit,
    onCloseClick: () -> Unit
) {
    BottomAppBar(
        containerColor = Color(0xFF2B2B2B),
        contentColor = Color(0xFFA9B7C6)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            IconButton(onClick = onFolderClick) {
                Icon(Icons.Default.FolderOpen, contentDescription = "Folder")
            }
            IconButton(onClick = onUndoClick) {
                Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = "Undo")
            }
            IconButton(onClick = onRedoClick) {
                Icon(Icons.AutoMirrored.Filled.Redo, contentDescription = "Redo")
            }
            IconButton(onClick = onSearchClick) {
                Icon(Icons.Default.Search, contentDescription = "Search")
            }
            IconButton(onClick = onLeftClick) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Left Arrow")
            }
            IconButton(onClick = onRightClick) {
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Right Arrow")
            }
            IconButton(onClick = onSaveClick) {
                Icon(Icons.Default.Save, contentDescription = "Save")
            }
            IconButton(onClick = onPlayClick) {
                Icon(Icons.Default.PlayArrow, contentDescription = "Play")
            }
            IconButton(onClick = onCloseClick) {
                Icon(Icons.Default.Close, contentDescription = "Close")
            }
        }
    }
}

@Composable
fun CodeEditorUI(codeText: TextFieldValue, onCodeChange: (TextFieldValue) -> Unit) {
    
    val verticalScrollState = rememberScrollState()
    val horizontalScrollState = rememberScrollState()
    
    val textStyle = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontSize = 14.sp,
        color = Color(0xFFA9B7C6),
        lineHeight = 20.sp
    )
    
    val linesCount = codeText.text.count { it == '\n' } + 1
    val lineNumbers = (1..linesCount).joinToString("\n")
    
    Surface(
        color = Color(0xFF2B2B2B),
        contentColor = Color(0xFFA9B7C6),
        modifier = Modifier.fillMaxSize()
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                // Synchronize vertical scrolling
                .verticalScroll(verticalScrollState)
        ) {
            // Line numbers column
            Text(
                text = lineNumbers,
                style = textStyle.copy(
                    color = Color(0xFF606366),
                    textAlign = TextAlign.End
                ),
                modifier = Modifier
                    .background(Color(0xFF313335))
                    .padding(vertical = 16.dp, horizontal = 8.dp)
                    .width(32.dp)
            )
            
            // Code Editor core
            BasicTextField(
                value = codeText,
                onValueChange = onCodeChange,
                textStyle = textStyle,
                visualTransformation = CodeVisualTransformation(),
                cursorBrush = SolidColor(Color(0xFFA9B7C6)),
                modifier = Modifier
                    .fillMaxWidth()
                    // Allow horizontal scrolling for code lines that exceed screen width
                    .horizontalScroll(horizontalScrollState)
                    .padding(16.dp)
            )
        }
    }
}

val initialCode = """
package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity

// A simple Android code editor
class MainActivity : ComponentActivity() {
    
    val defaultTarget = 34
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        println("Hello, Android SDK 34!")
        
        for (i in 1..10) {
            if (i % 2 == 0) {
                // Even number
            } else {
                /* Odd number */
            }
        }
    }
}
""".trimIndent()
